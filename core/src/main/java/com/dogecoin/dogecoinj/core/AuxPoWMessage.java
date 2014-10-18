/*
 * Copyright 2011 Google Inc.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This is the code to deserialize the AuxPoW header data
 */
public class AuxPoWMessage extends Message {
    private static final Logger log = LoggerFactory.getLogger(AuxPoWMessage.class);

    private AuxHeader header;

    public AuxPoWMessage(byte[] payload, int cursor) throws ProtocolException {
        this.payload = payload;
        this.cursor = cursor;
        this.header = new AuxHeader();
    }

    @Override
    void parse() throws ProtocolException {
        header.parentCoinbaseVerion = readUint32();
        header.parentCoinbaseTxInCount = readVarInt();
        header.parentCointbasePrevOut = readBytes(36); // Always the same on coinbase
        header.parentCoinbaseInScriptLength  = readVarInt();
        header.parentCoinbaseInScript = readBytes((int) header.parentCoinbaseInScriptLength); // Script length is limited so this cast should be fine.
        header.parentCoinBaseSequenceNumber = readUint32();
        header.parentCoinbaseTxOutCount = readVarInt();
        header.parentCoinbaseOuts = new ArrayList<AuxCoinbaseOut>();
        for (int i = 0; i < header.parentCoinbaseTxOutCount; i++) {
            AuxCoinbaseOut out = new AuxCoinbaseOut();
            out.amount = readInt64();
            out.scriptLength = readVarInt();
            out.script = readBytes((int) out.scriptLength); // Script length is limited so this cast should be fine.
            header.parentCoinbaseOuts.add(out);
        }
        header.parentCoinbaseLockTime = readUint32();

        header.parentBlockHeaderHash = readHash();
        header.numOfCoinbaseLinks = readVarInt();
        header.coinbaseLinks = new ArrayList<Sha256Hash>();
        for (int i = 0; i < header.numOfCoinbaseLinks; i++) {
            header.coinbaseLinks.add(readHash());
        }
        header.coinbaseBranchBitmask = readUint32();

        header.numOfAuxChainLinks = readVarInt();
        header.auxChainLinks = new ArrayList<Sha256Hash>();
        for (int i = 0; i < header.numOfAuxChainLinks; i++) {
            header.auxChainLinks.add(readHash());
        }
        header.auxChainBranchBitmask = readUint32();

        header.parentBlockVersion = readUint32();
        header.parentBlockPrev = readHash();
        header.parentBlockMerkleRoot = readHash();
        header.parentBlockTime = readUint32();
        header.parentBlockBits = readUint32();
        header.parentBlockNonce = readUint32();
    }

    @Override
    protected void parseLite() throws ProtocolException {
        // noop
    }

    public byte[] constructParentHeader() {

        ByteArrayOutputStream stream = new UnsafeByteArrayOutputStream(Block.HEADER_SIZE);
        try {
            Utils.uint32ToByteStreamLE(header.parentBlockVersion, stream);
            stream.write(Utils.reverseBytes(header.parentBlockPrev.getBytes()));
            stream.write(Utils.reverseBytes(header.parentBlockMerkleRoot.getBytes()));
            Utils.uint32ToByteStreamLE(header.parentBlockTime, stream);
            Utils.uint32ToByteStreamLE(header.parentBlockBits, stream);
            Utils.uint32ToByteStreamLE(header.parentBlockNonce, stream);
        } catch (IOException e) {
            throw new RuntimeException(); // Can't actually happen
        }
        return stream.toByteArray();
    }

    public class AuxHeader {

        // Parent coinbase
        public long parentCoinbaseVerion;
        public long parentCoinbaseTxInCount;
        public byte[] parentCointbasePrevOut;
        public long parentCoinbaseInScriptLength;
        public byte[] parentCoinbaseInScript;
        public long parentCoinBaseSequenceNumber;
        public long parentCoinbaseTxOutCount;
        public ArrayList<AuxCoinbaseOut> parentCoinbaseOuts;
        public long parentCoinbaseLockTime;

        // Coinbase link
        public Sha256Hash parentBlockHeaderHash;
        public long numOfCoinbaseLinks;
        public ArrayList<Sha256Hash> coinbaseLinks;
        public long coinbaseBranchBitmask;

        // Aux chanin link
        public long numOfAuxChainLinks;
        public ArrayList<Sha256Hash> auxChainLinks;
        public long auxChainBranchBitmask;

        // Parent block header
        public long parentBlockVersion;
        public Sha256Hash parentBlockPrev;
        public Sha256Hash parentBlockMerkleRoot;
        public long parentBlockTime;
        public long parentBlockBits;
        public long parentBlockNonce;
    }

    public class AuxCoinbaseOut {
        public long amount;
        public long scriptLength;
        public byte[] script;
    }
}