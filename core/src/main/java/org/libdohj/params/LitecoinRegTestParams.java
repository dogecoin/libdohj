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

import org.bitcoinj.core.Block;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;

import java.math.BigInteger;

import static com.google.common.base.Preconditions.checkState;

/**
 * Network parameters for the regression test mode of bitcoind in which all blocks are trivially solvable.
 */
public class LitecoinRegTestParams extends LitecoinTestNet3Params {
    private static final BigInteger MAX_TARGET = new BigInteger("7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);

    public LitecoinRegTestParams() {
        super();
        // Difficulty adjustments are disabled for regtest.
        // By setting the block interval for difficulty adjustments to Integer.MAX_VALUE we make sure difficulty never changes.
        interval = Integer.MAX_VALUE;
        maxTarget = MAX_TARGET;
        subsidyDecreaseBlockCount = 150;
        port = 19444;
        id = ID_LITE_REGTEST;
        packetMagic = 0xfabfb5da;
    }

    @Override
    public boolean allowEmptyPeerChain() {
        return true;
    }

    private static Block genesis;


    /**
     * Extract from Litecoin source code, definition of regtest params.
     * https://github.com/litecoin-project/litecoin/blob/edc66b374ea68107c721062152dd95e6aa037d53/src/chainparams.cpp
     */
    @Override
    public Block getGenesisBlock() {
        synchronized (LitecoinRegTestParams.class) {
            if (genesis == null) {
                genesis = super.getGenesisBlock();
                genesis.setNonce(0);
                genesis.setDifficultyTarget(0x207fffffL);
                genesis.setTime(1296688602L);
                checkState(genesis.getVersion() == 1);
                checkState(genesis.getMerkleRoot().toString().equals("97ddfbbae6be97fd6cdf3e7ca13232a3afff2353e29badfab7f73011edd4ced9"));
                checkState(genesis.getHashAsString().toLowerCase().equals("530827f38f93b43ed12af0b3ad25a288dc02ed74d6d7857862df51fc56c416f9"));
                genesis.verifyHeader();
            }
            return genesis;
        }
    }

    private static LitecoinRegTestParams instance;

    public static synchronized LitecoinRegTestParams get() {
        if (instance == null) {
            instance = new LitecoinRegTestParams();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return ID_LITE_REGTEST;
    }

    @Override
    /** the testnet rules don't work for regtest, where difficulty stays the same */
    public long calculateNewDifficultyTarget(StoredBlock storedPrev, Block nextBlock, BlockStore blockStore)
            throws VerificationException, BlockStoreException {
        final Block prev = storedPrev.getHeader();
        return prev.getDifficultyTarget();
    }
}
