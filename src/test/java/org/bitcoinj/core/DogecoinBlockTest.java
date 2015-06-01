/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitcoinj.core;

import org.altcoinj.core.AltcoinSerializer;
import java.io.IOException;
import org.altcoinj.params.DogecoinMainNetParams;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author jrn
 */
public class DogecoinBlockTest {
    private final NetworkParameters params = DogecoinMainNetParams.get();

    @Before
    public void setUp() throws Exception {
        Context context = new Context(params);
    }

    @Test
    public void shouldParseBlock1() throws IOException {
        byte[] payload = Util.getBytes(getClass().getResourceAsStream("dogecoin_block1.bin"));
        AltcoinSerializer serializer = (AltcoinSerializer)params.getDefaultSerializer();
        final AltcoinBlock block = (AltcoinBlock)serializer.makeBlock(payload);
        assertEquals("82bc68038f6034c0596b6e313729793a887fded6e92a31fbdf70863f89d9bea2", block.getHashAsString());
        assertEquals(1, block.getTransactions().size());
    }

    /**
     * Test the first hardfork block.
     * @throws IOException 
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
     * 
     * @throws IOException 
     */
    @Test
    public void shouldParseBlock371337() throws IOException {
        byte[] payload = Util.getBytes(getClass().getResourceAsStream("dogecoin_block371337.bin"));
        AltcoinSerializer serializer = (AltcoinSerializer)params.getDefaultSerializer();
        final AltcoinBlock block = (AltcoinBlock)serializer.makeBlock(payload);
        assertEquals("60323982f9c5ff1b5a954eac9dc1269352835f47c2c5222691d80f0d50dcf053", block.getHashAsString());
        assertEquals(0, block.getNonce());
        final AuxPoW auxpow = block.getAuxPoW();
        assertNotNull(auxpow);
        final Transaction auxpowCoinbase = auxpow.getCoinbase();
        assertEquals("e5422732b20e9e7ecc243427abbe296e9528d308bb111aae8d30c3465e442de8", auxpowCoinbase.getHashAsString());
        final Block parentBlock = auxpow.getParentBlockHeader();
        assertEquals("45df41e40aba5b2a03d08bd1202a1c02ef3954d8aa22ea6c5ae62fd00f290ea9", parentBlock.getHashAsString());
        assertNull(parentBlock.getTransactions());

        final MerkleBranch blockchainMerkleBranch = auxpow.getChainMerkleBranch();
        Sha256Hash[] expected = new Sha256Hash[] {
            new Sha256Hash("b541c848bc001d07d2bdf8643abab61d2c6ae50d5b2495815339a4b30703a46f"),
            new Sha256Hash("78d6abe48cee514cf3496f4042039acb7e27616dcfc5de926ff0d6c7e5987be7"),
            new Sha256Hash("a0469413ce64d67c43902d54ee3a380eff12ded22ca11cbd3842e15d48298103")
        };

        assertArrayEquals(expected, blockchainMerkleBranch.getHashes().toArray(new Sha256Hash[blockchainMerkleBranch.size()]));

        final MerkleBranch coinbaseMerkleBranch = auxpow.getCoinbaseBranch();
        expected = new Sha256Hash[] {
            new Sha256Hash("cd3947cd5a0c26fde01b05a3aa3d7a38717be6ae11d27239365024db36a679a9"),
            new Sha256Hash("48f9e8fef3411944e27f49ec804462c9e124dca0954c71c8560e8a9dd218a452"),
            new Sha256Hash("d11293660392e7c51f69477a6130237c72ecee2d0c1d3dc815841734c370331a")
        };
        assertArrayEquals(expected, coinbaseMerkleBranch.getHashes().toArray(new Sha256Hash[coinbaseMerkleBranch.size()]));

        assertEquals(6, block.getTransactions().size());
    }
}
