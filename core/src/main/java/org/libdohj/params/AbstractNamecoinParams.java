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

package org.libdohj.params;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;

import org.bitcoinj.core.AltcoinBlock;
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

// TODO: review this

/**
 * Common parameters for Namecoin networks.
 */
public abstract class AbstractNamecoinParams extends NetworkParameters implements AuxPoWNetworkParameters {
    /** Standard format for the NMC denomination. */
    public static final MonetaryFormat NMC;
    /** Standard format for the mNMC denomination. */
    public static final MonetaryFormat MNMC;
    /** Standard format for the uBTC denomination. */
    public static final MonetaryFormat UNMC;

    public static final int AUXPOW_CHAIN_ID = 0x0001; // 1

    /** Currency code for base 1 Namecoin. */
    public static final String CODE_NMC = "NMC";
    /** Currency code for base 1/1,000 Namecoin. */
    public static final String CODE_MNMC = "mNMC";
    /** Currency code for base 1/1,000,000 Namecoin. */
    public static final String CODE_UNMC = "µNMC";

    protected int auxpowStartHeight;
    
    private static final int BLOCK_VERSION_FLAG_AUXPOW = 0x00000100;

    static {
        NMC = MonetaryFormat.BTC.noCode()
            .code(0, CODE_NMC)
            .code(3, CODE_MNMC)
            .code(6, CODE_UNMC);
        MNMC = NMC.shift(3).minDecimals(2).optionalDecimals(2);
        UNMC = NMC.shift(6).minDecimals(0).optionalDecimals(0);
    }

    /** The string returned by getId() for the main, production network where people trade things. */
    public static final String ID_NMC_MAINNET = "org.namecoin.production";
    /** The string returned by getId() for the testnet. */
    public static final String ID_NMC_TESTNET = "org.namecoin.test";

    protected Logger log = LoggerFactory.getLogger(AbstractNamecoinParams.class);
    
    public static final int NAMECOIN_PROTOCOL_VERSION_GETHEADERS = 38000;

    public AbstractNamecoinParams() {
        super();
        genesisBlock = createGenesis(this);
        interval = INTERVAL;
        targetTimespan = TARGET_TIMESPAN;
        maxTarget = Utils.decodeCompactBits(0x1e0fffffL); // TODO: figure out the Namecoin value of this
        
        // BIP 43 recommends using these values regardless of which blockchain is in use.
        bip32HeaderP2PKHpub = 0x0488B21E; //The 4 byte header that serializes in base58 to "xpub".
        bip32HeaderP2PKHpriv = 0x0488ADE4; //The 4 byte header that serializes in base58 to "xprv"
    }

    private static AltcoinBlock createGenesis(NetworkParameters params) {
        AltcoinBlock genesisBlock = new AltcoinBlock(params, Block.BLOCK_VERSION_GENESIS);
        Transaction t = new Transaction(params);
        try {
            // "... choose what comes next.  Lives of your own, or a return to chains. -- V"
            byte[] bytes = Utils.HEX.decode
                    ("04ff7f001c020a024b2e2e2e2063686f6f7365207768617420636f6d6573206e6578742e20204c69766573206f6620796f7572206f776e2c206f7220612072657475726e20746f20636861696e732e202d2d2056");
            t.addInput(new TransactionInput(params, t, bytes));
            ByteArrayOutputStream scriptPubKeyBytes = new ByteArrayOutputStream();
            Script.writeBytes(scriptPubKeyBytes, Utils.HEX.decode
                    ("04b620369050cd899ffbbc4e8ee51e8c4534a855bb463439d63d235d4779685d8b6f4870a238cf365ac94fa13ef9a2a22cd99d0d5ee86dcabcafce36c7acf43ce5"));
            scriptPubKeyBytes.write(ScriptOpCodes.OP_CHECKSIG);
            t.addOutput(new TransactionOutput(params, t, COIN.multiply(50), scriptPubKeyBytes.toByteArray()));
        } catch (Exception e) {
            // Cannot happen.
            throw new RuntimeException(e);
        }
        genesisBlock.addTransaction(t);
        return genesisBlock;
    }

    @Override
    public Coin getBlockSubsidy(final int height) {
        return COIN.multiply(50).shiftRight(height / getSubsidyDecreaseBlockCount());
    }

    @Override
    public MonetaryFormat getMonetaryFormat() {
        return NMC;
    }

    @Override
    public Coin getMaxMoney() {
        return MAX_MONEY;
    }

    // TODO: this is Bitcoin, need to figure out if it's the same for Namecoin
    @Override
    public Coin getMinNonDustOutput() {
        return Transaction.MIN_NONDUST_OUTPUT;
    }

    @Override
    public String getUriScheme() {
        return "namecoin";
    }

    @Override
    public boolean hasMaxMoney() {
        return true;
    }

    // This is copied from Bitcoin
    /**
     * Checks if we are at a difficulty transition point.
     * @param storedPrev The previous stored block
     * @return If this is a difficulty transition point
     */
    protected boolean isDifficultyTransitionPoint(StoredBlock storedPrev) {
        return ((storedPrev.getHeight() + 1) % this.getInterval()) == 0;
    }
    
    @Override
    public void checkDifficultyTransitions(StoredBlock storedPrev, Block nextBlock, BlockStore blockStore)
        throws VerificationException, BlockStoreException {        
        // This is copied verbatim from Bitcoin except for the Namecoin changes marked accordingly
        Block prev = storedPrev.getHeader();

        // Is this supposed to be a difficulty transition point?
        if (!isDifficultyTransitionPoint(storedPrev)) {

            // No ... so check the difficulty didn't actually change.
            if (nextBlock.getDifficultyTarget() != prev.getDifficultyTarget())
                throw new VerificationException("Unexpected change in difficulty at height " + storedPrev.getHeight() +
                        ": " + Long.toHexString(nextBlock.getDifficultyTarget()) + " vs " +
                        Long.toHexString(prev.getDifficultyTarget()));
            return;
        }

        // We need to find a block far back in the chain. It's OK that this is expensive because it only occurs every
        // two weeks after the initial block chain download.
        final Stopwatch watch = Stopwatch.createStarted();
        StoredBlock cursor = blockStore.get(prev.getHash());
        
        // Namecoin addition
        int blocksBack = this.getInterval() - 1;
        if (storedPrev.getHeight() >= this.getAuxpowStartHeight() && (storedPrev.getHeight() + 1 > this.getInterval())) {
	    blocksBack = this.getInterval();
        }
        
        // Namecoin modification
        //for (int i = 0; i < this.getInterval() - 1; i++) {
        for (int i = 0; i < blocksBack; i++) {
            if (cursor == null) {
                // This should never happen. If it does, it means we are following an incorrect or busted chain.
                throw new VerificationException(
                        "Difficulty transition point but we did not find a way back to the genesis block.");
            }
            cursor = blockStore.get(cursor.getHeader().getPrevBlockHash());
        }
        watch.stop();
        if (watch.elapsed(TimeUnit.MILLISECONDS) > 50)
            log.info("Difficulty transition traversal took {}", watch);

        Block blockIntervalAgo = cursor.getHeader();
        int timespan = (int) (prev.getTimeSeconds() - blockIntervalAgo.getTimeSeconds());
        // Limit the adjustment step.
        final int targetTimespan = this.getTargetTimespan();
        if (timespan < targetTimespan / 4)
            timespan = targetTimespan / 4;
        if (timespan > targetTimespan * 4)
            timespan = targetTimespan * 4;

        BigInteger newTarget = Utils.decodeCompactBits(prev.getDifficultyTarget());
        newTarget = newTarget.multiply(BigInteger.valueOf(timespan));
        newTarget = newTarget.divide(BigInteger.valueOf(targetTimespan));

        if (newTarget.compareTo(this.getMaxTarget()) > 0) {
            log.info("Difficulty hit proof of work limit: {}", newTarget.toString(16));
            newTarget = this.getMaxTarget();
        }

        int accuracyBytes = (int) (nextBlock.getDifficultyTarget() >>> 24) - 3;
        long receivedTargetCompact = nextBlock.getDifficultyTarget();

        // The calculated difficulty is to a higher precision than received, so reduce here.
        BigInteger mask = BigInteger.valueOf(0xFFFFFFL).shiftLeft(accuracyBytes * 8);
        newTarget = newTarget.and(mask);
        long newTargetCompact = Utils.encodeCompactBits(newTarget);

        if (newTargetCompact != receivedTargetCompact)
            throw new VerificationException("Network provided difficulty bits do not match what was calculated: " +
                    Long.toHexString(newTargetCompact) + " vs " + Long.toHexString(receivedTargetCompact));
    }

    @Override
    public int getChainID() {
        return AUXPOW_CHAIN_ID;
    }

    // TODO: re-add this when we introduce Testnet2
    /**
     * Whether this network has special rules to enable minimum difficulty blocks
     * after a long interval between two blocks (i.e. testnet).
     */
    //public abstract boolean allowMinDifficultyBlocks();

    /**
     * Get the hash to use for a block.
     */
    
    @Override
    public Sha256Hash getBlockDifficultyHash(Block block) {
        return block.getHash();
    }

    @Override
    public AltcoinSerializer getSerializer(boolean parseRetain) {
        return new AltcoinSerializer(this, parseRetain);
    }

    // TODO: look into allowing PeerGroups that don't support GetHeaders (since for full block retrieval it's not needed)
    @Override
    public int getProtocolVersionNum(final ProtocolVersion version) {
        switch (version) {
            case MINIMUM:
                return NAMECOIN_PROTOCOL_VERSION_GETHEADERS;
            default:
                return version.getBitcoinProtocolVersion();
        }
    }

    @Override
    public boolean isAuxPoWBlockVersion(long version) {
        return (version & BLOCK_VERSION_FLAG_AUXPOW) > 0;
    }
    
    public int getAuxpowStartHeight() {
	return auxpowStartHeight;
    }

    private static class CheckpointEncounteredException extends Exception {

        private CheckpointEncounteredException() {
        }
    }
}
