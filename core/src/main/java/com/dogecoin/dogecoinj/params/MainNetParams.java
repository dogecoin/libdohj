/*
 * Copyright 2013 Google Inc.
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

package com.dogecoin.dogecoinj.params;

import com.dogecoin.dogecoinj.core.NetworkParameters;
import com.dogecoin.dogecoinj.core.Sha256Hash;
import com.dogecoin.dogecoinj.core.Utils;

import static com.google.common.base.Preconditions.checkState;

/**
 * Parameters for the main production network on which people trade goods and services.
 */
public class MainNetParams extends NetworkParameters {
    public MainNetParams() {
        super();
        interval = INTERVAL;
        newInterval = INTERVAL_NEW;
        targetTimespan = TARGET_TIMESPAN;
        newTargetTimespan = TARGET_TIMESPAN_NEW;
        maxTarget = Utils.decodeCompactBits(0x1e0fffffL);
        dumpedPrivateKeyHeader = 158; //This is always addressHeader + 128
        addressHeader = 30;
        p2shHeader = 22;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        port = 22556;
        packetMagic = 0xc0c0c0c0;
        genesisBlock.setDifficultyTarget(0x1e0ffff0L);
        genesisBlock.setTime(1386325540L);
        genesisBlock.setNonce(99943L);
        id = ID_MAINNET;
        subsidyDecreaseBlockCount = 100000;
        spendableCoinbaseDepth = 100;

        diffChangeTarget = 145000;

        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("1a91e3dace36e2be3bf030a65679fe821aa1d6ef92e7c9902eb318182c355691"),
                genesisHash);

        // This contains (at a minimum) the blocks which are not BIP30 compliant. BIP30 changed how duplicate
        // transactions are handled. Duplicated transactions could occur in the case where a coinbase had the same
        // extraNonce and the same outputs but appeared at different heights, and greatly complicated re-org handling.
        // Having these here simplifies block connection logic considerably.
        checkpoints.put(    0, new Sha256Hash("1a91e3dace36e2be3bf030a65679fe821aa1d6ef92e7c9902eb318182c355691"));
        checkpoints.put( 42279, new Sha256Hash("8444c3ef39a46222e87584ef956ad2c9ef401578bd8b51e8e4b9a86ec3134d3a"));
        checkpoints.put( 42400, new Sha256Hash("557bb7c17ed9e6d4a6f9361cfddf7c1fc0bdc394af7019167442b41f507252b4"));
        checkpoints.put(104679, new Sha256Hash("35eb87ae90d44b98898fec8c39577b76cb1eb08e1261cfc10706c8ce9a1d01cf"));
        checkpoints.put(128370, new Sha256Hash("3f9265c94cab7dc3bd6a2ad2fb26c8845cb41cff437e0a75ae006997b4974be6"));
        checkpoints.put(145000, new Sha256Hash("cc47cae70d7c5c92828d3214a266331dde59087d4a39071fa76ddfff9b7bde72"));
        checkpoints.put(165393, new Sha256Hash("7154efb4009e18c1c6a6a79fc6015f48502bcd0a1edd9c20e44cd7cbbe2eeef1"));
        checkpoints.put(186774, new Sha256Hash("3c712c49b34a5f34d4b963750d6ba02b73e8a938d2ee415dcda141d89f5cb23a"));
        checkpoints.put(199992, new Sha256Hash("3408ff829b7104eebaf61fd2ba2203ef2a43af38b95b353e992ef48f00ebb190"));
        checkpoints.put(225000, new Sha256Hash("be148d9c5eab4a33392a6367198796784479720d06bfdd07bd547fe934eea15a"));
        checkpoints.put(250000, new Sha256Hash("0e4bcfe8d970979f7e30e2809ab51908d435677998cf759169407824d4f36460"));
        checkpoints.put(270639, new Sha256Hash("c587a36dd4f60725b9dd01d99694799bef111fc584d659f6756ab06d2a90d911"));
        checkpoints.put(299742, new Sha256Hash("1cc89c0c8a58046bf0222fe131c099852bd9af25a80e07922918ef5fb39d6742"));
        checkpoints.put(323141, new Sha256Hash("60c9f919f9b271add6ef5671e9538bad296d79f7fdc6487ba702bf2ba131d31d"));
        checkpoints.put(339202, new Sha256Hash("8c29048df5ae9df38a67ea9470fdd404d281a3a5c6f33080cd5bf14aa496ab03"));

        dnsSeeds = new String[] {
                "seed.dogecoin.com",
                "seed.mophides.com",
                "seed.dogechain.info",
        };
    }

    private static MainNetParams instance;
    public static synchronized MainNetParams get() {
        if (instance == null) {
            instance = new MainNetParams();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_MAINNET;
    }
}
