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
import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ross Nicoll
 */
public class LitecoinMainNetParamsTest {
    private static final LitecoinMainNetParams params = LitecoinMainNetParams.get();
    private static final BlockLoader loader = new BlockLoader(params);
    public static final String GENESIS_BLOCK_HASH = "12a765e31ffd4059bada1e25190f6e98c99d9714d334efa41a195a7e7e04bfe2";
    public static final String BLOCK_1_HASH = "80ca095ed10b02e53d769eb6eaf92cd04e9e0759e5be4a8477b42911ba49c78f";

    @Before
    public void setUp() throws Exception {
        Context context = new Context(params);
    }

    @Test
    public void shouldCloneAsHeader() {
        final Block genesis = params.getGenesisBlock();
        final Block header = genesis.cloneAsHeader();
        final Sha256Hash expected = genesis.getHash();
        final Sha256Hash actual = header.getHash();
        assertEquals(genesis.getVersion(), header.getVersion());
        assertEquals(genesis.getPrevBlockHash(), header.getPrevBlockHash());
        assertEquals(genesis.getMerkleRoot(), header.getMerkleRoot());
        assertEquals(genesis.getTime(), header.getTime());
        assertEquals(genesis.getDifficultyTarget(), header.getDifficultyTarget());
        assertEquals(genesis.getNonce(), header.getNonce());

        assertEquals(expected, actual);
    }

    @Test
    public void shouldHaveCorrectGenesisBlock() {
        final Block genesis = params.getGenesisBlock();
        final Sha256Hash expected = Sha256Hash.wrap(GENESIS_BLOCK_HASH);
        final Sha256Hash actual = genesis.getHash();
        assertEquals(expected, actual);
    }

    @Test
    public void shouldConnectBlock1() throws BlockStoreException, PrunedException, IOException {
        final BlockStore store = new MemoryBlockStore(params);
        final Wallet wallet = Wallet.createDeterministic(params, Script.ScriptType.P2PKH);
        final StoredBlock storedGenesis = new StoredBlock(params.getGenesisBlock(), params.genesisBlock.getWork(), 0);
        store.put(storedGenesis);

        // Verify we actually saved the genesis block correctly
        assertEquals(params.getGenesisBlock().cloneAsHeader(), store.get(Sha256Hash.wrap(GENESIS_BLOCK_HASH)).getHeader());

        final String[][] blocks = {
                {"litecoin_block1.bin", BLOCK_1_HASH},
        };
        final Map<String, AltcoinBlock> loadedBlocks = loader.loadBlocks(blocks);
        final BlockChain blockChain = new BlockChain(params, wallet, store);
        final Block block1 = Objects.requireNonNull(loadedBlocks.get(BLOCK_1_HASH));

        assertEquals(Sha256Hash.wrap(GENESIS_BLOCK_HASH), block1.getPrevBlockHash());
        assertTrue(blockChain.add(params.getGenesisBlock()));
        assertTrue(blockChain.add(block1));
    }
}
