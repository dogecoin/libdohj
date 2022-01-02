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

package org.libdohj.params;

import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptOpCodes;
import org.bouncycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;

import static com.google.common.base.Preconditions.checkState;
import static org.bitcoinj.core.Coin.COIN;

/**
 * Parameters for the Dogecoin testnet, a separate public network that has
 * relaxed rules suitable for development and testing of applications and new
 * Dogecoin versions.
 */
public class DogecoinTestNet3Params extends AbstractDogecoinParams {
    public static final int TESTNET_MAJORITY_WINDOW = 1000;
    public static final int TESTNET_MAJORITY_REJECT_BLOCK_OUTDATED = 750;
    public static final int TESTNET_MAJORITY_ENFORCE_BLOCK_UPGRADE = 501;
    protected static final int DIFFICULTY_CHANGE_TARGET = 145000;
    private static final Sha256Hash GENESIS_HASH = Sha256Hash.wrap("bb0a78264637406b6360aad926284d544d7049f45189db5664f3c4d07350559e");
    private static final long GENESIS_TIME = 1391503289L;
    private static final long GENESIS_NONCE = 997879;
    private static final long STANDARD_MAX_DIFFICULTY_TARGET = 0x1e0ffff0L;

    public DogecoinTestNet3Params() {
        super(DIFFICULTY_CHANGE_TARGET);
        id = ID_DOGE_TESTNET;

        packetMagic = 0xfcc1b7dc;

        maxTarget = Utils.decodeCompactBits(0x1e0fffffL);
        port = 44556;
        addressHeader = 113;
        p2shHeader = 196;
        dumpedPrivateKeyHeader = 241;
        segwitAddressHrp = "tdge";
        spendableCoinbaseDepth = 30;
        subsidyDecreaseBlockCount = 100000;

        majorityEnforceBlockUpgrade = TESTNET_MAJORITY_ENFORCE_BLOCK_UPGRADE;
        majorityRejectBlockOutdated = TESTNET_MAJORITY_REJECT_BLOCK_OUTDATED;
        majorityWindow = TESTNET_MAJORITY_WINDOW;

        dnsSeeds = new String[] {
            "testseed.jrn.me.uk"
        };
        // Note this is the same as the BIP32 testnet, as BIP44 makes HD wallets
        // chain agnostic. Dogecoin mainnet has its own headers for legacy reasons.
        bip32HeaderP2PKHpub = 0x043587CF;
        bip32HeaderP2PKHpriv = 0x04358394;
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

    @Override
    public Block getGenesisBlock() {
        synchronized (GENESIS_HASH) {
            if (genesisBlock == null) {
                genesisBlock = createGenesis(this);
                genesisBlock.setDifficultyTarget(STANDARD_MAX_DIFFICULTY_TARGET);
                genesisBlock.setTime(GENESIS_TIME);
                genesisBlock.setNonce(GENESIS_NONCE);
                checkState(genesisBlock.getHash().equals(GENESIS_HASH), "Invalid genesis hash");
            }
        }
        return genesisBlock;
    }

    private static DogecoinTestNet3Params instance;
    public static synchronized DogecoinTestNet3Params get() {
        if (instance == null) {
            instance = new DogecoinTestNet3Params();
        }
        return instance;
    }

    @Override
    public boolean allowMinDifficultyBlocks() {
        return true;
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
