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

import java.io.IOException;
import org.bitcoinj.core.AltcoinBlock;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.Util;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.libdohj.core.AltcoinSerializer;

/**
 *
 * @author jrn
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
        long newDifficulty = params.getNewDifficultyTarget(previousHeight, previousBlockTime, lastRetargetDifficulty, lastRetargetTime);
        assertEquals(0x1e00ffff, newDifficulty);

        previousHeight = 9599;
        previousBlockTime = 1386954113;
        lastRetargetDifficulty = 0x1c1a1206;
        lastRetargetTime = 1386942008; // Block 9359
        newDifficulty = params.getNewDifficultyTarget(previousHeight, previousBlockTime, lastRetargetDifficulty, lastRetargetTime);
        assertEquals(0x1c15ea59, newDifficulty);
    }

    /**
     * Test block 720, where the time interval is below the minimum time interval
     * (900 seconds).
     */
    @Test
    public void shouldConstrainActualTime() {
        int previousHeight = 719;
        long previousBlockTime = 1386476362; // Block 719
        long lastRetargetDifficulty = 0x1e00ffff;
        long lastRetargetTime = 1386475840; // Block 439
        long newDifficulty = params.getNewDifficultyTarget(previousHeight, previousBlockTime, lastRetargetDifficulty, lastRetargetTime);
        assertEquals(0x1d0ffff0, newDifficulty);
    }

    @Test
    public void shouldCalculateDigishieldDifficulty() {
        int previousHeight = 145000;
        long previousBlockTime = 1395094679;
        long lastRetargetDifficulty = 0x1b499dfd;
        long lastRetargetTime = 1395094427;
        long newDifficulty = params.getNewDifficultyTarget(previousHeight, previousBlockTime, lastRetargetDifficulty, lastRetargetTime);
        assertEquals(0x1b671062, newDifficulty);
    }

    @Test
    public void shouldCalculateDigishieldDifficultyRounding() {
        // Test case for correct rounding of modulated time
        int previousHeight = 145001;
        long previousBlockTime = 1395094727;
        long lastRetargetDifficulty = 0x1b671062;
        long lastRetargetTime = 1395094679;
        long newDifficulty = params.getNewDifficultyTarget(previousHeight, previousBlockTime, lastRetargetDifficulty, lastRetargetTime);
        assertEquals(0x1b6558a4, newDifficulty);
    }

    @Test
    public void shouldCalculateRetarget() throws IOException {
        // Do a more in-depth test for the first retarget
        byte[] payload = Util.getBytes(getClass().getResourceAsStream("dogecoin_block239.bin"));
        AltcoinSerializer serializer = (AltcoinSerializer)params.getDefaultSerializer();
        final AltcoinBlock block239 = (AltcoinBlock)serializer.makeBlock(payload);
        final AltcoinBlock block479;
        final AltcoinBlock block719;

        payload = Util.getBytes(getClass().getResourceAsStream("dogecoin_block479.bin"));
        block479 = (AltcoinBlock)serializer.makeBlock(payload);

        payload = Util.getBytes(getClass().getResourceAsStream("dogecoin_block719.bin"));
        block719 = (AltcoinBlock)serializer.makeBlock(payload);

        assertEquals(0x1e00ffff, params.getNewDifficultyTarget(479, block239, block479));
        assertEquals(0x1d0ffff0, params.getNewDifficultyTarget(719, block479, block719));
    }
}
