/*
 * Copyright 2015 Ross Nicoll
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
package org.bitcoinj.core;

/**
 *
 * @author jrn
 */
public interface AltcoinNetworkParameters {

    boolean isAuxPoWBlockVersion(long version);

    int getChainID();

    /**
     * Get the hash for the given block, for comparing against target difficulty.
     * This provides an extension hook for networks which use a hash other than
     * SHA256 twice (Bitcoin standard) for proof of work.
     */
    Sha256Hash getBlockDifficultyHash(Block block);
}
