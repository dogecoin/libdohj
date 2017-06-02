/*
 * Copyright 2015 J. Ross Nicoll
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
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author Ross Nicoll
 */
public class LitecoinRegTestParamsTest {
    private static final LitecoinRegTestParams params = LitecoinRegTestParams.get();

    @Before
    public void setUp() throws Exception {
        Context context = new Context(params);
    }

    @Test
    public void testRegTestGenesisBlock() {
        Block genesis = params.getGenesisBlock();
        assertEquals("530827f38f93b43ed12af0b3ad25a288dc02ed74d6d7857862df51fc56c416f9", genesis.getHashAsString());
    }
}
