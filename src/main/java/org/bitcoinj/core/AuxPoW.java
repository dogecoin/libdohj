/**
 * Copyright 2011 Google Inc.
 * Copyright 2014 Andreas Schildbach
 * Copyright 2015 J. Ross Nicoll
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bitcoinj.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;


/**
 * <p>An AuxPoW header wraps a block header from another coin, enabling the foreign
 * chain's proof of work to be used for this chain as well. <b>Note: </b>
 * NetworkParameters for AuxPoW networks <b>must</b> implement AltcoinNetworkParameters
 * in order for AuxPoW to work.</p>
 */
public class AuxPoW extends ChildMessage implements Serializable {

    public static final byte[] MERGED_MINING_HEADER = new byte[] {
        (byte) 0xfa, (byte) 0xbe, "m".getBytes()[0], "m".getBytes()[0]
    };

    private static final Logger log = LoggerFactory.getLogger(AuxPoW.class);
    private static final long serialVersionUID = -8567546957352643140L;

    private Transaction transaction;
    private Sha256Hash hashBlock;
    private MerkleBranch coinbaseBranch;
    private MerkleBranch chainMerkleBranch;
    private AltcoinBlock parentBlockHeader;

    // Transactions can be encoded in a way that will use more bytes than is optimal
    // (due to VarInts having multiple encodings)
    // MAX_BLOCK_SIZE must be compared to the optimal encoding, not the actual encoding, so when parsing, we keep track
    // of the size of the ideal encoding in addition to the actual message size (which Message needs) so that Blocks
    // can properly keep track of optimal encoded size
    private transient int optimalEncodingMessageSize;

    public AuxPoW(NetworkParameters params, @Nullable Message parent) {
        super(params);
        transaction = new Transaction(params);
        hashBlock = Sha256Hash.ZERO_HASH;
        coinbaseBranch = new MerkleBranch(params, this);
        chainMerkleBranch = new MerkleBranch(params, this);
        parentBlockHeader = null;
    }

    /**
     * Creates an AuxPoW header by reading payload starting from offset bytes in. Length of header is fixed.
     * @param params NetworkParameters object.
     * @param payload Bitcoin protocol formatted byte array containing message content.
     * @param offset The location of the first payload byte within the array.
     * @param parent The message element which contains this header.
     * @param serializer the serializer to use for this message.
     * @throws ProtocolException
     */
    public AuxPoW(NetworkParameters params, byte[] payload, int offset, Message parent, MessageSerializer serializer)
            throws ProtocolException {
        super(params, payload, offset, parent, serializer, Message.UNKNOWN_LENGTH);
    }

    /**
     * Creates an AuxPoW header by reading payload starting from offset bytes in. Length of header is fixed.
     * 
     * @param params NetworkParameters object.
     * @param payload Bitcoin protocol formatted byte array containing message content.
     * @param parent The message element which contains this header.
     * @param serializer the serializer to use for this message.
     */
    public AuxPoW(NetworkParameters params, byte[] payload, @Nullable Message parent, MessageSerializer serializer)
            throws ProtocolException {
        super(params, payload, 0, parent, serializer, Message.UNKNOWN_LENGTH);
    }

    @Override
    protected void parseLite() throws ProtocolException {
		length = calcLength(payload, offset);
		cursor = offset + length;
    }

    protected static int calcLength(byte[] buf, int offset) {
        VarInt varint;
        // jump past transaction
        int cursor = offset + Transaction.calcLength(buf, offset);

        // jump past header hash
        cursor += 4;

        // Coin base branch
        cursor += MerkleBranch.calcLength(buf, offset);

        // Block chain branch
        cursor += MerkleBranch.calcLength(buf, offset);

        // Block header
		cursor += Block.HEADER_SIZE;

        return cursor - offset + 4;
    }

    @Override
    void parse() throws ProtocolException {

        if (parsed)
            return;

        cursor = offset;
        transaction = new Transaction(params, payload, cursor, this, serializer, Message.UNKNOWN_LENGTH);
        cursor += transaction.getOptimalEncodingMessageSize();
        optimalEncodingMessageSize = transaction.getOptimalEncodingMessageSize();        

        hashBlock = readHash();
        optimalEncodingMessageSize += 32; // Add the hash size to the optimal encoding

        coinbaseBranch = new MerkleBranch(params, this, payload, cursor, serializer);
        cursor += coinbaseBranch.getOptimalEncodingMessageSize();
        optimalEncodingMessageSize += coinbaseBranch.getOptimalEncodingMessageSize();

        chainMerkleBranch = new MerkleBranch(params, this, payload, cursor, serializer);
        cursor += chainMerkleBranch.getOptimalEncodingMessageSize();
        optimalEncodingMessageSize += chainMerkleBranch.getOptimalEncodingMessageSize();

        // Make a copy of JUST the contained block header, so the block parser doesn't try reading
        // transactions past the end
        byte[] blockBytes = Arrays.copyOfRange(payload, cursor, cursor + Block.HEADER_SIZE);
        cursor += Block.HEADER_SIZE;
        parentBlockHeader = new AltcoinBlock(params, blockBytes, 0, this, serializer, Block.HEADER_SIZE);

        length = cursor - offset;
    }

    public int getOptimalEncodingMessageSize() {
        if (optimalEncodingMessageSize != 0)
            return optimalEncodingMessageSize;
        maybeParse();
        if (optimalEncodingMessageSize != 0)
            return optimalEncodingMessageSize;
        optimalEncodingMessageSize = getMessageSize();
        return optimalEncodingMessageSize;
    }

    @Override
    public String toString() {
        return toString(null);
    }

    /**
     * A human readable version of the transaction useful for debugging. The format is not guaranteed to be stable.
     * @param chain If provided, will be used to estimate lock times (if set). Can be null.
     */
    public String toString(@Nullable AbstractBlockChain chain) {
		return transaction.toString(chain);
    }

    @Override
    protected void bitcoinSerializeToStream(OutputStream stream) throws IOException {
        transaction.bitcoinSerialize(stream);
        stream.write(Utils.reverseBytes(hashBlock.getBytes()));

        coinbaseBranch.bitcoinSerialize(stream);
        chainMerkleBranch.bitcoinSerialize(stream);

        parentBlockHeader.bitcoinSerializeToStream(stream);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuxPoW input = (AuxPoW) o;
        if (!transaction.equals(input.transaction)) return false;
        if (!hashBlock.equals(input.hashBlock)) return false;
        if (!coinbaseBranch.equals(input.hashBlock)) return false;
        if (!chainMerkleBranch.equals(input.hashBlock)) return false;
        if (!parentBlockHeader.equals(input.hashBlock)) return false;
        return getHash().equals(input.getHash());
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + transaction.hashCode();
        result = 31 * result + hashBlock.hashCode();
        result = 31 * result + coinbaseBranch.hashCode();
        result = 31 * result + chainMerkleBranch.hashCode();
        result = 31 * result + parentBlockHeader.hashCode();
        return result;
    }

    /**
     * Ensure object is fully parsed before invoking java serialization.  The backing byte array
     * is transient so if the object has parseLazy = true and hasn't invoked checkParse yet
     * then data will be lost during serialization.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        maybeParse();
        out.defaultWriteObject();
    }

    /**
     * Get the block header from the parent blockchain. The hash of the header
     * is the value which should match the difficulty target. Note that blocks are
     * not necessarily part of the parent blockchain, they simply must be valid
     * blocks at the difficulty of the child blockchain.
     */
    public AltcoinBlock getParentBlockHeader() {
        return parentBlockHeader;
    }

    /**
     * Get the coinbase transaction from the AuxPoW header. This should contain a
     * reference back to the block hash in its input scripts, to prove that the
     * transaction was created after the block.
     */
    public Transaction getCoinbase() {
        return transaction;
    }

    /**
     * Get the Merkle branch used to connect the AuXPow header with this blockchain.
     */
    public MerkleBranch getChainMerkleBranch() {
        return chainMerkleBranch;
    }

    /**
     * Get the Merkle branch used to connect the coinbase transaction with this blockchain.
     */
    public MerkleBranch getCoinbaseBranch() {
        return coinbaseBranch;
    }

    /**
     * <p>Checks the transaction contents for sanity, in ways that can be done in a standalone manner.
     * Does <b>not</b> perform all checks on a transaction such as whether the inputs are already spent.
     * Specifically this method verifies:</p>
     *
     * <ul>
     *     <li>That there is at least one input and output.</li>
     *     <li>That the serialized size is not larger than the max block size.</li>
     *     <li>That no outputs have negative value.</li>
     *     <li>That the outputs do not sum to larger than the max allowed quantity of coin in the system.</li>
     *     <li>If the tx is a coinbase tx, the coinbase scriptSig size is within range. Otherwise that there are no
     *     coinbase inputs in the tx.</li>
     * </ul>
     *
     * @throws VerificationException
     */
    public void verify() throws VerificationException {
        maybeParse();
        // TODO: Verify the AuxPoW data
    }

    /**
     * Check the proof of work for this AuxPoW header meets the target
     * difficulty.
     *
     * @param hashAuxBlock hash of the block the AuxPoW header is attached to.
     * @param target the difficulty target after decoding from compact bits.
     */
    protected boolean checkProofOfWork(Sha256Hash hashAuxBlock,
        BigInteger target, boolean throwException) throws VerificationException {
        if (0 != this.getCoinbaseBranch().getIndex()) {
            if (throwException) {
                // I don't like the message, but it correlates with what's in the reference client.
                throw new VerificationException("AuxPow is not a generate");
            }
            return false;
        }

        /* if (!TestNet()
            parentBlockHeader.getChainID() == ((AuxPoWNetworkParameters) params).getChainID()) {
            if (throwException) {
                throw new VerificationException("Aux POW parent has our chain ID");
            }
            return false;
        } */

        if (this.getChainMerkleBranch().size() > 30) {
            if (throwException) {
                throw new VerificationException("Aux POW chain merkle branch too long");
            }
            return false;
        }

        // Check that the chain merkle root is in the coinbase
        Sha256Hash nRootHash = getChainMerkleBranch().calculateMerkleRoot(hashAuxBlock);
        final byte[] vchRootHash = nRootHash.getBytes();
        //std::reverse(vchRootHash.begin(), vchRootHash.end()); // correct endian// correct endian

        // Check that we are in the parent block merkle tree
        if (!getCoinbaseBranch().calculateMerkleRoot(getCoinbase().getHash()).equals(parentBlockHeader.getMerkleRoot())) {
            if (throwException) {
                throw new VerificationException("Aux POW merkle root incorrect");
            }
            return false;
        }

        final byte[] script = this.getCoinbase().getInput(0).getScriptBytes();

        // Check that the same work is not submitted twice to our chain.
        //
        int pcHead = -1;
        int pc = -1;

        for (int scriptIdx = 0; scriptIdx < script.length; scriptIdx++) {
            if (arrayMatch(script, scriptIdx, MERGED_MINING_HEADER)) {
                // Enforce only one chain merkle root by checking that a single instance of the merged
                // mining header exists just before.
                if (pcHead >= 0) {
                    if (throwException) {
                        throw new VerificationException("Multiple merged mining headers in coinbase");
                    }
                    return false;
                }
                pcHead = scriptIdx;
            }
            if (arrayMatch(script, scriptIdx, vchRootHash)) {
                pc = scriptIdx;
            }
        }

        if (-1 == pcHead) {
            if (throwException) {
                throw new VerificationException("MergedMiningHeader missing from parent coinbase");
            }
            return false;
        }

        if (-1 == pc) {
            if (throwException) {
                throw new VerificationException("Aux POW missing chain merkle root in parent coinbase");
            }
            return false;
        }

        if (pcHead + MERGED_MINING_HEADER.length != pc) {
            if (throwException) {
                throw new VerificationException("Merged mining header is not just before chain merkle root");
            }
            return false;
        }

        // Ensure we are at a deterministic point in the merkle leaves by hashing
        // a nonce and our chain ID and comparing to the index.
        pc += vchRootHash.length;
        if ((script.length - pc) < 8) {
            if (throwException) {
                throw new VerificationException("Aux POW missing chain merkle tree size and nonce in parent coinbase");
            }
            return false;
        }

        byte[] sizeBytes = Utils.reverseBytes(Arrays.copyOfRange(script, pc, pc + 4));
        int branchSize = ByteBuffer.wrap(sizeBytes).getInt();
        if (branchSize != (1 << getChainMerkleBranch().size())) {
            if (throwException) {
                throw new VerificationException("Aux POW merkle branch size does not match parent coinbase");
            }
            return false;
        }

        byte[] nonceBytes = Utils.reverseBytes(Arrays.copyOfRange(script, pc + 4, pc + 8));
        int nonce = ByteBuffer.wrap(nonceBytes).getInt();

        // Choose a pseudo-random slot in the chain merkle tree
        // but have it be fixed for a size/nonce/chain combination.
        //
        // This prevents the same work from being used twice for the
        // same chain while reducing the chance that two chains clash
        // for the same slot.
        long rand = nonce;
        rand = rand * 1103515245 + 12345;
        rand += ((AltcoinNetworkParameters) params).getChainID();
        rand = rand * 1103515245 + 12345;

        if (getChainMerkleBranch().getIndex() != (rand % branchSize)) {
            if (throwException) {
                throw new VerificationException("Aux POW wrong index");
            }
            return false;
        }

        BigInteger h = ((AltcoinNetworkParameters) params)
            .getBlockDifficultyHash(getParentBlockHeader())
            .toBigInteger();
        if (h.compareTo(target) > 0) {
            // Proof of work check failed!
            if (throwException) {
                throw new VerificationException("Hash is higher than target: " + getParentBlockHeader().getHashAsString() + " vs "
                        + target.toString(16));
            }
            return false;
        }

        return true;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    /**
     * Test whether one array is at a specific offset within the other.
     * 
     * @param script the longer array to test for containing another array.
     * @param offset the offset to start at within the larger array.
     * @param subArray the shorter array to test for presence in the longer array.
     * @return true if the shorter array is present at the offset, false otherwise.
     */
    private boolean arrayMatch(byte[] script, int offset, byte[] subArray) {
        for (int matchIdx = 0; matchIdx + offset < script.length && matchIdx < subArray.length; matchIdx++) {
            if (script[offset + matchIdx] != subArray[matchIdx]) {
                return false;
            }
        }
        return true;
    }
}
