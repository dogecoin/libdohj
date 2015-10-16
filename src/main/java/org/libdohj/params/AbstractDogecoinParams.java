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

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import org.bitcoinj.core.AltcoinBlock;

import org.libdohj.core.AltcoinNetworkParameters;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Coin;
import static org.bitcoinj.core.Coin.COIN;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptOpCodes;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.utils.MonetaryFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.libdohj.core.AltcoinSerializer;
import org.libdohj.core.AuxPoWNetworkParameters;

/**
 * Parameters for the main Dogecoin production network on which people trade goods and services.
 */
public abstract class AbstractDogecoinParams extends NetworkParameters implements AuxPoWNetworkParameters {
    /** Standard format for the DOGE denomination. */
    public static final MonetaryFormat DOGE;
    /** Standard format for the mDOGE denomination. */
    public static final MonetaryFormat MDOGE;
    /** Standard format for the Koinu denomination. */
    public static final MonetaryFormat KOINU;

    public static final int DIGISHIELD_BLOCK_HEIGHT = 145000; // Block height to use Digishield from
    public static final int AUXPOW_CHAIN_ID = 0x0062; // 98
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

    private static final int BLOCK_MIN_VERSION_AUXPOW = 0x00620002;
    private static final int BLOCK_VERSION_FLAG_AUXPOW = 0x00000100;

    static {
        DOGE = MonetaryFormat.BTC.noCode()
            .code(0, CODE_DOGE)
            .code(3, CODE_MDOGE)
            .code(7, CODE_KOINU);
        MDOGE = DOGE.shift(3).minDecimals(2).optionalDecimals(2);
        KOINU = DOGE.shift(7).minDecimals(0).optionalDecimals(2);
    }

    /** The string returned by getId() for the main, production network where people trade things. */
    public static final String ID_DOGE_MAINNET = "org.dogecoin.production";
    /** The string returned by getId() for the testnet. */
    public static final String ID_DOGE_TESTNET = "org.dogecoin.test";

    protected final int newInterval;
    protected final int newTargetTimespan;
    protected final int diffChangeTarget;

    protected Logger log = LoggerFactory.getLogger(AbstractDogecoinParams.class);

    public AbstractDogecoinParams(final int setDiffChangeTarget) {
        super();
        genesisBlock = createGenesis(this);
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

    private static AltcoinBlock createGenesis(NetworkParameters params) {
        AltcoinBlock genesisBlock = new AltcoinBlock(params, Block.BLOCK_VERSION_GENESIS);
        Transaction t = new Transaction(params);
        try {
            byte[] bytes = Utils.HEX.decode
                    ("04ffff001d0104084e696e746f6e646f");
            t.addInput(new TransactionInput(params, t, bytes));
            ByteArrayOutputStream scriptPubKeyBytes = new ByteArrayOutputStream();
            Script.writeBytes(scriptPubKeyBytes, Utils.HEX.decode
                    ("040184710fa689ad5023690c80f3a49c8f13f8d45b8c857fbcbc8bc4a8e4d3eb4b10f4d4604fa08dce601aaf0f470216fe1b51850b4acf21b179c45070ac7b03a9"));
            scriptPubKeyBytes.write(ScriptOpCodes.OP_CHECKSIG);
            t.addOutput(new TransactionOutput(params, t, COIN.multiply(88), scriptPubKeyBytes.toByteArray()));
        } catch (Exception e) {
            // Cannot happen.
            throw new RuntimeException(e);
        }
        genesisBlock.addTransaction(t);
        return genesisBlock;
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

    @Override
    public Coin getMaxMoney() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Coin getMinNonDustOutput() {
        return Coin.COIN;
    }

    @Override
    public String getUriScheme() {
        return "dogecoin";
    }

    @Override
    public boolean hasMaxMoney() {
        return false;
    }

    @Override
    public void checkDifficultyTransitions(StoredBlock storedPrev, Block nextBlock, BlockStore blockStore)
        throws VerificationException, BlockStoreException {
        final Block prev = storedPrev.getHeader();
        final int previousHeight = storedPrev.getHeight();
        final boolean digishieldAlgorithm = previousHeight + 1 >= this.getDigishieldBlockHeight();
        final int retargetInterval = digishieldAlgorithm
            ? this.getNewInterval()
            : this.getInterval();
        
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
        long newTargetCompact = this.getNewDifficultyTarget(previousHeight, prev.getTimeSeconds(),
            prev.getDifficultyTarget(), blockIntervalAgo.getTimeSeconds());

        if (newTargetCompact != receivedTargetCompact)
            throw new VerificationException("Network provided difficulty bits do not match what was calculated: " +
                    newTargetCompact + " vs " + receivedTargetCompact);
    }

    /**
     * 
     * @param previousHeight Height of the block immediately previous to the one we're calculating difficulty of.
     * @param previousBlockTime Time of the block immediately previous to the one we're calculating difficulty of.
     * @param lastDifficultyTarget Compact difficulty target of the last retarget block.
     * @param lastRetargetTime Time of the last difficulty retarget.
     * @return New difficulty target as compact bytes.
     */
    public long getNewDifficultyTarget(int previousHeight, long previousBlockTime,
        final long lastDifficultyTarget, final long lastRetargetTime) {
        final int height = previousHeight + 1;
        final boolean digishieldAlgorithm = height >= this.getDigishieldBlockHeight();
        final int retargetTimespan = digishieldAlgorithm
            ? this.getNewTargetTimespan()
            : this.getTargetTimespan();
        int actualTime = (int) (previousBlockTime - lastRetargetTime);
        final int minTimespan;
        final int maxTimespan;

        // Limit the adjustment step.
        if (digishieldAlgorithm)
        {
            // Round towards zero to match the C++ implementation.
            if (actualTime < retargetTimespan) {
                actualTime = (int)Math.ceil(retargetTimespan + (actualTime - retargetTimespan) / 8.0);
            } else {
                actualTime = (int)Math.floor(retargetTimespan + (actualTime - retargetTimespan) / 8.0);
            }
            minTimespan = retargetTimespan - (retargetTimespan / 4);
            maxTimespan = retargetTimespan + (retargetTimespan / 2);
        }
        else if (height > 10000)
        {
            minTimespan = retargetTimespan / 4;
            maxTimespan = retargetTimespan * 4;
        }
        else if (height > 5000)
        {
            minTimespan = retargetTimespan / 8;
            maxTimespan = retargetTimespan * 4;
        }
        else
        {
            minTimespan = retargetTimespan / 16;
            maxTimespan = retargetTimespan * 4;
        }
        actualTime = Math.min(maxTimespan, Math.max(minTimespan, actualTime));

        BigInteger newTarget = Utils.decodeCompactBits(lastDifficultyTarget);
        newTarget = newTarget.multiply(BigInteger.valueOf(actualTime));
        newTarget = newTarget.divide(BigInteger.valueOf(retargetTimespan));

        if (newTarget.compareTo(this.getMaxTarget()) > 0) {
            log.info("Difficulty hit proof of work limit: {}", newTarget.toString(16));
            newTarget = this.getMaxTarget();
        }

        int accuracyBytes = (int) (lastDifficultyTarget >>> 24) - 3;

        // The calculated difficulty is to a higher precision than received, so reduce here.
        BigInteger mask = BigInteger.valueOf(0xFFFFFFL).shiftLeft(accuracyBytes * 8);
        newTarget = newTarget.and(mask);
        return Utils.encodeCompactBits(newTarget);
    }

    /**
     * Get the block height from which the Digishield difficulty calculation
     * algorithm is used.
     */
    public int getDigishieldBlockHeight() {
        return DIGISHIELD_BLOCK_HEIGHT;
    }

    @Override
    public int getChainID() {
        return AUXPOW_CHAIN_ID;
    }

    /**
     * Get the hash to use for a block.
     */
    @Override
    public Sha256Hash getBlockDifficultyHash(Block block) {
        return ((AltcoinBlock) block).getScryptHash();
    }

    @Override
    public AltcoinSerializer getSerializer(boolean parseRetain) {
        return new AltcoinSerializer(this, parseRetain);
    }

    @Override
    public boolean isAuxPoWBlockVersion(long version) {
        return version >= BLOCK_MIN_VERSION_AUXPOW
            && (version & BLOCK_VERSION_FLAG_AUXPOW) > 0;
    }
}
