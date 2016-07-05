/*
 * Copyright 2016 Jeremy Rand.
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

package org.libdohj.names;

import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;

import java.util.concurrent.ConcurrentHashMap;

// TODO: breakout the 36000 expiration time into NetworkParameters.

// TODO: breakout the hash cache into its own class

// TODO: update blockHashCache with new blocks as they come into the chain

// TODO: document this

public class NameLookupByBlockHeightHashCache implements NameLookupByBlockHeight {
    
    protected BlockChain chain;
    protected BlockStore store;
    
    protected NameLookupByBlockHash hashLookup;
    
    protected ConcurrentHashMap<Integer, Sha256Hash> blockHashCache;
    
    public NameLookupByBlockHeightHashCache (BlockChain chain, NameLookupByBlockHash hashLookup) throws Exception {
        this.chain = chain;
        this.store = chain.getBlockStore();
        
        this.hashLookup = hashLookup;
        
        initBlockHashCache();
    }
    
    protected void initBlockHashCache() throws BlockStoreException {
        blockHashCache = new ConcurrentHashMap<Integer, Sha256Hash>(72000);
        
        StoredBlock blockPointer = chain.getChainHead();
        
        int headHeight = blockPointer.getHeight();
        int reorgSafety = 120;
        int newestHeight = headHeight - reorgSafety;
        int oldestHeight = headHeight - 36000 - reorgSafety; // 36000 = name expiration
        
        while (blockPointer.getHeight() >= oldestHeight) {
            
            if (blockPointer.getHeight() <= newestHeight) {
                blockHashCache.put(new Integer(blockPointer.getHeight()), blockPointer.getHeader().getHash());
            }
        
            blockPointer = blockPointer.getPrev(store);
        }
    }
    
    @Override
    public Transaction getNameTransaction(String name, int height, String identity) throws Exception {
        
        Sha256Hash blockHash = getBlockHash(height);
        
        Transaction tx = hashLookup.getNameTransaction(name, blockHash, identity);
        
        tx.getConfidence().setAppearedAtChainHeight(height); // TODO: test this line
        tx.getConfidence().setDepthInBlocks(chain.getChainHead().getHeight() - height + 1);
        
        return tx;
        
    }
    
    public Sha256Hash getBlockHash(int height) throws BlockStoreException {
        Sha256Hash maybeResult = blockHashCache.get(new Integer(height));
        
        if (maybeResult != null) {
            return maybeResult;
        }
        
        // If we got this far, the block height is uncached.
        // This could be because the block is immature, 
        // or it could be because the cache is only initialized on initial startup.
        
        StoredBlock blockPointer = chain.getChainHead();
        
        while (blockPointer.getHeight() != height) {
            blockPointer = blockPointer.getPrev(store);
        }
        
        return blockPointer.getHeader().getHash();
    }
    
}
