/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitcoinj.core;

import org.libdohj.params.DogecoinMainNetParams;
import org.bitcoinj.params.MainNetParams;

/**
 *
 * @author jrn
 */
public class ConvertPrivateKey {
    public static void main(final String[] argv) throws AddressFormatException {
        final NetworkParameters mainParams = MainNetParams.get();
        final NetworkParameters dogeParams = DogecoinMainNetParams.get();
        final DumpedPrivateKey key = new DumpedPrivateKey(mainParams, "5HpHagT65TZzG1PH3CSu63k8DbpvD8s5ip4nEB3kEsreAnchuDf");
        final DumpedPrivateKey newKey = new DumpedPrivateKey(dogeParams, key.getKey().getPrivKeyBytes(), false);
        System.out.println(newKey.toString());
    }
}
