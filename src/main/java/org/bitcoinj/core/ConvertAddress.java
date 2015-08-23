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
public class ConvertAddress {
    public static void main(final String[] argv) throws AddressFormatException {
        final NetworkParameters mainParams = MainNetParams.get();
        final NetworkParameters dogeParams = DogecoinMainNetParams.get();
        final Address address = Address.fromBase58(mainParams, "175tWpb8K1S7NmH4Zx6rewF9WQrcZv245W");
        final Address newAddress = new Address(dogeParams, 30, address.getHash160());

        System.out.println(newAddress.toBase58());
    }
}
