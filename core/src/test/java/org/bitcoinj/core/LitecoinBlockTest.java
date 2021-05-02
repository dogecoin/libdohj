package org.bitcoinj.core;

import org.junit.Before;
import org.junit.Test;
import org.libdohj.core.AltcoinSerializer;
import org.libdohj.params.LitecoinMainNetParams;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LitecoinBlockTest {
    private static final NetworkParameters params = LitecoinMainNetParams.get();

    @Before
    public void setUp() throws Exception {
        Context context = new Context(params);
    }

    @Test
    public void shouldParseBlock1() throws IOException {
        byte[] payload = Util.getBytes(getClass().getResourceAsStream("litecoin_block1.bin"));
        AltcoinSerializer serializer = (AltcoinSerializer)params.getDefaultSerializer();
        final AltcoinBlock block = (AltcoinBlock)serializer.makeBlock(payload);
        assertEquals("80ca095ed10b02e53d769eb6eaf92cd04e9e0759e5be4a8477b42911ba49c78f", block.getHashAsString());
        assertEquals(params.getGenesisBlock().getHash(), block.getPrevBlockHash());
        assertEquals(1, block.getTransactions().size());
        assertEquals(0x1e0ffff0L, block.getDifficultyTarget());
        assertTrue(block.checkProofOfWork(false));
    }
}
