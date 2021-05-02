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
package org.libdohj.names;

import org.junit.Before;
import org.junit.Test;
import org.libdohj.params.AbstractNamecoinParams;
import org.libdohj.params.NamecoinMainNetParams;

import org.bitcoinj.core.Context;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;

/**
 *
 * @author Jeremy Rand
 */
public class NameTransactionUtilsTest {
    private static final AbstractNamecoinParams params = NamecoinMainNetParams.get();

    @Before
    public void setUp() throws Exception {
        Context context = new Context(params);
    }
    
    @Test
    public void nameNewGetValueAsString() throws IOException {
        final Transaction tx = getNameNewTransaction();
        
        assertNull(NameTransactionUtils.getNameValueAsString(tx, "d/bitcoin"));
        
        assertNull(NameTransactionUtils.getNameValueAsString(tx, "wrongname"));
    }
    
    @Test
    public void nameFirstUpdateGetValueAsString() throws IOException {
        final Transaction tx = getNameFirstUpdateTransaction();
        
        assertEquals("webpagedeveloper.me/namecoin", NameTransactionUtils.getNameValueAsString(tx, "d/bitcoin"));
        
        assertNull(NameTransactionUtils.getNameValueAsString(tx, "wrongname"));
    }
    
    @Test
    public void nameUpdateGetValueAsString() throws IOException {
        final Transaction tx = getNameUpdateTransaction();
        
        assertEquals("{\"info\":{\"registrar\":\"http://register.dot-bit.org\"},\"email\": \"register@dot-bit.org\",\"ns\":[\"ns0.web-sweet-web.net\",\"ns1.web-sweet-web.net\"],\"map\":{\"\":{\"ns\":[\"ns0.web-sweet-web.net\",\"ns1.web-sweet-web.net\"]}}}", NameTransactionUtils.getNameValueAsString(tx, "d/bitcoin"));
        
        assertNull(NameTransactionUtils.getNameValueAsString(tx, "wrongname"));
    }
    
    @Test
    public void currencyGetValueAsString() throws IOException {
        final Transaction tx = getCurrencyTransaction();
        
        assertNull(NameTransactionUtils.getNameValueAsString(tx, "d/bitcoin"));
        
        assertNull(NameTransactionUtils.getNameValueAsString(tx, "wrongname"));
    }
    
    Transaction getNameNewTransaction() throws IOException {
        byte[] payload;
        final Transaction tx;
        
        // https://namecoin.webbtc.com/tx/6047ce28a076118403aa960909c9c4d0056f97ee0da4d37d109515f8367e2ccb
        
        payload = Util.getBytes(getClass().getResourceAsStream("namecoin_name_new_d_bitcoin.bin"));
        tx = new Transaction(params, payload);
        
        return tx;
    }
    
    Transaction getNameFirstUpdateTransaction() throws IOException {
        byte[] payload;
        final Transaction tx;
        
        // https://namecoin.webbtc.com/tx/ab1207bd605af57ed0b5325ac94d19578cff3bce668ebe8dda2f42a00b001f5d
        
        payload = Util.getBytes(getClass().getResourceAsStream("namecoin_name_firstupdate_d_bitcoin.bin"));
        tx = new Transaction(params, payload);
        
        return tx;
    }
    
    Transaction getNameUpdateTransaction() throws IOException {
        byte[] payload;
        final Transaction tx;
        
        // https://namecoin.webbtc.com/tx/3376c5e0e5b69d0a104863de8432d7c13f891065e7628a72487b770c6418d397
        
        payload = Util.getBytes(getClass().getResourceAsStream("namecoin_name_update_d_bitcoin.bin"));
        tx = new Transaction(params, payload);
        
        return tx;
    }
    
    Transaction getCurrencyTransaction() throws IOException {
        byte[] payload;
        final Transaction tx;
        
        // https://namecoin.webbtc.com/tx/4ea5d679d63ef46449a44ca056584a986412676641bdaf13d44a7c7c2e32cca1
        
        payload = Util.getBytes(getClass().getResourceAsStream("namecoin_p2pkh.bin"));
        tx = new Transaction(params, payload);
        
        return tx;
    }
}
