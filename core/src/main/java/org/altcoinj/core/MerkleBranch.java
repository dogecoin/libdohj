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

import static org.bitcoinj.core.Utils.doubleDigestTwoBuffers;
import static org.bitcoinj.core.Utils.reverseBytes;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A Merkle branch contains the hashes from a leaf of a Merkle tree
 * up to its root, plus a bitset used to define how the hashes are applied.
 * Given the hash of the leaf, this can be used to calculate the tree
 * root. This is useful for proving that a leaf belongs to a given tree.
 */
public class MerkleBranch extends ChildMessage implements Serializable {
    private static final long serialVersionUID = 2;

    // Merkle branches can be encoded in a way that will use more bytes than is optimal
    // (due to VarInts having multiple encodings)
    // MAX_BLOCK_SIZE must be compared to the optimal encoding, not the actual encoding, so when parsing, we keep track
    // of the size of the ideal encoding in addition to the actual message size (which Message needs) so that Blocks
    // can properly keep track of optimal encoded size
    private transient int optimalEncodingMessageSize;

	private List<Sha256Hash> branchHashes;
	private long branchSideMask;

    public MerkleBranch(NetworkParameters params, @Nullable ChildMessage parent) {
        super(params);
        setParent(parent);

		this.branchHashes = new ArrayList<Sha256Hash>();
		this.branchSideMask = 0;
    }

    /**
     * Deserializes an input message. This is usually part of a merkle branch message.
     */
    public MerkleBranch(NetworkParameters params, @Nullable ChildMessage parent, byte[] payload, int offset) throws ProtocolException {
        super(params, payload, offset);
        setParent(parent);
    }

    /**
     * Deserializes an input message. This is usually part of a merkle branch message.
     * @param params NetworkParameters object.
     * @param payload Bitcoin protocol formatted byte array containing message content.
     * @param offset The location of the first payload byte within the array.
     * @param parseLazy Whether to perform a full parse immediately or delay until a read is requested.
     * @param parseRetain Whether to retain the backing byte array for quick reserialization.  
     * If true and the backing byte array is invalidated due to modification of a field then 
     * the cached bytes may be repopulated and retained if the message is serialized again in the future.
     * as the length will be provided as part of the header.  If unknown then set to Message.UNKNOWN_LENGTH
     * @throws ProtocolException
     */
    public MerkleBranch(NetworkParameters params, ChildMessage parent, byte[] payload, int offset,
                            boolean parseLazy, boolean parseRetain)
            throws ProtocolException {
        super(params, payload, offset, parent, parseLazy, parseRetain, UNKNOWN_LENGTH);
    }

    public MerkleBranch(NetworkParameters params, @Nullable ChildMessage parent,
        final List<Sha256Hash> hashes, final long branchSideMask) {
        super(params);
        setParent(parent);

		this.branchHashes = hashes;
		this.branchSideMask = branchSideMask;
    }

    @Override
    protected void parseLite() throws ProtocolException {
		length = calcLength(payload, offset);
		cursor = offset + length;
    }

    protected static int calcLength(byte[] buf, int offset) {
        VarInt varint = new VarInt(buf, offset);

        return ((int) varint.value) * 4 + 4;
    }

    @Override
    void parse() throws ProtocolException {
        if (parsed)
            return;

        cursor = offset;

        final int hashCount = (int) readVarInt();
        optimalEncodingMessageSize += VarInt.sizeOf(hashCount);
		branchHashes = new ArrayList<Sha256Hash>(hashCount);
		for (int hashIdx = 0; hashIdx < hashCount; hashIdx++) {
			branchHashes.add(readHash());
		}
        optimalEncodingMessageSize += 32 * hashCount;
		branchSideMask = readUint32();
        optimalEncodingMessageSize += 4;
    }

    @Override
    protected void bitcoinSerializeToStream(OutputStream stream) throws IOException {
        stream.write(new VarInt(branchHashes.size()).encode());
		for (Sha256Hash hash: branchHashes) {
			stream.write(Utils.reverseBytes(hash.getBytes()));
		}
        Utils.uint32ToByteStreamLE(branchSideMask, stream);
    }

    /**
     * Calculate the merkle branch root based on the supplied hashes and the given leaf hash.
     * Used to verify that the given leaf and root are part of the same tree.
     */
    public Sha256Hash calculateMerkleRoot(final Sha256Hash leaf) {
        byte[] target = reverseBytes(leaf.getBytes());
        long mask = branchSideMask;

        for (Sha256Hash hash: branchHashes) {
            target = (mask & 1) == 0
                ? doubleDigestTwoBuffers(target, 0, 32, reverseBytes(hash.getBytes()), 0, 32)
                : doubleDigestTwoBuffers(reverseBytes(hash.getBytes()), 0, 32, target, 0, 32);
            mask >>= 1;
        }
        return new Sha256Hash(reverseBytes(target));
    }

    /**
     * Get the hashes which make up this branch.
     */
    public List<Sha256Hash> getHashes() {
        return Collections.unmodifiableList(this.branchHashes);
    }

    /**
     * Get the number of hashes in this branch.
     */
    public int getSize() {
        return branchHashes.size();
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

    /**
     * Returns a human readable debug string.
     */
    @Override
    public String toString() {
        return "Merkle branch";
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
     * Should check that the merkle branch side bits are not wider than the
	 * provided hashes.
     * @throws VerificationException If the branch is invalid.
     */
    public void verify() throws VerificationException {
        maybeParse();
		// TODO: Check the flags make sense for the inputs
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MerkleBranch input = (MerkleBranch) o;

		if (!branchHashes.equals(input.branchHashes)) return false;
		if (branchSideMask != input.branchSideMask) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + branchHashes.hashCode();
        result = 31 * result + (int) branchSideMask;
        return result;
    }
}
