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

package org.altcoinj.params;

import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;
import org.bitcoinj.utils.MonetaryFormat;

import static com.google.common.base.Preconditions.checkState;

/**
 * Parameters for the main Dogecoin production network on which people trade goods and services.
 */
public class AbstractDogecoinParams extends NetworkParameters {
    /** Standard format for the DOGE denomination. */
    public static final MonetaryFormat DOGE;
    /** Standard format for the mDOGE denomination. */
    public static final MonetaryFormat MDOGE;
    /** Standard format for the Koinu denomination. */
    public static final MonetaryFormat KOINU;

    public static final int DOGE_TARGET_TIMESPAN = 4 * 60 * 60;  // 4 hours per difficulty cycle, on average.
    public static final int DOGE_TARGET_TIMESPAN_NEW = 60;  // 60s per difficulty cycle, on average. Kicks in after block 145k.
    public static final int DOGE_TARGET_SPACING = 1 * 60;  // 1 minute per block.
    public static final int DOGE_INTERVAL = DOGE_TARGET_TIMESPAN / DOGE_TARGET_SPACING;
    public static final int DOGE_INTERVAL_NEW = DOGE_TARGET_TIMESPAN_NEW / DOGE_TARGET_SPACING;

    /** Currency code for base 1 Dogecoin. */
    public static final String CODE_DOGE = "DOGE";
    /** Currency code for base 1/1,000 Dogecoin. */
    public static final String CODE_MDOGE = "mDOGE";
    /** Currency code for base 1/100,000,000 Dogecoin. */
    public static final String CODE_KOINU = "Koinu";

    private static final Map<Integer, String> CURRENCY_CODES = new HashMap<Integer, String>();

    static {
        CURRENCY_CODES.put(0, CODE_DOGE);
        CURRENCY_CODES.put(3, CODE_MDOGE);
        CURRENCY_CODES.put(8, CODE_KOINU);
    
        DOGE = MonetaryFormat.BTC.replaceCodes(CURRENCY_CODES);
        MDOGE = DOGE.shift(3).minDecimals(2).optionalDecimals(2);
        KOINU = DOGE.shift(6).minDecimals(0).optionalDecimals(2);
    }

    /** The string returned by getId() for the main, production network where people trade things. */
    public static final String ID_DOGE_MAINNET = "org.dogecoin.production";
    /** The string returned by getId() for the testnet. */
    public static final String ID_DOGE_TESTNET = "org.dogecoin.test";

    protected final int newInterval;
    protected final int newTargetTimespan;
    protected final int diffChangeTarget;

    public AbstractDogecoinParams(final int setDiffChangeTarget) {
        super();
        interval = DOGE_INTERVAL;
        newInterval = DOGE_INTERVAL_NEW;
        targetTimespan = DOGE_TARGET_TIMESPAN;
        newTargetTimespan = DOGE_TARGET_TIMESPAN_NEW;
        maxTarget = Utils.decodeCompactBits(0x1e0fffffL);
        diffChangeTarget = setDiffChangeTarget;

        packetMagic = 0xc0c0c0c0;
        bip32HeaderPub = 0x0488C42E; //The 4 byte header that serializes in base58 to "xpub". (?)
        bip32HeaderPriv = 0x0488E1F4; //The 4 byte header that serializes in base58 to "xprv" (?)
    }

    /** How many blocks pass between difficulty adjustment periods. After new diff algo. */
    public int getNewInterval() {
        return newInterval;
    }

    /**
     * How much time in seconds is supposed to pass between "interval" blocks. If the actual elapsed time is
     * significantly different from this value, the network difficulty formula will produce a different value.
     * Dogecoin after block 145k uses 60 seconds.
     */
    public int getNewTargetTimespan() {
        return newTargetTimespan;
    }

    public MonetaryFormat getMonetaryFormat() {
        return DOGE;
    }
}
