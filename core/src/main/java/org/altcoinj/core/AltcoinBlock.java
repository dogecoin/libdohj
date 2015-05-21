/**
 * Copyright 2011 Google Inc.
 * Copyright 2014 Andreas Schildbach
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.bitcoinj.core.Coin.FIFTY_COINS;
import static org.bitcoinj.core.Utils.doubleDigest;
import static org.bitcoinj.core.Utils.doubleDigestTwoBuffers;

/**
 * <p>A block is a group of transactions, and is one of the fundamental data structures of the Bitcoin system.
 * It records a set of {@link Transaction}s together with some data that links it into a place in the global block
 * chain, and proves that a difficult calculation was done over its contents. See
 * <a href="http://www.bitcoin.org/bitcoin.pdf">the Bitcoin technical paper</a> for
 * more detail on blocks. <p/>
 *
 * To get a block, you can either build one from the raw bytes you can get from another implementation, or request one
 * specifically using {@link Peer#getBlock(Sha256Hash)}, or grab one from a downloaded {@link BlockChain}.
 */
public class AltcoinBlock extends org.bitcoinj.core.Block {
	/** Bit used to indicate that a block contains an AuxPoW section, where the network supports AuxPoW */
    public static final int BLOCK_FLAG_AUXPOW = (1 << 8);

    private boolean auxpowParsed = false;
    private boolean auxpowBytesValid = false;

	/** AuxPoW header element, if applicable. */
	@Nullable private AuxPoW auxpow;

    /** Special case constructor, used for the genesis node, cloneAsHeader and unit tests. */
    AltcoinBlock(NetworkParameters params) {
        super(params);
    }

    /** Constructs a block object from the Bitcoin wire format. */
    public AltcoinBlock(NetworkParameters params, byte[] payloadBytes) throws ProtocolException {
        super(params, payloadBytes, 0, false, false, payloadBytes.length);
    }

    /**
     * Contruct a block object from the Bitcoin wire format.
     * @param params NetworkParameters object.
     * @param parseLazy Whether to perform a full parse immediately or delay until a read is requested.
     * @param parseRetain Whether to retain the backing byte array for quick reserialization.  
     * If true and the backing byte array is invalidated due to modification of a field then 
     * the cached bytes may be repopulated and retained if the message is serialized again in the future.
     * @param length The length of message if known.  Usually this is provided when deserializing of the wire
     * as the length will be provided as part of the header.  If unknown then set to Message.UNKNOWN_LENGTH
     * @throws ProtocolException
     */
    public AltcoinBlock(NetworkParameters params, byte[] payloadBytes, boolean parseLazy, boolean parseRetain, int length)
            throws ProtocolException {
        super(params, payloadBytes, 0, parseLazy, parseRetain, length);
    }

    /**
     * Contruct a block object from the Bitcoin wire format. Used in the case of a block
     * contained within another message (i.e. for AuxPoW header).
     *
     * @param params NetworkParameters object.
     * @param payloadBytes Bitcoin protocol formatted byte array containing message content.
     * @param offset The location of the first payload byte within the array.
     * @param parent The message element which contains this block, maybe null for no parent.
     * @param parseLazy Whether to perform a full parse immediately or delay until a read is requested.
     * @param parseRetain Whether to retain the backing byte array for quick reserialization.  
     * If true and the backing byte array is invalidated due to modification of a field then 
     * the cached bytes may be repopulated and retained if the message is serialized again in the future.
     * @param length The length of message if known.  Usually this is provided when deserializing of the wire
     * as the length will be provided as part of the header.  If unknown then set to Message.UNKNOWN_LENGTH
     * @throws ProtocolException
     */
    public AltcoinBlock(NetworkParameters params, byte[] payloadBytes, int offset, @Nullable Message parent, boolean parseLazy, boolean parseRetain, int length)
            throws ProtocolException {
        // TODO: Keep the parent
        super(params, payloadBytes, offset, parseLazy, parseRetain, length);
    }


    /**
     * Construct a block initialized with all the given fields.
     * @param params Which network the block is for.
     * @param version This should usually be set to 1 or 2, depending on if the height is in the coinbase input.
     * @param prevBlockHash Reference to previous block in the chain or {@link Sha256Hash#ZERO_HASH} if genesis.
     * @param merkleRoot The root of the merkle tree formed by the transactions.
     * @param time UNIX time when the block was mined.
     * @param difficultyTarget Number which this block hashes lower than.
     * @param nonce Arbitrary number to make the block hash lower than the target.
     * @param transactions List of transactions including the coinbase.
     */
    public AltcoinBlock(NetworkParameters params, long version, Sha256Hash prevBlockHash, Sha256Hash merkleRoot, long time,
                 long difficultyTarget, long nonce, List<Transaction> transactions) {
        super(params, version, prevBlockHash, merkleRoot, time, difficultyTarget, nonce, transactions);
    }

    @Override
    protected void parseAuxPoW() throws ProtocolException {
        if (this.auxpowParsed)
            return;

		if (this.params.isAuxPoWBlockVersion(this.version)) {
			// The following is used in dogecoinj, but I don't think we necessarily need it
			// payload.length >= 160) { // We have at least 2 headers in an Aux block. Workaround for StoredBlocks
			this.auxpow = new AuxPoW(params, payload, cursor, this, parseLazy, parseRetain);
		} else {
			this.auxpow = null;
		}

        this.auxpowParsed = true;
        this.auxpowBytesValid = parseRetain;
    }

    @Override
    void parse() throws ProtocolException {
        parseHeader();
        parseAuxPoW();
        parseTransactions();
        length = cursor - offset;
    }

    @Override
    protected void parseLite() throws ProtocolException {
        // Ignore the header since it has fixed length. If length is not provided we will have to
        // invoke a light parse of transactions to calculate the length.
        if (length == UNKNOWN_LENGTH) {
            Preconditions.checkState(parseLazy,
                    "Performing lite parse of block transaction as block was initialised from byte array " +
                    "without providing length.  This should never need to happen.");
            parseTransactions();
            // TODO: Handle AuxPoW header space
            length = cursor - offset;
        } else {
            transactionBytesValid = !transactionsParsed || parseRetain && length > HEADER_SIZE;
        }
        headerBytesValid = !headerParsed || parseRetain && length >= HEADER_SIZE;
    }

    @Override
    void writeHeader(OutputStream stream) throws IOException {
        super.writeHeader(stream);
        // TODO: Write the AuxPoW header
    }

    /** Returns a copy of the block, but without any transactions. */
    @Override
    public Block cloneAsHeader() {
        maybeParseHeader();
        AltcoinBlock block = new AltcoinBlock(params);
        block.nonce = nonce;
        block.prevBlockHash = prevBlockHash;
        block.merkleRoot = getMerkleRoot();
        block.version = version;
        block.time = time;
        block.difficultyTarget = difficultyTarget;
        block.transactions = null;
        block.hash = getHash();
        block.auxpow = auxpow;
        return block;
    }

    /** Returns true if the hash of the block is OK (lower than difficulty target). */
    protected boolean checkProofOfWork(boolean throwException) throws VerificationException {
        // TODO: Add AuxPoW support

        // This part is key - it is what proves the block was as difficult to make as it claims
        // to be. Note however that in the context of this function, the block can claim to be
        // as difficult as it wants to be .... if somebody was able to take control of our network
        // connection and fork us onto a different chain, they could send us valid blocks with
        // ridiculously easy difficulty and this function would accept them.
        //
        // To prevent this attack from being possible, elsewhere we check that the difficultyTarget
        // field is of the right value. This requires us to have the preceeding blocks.
        BigInteger target = getDifficultyTargetAsInteger();

        BigInteger h = getHash().toBigInteger();
        if (h.compareTo(target) > 0) {
            // Proof of work check failed!
            if (throwException)
                throw new VerificationException("Hash is higher than target: " + getHashAsString() + " vs "
                        + target.toString(16));
            else
                return false;
        }
        return true;
    }

    /**
     * Checks the block data to ensure it follows the rules laid out in the network parameters. Specifically,
     * throws an exception if the proof of work is invalid, or if the timestamp is too far from what it should be.
     * This is <b>not</b> everything that is required for a block to be valid, only what is checkable independent
     * of the chain and without a transaction index.
     *
     * @throws VerificationException
     */
    @Override
    public void verifyHeader() throws VerificationException {
        super.verifyHeader();
    }
}
