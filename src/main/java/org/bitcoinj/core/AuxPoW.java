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
import java.util.*;


/**
 * <p>An AuxPoW header wraps a block header from another coin, enabling the foreign
 * chain's proof of work to be used for this chain as well.</p>
 */
public class AuxPoW extends ChildMessage implements Serializable {
    
    private static final Logger log = LoggerFactory.getLogger(AuxPoW.class);
    private static final long serialVersionUID = -8567546957352643140L;

	private Transaction transaction;
    private Sha256Hash hashBlock;
    private MerkleBranch coinbaseBranch;
    private MerkleBranch blockchainBranch;
    private Block parentBlockHeader;

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
		blockchainBranch = new MerkleBranch(params, this);
		parentBlockHeader = null;
    }

    /**
     * Creates an AuxPoW header by reading payload starting from offset bytes in. Length of header is fixed.
     * @param params NetworkParameters object.1
     * @param payload Bitcoin protocol formatted byte array containing message content.
     * @param offset The location of the first payload byte within the array.
     * @param parent The message element which contains this header.
     * @param parseLazy Whether to perform a full parse immediately or delay until a read is requested.
     * @param parseRetain Whether to retain the backing byte array for quick reserialization.  
     * If true and the backing byte array is invalidated due to modification of a field then 
     * the cached bytes may be repopulated and retained if the message is serialized again in the future.
     * @throws ProtocolException
     */
    public AuxPoW(NetworkParameters params, byte[] payload, int offset, Message parent, boolean parseLazy, boolean parseRetain)
            throws ProtocolException {
        super(params, payload, offset, parent, parseLazy, parseRetain, Message.UNKNOWN_LENGTH);
    }

    /**
     * Creates an AuxPoW header by reading payload starting from offset bytes in. Length of header is fixed.
     */
    public AuxPoW(NetworkParameters params, byte[] payload, @Nullable Message parent, boolean parseLazy, boolean parseRetain)
            throws ProtocolException {
        super(params, payload, 0, parent, parseLazy, parseRetain, Message.UNKNOWN_LENGTH);
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
        transaction = new Transaction(params, payload, cursor, this, parseLazy, parseRetain, Message.UNKNOWN_LENGTH);
        cursor += transaction.getOptimalEncodingMessageSize();
        optimalEncodingMessageSize = transaction.getOptimalEncodingMessageSize();        

        hashBlock = readHash();
		optimalEncodingMessageSize += 32; // Add the hash size to the optimal encoding

		coinbaseBranch = new MerkleBranch(params, this, payload, cursor, parseLazy, parseRetain);
		cursor += coinbaseBranch.getOptimalEncodingMessageSize();
		optimalEncodingMessageSize += coinbaseBranch.getOptimalEncodingMessageSize();

		blockchainBranch = new MerkleBranch(params, this, payload, cursor, parseLazy, parseRetain);
		cursor += blockchainBranch.getOptimalEncodingMessageSize();
		optimalEncodingMessageSize += blockchainBranch.getOptimalEncodingMessageSize();

        // Make a copy of JUST the contained block header, so the block parser doesn't try reading
        // transactions past the end
        byte[] blockBytes = Arrays.copyOfRange(payload, cursor, cursor + Block.HEADER_SIZE);
        cursor += Block.HEADER_SIZE;
		parentBlockHeader = new AltcoinBlock(params, blockBytes, 0, this, parseLazy, parseRetain, Block.HEADER_SIZE);

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
		blockchainBranch.bitcoinSerialize(stream);

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
		if (!blockchainBranch.equals(input.hashBlock)) return false;
		if (!parentBlockHeader.equals(input.hashBlock)) return false;
        return getHash().equals(input.getHash());
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + transaction.hashCode();
        result = 31 * result + hashBlock.hashCode();
        result = 31 * result + coinbaseBranch.hashCode();
        result = 31 * result + blockchainBranch.hashCode();
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
    public Block getParentBlockHeader() {
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
    public MerkleBranch getBlockchainBranch() {
        return blockchainBranch;
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
}
