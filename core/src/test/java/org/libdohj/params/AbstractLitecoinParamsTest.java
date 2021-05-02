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

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Context;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author Ross Nicoll
 */
public class AbstractLitecoinParamsTest {
    private static final AbstractLitecoinParams params = LitecoinMainNetParams.get();

    @Before
    public void setUp() throws Exception {
        Context context = new Context(params);
    }

    /**
     * Confirm subsidy rules follow Litecoin pattern.
     */
    @Test
    public void shouldCalculateSubsidy() {
        assertEquals(Coin.COIN.multiply(50), params.getBlockSubsidy(0));
        assertEquals(Coin.COIN.multiply(50), params.getBlockSubsidy(839999));
        assertEquals(Coin.COIN.multiply(25), params.getBlockSubsidy(840000));
        assertEquals(Coin.COIN.multiply(25), params.getBlockSubsidy(1679999));
    }
}
