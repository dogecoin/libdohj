/*
 * Copyright 2015, 2021 J. Ross Nicoll
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
import org.bitcoinj.core.Context;
import org.bitcoinj.core.Sha256Hash;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author Ross Nicoll
 */
public class DogecoinMainNetParamsTest {
    private static final DogecoinMainNetParams params = DogecoinMainNetParams.get();

    @Before
    public void setUp() throws Exception {
        Context context = new Context(params);
    }

    @Test
    public void shouldHaveCorrectGenesisBlock() {
        final Block genesis = params.getGenesisBlock();
        final Sha256Hash expected = Sha256Hash.wrap("1a91e3dace36e2be3bf030a65679fe821aa1d6ef92e7c9902eb318182c355691");
        final Sha256Hash actual = genesis.getHash();
        assertEquals(expected, actual);
    }
}
