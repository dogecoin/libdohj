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


    /* Extract from Litecoin source code, definition of regtest params.
    Commit Hash: 3b4ed770f88229b11bf62b90f128f3054b17ab36
    File: src/chainparams.cpp

        CRegTestParams() {
            strNetworkID = "regtest";
            consensus.nSubsidyHalvingInterval = 150;
            consensus.nMajorityEnforceBlockUpgrade = 750;
            consensus.nMajorityRejectBlockOutdated = 950;
            consensus.nMajorityWindow = 1000;
            consensus.BIP34Height = -1; // BIP34 has not necessarily activated on regtest
            consensus.BIP34Hash = uint256();
            consensus.powLimit = uint256S("7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
            consensus.nPowTargetTimespan = 3.5 * 24 * 60 * 60; // two weeks
            consensus.nPowTargetSpacing = 2.5 * 60;
            consensus.fPowAllowMinDifficultyBlocks = true;
            consensus.fPowNoRetargeting = true;
            consensus.nRuleChangeActivationThreshold = 108; // 75% for testchains
            consensus.nMinerConfirmationWindow = 144; // Faster than normal for regtest (144 instead of 2016)
            consensus.vDeployments[Consensus::DEPLOYMENT_TESTDUMMY].bit = 28;
            consensus.vDeployments[Consensus::DEPLOYMENT_TESTDUMMY].nStartTime = 0;
            consensus.vDeployments[Consensus::DEPLOYMENT_TESTDUMMY].nTimeout = 999999999999ULL;
            consensus.vDeployments[Consensus::DEPLOYMENT_CSV].bit = 0;
            consensus.vDeployments[Consensus::DEPLOYMENT_CSV].nStartTime = 0;
            consensus.vDeployments[Consensus::DEPLOYMENT_CSV].nTimeout = 999999999999ULL;
            consensus.vDeployments[Consensus::DEPLOYMENT_SEGWIT].bit = 1;
            consensus.vDeployments[Consensus::DEPLOYMENT_SEGWIT].nStartTime = 0;
            consensus.vDeployments[Consensus::DEPLOYMENT_SEGWIT].nTimeout = 999999999999ULL;

            // The best chain should have at least this much work.
            consensus.nMinimumChainWork = uint256S("0x00");

            pchMessageStart[0] = 0xfa;
            pchMessageStart[1] = 0xbf;
            pchMessageStart[2] = 0xb5;
            pchMessageStart[3] = 0xda;
            nDefaultPort = 19444;
            nPruneAfterHeight = 1000;

            genesis = CreateGenesisBlock(1296688602, 0, 0x207fffff, 1, 50 * COIN);
            consensus.hashGenesisBlock = genesis.GetHash();
            assert(consensus.hashGenesisBlock == uint256S("0x530827f38f93b43ed12af0b3ad25a288dc02ed74d6d7857862df51fc56c416f9"));
            assert(genesis.hashMerkleRoot == uint256S("0x97ddfbbae6be97fd6cdf3e7ca13232a3afff2353e29badfab7f73011edd4ced9"));

            vFixedSeeds.clear(); //!< Regtest mode doesn't have any fixed seeds.
            vSeeds.clear();      //!< Regtest mode doesn't have any DNS seeds.

            fMiningRequiresPeers = false;
            fDefaultConsistencyChecks = true;
            fRequireStandard = false;
            fMineBlocksOnDemand = true;
            fTestnetToBeDeprecatedFieldRPC = false;

            checkpointData = (CCheckpointData){
                    boost::assign::map_list_of
            ( 0, uint256S("0f9188f13cb7b2c71f2a335e3a4fc328bf5beb436012afca590b1a11466e2206")),
            0,
                    0,
                    0
        };

            base58Prefixes[PUBKEY_ADDRESS] = std::vector<unsigned char>(1,111);
            base58Prefixes[SCRIPT_ADDRESS] = std::vector<unsigned char>(1,196);
            base58Prefixes[SCRIPT_ADDRESS2] = std::vector<unsigned char>(1,58);
            base58Prefixes[SECRET_KEY] =     std::vector<unsigned char>(1,239);
            base58Prefixes[EXT_PUBLIC_KEY] = boost::assign::list_of(0x04)(0x35)(0x87)(0xCF).convert_to_container<std::vector<unsigned char> >();
            base58Prefixes[EXT_SECRET_KEY] = boost::assign::list_of(0x04)(0x35)(0x83)(0x94).convert_to_container<std::vector<unsigned char> >();
        }
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
