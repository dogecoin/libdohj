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
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.MerkleBranch;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Utils;
import org.bitcoinj.store.BlockStore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

// TODO: document this

public class NameLookupLatestRestMerkleApi implements NameLookupLatest {
    
    protected NetworkParameters params;
    protected BlockChain chain;
    protected BlockStore store;
    protected NameLookupByBlockHeightHashCache heightLookup; // only needed for the hash cache
    protected String restUrlPrefix;
    protected String restUrlSuffix;
    
    // TODO: break out the hash cache into its own class so that we don't need the NameLookup features.
    public NameLookupLatestRestMerkleApi (NetworkParameters params, String restUrlPrefix, String restUrlSuffix, BlockChain chain, BlockStore store, NameLookupByBlockHeightHashCache heightLookup) {
        this.params = params;
        this.restUrlPrefix = restUrlPrefix;
        this.restUrlSuffix = restUrlSuffix;
        this.chain = chain;
        this.store = store;
        this.heightLookup = heightLookup;
    }
    
    // TODO: make a new Exception class
    @Override
    public Transaction getNameTransaction(String name, String identity) throws Exception {
        
        NameData data = getLatestUntrustedNameData(name);
        
        Sha256Hash blockHash = heightLookup.getBlockHash(data.height);
        
        Block blockHeader = store.get(blockHash).getHeader();
        
        // Convert merkle hashes from String to Sha256Hash
        ArrayList<Sha256Hash> merkleHashes = new ArrayList<Sha256Hash>(data.mrkl_branch.size());
        for (String merkleHashString : data.mrkl_branch) {
            merkleHashes.add(Sha256Hash.wrap(merkleHashString));
        }
        
        long merkleBranchSideMask = data.tx_idx;
        
        MerkleBranch branch = new MerkleBranch(params, null, merkleHashes, merkleBranchSideMask);
        
        Transaction tx = new Transaction(params, Utils.HEX.decode(data.rawtx));
        
        Sha256Hash txId = tx.getHash();
        
        if(! blockHeader.getMerkleRoot().equals(branch.calculateMerkleRoot(txId))) {
            throw new Exception("Merkle proof failed to verify!");
        }
        
        tx.getConfidence().setAppearedAtChainHeight(data.height); // TODO: test this line
        tx.getConfidence().setDepthInBlocks(chain.getChainHead().getHeight() - data.height + 1);
        
        if (NameTransactionUtils.getNameAnyUpdateOutput(tx, name) == null) {
            throw new Exception("Not a name_anyupdate transaction or wrong name!");
        }
        
        return tx;
        
    }
    
    // TODO: break out the getHeight into its own class + interface
    // TODO: add identity isolation
    // TODO: use an older height if the newest height has insufficient confirmations, instead of throwing an Exception
    // NOTE: this might fail if special characters are in the name, since it's not URL-escaping them.
    public NameData getLatestUntrustedNameData(String name) throws Exception {
        ArrayList<NameData> untrustedNameHistory = getUntrustedNameHistory(name);
        
        int height;
        
        int index;
        
        for (index = untrustedNameHistory.size() - 1; index >= 0; index--) {
            
            NameData candidate = untrustedNameHistory.get(index);
            try {
                verifyHeightTrustworthy(candidate.height);
                return candidate;
            }
            catch (Exception e) {
                continue;
            }
        }
        
        throw new Exception("Height not trustworthy or name does not exist.");
    }
    
    // TODO: add identity isolation
    protected ArrayList<NameData> getUntrustedNameHistory(String name) throws Exception {
        URL nameUrl = new URL(restUrlPrefix + name + restUrlSuffix);
        
        ObjectMapper mapper = new ObjectMapper();
        
        ArrayList<NameData> untrustedNameHistory = new ArrayList<NameData>(Arrays.asList(mapper.readValue(nameUrl, NameData[].class)));
        
        return untrustedNameHistory;
    }
    
    protected void verifyHeightTrustworthy(int height) throws Exception {
        if (height < 1) {
            throw new Exception("Nonpositive block height; not trustworthy!");
        }
        
        int headHeight = chain.getChainHead().getHeight();
        
        int confirmations = headHeight - height + 1;
        
        // TODO: optionally use transaction chains (with signature checks) to verify transactions without 12 confirmations
        // TODO: the above needs to be optional, because some applications (e.g. cert transparency) require confirmations
        if (confirmations < 12) {
            throw new Exception("Block does not yet have 12 confirmations; not trustworthy!");
        }
        
        // TODO: check for off-by-one errors on this line
        if (confirmations >= 36000) {
            throw new Exception("Block has expired; not trustworthy!");
        }
    }
    
    // TODO: break this out into its own class; add the extra fields to bitcoinj-addons too
    static protected class NameData {

        public          String name;
        public          String value;
        public          String txid;
        public          String address;
        public          int expires_in;
        public          int height;
        public          long tx_idx;
        public          ArrayList<String> mrkl_branch;
        public          String rawtx;

        @JsonCreator
        public NameData(@JsonProperty("name")       String name,
                       @JsonProperty("value")       String value,
                       @JsonProperty("txid")        String txid,
                       @JsonProperty("address")     String address,
                       @JsonProperty("expires_in")  int expires_in,
                       @JsonProperty("height")      int height,
                       @JsonProperty("tx_idx")      long tx_idx,
                       @JsonProperty("mrkl_branch") ArrayList<String> mrkl_branch,
                       @JsonProperty("rawtx")       String rawtx) {
            this.name = name;
            this.value = value;
            this.txid = txid;
            this.address = address;
            this.expires_in = expires_in;
            this.height = height;
            this.tx_idx = tx_idx;
            this.mrkl_branch = mrkl_branch;
            this.rawtx = rawtx;
        }
    }
    
}
