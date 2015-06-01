package org.bitcoinj.core;

import java.util.Arrays;
import java.util.Collections;
import org.altcoinj.core.AltcoinSerializer;
import org.altcoinj.params.DogecoinMainNetParams;
import org.junit.Test;

import static org.bitcoinj.core.Util.getBytes;
import static org.bitcoinj.core.Utils.doubleDigestTwoBuffers;
import static org.bitcoinj.core.Utils.reverseBytes;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 * AuxPoW header parsing/serialization and validation
 */
public class AuxPoWTest {
    static final NetworkParameters params = DogecoinMainNetParams.get();
    private static final int MERKLE_ROOT_COINBASE_INDEX = 0;

    @Before
    public void setUp() throws Exception {
        Context context = new Context(params);
    }

    /**
     * Parse the AuxPoW header from Dogecoin block #403,931.
     */
    @Test
    public void parseAuxPoWHeader() throws Exception {
        byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, (ChildMessage) null, params.getDefaultSerializer());
        MerkleBranch branch = auxpow.getCoinbaseBranch();
        Sha256Hash expected = new Sha256Hash("089b911f5e471c0e1800f3384281ebec5b372fbb6f358790a92747ade271ccdf");

        assertEquals(expected, auxpow.getCoinbase().getHash());
        assertEquals(3, auxpow.getCoinbaseBranch().size());
        assertEquals(6, auxpow.getChainMerkleBranch().size());

        expected = new Sha256Hash("a22a9b01671d639fa6389f62ecf8ce69204c8ed41d5f1a745e0c5ba7116d5b4c");
        assertEquals(expected, auxpow.getParentBlockHeader().getHash());
    }

    /**
     * Test serializing the AuxPoW header from Dogecoin block #403,931.
     */
    @Test
    public void serializeAuxPoWHeader() throws Exception {
        byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, (ChildMessage) null, params.getDefaultSerializer());
        byte[] expected = auxpowAsBytes;
        byte[] actual = auxpow.bitcoinSerialize();

        assertArrayEquals(expected, actual);
    }

    /**
     * Validate the AuxPoW header from Dogecoin block #403,931.
     */
    @Test
    public void checkAuxPoWHeader() throws Exception {
        byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, (ChildMessage) null, params.getDefaultSerializer());
        auxpow.checkProofOfWork(new Sha256Hash("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
            Utils.decodeCompactBits(0x1b06f8f0), true);
    }
    
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    /**
     * Check that a non-generate AuxPoW transaction is rejected.
     */
    @Test
    public void shouldRejectNonGenerateAuxPoW() throws Exception {
        final byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        final AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, (ChildMessage) null, params.getDefaultSerializer());
        auxpow.getCoinbaseBranch().setIndex(0x01);
        expectedEx.expect(org.bitcoinj.core.VerificationException.class);
        expectedEx.expectMessage("AuxPow is not a generate");
        auxpow.checkProofOfWork(new Sha256Hash("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
            Utils.decodeCompactBits(0x1b06f8f0), true);
    }

    /**
     * Check that block headers from the child chain are rejected as parent
     * chain for AuxPoW, via checking of the chain IDs.
     */
    @Test
    public void shouldRejectOwnChainID() throws Exception {
        byte[] payload = Util.getBytes(getClass().getResourceAsStream("dogecoin_block371337.bin"));
        AltcoinSerializer serializer = (AltcoinSerializer)params.getDefaultSerializer();
        final AltcoinBlock block = (AltcoinBlock)serializer.makeBlock(payload);
        final AuxPoW auxpow = block.getAuxPoW();
        auxpow.setParentBlockHeader((AltcoinBlock)block.cloneAsHeader());
        expectedEx.expect(org.bitcoinj.core.VerificationException.class);
        expectedEx.expectMessage("Aux POW parent has our chain ID");
        auxpow.checkProofOfWork(new Sha256Hash("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
            Utils.decodeCompactBits(0x1b06f8f0), true);
    }

    /**
     * Check that where the merkle branch is far too long to use, it's rejected.
     */
    @Test
    public void shouldRejectVeryLongMerkleBranch() throws Exception {
        final byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        final AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, (ChildMessage) null, params.getDefaultSerializer());
        auxpow.getChainMerkleBranch().setHashes(Arrays.asList(new Sha256Hash[32]));
        expectedEx.expect(org.bitcoinj.core.VerificationException.class);
        expectedEx.expectMessage("Aux POW chain merkle branch too long");
        auxpow.checkProofOfWork(new Sha256Hash("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
            Utils.decodeCompactBits(0x1b06f8f0), true);
    }

    /**
     * Later steps in AuxPoW validation depend on the contents of the coinbase
     * transaction. Obviously that's useless if we don't check the coinbase
     * transaction is actually part of the parent chain block, so first we test
     * that the transaction hash is part of the merkle tree. This test modifies
     * the transaction, invalidating the hash, to confirm that it's rejected.
     */
    @Test
    public void shouldRejectIfCoinbaseTransactionNotInMerkleBranch() throws Exception {
        final byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        final AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, (ChildMessage) null, params.getDefaultSerializer());
        auxpow.getCoinbase().clearOutputs();
        expectedEx.expect(org.bitcoinj.core.VerificationException.class);
        expectedEx.expectMessage("Aux POW merkle root incorrect");
        auxpow.checkProofOfWork(new Sha256Hash("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
            Utils.decodeCompactBits(0x1b06f8f0), true);
    }

    /**
     * Ensure that in case of a malformed coinbase transaction (no inputs) it's
     * caught and processed neatly.
     */
    @Test
    public void shouldRejectIfCoinbaseTransactionHasNoInputs() throws Exception {
        final byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        final AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, (ChildMessage) null, params.getDefaultSerializer());

        // This will also break the difficulty check, but as that doesn't occur
        // until the end, we can get away with it.
        auxpow.getCoinbase().clearInputs();
        updateMerkleRootToMatchCoinbase(auxpow);

        expectedEx.expect(org.bitcoinj.core.VerificationException.class);
        expectedEx.expectMessage("Coinbase transaction has no inputs");
        auxpow.checkProofOfWork(new Sha256Hash("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
            Utils.decodeCompactBits(0x1b06f8f0), true);
    }

    /**
     * Catch the case that the merged mine header is missing from the coinbase
     * transaction.
     */
    @Test
    public void shouldRejectIfMergedMineHeaderMissing() throws Exception {
        final byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        final AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, (ChildMessage) null, params.getDefaultSerializer());

        // This will also break the difficulty check, but as that doesn't occur
        // until the end, we can get away with it.
        final TransactionInput in = auxpow.getCoinbase().getInput(0);
        in.getScriptBytes()[4] = 0; // Break the first byte of the header
        updateMerkleRootToMatchCoinbase(auxpow);

        expectedEx.expect(org.bitcoinj.core.VerificationException.class);
        expectedEx.expectMessage("MergedMiningHeader missing from parent coinbase");
        auxpow.checkProofOfWork(new Sha256Hash("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
            Utils.decodeCompactBits(0x1b06f8f0), true);
    }

    /**
     * Catch the case that more than one merged mine header is present in the 
     * coinbase transaction (this is considered an attempt to confuse the parser).
     */
    @Test
    public void shouldRejectIfMergedMineHeaderDuplicated() throws Exception {
        final byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        final AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, (ChildMessage) null, params.getDefaultSerializer());

        // This will also break the difficulty check, but as that doesn't occur
        // until the end, we can get away with it.
        final TransactionInput in = auxpow.getCoinbase().getInput(0);
        final byte[] newBytes = Arrays.copyOf(in.getScriptBytes(), in.getScriptBytes().length + 4);
        for (int byteIdx = 0; byteIdx < AuxPoW.MERGED_MINING_HEADER.length; byteIdx++) {
            newBytes[newBytes.length - 4 + byteIdx] = AuxPoW.MERGED_MINING_HEADER[byteIdx];
        }
        in.setScriptBytes(newBytes);
        updateMerkleRootToMatchCoinbase(auxpow);

        expectedEx.expect(org.bitcoinj.core.VerificationException.class);
        expectedEx.expectMessage("Multiple merged mining headers in coinbase");
        auxpow.checkProofOfWork(new Sha256Hash("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
            Utils.decodeCompactBits(0x1b06f8f0), true);
    }

    /**
     * Catch the case that the chain merkle branch is missing from the coinbase
     * transaction. The chain merkle branch is used to prove that the block was
     * mined for chain or chains including this one (i.e. random proof of work
     * cannot be taken from any merged-mined blockchain and reused).
     */
    @Test
    public void shouldRejectIfCoinbaseMissingChainMerkleRoot() throws Exception {
        final byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        final AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, (ChildMessage) null, params.getDefaultSerializer());

        // This will also break the difficulty check, but as that doesn't occur
        // until the end, we can get away with it.
        final TransactionInput in = auxpow.getCoinbase().getInput(0);
        in.getScriptBytes()[8] = 0; // Break the first byte of the chain merkle root
        updateMerkleRootToMatchCoinbase(auxpow);

        expectedEx.expect(org.bitcoinj.core.VerificationException.class);
        expectedEx.expectMessage("Aux POW missing chain merkle root in parent coinbase");
        auxpow.checkProofOfWork(new Sha256Hash("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
            Utils.decodeCompactBits(0x1b06f8f0), true);
    }

    /**
     * Catch the case that the chain merkle branch is not immediately after the
     * merged mine header in the coinbase transaction (this is considered an
     * attempt to confuse the parser).
     */
    @Test
    public void shouldRejectIfChainMerkleRootNotAfterHeader() throws Exception {
        final byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        final AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, (ChildMessage) null, params.getDefaultSerializer());

        // This will also break the difficulty check, but as that doesn't occur
        // until the end, we can get away with it.
        final TransactionInput in = auxpow.getCoinbase().getInput(0);
        final byte[] newBytes = Arrays.copyOf(in.getScriptBytes(), in.getScriptBytes().length + 1);
        // Copy every byte after the merged-mine header forward one byte. We
        // have to do this from the end of the array backwards to avoid overwriting
        // the next byte to copy.
        for (int byteIdx = newBytes.length - 1; byteIdx > 8; byteIdx--) {
            newBytes[byteIdx] = newBytes[byteIdx - 1];
        }
        newBytes[8] = (byte) 0xff;
        in.setScriptBytes(newBytes);
        updateMerkleRootToMatchCoinbase(auxpow);

        expectedEx.expect(org.bitcoinj.core.VerificationException.class);
        expectedEx.expectMessage("Merged mining header is not just before chain merkle root");
        auxpow.checkProofOfWork(new Sha256Hash("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
            Utils.decodeCompactBits(0x1b06f8f0), true);
    }

    /**
     * Catch the case that the chain merkle branch is not immediately after the
     * merged mine header in the coinbase transaction (this is considered an
     * attempt to confuse the parser).
     */
    @Test
    public void shouldRejectIfScriptBytesTooShort() throws Exception {
        final byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        final AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, (ChildMessage) null, params.getDefaultSerializer());

        // This will also break the difficulty check, but as that doesn't occur
        // until the end, we can get away with it.
        final TransactionInput in = auxpow.getCoinbase().getInput(0);
        final byte[] newBytes = Arrays.copyOf(in.getScriptBytes(), in.getScriptBytes().length - 12);
        in.setScriptBytes(newBytes);
        updateMerkleRootToMatchCoinbase(auxpow);

        expectedEx.expect(org.bitcoinj.core.VerificationException.class);
        expectedEx.expectMessage("Aux POW missing chain merkle tree size and nonce in parent coinbase");
        auxpow.checkProofOfWork(new Sha256Hash("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
            Utils.decodeCompactBits(0x1b06f8f0), true);
    }

    /**
     * Catch the case that the chain merkle branch size in the coinbase transaction
     * does not match the size of the merkle brach in the AuxPoW header.
     */
    @Test
    public void shouldRejectIfCoinbaseMerkleBranchSizeMismatch() throws Exception {
        final byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        final AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, (ChildMessage) null, params.getDefaultSerializer());

        // This will also break the difficulty check, but as that doesn't occur
        // until the end, we can get away with it.
        final TransactionInput in = auxpow.getCoinbase().getInput(0);
        in.getScriptBytes()[40] = 3; // Break the merkle branch length
        updateMerkleRootToMatchCoinbase(auxpow);

        expectedEx.expect(org.bitcoinj.core.VerificationException.class);
        expectedEx.expectMessage("Aux POW merkle branch size does not match parent coinbase");
        auxpow.checkProofOfWork(new Sha256Hash("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
            Utils.decodeCompactBits(0x1b06f8f0), true);
    }

    /**
     * In order to ensure that the same work is not submitted more than once,
     * confirm that the merkle branch index is correct for our chain ID.
     */
    @Test
    public void shouldRejectIfNonceIncorrect() throws Exception {
        final byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        final AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, (ChildMessage) null, params.getDefaultSerializer());

        // This will also break the difficulty check, but as that doesn't occur
        // until the end, we can get away with it.
        final TransactionInput in = auxpow.getCoinbase().getInput(0);
        in.getScriptBytes()[44] = (byte) 0xff; // Break the nonce value
        updateMerkleRootToMatchCoinbase(auxpow);

        expectedEx.expect(org.bitcoinj.core.VerificationException.class);
        expectedEx.expectMessage("Aux POW wrong index");
        auxpow.checkProofOfWork(new Sha256Hash("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
            Utils.decodeCompactBits(0x1b06f8f0), true);
    }

    /**
     * Having validated the AuxPoW header, the last check is that the block hash
     * meets the target difficulty.
     */
    @Test
    public void shouldRejectHashAboveTarget() throws Exception {
        final byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        final AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, (ChildMessage) null, params.getDefaultSerializer());

        expectedEx.expect(org.bitcoinj.core.VerificationException.class);
        expectedEx.expectMessage("Hash is higher than target: a22a9b01671d639fa6389f62ecf8ce69204c8ed41d5f1a745e0c5ba7116d5b4c vs 0");
        
        auxpow.checkProofOfWork(new Sha256Hash("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
            Utils.decodeCompactBits(0x00), true);
    }

    /**
     * Fix up the merkle root of the parent block header to match the
     * coinbase transaction.
     */
    private void updateMerkleRootToMatchCoinbase(final AuxPoW auxpow) {
        final Transaction coinbase = auxpow.getCoinbase();

        final Sha256Hash revisedCoinbaseHash = coinbase.getHash();
        // The coinbase hash is the single leaf node in the merkle tree,
        // so to get the root we need to hash it with itself.
        // Note that bytes are reversed for hashing
        final byte[] revisedMerkleRootBytes = doubleDigestTwoBuffers(
                reverseBytes(revisedCoinbaseHash.getBytes()), 0, 32,
                reverseBytes(revisedCoinbaseHash.getBytes()), 0, 32);
        final Sha256Hash revisedMerkleRoot = new Sha256Hash(reverseBytes(revisedMerkleRootBytes));
        auxpow.getParentBlockHeader().setMerkleRoot(revisedMerkleRoot);
        auxpow.setCoinbaseBranch(new MerkleBranch(params, auxpow,
                Collections.singletonList(revisedCoinbaseHash), MERKLE_ROOT_COINBASE_INDEX));
    }
}
