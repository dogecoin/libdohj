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

import org.bitcoinj.core.Block;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;

import java.util.EnumSet;

// TODO: document this

public class NameLookupByBlockHashOneFullBlock implements NameLookupByBlockHash {
    
    protected PeerGroup peerGroup;
    
    public NameLookupByBlockHashOneFullBlock (PeerGroup peerGroup) {
        this.peerGroup = peerGroup;
    }
    
    @Override
    public Transaction getNameTransaction(String name, Sha256Hash blockHash, String identity) throws Exception {
        
        Block nameFullBlock = peerGroup.getDownloadPeer().getBlock(blockHash).get();
        
        // The full block hasn't been verified in any way!
        // So let's do that now.
        
        if (! nameFullBlock.getHash().equals(blockHash)) {
            throw new Exception("Block hash mismatch!");
        }
        
        // Now we know that the received block actually does have a header that matches the hash that we requested.
        // However, that doesn't mean that the block's contents are valid.
        
        final EnumSet<Block.VerifyFlag> flags = EnumSet.noneOf(Block.VerifyFlag.class);
        nameFullBlock.verify(-1, flags);
        
        // Now we know that the block is internally valid (including the merkle root).
        // We haven't verified signature validity, but our threat model is SPV.
        
        for (Transaction tx : nameFullBlock.getTransactions()) {
            if (NameTransactionUtils.getNameAnyUpdateOutput(tx, name) != null) {
                return tx;
            }
        }
        
        // The name wasn't found.
        return null;
    }
    
}
