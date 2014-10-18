/*
 * Copyright 2013 Google Inc.
 * Copyright 2014 Andreas Schildbach
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
import com.dogecoin.dogecoinj.core.Utils;
import org.spongycastle.util.encoders.Hex;

import static com.google.common.base.Preconditions.checkState;

/**
 * Parameters for the testnet, a separate public instance of Bitcoin that has relaxed rules suitable for development
 * and testing of applications and new Bitcoin versions.
 */
public class TestNet3Params extends NetworkParameters {
    public TestNet3Params() {
        super();
        id = ID_TESTNET;
        // Genesis hash is bb0a78264637406b6360aad926284d544d7049f45189db5664f3c4d07350559e
        packetMagic = 0xfcc1b7dc;
        interval = INTERVAL;
        newInterval = INTERVAL_NEW;
        targetTimespan = TARGET_TIMESPAN;
        newTargetTimespan = TARGET_TIMESPAN_NEW;
        maxTarget = Utils.decodeCompactBits(0x1e0fffffL);
        port = 44556;
        addressHeader = 113;
        p2shHeader = 196;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        dumpedPrivateKeyHeader = 241;
        genesisBlock.setTime(1391503289L);
        genesisBlock.setDifficultyTarget(0x1e0ffff0L);
        genesisBlock.setNonce(997879);
        spendableCoinbaseDepth = 30;
        subsidyDecreaseBlockCount = 100000;
        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("bb0a78264637406b6360aad926284d544d7049f45189db5664f3c4d07350559e"));
        alertSigningKey = Hex.decode("042756726da3c7ef515d89212ee1705023d14be389e25fe15611585661b9a20021908b2b80a3c7200a0139dd2b26946606aab0eef9aa7689a6dc2c7eee237fa834");

        diffChangeTarget = 145000;

        dnsSeeds = new String[] {
                "testnet-seed.alexykot.me",           // Alex Kotenko
                "testnet-seed.bitcoin.schildbach.de", // Andreas Schildbach
                "testnet-seed.bitcoin.petertodd.org"  // Peter Todd
        };
    }

    private static TestNet3Params instance;
    public static synchronized TestNet3Params get() {
        if (instance == null) {
            instance = new TestNet3Params();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_TESTNET;
    }
}
