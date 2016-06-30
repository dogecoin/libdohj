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
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.store.BlockStore;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

// This lookup client only downloads a single transaction from the API rather than a history.
// This means that it's usually faster, but the API has to be careful to choose the correct transaction.
// As of writing (2016 Jun 26), webbtc does *not* always make the correct choice.
// That means that using this lookup client will result in an incorrect "nonexistent" result
// if the latest name_update for the targeted name has a depth between 1 and 11 (inclusive).
// I'm engaging with Marius from webbtc and hope to have a solution soon.
// -- Jeremy

public class NameLookupLatestRestMerkleApiSingleTx extends NameLookupLatestRestMerkleApi {

    public NameLookupLatestRestMerkleApiSingleTx (NetworkParameters params, String restUrlPrefix, String restUrlSuffix, BlockChain chain, BlockStore store, NameLookupByBlockHeightHashCache heightLookup) {
        super(params, restUrlPrefix, restUrlSuffix, chain, store, heightLookup);
    }

    @Override
    protected ArrayList<NameData> getUntrustedNameHistory(String name) throws Exception {
        URL nameUrl = new URL(restUrlPrefix + name + restUrlSuffix);

        ObjectMapper mapper = new ObjectMapper();
        
        NameData[] untrustedNameSingleEntry = {mapper.readValue(nameUrl, NameData.class)};
        ArrayList<NameData> untrustedNameHistory = new ArrayList<NameData>(Arrays.asList(untrustedNameSingleEntry));
        
        return untrustedNameHistory;
    }

}
