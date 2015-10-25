package org.libdohj.core;

import org.bitcoinj.core.*;
import org.bitcoinj.core.Utils;

/**
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

    @Override
    public FilteredBlock makeFilteredBlock(byte[] payloadBytes) throws ProtocolException {
        long blockVersion = Utils.readUint32(payloadBytes, 0);
        int headerSize = Block.HEADER_SIZE;

        byte[] headerBytes = new byte[Block.HEADER_SIZE + 1];
        System.arraycopy(payloadBytes, 0, headerBytes, 0, headerSize);
        headerBytes[80] = 0; // Need to provide 0 transactions so the block header can be constructed

        if (this.getParameters() instanceof AuxPoWNetworkParameters) {
            final AuxPoWNetworkParameters auxPoWParams = (AuxPoWNetworkParameters) this.getParameters();
            if (auxPoWParams.isAuxPoWBlockVersion(blockVersion)) {
                final AltcoinBlock header = (AltcoinBlock) makeBlock(headerBytes, 0, Message.UNKNOWN_LENGTH);
                final AuxPoW auxpow = new AuxPoW(this.getParameters(), payloadBytes, Block.HEADER_SIZE, null, this);
                header.setAuxPoW(auxpow);

                int pmtOffset = headerSize + auxpow.getMessageSize();
                int pmtLength = payloadBytes.length - pmtOffset;
                byte[] pmtBytes = new byte[pmtLength];
                System.arraycopy(payloadBytes, pmtOffset, pmtBytes, 0, pmtLength);
                PartialMerkleTree pmt = new PartialMerkleTree(this.getParameters(), pmtBytes, 0);

                return new FilteredBlock(this.getParameters(), header, pmt);
            }
        }

        // We are either not in AuxPoW mode, or the block is not an AuxPoW block.
        return super.makeFilteredBlock(payloadBytes);
    }
}
