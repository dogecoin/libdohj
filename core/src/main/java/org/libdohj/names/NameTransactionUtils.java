/*
 * Copyright 2016 Jeremy Rand.
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

import org.libdohj.script.NameScript;

import org.bitcoinj.script.ScriptException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;

import java.io.UnsupportedEncodingException;

// TODO: document this

// TODO: look into allowing name and value to be ArrayList<byte> as an alternative to String.

public class NameTransactionUtils {
    
    // Providing the name is, in theory, superfluous, since only 1 name output can exist per transaction.
    // However, this might be changed in the future, to allow atomic updates of multiple names.
    // This could enable things like CoinJoin for names.
    public static TransactionOutput getNameAnyUpdateOutput(Transaction tx, String name) {
        for (TransactionOutput output : tx.getOutputs()) {
            try {
                Script scriptPubKey = output.getScriptPubKey();
                NameScript ns = new NameScript(scriptPubKey);
                if(ns.isNameOp() && ns.isAnyUpdate() && new String(ns.getOpName().data, "ISO-8859-1").equals(name)) {
                    return output;
                }
            } catch (ScriptException e) {
                continue;
            } catch (UnsupportedEncodingException e) {
                continue;
            }
        }
        
        // No such output was found.
        return null;
    }
    
    public static NameScript getNameAnyUpdateScript(Transaction tx, String name) {
        TransactionOutput output = getNameAnyUpdateOutput(tx, name);
        
        if (output == null) {
            return null;
        }
        
        Script scriptPubKey = output.getScriptPubKey();
        return new NameScript(scriptPubKey);
    }
    
    public static String getNameValueAsString(Transaction tx, String name) throws UnsupportedEncodingException {
        NameScript ns = getNameAnyUpdateScript(tx, name);
        
        if (ns == null) {
            return null;
        }
        
        return new String(ns.getOpValue().data, "ISO-8859-1");
    }
}
