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

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A Merkle branch contains the hashes from a leaf of a Merkle tree
 * up to its root, plus a bitset used to define how the hashes are applied.
 * Given the hash of the leaf, this can be used to calculate the tree
 * root. This is useful for proving that a leaf belongs to a given tree.
 * 
 * TODO: Has a lot of similarity to PartialMerkleTree, should attempt to merge
 * the two.
 */
public class MerkleBranch extends ChildMessage {
    private static final long serialVersionUID = 2;

    // Merkle branches can be encoded in a way that will use more bytes than is optimal
    // (due to VarInts having multiple encodings)
    // MAX_BLOCK_SIZE must be compared to the optimal encoding, not the actual encoding, so when parsing, we keep track
    // of the size of the ideal encoding in addition to the actual message size (which Message needs) so that Blocks
    // can properly keep track of optimal encoded size
    private transient int optimalEncodingMessageSize;

    private List<Sha256Hash> hashes;
    private long index;

    public MerkleBranch(NetworkParameters params, @Nullable ChildMessage parent) {
        super(params);
        setParent(parent);

        this.hashes = new ArrayList<Sha256Hash>();
        this.index = 0;
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
     * @param serializer the serializer to use for this message.
     * @throws ProtocolException
     */
    public MerkleBranch(NetworkParameters params, ChildMessage parent, byte[] payload, int offset,
                        MessageSerializer serializer)
            throws ProtocolException {
        super(params, payload, offset, parent, serializer, UNKNOWN_LENGTH);
    }

    public MerkleBranch(NetworkParameters params, @Nullable ChildMessage parent,
        final List<Sha256Hash> hashes, final long branchSideMask) {
        super(params);
        setParent(parent);

        this.hashes = hashes;
        this.index = branchSideMask;
    }

    public static int calcLength(byte[] buf, int offset) {
        VarInt varint = new VarInt(buf, offset);

        return ((int) varint.value) * 4 + 4;
    }

    @Override
    protected void parse() throws ProtocolException {
        cursor = offset;

        final int hashCount = (int) readVarInt();
        optimalEncodingMessageSize += VarInt.sizeOf(hashCount);
        hashes = new ArrayList<Sha256Hash>(hashCount);
        for (int hashIdx = 0; hashIdx < hashCount; hashIdx++) {
            hashes.add(readHash());
        }
        optimalEncodingMessageSize += 32 * hashCount;
        setIndex(readUint32());
        optimalEncodingMessageSize += 4;
        length = cursor - offset;
    }

    @Override
    protected void bitcoinSerializeToStream(OutputStream stream) throws IOException {
        stream.write(new VarInt(hashes.size()).encode());
        for (Sha256Hash hash: hashes) {
                stream.write(Utils.reverseBytes(hash.getBytes()));
        }
        Utils.uint32ToByteStreamLE(index, stream);
    }

    /**
     * Calculate the merkle branch root based on the supplied hashes and the given leaf hash.
     * Used to verify that the given leaf and root are part of the same tree.
     */
    public Sha256Hash calculateMerkleRoot(final Sha256Hash leaf) {
        byte[] target = leaf.getReversedBytes();
        long mask = index;
        MessageDigest digest = Sha256Hash.newDigest();

        for (Sha256Hash hash: hashes) {
            digest.reset();
            if ((mask & 1) == 0) { // 0 means it goes on the right
                digest.update(target);
                digest.update(hash.getReversedBytes());
            } else {
                digest.update(hash.getReversedBytes());
                digest.update(target);
            }
            // Double-digest the values
            target = digest.digest();
            digest.reset();
            target = digest.digest(target);
            mask >>= 1;
        }
        return Sha256Hash.wrapReversed(target);
    }

    /**
     * Get the hashes which make up this branch.
     */
    public List<Sha256Hash> getHashes() {
        return Collections.unmodifiableList(this.hashes);
    }

    /**
     * Return the mask used to determine which side the hashes are applied on.
     * Each bit represents a hash. Zero means it goes on the right, one means
     * on the left.
     */
    public long getIndex() {
        return index;
    }

    /**
     * @param hashes the hashes to set
     */
    public void setHashes(List<Sha256Hash> hashes) {
        this.hashes = hashes;
    }

    /**
     * Set the mask used to determine the sides in which hashes are applied.
     */
    public void setIndex(final long newIndex) {
        assert newIndex >= 0;
        this.index = newIndex;
    }

    /**
     * Get the number of hashes in this branch.
     */
    public int size() {
        return hashes.size();
    }

    public int getOptimalEncodingMessageSize() {
        if (optimalEncodingMessageSize != 0)
            return optimalEncodingMessageSize;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MerkleBranch input = (MerkleBranch) o;

        if (!hashes.equals(input.hashes)) return false;
        if (index != input.index) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + hashes.hashCode();
        result = 31 * result + (int) index;
        return result;
    }
}
