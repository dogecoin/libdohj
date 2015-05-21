package org.bitcoinj.core;

import java.io.ByteArrayOutputStream;

import org.bitcoinj.params.TestNet3Params;
import org.junit.Before;
import org.junit.Test;

import static org.bitcoinj.core.CoinbaseBlockTest.getBytes;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Check merkle branch parsing and root calculation.
 */
public class MerkleBranchTest {
    static final NetworkParameters params = TestNet3Params.get();

    /**
     * Parse the coinbase merkle branch from Dogecoin block #403,931.
     */
    @Test
    public void parseMerkleBranch() throws Exception {
        byte[] branchAsBytes = getBytes(getClass().getResourceAsStream("auxpow_merkle_branch.bin"));
        MerkleBranch branch = new MerkleBranch(params, (ChildMessage) null, branchAsBytes, 0);
        Sha256Hash[] expected = new Sha256Hash[] {
            new Sha256Hash("be079078869399faccaa764c10e9df6e9981701759ad18e13724d9ca58831348"),
            new Sha256Hash("5f5bfb2c79541778499cab956a103887147f2ab5d4a717f32f9eeebd29e1f894"),
            new Sha256Hash("d8c6fe42ca25076159cd121a5e20c48c1bc53ab90730083e44a334566ea6bbcb")
        };

        assertArrayEquals(expected, branch.getHashes().toArray(new Sha256Hash[branch.getSize()]));
    }

    /**
     * Parse the transaction merkle branch from Dogecoin block #403,931, then
     * serialize it back again to verify serialization works.
     */
    @Test
    public void serializeMerkleBranch() throws Exception {
        byte[] expected = getBytes(getClass().getResourceAsStream("auxpow_merkle_branch.bin"));
        MerkleBranch branch = new MerkleBranch(params, (ChildMessage) null, expected, 0,
            false, false);
        byte[] actual = branch.bitcoinSerialize();

        assertArrayEquals(expected, actual);
    }

    /**
     * Calculate the AuxPoW merkle branch root from Dogecoin block #403,931.
     */
    @Test
    public void calculateRootBranch() throws Exception {
        byte[] branchAsBytes = getBytes(getClass().getResourceAsStream("auxpow_merkle_branch2.bin"));
        MerkleBranch branch = new MerkleBranch(params, (ChildMessage) null, branchAsBytes, 0);
        Sha256Hash txId = new Sha256Hash("0c836b86991631d34a8a68054e2f62db919b39d1ee43c27ab3344d6aa82fa609");
        Sha256Hash expected = new Sha256Hash("ce3040fdb7e37484f6a1ca4f8f5da81e6b7e404ec91102315a233e03a0c39c95");

        assertEquals(expected, branch.calculateMerkleRoot(txId));
    }
}
