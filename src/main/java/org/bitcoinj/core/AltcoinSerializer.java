    /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bitcoinj.core;

/**
 *
 * @author jrn
 */
public class AltcoinSerializer extends BitcoinSerializer {

    public AltcoinSerializer(NetworkParameters params, boolean parseLazy, boolean parseRetain) {
        super(params, parseLazy, parseRetain);
    }

    @Override
    public Block makeBlock(byte[] payloadBytes) throws ProtocolException {
        return new AltcoinBlock(getParameters(), payloadBytes, this, payloadBytes.length);
    }

    @Override
    public Block makeBlock(byte[] payloadBytes, int length) throws ProtocolException {
        return new AltcoinBlock(getParameters(), payloadBytes, this, length);
    }
}
