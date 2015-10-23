    /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.libdohj.core;

import org.bitcoinj.core.AltcoinBlock;
import org.bitcoinj.core.BitcoinSerializer;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.ProtocolException;

/**
 *
 * @author jrn
 */
public class AltcoinSerializer extends BitcoinSerializer {

    public AltcoinSerializer(NetworkParameters params, boolean parseRetain) {
        super(params, parseRetain);
    }

    @Override
    public Block makeBlock(final byte[] payloadBytes, final int offset, final int length) throws ProtocolException {
        return new AltcoinBlock(getParameters(), payloadBytes, offset, this, length);
    }
}
