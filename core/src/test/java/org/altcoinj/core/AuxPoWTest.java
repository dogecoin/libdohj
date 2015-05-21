package org.bitcoinj.core;

import java.io.ByteArrayOutputStream;

import org.bitcoinj.params.TestNet3Params;
import org.junit.Before;
import org.junit.Test;

import static org.bitcoinj.core.CoinbaseBlockTest.getBytes;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * AuxPoW header parsing/serialization and validation
 */
public class AuxPoWTest {
    static final NetworkParameters params = TestNet3Params.get();

    /**
     * Parse the AuxPoW header from Dogecoin block #403,931.
     */
    @Test
    public void parseAuxPoWHeader() throws Exception {
        byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, (ChildMessage) null, false, false);
        MerkleBranch branch = auxpow.getCoinbaseBranch();
        Sha256Hash expected = new Sha256Hash("089b911f5e471c0e1800f3384281ebec5b372fbb6f358790a92747ade271ccdf");

        assertEquals(expected, auxpow.getCoinbase().getHash());
        assertEquals(3, auxpow.getCoinbaseBranch().getSize());
        assertEquals(6, auxpow.getBlockchainBranch().getSize());

        expected = new Sha256Hash("a22a9b01671d639fa6389f62ecf8ce69204c8ed41d5f1a745e0c5ba7116d5b4c");
        assertEquals(expected, auxpow.getParentBlockHeader().getHash());
    }

    /**
     * Test serializing the AuxPoW header from Dogecoin block #403,931.
     */
    @Test
    public void serializeAuxPoWHeader() throws Exception {
        byte[] auxpowAsBytes = getBytes(getClass().getResourceAsStream("auxpow_header.bin"));
        AuxPoW auxpow = new AuxPoW(params, auxpowAsBytes, (ChildMessage) null, false, false);
        byte[] expected = auxpowAsBytes;
        byte[] actual = auxpow.bitcoinSerialize();

        assertArrayEquals(expected, actual);
    }
}
