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

import org.bitcoinj.core.AltcoinBlock;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Context;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author Ross Nicoll
 */
public class AbstractDogecoinParamsTest {
    private static final AbstractDogecoinParams params = DogecoinMainNetParams.get();

    @Before
    public void setUp() throws Exception {
        Context context = new Context(params);
    }

    @Test
    public void shouldCalculateBitcoinLikeDifficulty() {
        int previousHeight = 239;
        long previousBlockTime = 1386475638; // Block 239
        long lastRetargetDifficulty = 0x1e0ffff0;
        long lastRetargetTime = 1386474927; // Block 1
        long nextDifficulty = 0x1e00ffff;
        long newDifficulty =
            params.calculateNewDifficultyTargetInner(previousHeight, previousBlockTime,
                lastRetargetDifficulty, lastRetargetTime, nextDifficulty);
        assertEquals(0x1e00ffff, newDifficulty);

        previousHeight = 9599;
        previousBlockTime = 1386954113;
        lastRetargetDifficulty = 0x1c1a1206;
        lastRetargetTime = 1386942008; // Block 9359
        nextDifficulty = 0x1c15ea59;
        newDifficulty =
            params.calculateNewDifficultyTargetInner(previousHeight, previousBlockTime,
                lastRetargetDifficulty, lastRetargetTime, nextDifficulty);
        assertEquals(0x1c15ea59, newDifficulty);
    }

    /**
     * Test block 720, where the time interval is below the minimum time interval
     * (900 seconds).
     */
    @Test
    public void shouldConstrainActualTime() {
        final int previousHeight = 719;
        final long previousBlockTime = 1386476362; // Block 719
        final long lastRetargetDifficulty = 0x1e00ffff;
        final long lastRetargetTime = 1386475840; // Block 479
        final long nextDifficulty = 0x1d0ffff0; // Block 720
        final long newDifficulty =
            params.calculateNewDifficultyTargetInner(previousHeight, previousBlockTime,
                lastRetargetDifficulty, lastRetargetTime, nextDifficulty);
        assertEquals(0x1d0ffff0, newDifficulty);
    }

    @Test
    public void shouldCalculateDigishieldDifficulty() {
        final int previousHeight = 145000;
        final long previousBlockTime = 1395094679;
        final long lastRetargetDifficulty = 0x1b499dfd;
        final long lastRetargetTime = 1395094427;
        final long nextDifficulty = 0x1b671062;
        final long newDifficulty =
            params.calculateNewDifficultyTargetInner(previousHeight, previousBlockTime,
                lastRetargetDifficulty, lastRetargetTime, nextDifficulty);
        assertEquals(0x1b671062, newDifficulty);
    }

    @Test
    public void shouldCalculateDigishieldDifficultyRounding() {
        // Test case for correct rounding of modulated time
        final int previousHeight = 145001;
        final long previousBlockTime = 1395094727;
        final long lastRetargetDifficulty = 0x1b671062;
        final long lastRetargetTime = 1395094679;
        final long nextDifficulty = 0x1b6558a4;
        final long newDifficulty =
            params.calculateNewDifficultyTargetInner(previousHeight, previousBlockTime,
                lastRetargetDifficulty, lastRetargetTime, nextDifficulty);
        assertEquals(0x1b6558a4, newDifficulty);
    }

    @Test
    public void shouldCalculateRetarget() throws IOException {
        // Do a more in-depth test for the first retarget
        final String[][] blocks = {
            {"dogecoin_block239.bin", "f9533416310fc4484cf43405a858b06afc9763ad401d267c1835d77e7d225a4e"},
            {"dogecoin_block479.bin", "ed83c923b532835f6597f70def42910aa9e06880e8a19b68f6b4a787f2b4b69f"},
            {"dogecoin_block480.bin", "a0e6d1cdef02b394d31628c3281f67e8534bec74fda1a4294b58be80c3fdf3f3"},
            {"dogecoin_block719.bin", "82e56e141ccfe019d475382d9a108ef86afeb297d95443dfd7250e57af805696"},
            {"dogecoin_block720.bin", "6b34f1a7de1954beb0ddf100bb2b618ff0183b6ae2b4a9376721ef8e04ab3b39"}
        };
        final BlockLoader loader = new BlockLoader(params);
        final Map<String, AltcoinBlock> loadedBlocks = loader.loadBlocks(blocks);
        final AltcoinBlock block239 = loadedBlocks.get("f9533416310fc4484cf43405a858b06afc9763ad401d267c1835d77e7d225a4e");
        final AltcoinBlock block479 = loadedBlocks.get("ed83c923b532835f6597f70def42910aa9e06880e8a19b68f6b4a787f2b4b69f");
        final AltcoinBlock block480 = loadedBlocks.get("a0e6d1cdef02b394d31628c3281f67e8534bec74fda1a4294b58be80c3fdf3f3");
        final AltcoinBlock block719 = loadedBlocks.get("82e56e141ccfe019d475382d9a108ef86afeb297d95443dfd7250e57af805696");
        final AltcoinBlock block720 = loadedBlocks.get("6b34f1a7de1954beb0ddf100bb2b618ff0183b6ae2b4a9376721ef8e04ab3b39");

        assertEquals(block480.getDifficultyTarget(), params.calculateNewDifficultyTargetInner(479, block479, block480, block239));
        assertEquals(block720.getDifficultyTarget(), params.calculateNewDifficultyTargetInner(719, block719, block720, block479));
    }

    /**
     * Confirm subsidy rules follow Dogecoin pattern.
     */
    @Test
    public void shouldCalculateSubsidy() {
        assertEquals(Coin.COIN.multiply(1000000), params.getBlockSubsidy(0));
        assertEquals(Coin.COIN.multiply(1000000), params.getBlockSubsidy(99999));
        assertEquals(Coin.COIN.multiply(500000), params.getBlockSubsidy(100000));
        assertEquals(Coin.COIN.multiply(500000), params.getBlockSubsidy(144999));
        assertEquals(Coin.COIN.multiply(250000), params.getBlockSubsidy(145000));
        assertEquals(Coin.COIN.multiply(250000), params.getBlockSubsidy(199999));
        assertEquals(Coin.COIN.multiply(125000), params.getBlockSubsidy(200000));
        assertEquals(Coin.COIN.multiply(125000), params.getBlockSubsidy(299999));
        assertEquals(Coin.COIN.multiply(62500), params.getBlockSubsidy(300000));
        assertEquals(Coin.COIN.multiply(62500), params.getBlockSubsidy(399999));
        assertEquals(Coin.COIN.multiply(31250), params.getBlockSubsidy(400000));
        assertEquals(Coin.COIN.multiply(31250), params.getBlockSubsidy(499999));
        assertEquals(Coin.COIN.multiply(15625), params.getBlockSubsidy(500000));
        assertEquals(Coin.COIN.multiply(15625), params.getBlockSubsidy(599999));
        assertEquals(Coin.COIN.multiply(10000), params.getBlockSubsidy(600000));
        assertEquals(Coin.COIN.multiply(10000), params.getBlockSubsidy(699999));
    }

    @Test
    public void targetSpacingShouldBe60() {
        // The getTargetSpacing() method only really exists for future expansion,
        // and currently should always return 60 seconds
        assertEquals(60, params.getTargetSpacing(0));
        assertEquals(60, params.getTargetSpacing(1));
        assertEquals(60, params.getTargetSpacing(params.getDigishieldBlockHeight() - 1));
        assertEquals(60, params.getTargetSpacing(params.getDigishieldBlockHeight()));
        assertEquals(60, params.getTargetSpacing(params.getDigishieldBlockHeight() + 1));
    }
}
