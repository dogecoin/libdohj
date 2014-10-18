package com.dogecoin.dogecoinj.core;

import com.dogecoin.dogecoinj.store.BlockStoreException;
import com.dogecoin.dogecoinj.store.FullPrunedBlockStore;
import com.dogecoin.dogecoinj.store.MemoryFullPrunedBlockStore;

/**
 * A MemoryStore implementation of the FullPrunedBlockStoreTest
 */
public class MemoryFullPrunedBlockChainTest extends AbstractFullPrunedBlockChainTest
{
    @Override
    public FullPrunedBlockStore createStore(NetworkParameters params, int blockCount) throws BlockStoreException
    {
        return new MemoryFullPrunedBlockStore(params, blockCount);
    }

    @Override
    public void resetStore(FullPrunedBlockStore store) throws BlockStoreException
    {
        //No-op for memory store, because it's not persistent
    }
}
