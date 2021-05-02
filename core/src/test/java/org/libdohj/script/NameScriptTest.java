/*
 * Copyright 2016 Jeremy Rand
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
package org.libdohj.script;

import org.libdohj.params.AbstractNamecoinParams;
import org.libdohj.params.NamecoinMainNetParams;

import org.bitcoinj.core.Context;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Util;
import org.bitcoinj.script.Script;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * @author Jeremy Rand
 */
public class NameScriptTest {
    private static final AbstractNamecoinParams params = NamecoinMainNetParams.get();

    @Before
    public void setUp() throws Exception {
        Context context = new Context(params);
    }

    @Test
    public void nameNewIsNameOp() throws IOException {
        final NameScript ns = getNameNewNameScript();

        assertTrue(ns.isNameOp());
    }

    @Test
    public void nameNewGetAddress() throws IOException {
        final NameScript ns = getNameNewNameScript();

        assertEquals("MyVbKbD4MYNUMEpdNAm3Jd3nbr5t8djALC", ns.getAddress().getToAddress(params).toString());
    }

    // TODO: getNameOp when it's implemented

    @Test
    public void nameNewIsAnyUpdate() throws IOException {
        final NameScript ns = getNameNewNameScript();

        assertFalse(ns.isAnyUpdate());
    }

    @Test
    public void nameNewGetOpName() throws IOException {
        final NameScript ns = getNameNewNameScript();

        assertThrows("Not an AnyUpdate op",
                org.bitcoinj.script.ScriptException.class, () -> ns.getOpName());
    }

    @Test
    public void nameNewGetOpValue() throws IOException {
        final NameScript ns = getNameNewNameScript();

        assertThrows("Not an AnyUpdate op",
                org.bitcoinj.script.ScriptException.class, () -> ns.getOpValue());

    }

    // TODO: getOpRand, getOpHash, isNameScript when they're implemented

    @Test
    public void nameFirstUpdateIsNameOp() throws IOException {
        final NameScript ns = getNameFirstUpdateNameScript();

        assertTrue(ns.isNameOp());
    }

    @Test
    public void nameFirstUpdateGetAddress() throws IOException {
        final NameScript ns = getNameFirstUpdateNameScript();

        assertEquals("NGcTVLgw6cgdavaE7C9QvWaY7gKiWbLrjP", ns.getAddress().getToAddress(params).toString());
    }

    // TODO: getNameOp when it's implemented

    @Test
    public void nameFirstUpdateIsAnyUpdate() throws IOException {
        final NameScript ns = getNameFirstUpdateNameScript();

        assertTrue(ns.isAnyUpdate());
    }

    @Test
    public void nameFirstUpdateGetOpName() throws IOException {
        final NameScript ns = getNameFirstUpdateNameScript();

        assertEquals("d/bitcoin", new String(ns.getOpName().data, "ISO-8859-1"));
    }

    @Test
    public void nameFirstUpdateGetOpValue() throws IOException {
        final NameScript ns = getNameFirstUpdateNameScript();

        assertEquals("webpagedeveloper.me/namecoin", new String(ns.getOpValue().data, "ISO-8859-1"));
    }

    // TODO: getOpRand, getOpHash, isNameScript when they're implemented

    @Test
    public void nameUpdateIsNameOp() throws IOException {
        final NameScript ns = getNameUpdateNameScript();

        assertTrue(ns.isNameOp());
    }

    @Test
    public void nameUpdateGetAddress() throws IOException {
        final NameScript ns = getNameUpdateNameScript();

        assertEquals("N9dLs1zHRfZr5cJNjSrvhWrrUcmNSthdmz", ns.getAddress().getToAddress(params).toString());
    }

    // TODO: getNameOp when it's implemented

    @Test
    public void nameUpdateIsAnyUpdate() throws IOException {
        final NameScript ns = getNameUpdateNameScript();

        assertTrue(ns.isAnyUpdate());
    }

    @Test
    public void nameUpdateGetOpName() throws IOException {
        final NameScript ns = getNameUpdateNameScript();

        assertEquals("d/bitcoin", new String(ns.getOpName().data, "ISO-8859-1"));
    }

    @Test
    public void nameUpdateGetOpValue() throws IOException {
        final NameScript ns = getNameUpdateNameScript();

        assertEquals("{\"info\":{\"registrar\":\"http://register.dot-bit.org\"},\"email\": \"register@dot-bit.org\",\"ns\":[\"ns0.web-sweet-web.net\",\"ns1.web-sweet-web.net\"],\"map\":{\"\":{\"ns\":[\"ns0.web-sweet-web.net\",\"ns1.web-sweet-web.net\"]}}}", new String(ns.getOpValue().data, "ISO-8859-1"));
    }

    // TODO: getOpRand, getOpHash, isNameScript when they're implemented

    @Test
    public void currencyIsNameOp() throws IOException {
        final NameScript ns = getCurrencyNameScript();

        assertFalse(ns.isNameOp());
    }

    @Test
    public void currencyGetAddress() throws IOException {
        final NameScript ns = getCurrencyNameScript();

        assertEquals("NCMmrGC7uaJ3uv8feLgBTtwGLQSWfmxMCk", ns.getAddress().getToAddress(params).toString());
    }

    // TODO: getNameOp when it's implemented

    @Test
    public void currencyIsAnyUpdate() throws IOException {
        final NameScript ns = getCurrencyNameScript();

        assertThrows("Not a name op",
                org.bitcoinj.script.ScriptException.class, () -> ns.isAnyUpdate());
    }

    @Test
    public void currencyGetOpName() throws IOException {
        final NameScript ns = getCurrencyNameScript();

        assertThrows("Not an AnyUpdate op",
                org.bitcoinj.script.ScriptException.class, () -> ns.getOpName());
    }

    @Test
    public void currencyGetOpValue() throws IOException {
        final NameScript ns = getCurrencyNameScript();

        assertThrows("Not an AnyUpdate op",
                org.bitcoinj.script.ScriptException.class, () -> ns.getOpValue());
    }

    // TODO: getOpRand, getOpHash, isNameScript when they're implemented

    @Test
    public void returnIsNameOp() throws IOException {
        final NameScript ns = getReturnNameScript();

        assertFalse(ns.isNameOp());
    }

    @Test
    public void returnGetAddress() throws IOException {
        final NameScript ns = getReturnNameScript();

        assertThrows("Cannot cast this script to an address",
                org.bitcoinj.script.ScriptException.class,
                () -> ns.getAddress().getToAddress(params).toString());
    }

    // TODO: getNameOp when it's implemented

    @Test
    public void returnIsAnyUpdate() throws IOException {
        final NameScript ns = getReturnNameScript();

        assertThrows("Not a name op",
                org.bitcoinj.script.ScriptException.class,
                () -> ns.isAnyUpdate());
    }

    @Test
    public void returnGetOpName() throws IOException {
        final NameScript ns = getReturnNameScript();

        assertThrows("Not an AnyUpdate op",
                org.bitcoinj.script.ScriptException.class,
                () -> ns.getOpName());
    }

    @Test
    public void returnGetOpValue() throws IOException {
        final NameScript ns = getReturnNameScript();

        assertThrows("Not an AnyUpdate op",
                org.bitcoinj.script.ScriptException.class,
                () -> ns.getOpValue());
    }

    // TODO: getOpRand, getOpHash, isNameScript when they're implemented

    NameScript getNameNewNameScript() throws IOException {
        byte[] payload;
        final Transaction tx;
        final TransactionOutput out;
        final Script outScript;
        final NameScript ns;

        // https://namecoin.webbtc.com/tx/6047ce28a076118403aa960909c9c4d0056f97ee0da4d37d109515f8367e2ccb

        payload = Util.getBytes(getClass().getResourceAsStream("namecoin_name_new_d_bitcoin.bin"));
        tx = new Transaction(params, payload);
        out = tx.getOutputs().get(1);
        outScript = out.getScriptPubKey();
        ns = new NameScript(outScript);

        return ns;
    }

    NameScript getNameFirstUpdateNameScript() throws IOException {
        byte[] payload;
        final Transaction tx;
        final TransactionOutput out;
        final Script outScript;
        final NameScript ns;

        // https://namecoin.webbtc.com/tx/ab1207bd605af57ed0b5325ac94d19578cff3bce668ebe8dda2f42a00b001f5d

        payload = Util.getBytes(getClass().getResourceAsStream("namecoin_name_firstupdate_d_bitcoin.bin"));
        tx = new Transaction(params, payload);
        out = tx.getOutputs().get(1);
        outScript = out.getScriptPubKey();
        ns = new NameScript(outScript);

        return ns;
    }

    NameScript getNameUpdateNameScript() throws IOException {
        byte[] payload;
        final Transaction tx;
        final TransactionOutput out;
        final Script outScript;
        final NameScript ns;

        // https://namecoin.webbtc.com/tx/3376c5e0e5b69d0a104863de8432d7c13f891065e7628a72487b770c6418d397

        payload = Util.getBytes(getClass().getResourceAsStream("namecoin_name_update_d_bitcoin.bin"));
        tx = new Transaction(params, payload);
        out = tx.getOutputs().get(1);
        outScript = out.getScriptPubKey();
        ns = new NameScript(outScript);

        return ns;
    }

    NameScript getCurrencyNameScript() throws IOException {
        byte[] payload;
        final Transaction tx;
        final TransactionOutput out;
        final Script outScript;
        final NameScript ns;

        // https://namecoin.webbtc.com/tx/4ea5d679d63ef46449a44ca056584a986412676641bdaf13d44a7c7c2e32cca1

        payload = Util.getBytes(getClass().getResourceAsStream("namecoin_p2pkh.bin"));
        tx = new Transaction(params, payload);
        out = tx.getOutputs().get(0);
        outScript = out.getScriptPubKey();
        ns = new NameScript(outScript);

        return ns;
    }

    NameScript getReturnNameScript() throws IOException {
        byte[] payload;
        final Transaction tx;
        final TransactionOutput out;
        final Script outScript;
        final NameScript ns;

        // https://namecoin.webbtc.com/tx/ab1207bd605af57ed0b5325ac94d19578cff3bce668ebe8dda2f42a00b001f5d

        payload = Util.getBytes(getClass().getResourceAsStream("namecoin_name_firstupdate_d_bitcoin.bin"));
        tx = new Transaction(params, payload);
        out = tx.getOutputs().get(2);
        outScript = out.getScriptPubKey();
        ns = new NameScript(outScript);

        return ns;
    }
}
