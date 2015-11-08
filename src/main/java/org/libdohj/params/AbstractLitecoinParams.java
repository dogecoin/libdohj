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

package org.libdohj.params;

import java.math.BigInteger;
import org.bitcoinj.core.AltcoinBlock;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.Coin;
import static org.bitcoinj.core.Coin.COIN;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Utils;
import org.bitcoinj.utils.MonetaryFormat;
import org.libdohj.core.AltcoinNetworkParameters;
import org.libdohj.core.AltcoinSerializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Common parameters for Litecoin networks.
 */
public abstract class AbstractLitecoinParams extends NetworkParameters implements AltcoinNetworkParameters {
    /** Standard format for the LITE denomination. */
    public static final MonetaryFormat LITE;
    /** Standard format for the mLITE denomination. */
    public static final MonetaryFormat MLITE;
    /** Standard format for the Liteoshi denomination. */
    public static final MonetaryFormat LITEOSHI;

    public static final int LITE_TARGET_TIMESPAN = (int) (3.5 * 24 * 60 * 60); // 3.5 days
    public static final int LITE_TARGET_SPACING = (int) (2.5 * 60); // 2.5 minutes
    public static final int LITE_INTERVAL = LITE_TARGET_TIMESPAN / LITE_TARGET_SPACING;
    
    /**
     * The maximum number of coins to be generated
     */
    public static final long MAX_LITECOINS = 21000000; // TODO: Needs to be 840000000

    /**
     * The maximum money to be generated
     */
    public static final Coin MAX_LITECOIN_MONEY = COIN.multiply(MAX_LITECOINS);

    /** Currency code for base 1 Litecoin. */
    public static final String CODE_LITE = "LITE";
    /** Currency code for base 1/1,000 Litecoin. */
    public static final String CODE_MLITE = "mLITE";
    /** Currency code for base 1/100,000,000 Litecoin. */
    public static final String CODE_LITEOSHI = "Liteoshi";

    static {
        LITE = MonetaryFormat.BTC.noCode()
            .code(0, CODE_LITE)
            .code(3, CODE_MLITE)
            .code(7, CODE_LITEOSHI);
        MLITE = LITE.shift(3).minDecimals(2).optionalDecimals(2);
        LITEOSHI = LITE.shift(7).minDecimals(0).optionalDecimals(2);
    }

    /** The string returned by getId() for the main, production network where people trade things. */
    public static final String ID_LITE_MAINNET = "org.litecoin.production";
    /** The string returned by getId() for the testnet. */
    public static final String ID_LITE_TESTNET = "org.litecoin.test";

    protected Logger log = LoggerFactory.getLogger(AbstractLitecoinParams.class);
    public static final int LITECOIN_PROTOCOL_VERSION_MINIMUM = 70002;
    public static final int LITECOIN_PROTOCOL_VERSION_CURRENT = 70003;

    public AbstractLitecoinParams() {
        super();
        interval = LITE_INTERVAL;
        targetTimespan = LITE_TARGET_TIMESPAN;
        maxTarget = Utils.decodeCompactBits(0x1e0fffffL);

        packetMagic = 0xfbc0b6db;
        bip32HeaderPub = 0x0488C42E; //The 4 byte header that serializes in base58 to "xpub". (?)
        bip32HeaderPriv = 0x0488E1F4; //The 4 byte header that serializes in base58 to "xprv" (?)
    }

    /**
     * Get the hash to use for a block.
     */
    @Override
    public Sha256Hash getBlockDifficultyHash(Block block) {
        return ((AltcoinBlock) block).getScryptHash();
    }

    public MonetaryFormat getMonetaryFormat() {
        return LITE;
    }

    @Override
    public Coin getMaxMoney() {
        return MAX_LITECOIN_MONEY;
    }

    @Override
    public Coin getMinNonDustOutput() {
        return Coin.COIN;
    }

    @Override
    public String getUriScheme() {
        return "litecoin";
    }

    @Override
    public boolean hasMaxMoney() {
        return true;
    }

    @Override
    public void checkDifficultyTransitions(StoredBlock storedPrev, Block nextBlock, BlockStore blockStore)
        throws VerificationException, BlockStoreException {
        final Block prev = storedPrev.getHeader();
        final int previousHeight = storedPrev.getHeight();
        final int retargetInterval = this.getInterval();
        
        // Is this supposed to be a difficulty transition point?
        if ((storedPrev.getHeight() + 1) % retargetInterval != 0) {
            // No ... so check the difficulty didn't actually change.
            if (nextBlock.getDifficultyTarget() != prev.getDifficultyTarget())
                throw new VerificationException("Unexpected change in difficulty at height " + storedPrev.getHeight() +
                        ": " + Long.toHexString(nextBlock.getDifficultyTarget()) + " vs " +
                        Long.toHexString(prev.getDifficultyTarget()));
            return;
        }

        // We need to find a block far back in the chain. It's OK that this is expensive because it only occurs every
        // two weeks after the initial block chain download.
        StoredBlock cursor = blockStore.get(prev.getHash());
        int goBack = retargetInterval - 1;
        if (cursor.getHeight()+1 != retargetInterval)
            goBack = retargetInterval;

        for (int i = 0; i < goBack; i++) {
            if (cursor == null) {
                // This should never happen. If it does, it means we are following an incorrect or busted chain.
                throw new VerificationException(
                        "Difficulty transition point but we did not find a way back to the genesis block.");
            }
            cursor = blockStore.get(cursor.getHeader().getPrevBlockHash());
        }

        //We used checkpoints...
        if (cursor == null) {
            log.debug("Difficulty transition: Hit checkpoint!");
            return;
        }

        Block blockIntervalAgo = cursor.getHeader();
        long receivedTargetCompact = nextBlock.getDifficultyTarget();
        long newTargetCompact = this.getNewDifficultyTarget(previousHeight, prev,
            nextBlock, blockIntervalAgo);

        if (newTargetCompact != receivedTargetCompact)
            throw new VerificationException("Network provided difficulty bits do not match what was calculated: " +
                    newTargetCompact + " vs " + receivedTargetCompact);
    }

    /**
     * 
     * @param previousHeight height of the block immediately before the retarget.
     * @param prev the block immediately before the retarget block.
     * @param nextBlock the block the retarget happens at.
     * @param blockIntervalAgo The last retarget block.
     * @return New difficulty target as compact bytes.
     */
    public long getNewDifficultyTarget(int previousHeight, final Block prev, final Block nextBlock,
        final Block blockIntervalAgo) {
        return this.getNewDifficultyTarget(previousHeight, prev.getTimeSeconds(),
            prev.getDifficultyTarget(), blockIntervalAgo.getTimeSeconds(),
            nextBlock.getDifficultyTarget());
    }

    /**
     * 
     * @param previousHeight Height of the block immediately previous to the one we're calculating difficulty of.
     * @param previousBlockTime Time of the block immediately previous to the one we're calculating difficulty of.
     * @param lastDifficultyTarget Compact difficulty target of the last retarget block.
     * @param lastRetargetTime Time of the last difficulty retarget.
     * @param nextDifficultyTarget The expected difficulty target of the next
     * block, used for determining precision of the result.
     * @return New difficulty target as compact bytes.
     */
    protected long getNewDifficultyTarget(int previousHeight, long previousBlockTime,
        final long lastDifficultyTarget, final long lastRetargetTime,
        final long nextDifficultyTarget) {
        final int retargetTimespan = this.getTargetTimespan();
        int actualTime = (int) (previousBlockTime - lastRetargetTime);
        final int minTimespan = retargetTimespan / 4;
        final int maxTimespan = retargetTimespan * 4;

        actualTime = Math.min(maxTimespan, Math.max(minTimespan, actualTime));

        BigInteger newTarget = Utils.decodeCompactBits(lastDifficultyTarget);
        newTarget = newTarget.multiply(BigInteger.valueOf(actualTime));
        newTarget = newTarget.divide(BigInteger.valueOf(retargetTimespan));

        if (newTarget.compareTo(this.getMaxTarget()) > 0) {
            log.info("Difficulty hit proof of work limit: {}", newTarget.toString(16));
            newTarget = this.getMaxTarget();
        }

        int accuracyBytes = (int) (nextDifficultyTarget >>> 24) - 3;

        // The calculated difficulty is to a higher precision than received, so reduce here.
        BigInteger mask = BigInteger.valueOf(0xFFFFFFL).shiftLeft(accuracyBytes * 8);
        newTarget = newTarget.and(mask);
        return Utils.encodeCompactBits(newTarget);
    }

    @Override
    public AltcoinSerializer getSerializer(boolean parseRetain) {
        return new AltcoinSerializer(this, parseRetain);
    }

    @Override
    public int getProtocolVersionNum(final ProtocolVersion version) {
        switch (version) {
            case PONG:
            case BLOOM_FILTER:
                return version.getBitcoinProtocolVersion();
            case CURRENT:
                return LITECOIN_PROTOCOL_VERSION_CURRENT;
            case MINIMUM:
            default:
                return LITECOIN_PROTOCOL_VERSION_MINIMUM;
        }
    }
}
