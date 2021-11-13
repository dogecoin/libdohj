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
import org.spongycastle.util.encoders.Hex;

import static com.google.common.base.Preconditions.checkState;
import java.io.ByteArrayOutputStream;

import static org.bitcoinj.core.Coin.COIN;

import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptOpCodes;

/**
 * Parameters for the Litecoin main production network on which people trade
 * goods and services.
 */
public class LitecoinMainNetParams extends AbstractLitecoinParams {
    public static final int MAINNET_MAJORITY_WINDOW = MainNetParams.MAINNET_MAJORITY_WINDOW;
    public static final int MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED = MainNetParams.MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED;
    public static final int MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE = MainNetParams.MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE;
    private static final Sha256Hash GENESIS_HASH = Sha256Hash.wrap("12a765e31ffd4059bada1e25190f6e98c99d9714d334efa41a195a7e7e04bfe2");
    private static final long GENESIS_TIME = 1317972665L;
    private static final long GENESIS_NONCE = 2084524493;
    private static final long STANDARD_MAX_DIFFICULTY_TARGET = 0x1e0ffff0L;


    public LitecoinMainNetParams() {
        super();
        id = ID_LITE_MAINNET;
        packetMagic = 0xfbc0b6db;

        maxTarget = Utils.decodeCompactBits(0x1e0fffffL);
        port = 9333;
        addressHeader = 48;
        p2shHeader = 50;
        dumpedPrivateKeyHeader = 176;
        segwitAddressHrp = "ltc";

        spendableCoinbaseDepth = 100;
        subsidyDecreaseBlockCount = 840000;

        majorityEnforceBlockUpgrade = MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE;
        majorityRejectBlockOutdated = MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED;
        majorityWindow = MAINNET_MAJORITY_WINDOW;

        dnsSeeds = new String[] {
            "dnsseed.litecointools.com",
            "dnsseed.litecoinpool.org",
            "dnsseed.ltc.xurious.com",
            "dnsseed.koin-project.com",
            "dnsseed.weminemnc.com"
        };
        bip32HeaderP2PKHpub = 0x0488B21E;
        bip32HeaderP2PKHpriv = 0x0488ADE4;
    }

    private static AltcoinBlock createGenesis(NetworkParameters params) {
        AltcoinBlock genesisBlock = new AltcoinBlock(params, Block.BLOCK_VERSION_GENESIS);
        Transaction t = new Transaction(params);
        try {
            byte[] bytes = Hex.decode
                    ("04ffff001d0104404e592054696d65732030352f4f63742f32303131205374657665204a6f62732c204170706c65e280997320566973696f6e6172792c2044696573206174203536");
            t.addInput(new TransactionInput(params, t, bytes));
            ByteArrayOutputStream scriptPubKeyBytes = new ByteArrayOutputStream();
            Script.writeBytes(scriptPubKeyBytes, Hex.decode
                    ("040184710fa689ad5023690c80f3a49c8f13f8d45b8c857fbcbc8bc4a8e4d3eb4b10f4d4604fa08dce601aaf0f470216fe1b51850b4acf21b179c45070ac7b03a9"));
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


    private static LitecoinMainNetParams instance;
    public static synchronized LitecoinMainNetParams get() {
        if (instance == null) {
            instance = new LitecoinMainNetParams();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return ID_LITE_MAINNET;
    }

    @Override
    public boolean isTestNet() {
        return false;
    }
}
