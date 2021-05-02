package org.bitcoinj.core;

import org.junit.Before;
import org.junit.Test;
import org.libdohj.core.AltcoinSerializer;
import org.libdohj.params.DogecoinMainNetParams;
import org.libdohj.params.DogecoinTestNet3Params;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

import static org.bitcoinj.core.Util.getBytes;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

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
        AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, null, params.getDefaultSerializer());
        MerkleBranch branch = auxpow.getCoinbaseBranch();
        Sha256Hash expected = Sha256Hash.wrap("089b911f5e471c0e1800f3384281ebec5b372fbb6f358790a92747ade271ccdf");

        assertEquals(expected, auxpow.getCoinbase().getHash());
        assertEquals(3, auxpow.getCoinbaseBranch().size());
        assertEquals(6, auxpow.getChainMerkleBranch().size());

        expected = Sha256Hash.wrap("a22a9b01671d639fa6389f62ecf8ce69204c8ed41d5f1a745e0c5ba7116d5b4c");
        assertEquals(expected, auxpow.getParentBlockHeader().getHash());
        expected = Sha256Hash.wrap("f29cd14243ed542d9a0b495efcb9feca1b208bb5b717dc5ac04f068d2fef595a");
        assertEquals(expected, auxpow.getParentBlockHeader().getMerkleRoot());
    }

    /**
     * Test serializing the AuxPoW header from Dogecoin block #403,931.
     */
    @Test
    public void serializeAuxPoWHeader() throws Exception {
        byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, null, params.getDefaultSerializer());
        byte[] actual = auxpow.bitcoinSerialize();

        assertArrayEquals(auxpowAsBytes, actual);
    }

    /**
     * Validate the AuxPoW header from Dogecoin block #403,931.
     */
    @Test
    public void checkAuxPoWHeader() throws Exception {
        byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, null, params.getDefaultSerializer());
        auxpow.checkProofOfWork(Sha256Hash.wrap("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
                Utils.decodeCompactBits(0x1b06f8f0), true);
    }

    /**
     * Validate the AuxPoW header with no explicit data header in the coinbase
     * transaction. Namecoin block #19,414
     */
    @Test
    public void checkAuxPoWHeaderNoTxHeader() throws Exception {
        // Emulate Namecoin block hashing for this test
        final NetworkParameters namecoinLikeParams = new DogecoinTestNet3Params() {
            @Override
            public BigInteger getBlockDifficulty(Block block) {
                // Namecoin uses SHA256 hashes
                return block.getHash().toBigInteger();
            }
        };
        byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header_no_tx_header.bin"));
        AuxPoW auxpow = new AuxPoW(namecoinLikeParams, auxpowAsBytes, null, namecoinLikeParams.getDefaultSerializer());
        auxpow.checkProofOfWork(Sha256Hash.wrap("5fb89c3b18c27bc38d351d516177cbd3504c95ca0494cbbbbd52f2fb5f2ff1ec"),
                Utils.decodeCompactBits(0x1b00b269), true);
    }

    /**
     * Check that a non-generate AuxPoW transaction is rejected.
     */
    @Test
    public void shouldRejectNonGenerateAuxPoW() throws Exception {
        final byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        final AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, null, params.getDefaultSerializer());
        auxpow.getCoinbaseBranch().setIndex(0x01);
        assertThrows("AuxPow is not a generate", org.bitcoinj.core.VerificationException.class, () -> {
            auxpow.checkProofOfWork(Sha256Hash.wrap("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
                    Utils.decodeCompactBits(0x1b06f8f0), true);
        });
    }

    /**
     * Check that block headers from the child chain are rejected as parent
     * chain for AuxPoW, via checking of the chain IDs.
     */
    @Test
    public void shouldRejectOwnChainID() throws Exception {
        byte[] payload = Util.getBytes(getClass().getResourceAsStream("dogecoin_block371337.bin"));
        AltcoinSerializer serializer = (AltcoinSerializer) params.getDefaultSerializer();
        final AltcoinBlock block = (AltcoinBlock) serializer.makeBlock(payload);
        assertEquals(98, block.getChainID());
        final AuxPoW auxpow = block.getAuxPoW();
        assertNotNull(auxpow);
        auxpow.setParentBlockHeader((AltcoinBlock) block.cloneAsHeader());
        assertThrows("Aux POW parent has our chain ID",
                org.bitcoinj.core.VerificationException.class, () -> {
                    auxpow.checkProofOfWork(Sha256Hash.wrap("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
                            Utils.decodeCompactBits(0x1b06f8f0), true);
                });
    }

    /**
     * Check that where the merkle branch is far too long to use, it's rejected.
     */
    @Test
    public void shouldRejectVeryLongMerkleBranch() throws Exception {
        final byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        final AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, null, params.getDefaultSerializer());
        auxpow.getChainMerkleBranch().setHashes(Arrays.asList(new Sha256Hash[32]));
        assertThrows("Aux POW chain merkle branch too long", org.bitcoinj.core.VerificationException.class, () -> {
            auxpow.checkProofOfWork(Sha256Hash.wrap("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
                    Utils.decodeCompactBits(0x1b06f8f0), true);
        });
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
        final AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, null, params.getDefaultSerializer());
        auxpow.getCoinbase().clearOutputs();
        assertThrows("Aux POW merkle root incorrect", org.bitcoinj.core.VerificationException.class, () -> {
            auxpow.checkProofOfWork(Sha256Hash.wrap("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
                    Utils.decodeCompactBits(0x1b06f8f0), true);
        });
    }

    /**
     * Ensure that in case of a malformed coinbase transaction (no inputs) it's
     * caught and processed neatly.
     */
    @Test
    public void shouldRejectIfCoinbaseTransactionHasNoInputs() throws Exception {
        final byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        final AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, null, params.getDefaultSerializer());

        // This will also break the difficulty check, but as that doesn't occur
        // until the end, we can get away with it.
        auxpow.getCoinbase().clearInputs();
        updateMerkleRootToMatchCoinbase(auxpow);

        assertThrows("Coinbase transaction has no inputs",
                org.bitcoinj.core.VerificationException.class, () -> {
            auxpow.checkProofOfWork(Sha256Hash.wrap("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
                    Utils.decodeCompactBits(0x1b06f8f0), true);
        });
    }

    /**
     * Catch the case that the coinbase transaction does not contain details of
     * the merged block. In this case we make the transaction script too short
     * for it to do so.
     */
    @Test
    public void shouldRejectIfMergedMineHeaderMissing() throws Exception {
        final byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        final AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, null, params.getDefaultSerializer());

        // This will also break the difficulty check, but as that doesn't occur
        // until the end, we can get away with it.
        final TransactionInput in = auxpow.getCoinbase().getInput(0);
        final byte[] paddedScriptBytes = new byte[in.getScriptBytes().length + (AuxPoW.MAX_INDEX_PC_BACKWARDS_COMPATIBILITY + 4)];
        Arrays.fill(paddedScriptBytes, (byte) 0);
        System.arraycopy(in.getScriptBytes(), 8, paddedScriptBytes, (AuxPoW.MAX_INDEX_PC_BACKWARDS_COMPATIBILITY + 4), in.getScriptBytes().length - 8);
        in.setScriptBytes(paddedScriptBytes);
        updateMerkleRootToMatchCoinbase(auxpow);

        assertThrows("Aux POW chain merkle root must start in the first 20 bytes of the parent coinbase",
                org.bitcoinj.core.VerificationException.class, () -> {
            auxpow.checkProofOfWork(Sha256Hash.wrap("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
                    Utils.decodeCompactBits(0x1b06f8f0), true);
        });
    }

    /**
     * Catch the case that more than one merged mine header is present in the
     * coinbase transaction (this is considered an attempt to confuse the parser).
     */
    @Test
    public void shouldRejectIfMergedMineHeaderDuplicated() throws Exception {
        final byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        final AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, null, params.getDefaultSerializer());

        // This will also break the difficulty check, but as that doesn't occur
        // until the end, we can get away with it.
        final TransactionInput in = auxpow.getCoinbase().getInput(0);
        final byte[] newBytes = Arrays.copyOf(in.getScriptBytes(), in.getScriptBytes().length + 4);
        for (int byteIdx = 0; byteIdx < AuxPoW.MERGED_MINING_HEADER.length; byteIdx++) {
            newBytes[newBytes.length - 4 + byteIdx] = AuxPoW.MERGED_MINING_HEADER[byteIdx];
        }
        in.setScriptBytes(newBytes);
        updateMerkleRootToMatchCoinbase(auxpow);

        assertThrows("Multiple merged mining headers in coinbase",
                org.bitcoinj.core.VerificationException.class, () -> {
            auxpow.checkProofOfWork(Sha256Hash.wrap("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
                    Utils.decodeCompactBits(0x1b06f8f0), true);
        });
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
        final AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, null, params.getDefaultSerializer());

        // This will also break the difficulty check, but as that doesn't occur
        // until the end, we can get away with it.
        final TransactionInput in = auxpow.getCoinbase().getInput(0);
        in.getScriptBytes()[8] = 0; // Break the first byte of the chain merkle root
        updateMerkleRootToMatchCoinbase(auxpow);

        assertThrows("Aux POW missing chain merkle root in parent coinbase",
                org.bitcoinj.core.VerificationException.class, () -> {
            auxpow.checkProofOfWork(Sha256Hash.wrap("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
                    Utils.decodeCompactBits(0x1b06f8f0), true);
        });
    }

    /**
     * Catch the case that the chain merkle branch is not immediately after the
     * merged mine header in the coinbase transaction (this is considered an
     * attempt to confuse the parser).
     */
    @Test
    public void shouldRejectIfChainMerkleRootNotAfterHeader() throws Exception {
        final byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        final AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, null, params.getDefaultSerializer());

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

        assertThrows("Merged mining header is not just before chain merkle root",
                org.bitcoinj.core.VerificationException.class, () -> {
            auxpow.checkProofOfWork(Sha256Hash.wrap("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
                    Utils.decodeCompactBits(0x1b06f8f0), true);
        });
    }

    /**
     * Catch the case that the chain merkle branch is not immediately after the
     * merged mine header in the coinbase transaction (this is considered an
     * attempt to confuse the parser).
     */
    @Test
    public void shouldRejectIfScriptBytesTooShort() throws Exception {
        final byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        final AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, null, params.getDefaultSerializer());

        // This will also break the difficulty check, but as that doesn't occur
        // until the end, we can get away with it.
        final TransactionInput in = auxpow.getCoinbase().getInput(0);
        final byte[] newBytes = Arrays.copyOf(in.getScriptBytes(), in.getScriptBytes().length - 12);
        in.setScriptBytes(newBytes);
        updateMerkleRootToMatchCoinbase(auxpow);

        assertThrows("Aux POW missing chain merkle tree size and nonce in parent coinbase",
                org.bitcoinj.core.VerificationException.class, () -> {
            auxpow.checkProofOfWork(Sha256Hash.wrap("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
                    Utils.decodeCompactBits(0x1b06f8f0), true);
        });
    }

    /**
     * Catch the case that the chain merkle branch size in the coinbase transaction
     * does not match the size of the merkle brach in the AuxPoW header.
     */
    @Test
    public void shouldRejectIfCoinbaseMerkleBranchSizeMismatch() throws Exception {
        final byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        final AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, null, params.getDefaultSerializer());

        // This will also break the difficulty check, but as that doesn't occur
        // until the end, we can get away with it.
        final TransactionInput in = auxpow.getCoinbase().getInput(0);
        in.getScriptBytes()[40] = 3; // Break the merkle branch length
        updateMerkleRootToMatchCoinbase(auxpow);

        assertThrows("Aux POW merkle branch size does not match parent coinbase",
                org.bitcoinj.core.VerificationException.class, () -> {
            auxpow.checkProofOfWork(Sha256Hash.wrap("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
                    Utils.decodeCompactBits(0x1b06f8f0), true);
        });
    }

    /**
     * In order to ensure that the same work is not submitted more than once,
     * confirm that the merkle branch index is correct for our chain ID.
     */
    @Test
    public void shouldRejectIfNonceIncorrect() throws Exception {
        final byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        final AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, null, params.getDefaultSerializer());

        // This will also break the difficulty check, but as that doesn't occur
        // until the end, we can get away with it.
        final TransactionInput in = auxpow.getCoinbase().getInput(0);
        in.getScriptBytes()[44] = (byte) 0xff; // Break the nonce value
        updateMerkleRootToMatchCoinbase(auxpow);

        assertThrows("Aux POW wrong index",
                org.bitcoinj.core.VerificationException.class, () -> {
            auxpow.checkProofOfWork(Sha256Hash.wrap("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
                    Utils.decodeCompactBits(0x1b06f8f0), true);
        });
    }

    /**
     * Having validated the AuxPoW header, the last check is that the block hash
     * meets the target difficulty.
     */
    @Test
    public void shouldRejectHashAboveTarget() throws Exception {
        final byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        final AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, null, params.getDefaultSerializer());

        assertThrows("Hash is higher than target: 000000000003178bb23160cdbc81af53f47cae9f479acf1e69849da42fd5bfca vs 0",
                org.bitcoinj.core.VerificationException.class, () -> {
            auxpow.checkProofOfWork(Sha256Hash.wrap("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609"),
                    Utils.decodeCompactBits(0x00), true);
        });
    }

    /**
     * Fix up the merkle root of the parent block header to match the
     * coinbase transaction.
     */
    private void updateMerkleRootToMatchCoinbase(final AuxPoW auxpow) {
        final Transaction coinbase = auxpow.getCoinbase();

        final Sha256Hash revisedCoinbaseHash = coinbase.getTxId();
        // The coinbase hash is the single leaf node in the merkle tree,
        // so to get the root we need to hash it with itself.
        // Note that bytes are reversed for hashing
        final Sha256Hash revisedMerkleRoot = Sha256Hash.wrapReversed(
                Sha256Hash.hashTwice(revisedCoinbaseHash.getReversedBytes(), 0, 32, revisedCoinbaseHash.getReversedBytes(), 0, 32)
        );
        auxpow.getParentBlockHeader().setMerkleRoot(revisedMerkleRoot);
        auxpow.setCoinbaseBranch(new MerkleBranch(params, auxpow,
                Collections.singletonList(revisedCoinbaseHash), MERKLE_ROOT_COINBASE_INDEX));
    }

    /**
     * Test extraction of a nonce value from the coinbase transaction pubscript.
     * This test primarily exists to ensure that byte order is correct, and that
     * a nonce value above Integer.MAX_VALUE is still returned as a positive
     * integer.
     */
    @Test
    public void testGetNonceFromScript() {
        final byte[] script = Utils.HEX.decode("03251d0de4b883e5bda9e7a59ee4bb99e9b1bcfabe6d6dc6c83f297ee373df0d826f3148f218e4e4eb349e0bba715ad793ccc2d6beb6df40000000f09f909f4d696e65642062792079616e6779616e676368656e00000000000000000000000000000000");
        final int pc = 55;
        final long expResult = 0x9f909ff0L;
        final long result = AuxPoW.getNonceFromScript(script, pc);
        assertEquals(expResult, result);
    }

    /**
     * Test of getExpectedIndex method, of class AuxPoW.
     */
    @Test
    public void testGetExpectedIndex() {
        final long nonce = 0x9f909ff0L;
        final int chainId = 98;
        final int merkleHeight = 6;
        final int expResult = 40;
        final int result = AuxPoW.getExpectedIndex(nonce, chainId, merkleHeight);
        assertEquals(expResult, result);
    }

    /**
     * Tests the array matching algorithm for not accepting part of the array when it is in the end of the script.
     */
    @Test
    public void testArrayMatch() {
        byte[] script = Utils.HEX.decode("089b911f5e471c0e1800f3384281ebec5b372fbb6f358790a92747ade271ccdf");
        byte[] prefix = Utils.HEX.decode("089b911f");
        byte[] suffix = Utils.HEX.decode("e271ccdf");
        byte[] anywhere = Utils.HEX.decode("384281eb");
        byte[] overTheEnd = Utils.HEX.decode("e271ccdf000000");

        assertTrue(AuxPoW.arrayMatch(script, 0, prefix));
        assertTrue(AuxPoW.arrayMatch(script, 28, suffix));
        assertTrue(AuxPoW.arrayMatch(script, 11, anywhere));
        assertFalse(AuxPoW.arrayMatch(script, 28, overTheEnd));
    }
}
