/*
 * Copyright 2013 Google Inc, 2016 Jeremy Rand.
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

import org.bitcoinj.core.Block;
import org.bitcoinj.core.Sha256Hash;
import org.bouncycastle.util.encoders.Hex;

import static com.google.common.base.Preconditions.checkState;

// TODO: review this

/**
 * Parameters for the main Namecoin production network on which people trade
 * goods and services.
 */
public class NamecoinMainNetParams extends AbstractNamecoinParams {
    public static final int MAINNET_MAJORITY_WINDOW = 1000;
    public static final int MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED = 950;
    public static final int MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE = 750;

    public NamecoinMainNetParams() {
        super();
        dumpedPrivateKeyHeader = 180; //This is always addressHeader + 128
        addressHeader = 52;
        p2shHeader = 13;
        port = 8334;
        packetMagic = 0xf9beb4fe;
        
        genesisBlock.setDifficultyTarget(0x1C007FFFL);
        genesisBlock.setTime(1303000001L);
        genesisBlock.setNonce(2719916434L);
        id = ID_NMC_MAINNET;
        subsidyDecreaseBlockCount = 210000;
        spendableCoinbaseDepth = 100;
        auxpowStartHeight = 19200;

        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("000000000062b72c5e2ceb45fbc8587e807c155b0da735e6483dfba2f0a9c770"),
                genesisHash);

        majorityEnforceBlockUpgrade = MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE;
        majorityRejectBlockOutdated = MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED;
        majorityWindow = MAINNET_MAJORITY_WINDOW;

        // TODO: check whether there are any non BIP30 blocks in Namecoin; add them here if they exist
        // This contains (at a minimum) the blocks which are not BIP30 compliant. BIP30 changed how duplicate
        // transactions are handled. Duplicated transactions could occur in the case where a coinbase had the same
        // extraNonce and the same outputs but appeared at different heights, and greatly complicated re-org handling.
        // Having these here simplifies block connection logic considerably.
        checkpoints.put(  2016, Sha256Hash.wrap("0000000000660bad0d9fbde55ba7ee14ddf766ed5f527e3fbca523ac11460b92"));
        checkpoints.put(  4032, Sha256Hash.wrap("0000000000493b5696ad482deb79da835fe2385304b841beef1938655ddbc411"));
        checkpoints.put(  6048, Sha256Hash.wrap("000000000027939a2e1d8bb63f36c47da858e56d570f143e67e85068943470c9"));
        checkpoints.put(  8064, Sha256Hash.wrap("000000000003a01f708da7396e54d081701ea406ed163e519589717d8b7c95a5"));
        checkpoints.put( 10080, Sha256Hash.wrap("00000000000fed3899f818b2228b4f01b9a0a7eeee907abd172852df71c64b06"));
        checkpoints.put( 12096, Sha256Hash.wrap("0000000000006c06988ff361f124314f9f4bb45b6997d90a7ee4cedf434c670f"));
        checkpoints.put( 14112, Sha256Hash.wrap("00000000000045d95e0588c47c17d593c7b5cb4fb1e56213d1b3843c1773df2b"));
        checkpoints.put( 16128, Sha256Hash.wrap("000000000001d9964f9483f9096cf9d6c6c2886ed1e5dec95ad2aeec3ce72fa9"));
        checkpoints.put( 18940, Sha256Hash.wrap("00000000000087f7fc0c8085217503ba86f796fa4984f7e5a08b6c4c12906c05"));
        checkpoints.put( 30240, Sha256Hash.wrap("e1c8c862ff342358384d4c22fa6ea5f669f3e1cdcf34111f8017371c3c0be1da"));
        checkpoints.put( 57000, Sha256Hash.wrap("aa3ec60168a0200799e362e2b572ee01f3c3852030d07d036e0aa884ec61f203"));
        checkpoints.put(112896, Sha256Hash.wrap("73f880e78a04dd6a31efc8abf7ca5db4e262c4ae130d559730d6ccb8808095bf"));
        checkpoints.put(182000, Sha256Hash.wrap("d47b4a8fd282f635d66ce34ebbeb26ffd64c35b41f286646598abfd813cba6d9"));
        checkpoints.put(193000, Sha256Hash.wrap("3b85e70ba7f5433049cfbcf0ae35ed869496dbedcd1c0fafadb0284ec81d7b58"));

        dnsSeeds = new String[] {
                "namecoindnsseed.digi-masters.com",  // George Lloyd
                "namecoindnsseed.digi-masters.uk",   // George Lloyd
                "seed.namecoin.domob.eu",            // Daniel Kraft
                "nmc.seed.quisquis.de",              // Peter Conrad
                "dnsseed.namecoin.webbtc.com",       // Marius Hanne
        };
        
        // TODO: look into HTTP seeds or Addr seeds as is done for Bitcoin
    }

    private static NamecoinMainNetParams instance;
    public static synchronized NamecoinMainNetParams get() {
        if (instance == null) {
            instance = new NamecoinMainNetParams();
        }
        return instance;
    }

    // TODO: re-add this when we introduce Testnet2
    /*
    @Override
    public boolean allowMinDifficultyBlocks() {
        return false;
    }
    */

    @Override
    public String getPaymentProtocolId() {
        // TODO: CHANGE THIS (comment from Dogecoin)
        return ID_NMC_MAINNET;
    }

    @Override
    public Block getGenesisBlock() {
        return genesisBlock;
    }

    @Override
    public boolean isTestNet() {
        return false;
    }
}
