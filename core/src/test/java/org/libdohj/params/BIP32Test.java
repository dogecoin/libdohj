/*
 * Copyright 2013 Matija Mazi.
 * Copyright 2014 Andreas Schildbach
 * Copyright 2021 Google Inc.
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

import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.bitcoinj.core.Utils.HEX;
import static org.junit.Assert.assertEquals;

/**
 * Test encoding Dogecoin BIP32 extended public and private keys. Although this
 * key format (starting `dgub`, `dgpv`) should never have existed, it was
 * introduced in early Dogecoin and as extended public/private keys saw little
 * use until 2021-ish, it was then left as-is until third party implementations
 * started diverging.
 *
 * `xpub` and `xprv` prefixes are more correct, given extended public/private keys
 * are designed to be shared across multiple chains. Remember that the chain a
 * derived key "belongs" to is part of the derivation path ( https://wiki.trezor.io/Address_path_(BIP32) ).
 * There is a list of chain IDs uesd in derivation paths at https://github.com/satoshilabs/slips/blob/master/slip-0044.md
 *
 * See also https://github.com/dogecoin/dogecoin/issues/2344 for context.
 */
public class BIP32Test {
    private static final Logger log = LoggerFactory.getLogger(BIP32Test.class);
    private static final NetworkParameters DOGECOIN_MAIN_NET = DogecoinMainNetParams.get();

    @Test
    public void testVector1() throws Exception {
        // These test values are copied from https://github.com/dogecoin/dogecoin/blob/1.21-dev/src/test/bip32_tests.cpp#L40
        TestCase tv = new TestCase(
                "000102030405060708090a0b0c0d0e0f",
                "dgpv51eADS3spNJh9Gjth94XcPwAczvQaDJs9rqx11kvxKs6r3Ek8AgERHhjLs6mzXQFHRzQqGwqdeoDkZmr8jQMBfi43b7sT3sx3cCSk5fGeUR",
                "dgub8kXBZ7ymNWy2S8Q3jNgVjFUm5ZJ3QLLaSTdAA89ukSv7Q6MSXwE14b7Nv6eDpE9JJXinTKc8LeLVu19uDPrm5uJuhpKNzV2kAgncwo6bNpP",
                Collections.singletonList(
                        new TestCase.DerivedTestCase(
                                "Test m/0H",
                                new ChildNumber[]{new ChildNumber(0, true)},
                                "dgpv53uaD9MLudRgHssbttwAVS3GwpUkxHnsqUGqy793vX4PDKXvYQDKYS4988T7QEnCzUt7CaGi21e6UKoZnKgXyjna7To1h1aqkcqJBDM65ur",
                                "dgub8nnbYqHETn61ajXkw8Z8cHasQNrPnQpb85448DY2ie7PmNecxAm6BjTnhNCvZY3qJk1MKZ9Z5HQasQ83ARb99nmduT7dunvxgcvBFVHuvrq"
                        )
                )
        );
        DeterministicKey masterPrivateKey = HDKeyDerivation.createMasterPrivateKey(HEX.decode(tv.seed));
        assertEquals(testEncode(tv.priv), testEncode(masterPrivateKey.serializePrivB58(DOGECOIN_MAIN_NET)));
        assertEquals(testEncode(tv.pub), testEncode(masterPrivateKey.serializePubB58(DOGECOIN_MAIN_NET)));
        DeterministicHierarchy dh = new DeterministicHierarchy(masterPrivateKey);
        for (int i = 0; i < tv.derived.size(); i++) {
            TestCase.DerivedTestCase tc = tv.derived.get(i);
            log.info("{}", tc.name);
            assertEquals(tc.name, String.format(Locale.US, "Test %s", tc.getPathDescription()));
            int depth = tc.path.length - 1;
            DeterministicKey ehkey = dh.deriveChild(Arrays.asList(tc.path).subList(0, depth), false, true, tc.path[depth]);
            assertEquals(testEncode(tc.priv), testEncode(ehkey.serializePrivB58(DOGECOIN_MAIN_NET)));
            assertEquals(testEncode(tc.pub), testEncode(ehkey.serializePubB58(DOGECOIN_MAIN_NET)));
        }
    }

    private String testEncode(String what) {
        return HEX.encode(Base58.decodeChecked(what));
    }

    static class TestCase {
        final String seed;
        final String priv;
        final String pub;
        final List<DerivedTestCase> derived;

        TestCase(String seed, String priv, String pub, List<DerivedTestCase> derived) {
            this.seed = seed;
            this.priv = priv;
            this.pub = pub;
            this.derived = derived;
        }

        static class DerivedTestCase {
            final String name;
            final ChildNumber[] path;
            final String pub;
            final String priv;

            DerivedTestCase(String name, ChildNumber[] path, String priv, String pub) {
                this.name = name;
                this.path = path;
                this.pub = pub;
                this.priv = priv;
            }

            String getPathDescription() {
                return "m/" + Joiner.on("/").join(Arrays.stream(path).map(Functions.toStringFunction()::apply).collect(Collectors.toList()));
            }
        }
    }
}
