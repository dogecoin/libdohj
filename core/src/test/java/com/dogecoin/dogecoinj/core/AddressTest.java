/**
 * Copyright 2011 Google Inc.
 * Copyright 2014 Andreas Schildbach
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

package com.dogecoin.dogecoinj.core;

import com.dogecoin.dogecoinj.params.MainNetParams;
import com.dogecoin.dogecoinj.params.Networks;
import com.dogecoin.dogecoinj.params.TestNet3Params;
import com.dogecoin.dogecoinj.script.Script;
import com.dogecoin.dogecoinj.script.ScriptBuilder;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.dogecoin.dogecoinj.core.Utils.HEX;
import static org.junit.Assert.*;

public class AddressTest {
    static final NetworkParameters testParams = TestNet3Params.get();
    static final NetworkParameters mainParams = MainNetParams.get();

    @Test
    public void stringification() throws Exception {
        // Test a testnet address.
        Address a = new Address(testParams, HEX.decode("8e34b02b3c2552791c2151394a1958fe8d40348d"));
        assertEquals("nhA5LMB3mtmVf1xNsHnoGakmFyq1fuzyb7", a.toString());
        assertFalse(a.isP2SHAddress());

        Address b = new Address(mainParams, HEX.decode("6bf21708a0ee6cabde2f3bec6f7880c29b1b1d7e"));
        assertEquals("DEyrrVQspH26SS2wjfZdFT579NLBto1x64", b.toString());
        assertFalse(b.isP2SHAddress());
    }
    
    @Test
    public void decoding() throws Exception {
        Address a = new Address(testParams, "nhA5LMB3mtmVf1xNsHnoGakmFyq1fuzyb7");
        assertEquals("8e34b02b3c2552791c2151394a1958fe8d40348d", Utils.HEX.encode(a.getHash160()));

        Address b = new Address(mainParams, "DEyrrVQspH26SS2wjfZdFT579NLBto1x64");
        assertEquals("6bf21708a0ee6cabde2f3bec6f7880c29b1b1d7e", Utils.HEX.encode(b.getHash160()));
    }
    
    @Test
    public void errorPaths() {
        // Check what happens if we try and decode garbage.
        try {
            new Address(testParams, "this is not a valid address!");
            fail();
        } catch (WrongNetworkException e) {
            fail();
        } catch (AddressFormatException e) {
            // Success.
        }

        // Check the empty case.
        try {
            new Address(testParams, "");
            fail();
        } catch (WrongNetworkException e) {
            fail();
        } catch (AddressFormatException e) {
            // Success.
        }

        // Check the case of a mismatched network.
        try {
            new Address(testParams, "DEyrrVQspH26SS2wjfZdFT579NLBto1x64");
            fail();
        } catch (WrongNetworkException e) {
            // Success.
            assertEquals(e.verCode, MainNetParams.get().getAddressHeader());
            assertTrue(Arrays.equals(e.acceptableVersions, TestNet3Params.get().getAcceptableAddressCodes()));
        } catch (AddressFormatException e) {
            fail();
        }
    }

    @Test
    public void getNetwork() throws Exception {
        NetworkParameters params = Address.getParametersFromAddress("DEyrrVQspH26SS2wjfZdFT579NLBto1x64");
        assertEquals(MainNetParams.get().getId(), params.getId());
        params = Address.getParametersFromAddress("nhA5LMB3mtmVf1xNsHnoGakmFyq1fuzyb7");
        assertEquals(TestNet3Params.get().getId(), params.getId());
    }

    @Test
    public void getAltNetwork() throws Exception {
        // An alternative network
        class AltNetwork extends MainNetParams {
            AltNetwork() {
                super();
                id = "alt.network";
                addressHeader = 48;
                p2shHeader = 5;
                acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
            }
        }
        AltNetwork altNetwork = new AltNetwork();
        // Add new network params
        Networks.register(altNetwork);
        // Check if can parse address
        NetworkParameters params = Address.getParametersFromAddress("LLxSnHLN2CYyzB5eWTR9K9rS9uWtbTQFb6");
        assertEquals(altNetwork.getId(), params.getId());
        // Check if main network works as before
        params = Address.getParametersFromAddress("DEyrrVQspH26SS2wjfZdFT579NLBto1x64");
        assertEquals(MainNetParams.get().getId(), params.getId());
        // Unregister network
        Networks.unregister(altNetwork);
        try {
            Address.getParametersFromAddress("LLxSnHLN2CYyzB5eWTR9K9rS9uWtbTQFb6");
            fail();
        } catch (AddressFormatException e) { }
    }
    
    @Test
    public void p2shAddress() throws Exception {
        // Test that we can construct P2SH addresses
        Address mainNetP2SHAddress = new Address(MainNetParams.get(), "9wWHL91mYrdiBEw9uHuBtS42in2XqWKrRY");
        assertEquals(mainNetP2SHAddress.version, MainNetParams.get().p2shHeader);
        assertTrue(mainNetP2SHAddress.isP2SHAddress());
        Address testNetP2SHAddress = new Address(TestNet3Params.get(), "2N8jyJyivK4trjisMYPHyMsdnTyWVDTWxaL");
        assertEquals(testNetP2SHAddress.version, TestNet3Params.get().p2shHeader);
        assertTrue(testNetP2SHAddress.isP2SHAddress());

        // Test that we can determine what network a P2SH address belongs to
        NetworkParameters mainNetParams = Address.getParametersFromAddress("9wWHL91mYrdiBEw9uHuBtS42in2XqWKrRY");
        assertEquals(MainNetParams.get().getId(), mainNetParams.getId());
        NetworkParameters testNetParams = Address.getParametersFromAddress("2N8jyJyivK4trjisMYPHyMsdnTyWVDTWxaL");
        assertEquals(TestNet3Params.get().getId(), testNetParams.getId());

        // Test that we can convert them from hashes
        byte[] hex = HEX.decode("379ad9b7ba73bdc1e29e286e014d4e2e1f6884e3");
        Address a = Address.fromP2SHHash(mainParams, hex);
        assertEquals("9wWHL91mYrdiBEw9uHuBtS42in2XqWKrRY", a.toString());
        Address b = Address.fromP2SHHash(testParams, HEX.decode("a9f9b28507bbe69c13eaed4f880bb22d17450b56"));
        assertEquals("2N8jyJyivK4trjisMYPHyMsdnTyWVDTWxaL", b.toString());
        Address c = Address.fromP2SHScript(mainParams, ScriptBuilder.createP2SHOutputScript(hex));
        assertEquals("9wWHL91mYrdiBEw9uHuBtS42in2XqWKrRY", c.toString());
    }

    @Test
    public void p2shAddressCreationFromKeys() throws Exception {
        // import some keys from this example: https://gist.github.com/gavinandresen/3966071
        ECKey key1 = new DumpedPrivateKey(mainParams, "QVUd4dwqxqePZgBaC6ny5rHvNHu6CoT8t1sTTPnF5RfFAjtKjTQH").getKey();
        key1 = ECKey.fromPrivate(key1.getPrivKeyBytes());
        ECKey key2 = new DumpedPrivateKey(mainParams, "QTZSzo8RphsaJFiEJAEvjvRqqP9MVyWpT1ns9hVRij4nXTE3XTzP").getKey();
        key2 = ECKey.fromPrivate(key2.getPrivKeyBytes());
        ECKey key3 = new DumpedPrivateKey(mainParams, "QS2YZKyPB6nDH7WnMuT4YKwLQwZQ3vN2FCPTwTCeyUNSKeRyPgRk").getKey();
        key3 = ECKey.fromPrivate(key3.getPrivKeyBytes());

        List<ECKey> keys = Arrays.asList(key1, key2, key3);
        Script p2shScript = ScriptBuilder.createP2SHOutputScript(2, keys);
        Address address = Address.fromP2SHScript(mainParams, p2shScript);
        assertEquals("ACdJj7YT7dJkV6bv6cRenUMCTDQxSdZSo5", address.toString());
    }
}
