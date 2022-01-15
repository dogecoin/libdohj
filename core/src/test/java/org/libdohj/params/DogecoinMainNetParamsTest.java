/*
 * Copyright 2015, 2021 J. Ross Nicoll
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
package org.libdohj.params;

import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.MemoryBlockStore;
import org.bitcoinj.wallet.Wallet;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Ross Nicoll
 */
public class DogecoinMainNetParamsTest {
    private static final DogecoinMainNetParams params = DogecoinMainNetParams.get();
    private static final BlockLoader loader = new BlockLoader(params);
    private static final String BLOCK_240_HASH = "3752567a4c6085970f5b726feee3b8fc0f37ca95bb2a8daf497683b5168ec8d1";

    @Before
    public void setUp() throws Exception {
        Context context = new Context(params);
    }

    @Test
    public void shouldHaveCorrectGenesisBlock() {
        final Block genesis = params.getGenesisBlock();
        final Sha256Hash expected = Sha256Hash.wrap("1a91e3dace36e2be3bf030a65679fe821aa1d6ef92e7c9902eb318182c355691");
        final Sha256Hash actual = genesis.getHash();
        assertEquals(expected, actual);
    }

    /**
     * This connects the first n blocks - this is more of a functional test than the single block
     * test, and should handle cases such as difficulty recalculation.
     */
    @Test
    public void shouldConnectBlocks() throws BlockStoreException, PrunedException, IOException {
        final BlockStore store = new MemoryBlockStore(params);
        final Wallet wallet = Wallet.createDeterministic(params, Script.ScriptType.P2PKH);
        final StoredBlock storedGenesis = new StoredBlock(params.getGenesisBlock(), params.genesisBlock.getWork(), 0);
        store.put(storedGenesis);
        final Map<String, AltcoinBlock> loadedBlocks = loader.loadAllHeaders("dogecoin_block1-240.bin", 80);
        final BlockChain blockChain = new BlockChain(params, wallet, store);
        assertTrue(blockChain.add(params.getGenesisBlock()));
        for (Block block: loadedBlocks.values()) {
            assertTrue(blockChain.add(block));
        }
        assertEquals(BLOCK_240_HASH, blockChain.getChainHead().getHeader().getHashAsString());
    }
}
