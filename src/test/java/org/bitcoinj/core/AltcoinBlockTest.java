/*
 * Copyright 2015 jrn.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bitcoinj.core;

import java.util.BitSet;

import static org.junit.Assert.assertArrayEquals;
import org.junit.Before;
import org.junit.Test;

import org.libdohj.params.DogecoinMainNetParams;

/**
 *
 * @author jrn
 */
public class AltcoinBlockTest {
    private final NetworkParameters params = DogecoinMainNetParams.get();

    @Before
    public void setUp() throws Exception {
        Context context = new Context(params);
    }

    /**
     * Test extraction of flags from the block version, for coins with AuxPoW
     * support.
     */
    @Test
    public void testGetVersionFlags() {
        AltcoinBlock block = new AltcoinBlock(params, 0L);
        BitSet expected = new BitSet(8);
        assertArrayEquals(block.getVersionFlags().toByteArray(), expected.toByteArray());

        // Set everything but the version flags
        block = new AltcoinBlock(params, 0xffff00ff);
        assertArrayEquals(block.getVersionFlags().toByteArray(), expected.toByteArray());

        // Set everything
        block = new AltcoinBlock(params, 0xffffffff);
        expected.set(0, 8);
        assertArrayEquals(block.getVersionFlags().toByteArray(), expected.toByteArray());

        // Set only the version flags
        block = new AltcoinBlock(params, 0x0000ff00);
        assertArrayEquals(block.getVersionFlags().toByteArray(), expected.toByteArray());

        // Set some of the version flags
        block = new AltcoinBlock(params, 0x00001700);
        expected.clear(0, 8);
        expected.set(0, 3);
        expected.set(4);
        assertArrayEquals(block.getVersionFlags().toByteArray(), expected.toByteArray());
    }
}
