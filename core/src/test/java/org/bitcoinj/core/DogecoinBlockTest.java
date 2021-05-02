package org.bitcoinj.core;

import org.junit.Before;
import org.junit.Test;
import org.libdohj.core.AltcoinSerializer;
import org.libdohj.params.DogecoinMainNetParams;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DogecoinBlockTest {
    private static final NetworkParameters params = DogecoinMainNetParams.get();

    @Before
    public void setUp() throws Exception {
        Context context = new Context(params);
    }

    @Test
    public void shouldExtractChainID() {
        final long baseVersion = 2;
        final long flags = 1;
        final long chainID = 98;
        final long auxpowVersion = (chainID << 16) | (flags << 8) | baseVersion;
        assertEquals(chainID, AltcoinBlock.getChainID(auxpowVersion));
    }

    @Test
    public void shouldExtractBaseVersion() {
        final long baseVersion = 2;
        final long flags = 1;
        final long chainID = 98;
        final long auxpowVersion = (chainID << 16) | (flags << 8) | baseVersion;
        assertEquals(baseVersion, AltcoinBlock.getBaseVersion(auxpowVersion));
    }

    @Test
    public void shouldParseBlock1() throws IOException {
        byte[] payload = Util.getBytes(getClass().getResourceAsStream("dogecoin_block1.bin"));
        AltcoinSerializer serializer = (AltcoinSerializer)params.getDefaultSerializer();
        final AltcoinBlock block = (AltcoinBlock)serializer.makeBlock(payload);
        assertEquals("82bc68038f6034c0596b6e313729793a887fded6e92a31fbdf70863f89d9bea2", block.getHashAsString());
        assertEquals(1, block.getTransactions().size());
        assertEquals(0x1e0ffff0L, block.getDifficultyTarget());
    }

    /**
     * Test the first hardfork block.
     */
    @Test
    public void shouldParseBlock250000() throws IOException {
        byte[] payload = Util.getBytes(getClass().getResourceAsStream("dogecoin_block250000.bin"));
        AltcoinSerializer serializer = (AltcoinSerializer)params.getDefaultSerializer();
        final AltcoinBlock block = (AltcoinBlock)serializer.makeBlock(payload);
        assertEquals(2469341065L, block.getNonce());
        final AuxPoW auxpow = block.getAuxPoW();
        assertNull(auxpow);

        assertEquals(6, block.getTransactions().size());
        assertEquals("0e4bcfe8d970979f7e30e2809ab51908d435677998cf759169407824d4f36460", block.getHashAsString());
    }

    /**
     * Confirm parsing of the first merged-mine block.
     */
    @Test
    public void shouldParseBlock371337() throws IOException {
        byte[] payload = Util.getBytes(getClass().getResourceAsStream("dogecoin_block371337.bin"));
        AltcoinSerializer serializer = (AltcoinSerializer)params.getDefaultSerializer();
        final AltcoinBlock block = (AltcoinBlock)serializer.makeBlock(payload);
        assertEquals("60323982f9c5ff1b5a954eac9dc1269352835f47c2c5222691d80f0d50dcf053", block.getHashAsString());
        assertEquals(0, block.getNonce());

        // Check block version values
        assertEquals(2, block.getVersion());
        assertEquals(98, block.getChainID());
        assertTrue(block.getVersionFlags().get(0));

        final AuxPoW auxpow = block.getAuxPoW();
        assertNotNull(auxpow);
        final Transaction auxpowCoinbase = auxpow.getCoinbase();
        assertEquals("e5422732b20e9e7ecc243427abbe296e9528d308bb111aae8d30c3465e442de8", auxpowCoinbase.getTxId().toString());
        final Block parentBlock = auxpow.getParentBlockHeader();
        assertEquals("45df41e40aba5b2a03d08bd1202a1c02ef3954d8aa22ea6c5ae62fd00f290ea9", parentBlock.getHashAsString());
        assertNull(parentBlock.getTransactions());

        final MerkleBranch blockchainMerkleBranch = auxpow.getChainMerkleBranch();
        Sha256Hash[] expected = new Sha256Hash[] {
            Sha256Hash.wrap("b541c848bc001d07d2bdf8643abab61d2c6ae50d5b2495815339a4b30703a46f"),
            Sha256Hash.wrap("78d6abe48cee514cf3496f4042039acb7e27616dcfc5de926ff0d6c7e5987be7"),
            Sha256Hash.wrap("a0469413ce64d67c43902d54ee3a380eff12ded22ca11cbd3842e15d48298103")
        };

        assertArrayEquals(expected, blockchainMerkleBranch.getHashes().toArray(new Sha256Hash[blockchainMerkleBranch.size()]));

        final MerkleBranch coinbaseMerkleBranch = auxpow.getCoinbaseBranch();
        expected = new Sha256Hash[] {
            Sha256Hash.wrap("cd3947cd5a0c26fde01b05a3aa3d7a38717be6ae11d27239365024db36a679a9"),
            Sha256Hash.wrap("48f9e8fef3411944e27f49ec804462c9e124dca0954c71c8560e8a9dd218a452"),
            Sha256Hash.wrap("d11293660392e7c51f69477a6130237c72ecee2d0c1d3dc815841734c370331a")
        };
        assertArrayEquals(expected, coinbaseMerkleBranch.getHashes().toArray(new Sha256Hash[coinbaseMerkleBranch.size()]));

        assertEquals(6, block.getTransactions().size());

        assertTrue(auxpow.checkProofOfWork(block.getHash(), block.getDifficultyTargetAsInteger(), false));
    }

    /**
     * Confirm parsing of block with a nonce value above Integer.MAX_VALUE.
     * See https://github.com/rnicoll/libdohj/pull/7
     */
    @Test
    public void shouldParseBlock748634() throws IOException {
        byte[] payload = Util.getBytes(getClass().getResourceAsStream("dogecoin_block748634.bin"));
        AltcoinSerializer serializer = (AltcoinSerializer)params.getDefaultSerializer();
        final AltcoinBlock block = (AltcoinBlock)serializer.makeBlock(payload);
        assertEquals("bd98a06391115285265c04984e8505229739f6ffa5d498929a91fbe7c281ea7b", block.getHashAsString());
        assertEquals(0, block.getNonce());

        // Check block version values
        assertEquals(2, block.getVersion());
        assertEquals(98, block.getChainID());
        assertTrue(block.getVersionFlags().get(0));

        final AuxPoW auxpow = block.getAuxPoW();
        assertNotNull(auxpow);

        assertTrue(auxpow.checkProofOfWork(block.getHash(), block.getDifficultyTargetAsInteger(), true));
    }

    /**
     * Confirm parsing of block with a nonce value above Integer.MAX_VALUE.
     * See https://github.com/rnicoll/libdohj/issues/5
     */
    @Test
    public void shouldParseBlock894863() throws IOException {
        byte[] payload = Util.getBytes(getClass().getResourceAsStream("dogecoin_block894863.bin"));
        AltcoinSerializer serializer = (AltcoinSerializer)params.getDefaultSerializer();
        final AltcoinBlock block = (AltcoinBlock)serializer.makeBlock(payload);
        assertEquals("93a207e6d227f4d60ee64fad584b47255f654b0b6378d78e774123dd66f4fef9", block.getHashAsString());
        assertEquals(0, block.getNonce());

        // Check block version values
        assertEquals(2, block.getVersion());
        assertEquals(98, block.getChainID());
        assertTrue(block.getVersionFlags().get(0));

        final AuxPoW auxpow = block.getAuxPoW();
        assertNotNull(auxpow);
        final Transaction auxpowCoinbase = auxpow.getCoinbase();
        assertEquals("c84431cf41f592373cc70db07f6804f945202f5f7baad31a8bbab89aaecb7b8b", auxpowCoinbase.getHashAsString());

        assertTrue(auxpow.checkProofOfWork(block.getHash(), block.getDifficultyTargetAsInteger(), true));
    }

    /**
     * Confirm that checking proof of work on an AuxPoW block works. 
     */
    @Test
    public void shouldCheckAuxPoWProofOfWork() throws IOException {
        byte[] payload = Util.getBytes(getClass().getResourceAsStream("dogecoin_block371337.bin"));
        AltcoinSerializer serializer = (AltcoinSerializer)params.getDefaultSerializer();
        final AltcoinBlock block = (AltcoinBlock)serializer.makeBlock(payload);
        assertTrue(block.checkProofOfWork(true));
    }
}
