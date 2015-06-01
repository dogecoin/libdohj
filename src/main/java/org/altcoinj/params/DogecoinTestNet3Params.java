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

package org.altcoinj.params;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Utils;
import org.spongycastle.util.encoders.Hex;

import static com.google.common.base.Preconditions.checkState;

/**
 * Parameters for the testnet, a separate public instance of Bitcoin that has relaxed rules suitable for development
 * and testing of applications and new Bitcoin versions.
 */
public class DogecoinTestNet3Params extends AbstractDogecoinParams {
    protected static final int DIFFICULTY_CHANGE_TARGET = 145000;

    public DogecoinTestNet3Params() {
        super(DIFFICULTY_CHANGE_TARGET);
        id = ID_DOGE_TESTNET;
        // Genesis hash is bb0a78264637406b6360aad926284d544d7049f45189db5664f3c4d07350559e
        packetMagic = 0xfcc1b7dc;

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

        dnsSeeds = new String[] {
            "testnets.chain.so"  // Chain.so
        };
        bip32HeaderPub = 0x043587CF;
        bip32HeaderPriv = 0x04358394;
    }

    private static DogecoinTestNet3Params instance;
    public static synchronized DogecoinTestNet3Params get() {
        if (instance == null) {
            instance = new DogecoinTestNet3Params();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        // TODO: CHANGE ME
        return PAYMENT_PROTOCOL_ID_TESTNET;
    }

    @Override
    public boolean isTestNet() {
        return true;
    }
}
