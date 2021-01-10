/*
 * Copyright 2016-2017 Jeremy Rand.
 * Based on LevelDBBlockStore.java, copyright the BitcoinJ authors.
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

import org.bitcoinj.core.AbstractBlockChain;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.core.listeners.NewBestBlockListener;
import org.bitcoinj.core.listeners.ReorganizeListener;
import org.bitcoinj.core.listeners.TransactionReceivedInBlockListener;
import org.bitcoinj.script.ScriptException;
import org.bitcoinj.script.Script;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.utils.Threading;

import org.fusesource.leveldbjni.*;
import org.iq80.leveldb.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

import java.io.*;
import java.nio.*;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

// TODO: dead blocks' name transactions are stored in memory indefinitely.  We should probably fix that, although doing so will slow down processing of reorgs.

public class NameLookupLatestLevelDBTransactionCache implements NameLookupLatest, NewBestBlockListener, ReorganizeListener, TransactionReceivedInBlockListener {
    
    
    protected static final byte[] CHAIN_HEAD_KEY = "Head".getBytes();
    protected static final byte[] HEIGHT_KEY = "Height".getBytes();
    
    protected BlockChain chain;
    protected BlockStore store;
    protected PeerGroup peerGroup;
    
    protected Context context;
    protected NetworkParameters params;
    protected File path;
    
    protected DB db;
    
    protected SetMultimap<Sha256Hash, Transaction> pendingBlockTransactions = Multimaps.synchronizedSetMultimap(HashMultimap.<Sha256Hash, Transaction>create());
    
    protected Logger log = LoggerFactory.getLogger(NameLookupLatestLevelDBTransactionCache.class);
    
    public NameLookupLatestLevelDBTransactionCache (Context context, File directory, BlockChain chain, BlockStore store, PeerGroup peerGroup) throws IOException {
        this(context, directory, JniDBFactory.factory, chain, store, peerGroup);
    }
    
    public NameLookupLatestLevelDBTransactionCache (Context context, File directory, DBFactory dbFactory, BlockChain chain, BlockStore store, PeerGroup peerGroup) throws IOException {
        this.chain = chain;
        this.store = store;
        this.peerGroup = peerGroup;
        
        this.context = context;
        this.params = context.getParams();
        
        this.path = directory;
        Options options = new Options();
        options.createIfMissing();
        
        try {
            tryOpen(directory, dbFactory, options);
        } catch (IOException e) {
            dbFactory.repair(directory, options);
            tryOpen(directory, dbFactory, options);
        }
        
        chain.addNewBestBlockListener(Threading.SAME_THREAD, this);
        chain.addReorganizeListener(Threading.SAME_THREAD, this);
        chain.addTransactionReceivedListener(Threading.SAME_THREAD, this);
    }
    
    protected void tryOpen(File directory, DBFactory dbFactory, Options options) throws IOException {
        db = dbFactory.open(directory, options);
        initStoreIfNeeded();
    }
    
    protected synchronized void initStoreIfNeeded() {
        if (db.get(CHAIN_HEAD_KEY) != null)
            return;   // Already initialised.
        
        setChainHead(0);
    }
    
    protected StoredBlock getSafeBlock(StoredBlock block) throws BlockStoreException {
        
        StoredBlock result = block;
        
        int safetyCount;
        for (safetyCount = 0; safetyCount < 12; safetyCount++) {
            result = result.getPrev(store);
        }
        
        return result;
    }
    
    protected synchronized void putBlockChain(StoredBlock block) throws Exception {
        
        // TODO: use BIP 113 timestamps
        if ( (new Date().getTime() / 1000 ) - block.getHeader().getTimeSeconds() > 366 * 24 * 60 * 60) {
            log.debug("NameDB halting walkbalk due to timestamp expiration, height " + block.getHeight());
            return;
        }
        
        if (block.getHeight() > getChainHead() + 1) {
            putBlockChain(block.getPrev(store));
        }
        
        putBlock(block);
    }
    
    // TODO: try a different peer if downloading a block fails, otherwise we're likely to stall the syncup
    protected synchronized void putBlock(StoredBlock block) throws Exception {
        
        Sha256Hash blockHash = block.getHeader().getHash();
        
        // We might not have the block's transactions already; if we don't, we have to download the block again.
        // This should be very rare; I'm not actually certain what circumstances would trigger it.
        // (I guess it would happen if block.transactions is null?)
        if (! pendingBlockTransactions.containsKey(block.getHeader().getHash())) {
            log.warn("Transactions missing from block " + blockHash + "; re-downloading block...");
            
            Block nameFullBlock = peerGroup.getDownloadPeer().getBlock(blockHash).get();
            
            // The full block hasn't been verified in any way!
            // So let's do that now.
            
            if (! nameFullBlock.getHash().equals(blockHash)) {
                throw new VerificationException("Block hash mismatch!");
            }
            
            // Now we know that the received block actually does match the hash that we requested.
            // However, that doesn't mean that the block's contents are valid.
            
            final EnumSet<Block.VerifyFlag> flags = EnumSet.noneOf(Block.VerifyFlag.class);
            nameFullBlock.verify(-1, flags);
            
            // Now we know that the block is internally valid (including the merkle root).
            // We haven't verified signature validity, but our threat model is SPV.
            
            for (Transaction tx : nameFullBlock.getTransactions()) {
                for (TransactionOutput output : tx.getOutputs()) {
                    try {
                        Script scriptPubKey = output.getScriptPubKey();
                        NameScript ns = new NameScript(scriptPubKey);
                        if(ns.isNameOp() && ns.isAnyUpdate() ) {
                            pendingBlockTransactions.put(block.getHeader().getHash(), tx);
                        }
                    } catch (ScriptException e) {
                        // Our threat model is lightweight SPV, which means we
                        // don't attempt to reject a blockchain due to a single
                        // invalid transaction.  As such, if we see a
                        // ScriptException, we just discard the transaction
                        // (and log a warning) rather than rejecting the block.
                        log.warn("Error checking TransactionOutput for name_anyupdate script!", e);
                        continue;
                    }
                }
            }
        }
        
        int height = block.getHeight();
        
        // See thread safety warning:
        // https://google.github.io/guava/releases/snapshot/api/docs/com/google/common/collect/Multimaps.html#synchronizedMultimap%28com.google.common.collect.Multimap%29
        synchronized (pendingBlockTransactions) {
            for (Transaction tx : pendingBlockTransactions.get(block.getHeader().getHash())) {
                for (TransactionOutput output : tx.getOutputs()) {
                    try {
                        Script scriptPubKey = output.getScriptPubKey();
                        NameScript ns = new NameScript(scriptPubKey);
                        if(ns.isNameOp() && ns.isAnyUpdate() ) {
                            putNameScript(scriptPubKey, ns, height);
                        }
                    } catch (ScriptException e) {
                        continue;
                    }
                }
            }
        }
        
        pendingBlockTransactions.removeAll(block.getHeader().getHash());
        
        setChainHead(block.getHeight());
    }
    
    protected synchronized void putNameScript(Script scriptPubKey, NameScript ns, int height) throws UnsupportedEncodingException {
        
        // TODO: check if name is relevant (e.g. namespace is id/, has zeronet field)
        
        // key format:
        byte[] headerBytes = "NamScr".getBytes("ISO-8859-1");
        byte[] nameBytes = ns.getOpName().data;
        
        // record format:
        // height goes here
        byte[] scriptBytes = scriptPubKey.getProgram();
        
        ByteBuffer keyBuffer = ByteBuffer.allocate(headerBytes.length + nameBytes.length);
        ByteBuffer recordBuffer = ByteBuffer.allocate(4 + scriptBytes.length);
        
        keyBuffer.put(headerBytes).put(nameBytes);
        recordBuffer.putInt(height).put(scriptBytes);
        
        db.put(keyBuffer.array(), recordBuffer.array());
    }
    
    // TODO: stop duplicating code from the other NameLookupLatest implementations
    protected void verifyHeightTrustworthy(int height) throws IllegalArgumentException, VerificationException {
        if (height < 1) {
            throw new IllegalArgumentException("Nonpositive block height; not trustworthy!");
        }
        
        int headHeight = chain.getChainHead().getHeight();
        
        int confirmations = headHeight - height + 1;
        
        // TODO: optionally use transaction chains (with signature checks) to verify transactions without 12 confirmations
        // TODO: the above needs to be optional, because some applications (e.g. cert transparency) require confirmations
        if (confirmations < 12) {
            throw new VerificationException("Block does not yet have 12 confirmations; not trustworthy!");
        }
        
        // TODO: check for off-by-one errors on this line
        if (confirmations >= 36000) {
            throw new VerificationException("Block has expired; not trustworthy!");
        }
    }
    
    // TODO: make a new Exception class
    @Override
    public Transaction getNameTransaction(String name, String identity) throws Exception {
        
        byte[] headerBytes = "NamScr".getBytes("ISO-8859-1");
        byte[] nameBytes = name.getBytes("ISO-8859-1");
        // name goes here
        
        ByteBuffer keyBuffer = ByteBuffer.allocate(headerBytes.length + nameBytes.length);
        keyBuffer.put(headerBytes).put(nameBytes);
        
        byte[] recordBytes = db.get(keyBuffer.array());
        if (recordBytes == null)
            return null;
        
        ByteBuffer recordBuffer = ByteBuffer.wrap(recordBytes);
        
        int height = recordBuffer.getInt();
        
        verifyHeightTrustworthy(height);
        
        byte[] scriptPubKeyBytes = Arrays.copyOfRange(recordBytes, 4, recordBytes.length);

        Transaction tx = new Transaction(params);
        Script scriptPubKey = new Script(scriptPubKeyBytes);
        tx.addOutput(Coin.CENT, scriptPubKey);
        
        tx.getConfidence().setAppearedAtChainHeight(height); // TODO: test this line
        tx.getConfidence().setDepthInBlocks(chain.getChainHead().getHeight() - height + 1);
        
        return tx;
    }
    
    protected synchronized int getChainHead() {
        return ByteBuffer.wrap(db.get(CHAIN_HEAD_KEY)).getInt();
    }
    
    protected synchronized void setChainHead(int chainHead) {
        db.put(CHAIN_HEAD_KEY, ByteBuffer.allocate(4).putInt(chainHead).array());
    }
    
    public synchronized void close() throws IOException {
        db.close();
    }
    
    /** Erases the contents of the database (but NOT the underlying files themselves) and then reinitialises with the genesis block. */
    protected synchronized void reset() throws IOException {
        WriteBatch batch = db.createWriteBatch();
        try {
            DBIterator it = db.iterator();
            try {
                it.seekToFirst();
                while (it.hasNext())
                    batch.delete(it.next().getKey());
                db.write(batch);
            } finally {
                it.close();
            }
        } finally {
            batch.close();
        }
        initStoreIfNeeded();
    }
    
    protected synchronized void destroy() throws IOException {
        JniDBFactory.factory.destroy(path, new Options());
    }
    
    @Override
    public void notifyNewBestBlock (StoredBlock block) throws VerificationException {
        // TODO: use BIP 113 timestamps
        if ( (new Date().getTime() / 1000 ) - block.getHeader().getTimeSeconds() > 366 * 24 * 60 * 60) {
            log.debug("NameDB skipping block at height " + block.getHeight() + " due to timestamp " + block.getHeader().getTimeSeconds());
            return;
        }
        
        log.debug("NameDB started processing new best block at height " + block.getHeight());
        
        try {
            putBlockChain(getSafeBlock(block));
        }
        catch (Exception e) {
            log.error("NameDB Exception while processing new best block", e);
            throw new VerificationException(e);
        }
        
        log.debug("NameDB finished processing new best block at height " + block.getHeight());
    }
    
    // WARNING: in a reorg that is at least 12 blocks deep, any names updated in the old blocks that aren't updated in the new blocks will remain in their old state in the database.
    // That is incorrect behavior, but it usually isn't advantageous to an attacker.
    // In certain applications where proof of existence is used, this incorrect behavior could allow a true existence claim to be accepted,
    // even though the rest of the network will incorrectly reject it.
    // I don't see any other significant attacks here.  Have I missed something?
    // If we're really worried about this, the "right" solution is to either store name history in the database,
    // or redownload all of the last 36 kiloblocks.
    @Override
    public void reorganize(StoredBlock splitPoint, List<StoredBlock> oldBlocks, List<StoredBlock> newBlocks) throws VerificationException {
        // TODO: use BIP 113 timestamps
        if ( (new Date().getTime() / 1000 ) - newBlocks.get(0).getHeader().getTimeSeconds() > 366 * 24 * 60 * 60) {
            return;
        }
        
        setChainHead(splitPoint.getHeight() - 12);
        
        try {
            putBlockChain(getSafeBlock(newBlocks.get(0)));
        }
        catch (Exception e) {
            log.error("Exception during NameDB reorganize", e);
            throw new VerificationException(e);
        }
        
        log.warn("Finished NameDB reorganize, height " + newBlocks.get(0).getHeight());
    }
    
    @Override
    public void receiveFromBlock(Transaction tx, StoredBlock block, AbstractBlockChain.NewBlockType blockType, int relativityOffset) throws VerificationException {
        // TODO: use BIP 113 timestamps
        if ( (new Date().getTime() / 1000 ) - block.getHeader().getTimeSeconds() > 366 * 24 * 60 * 60) {
            log.debug("NameDB skipping new transaction at height " + block.getHeight() + " due to timestamp " + block.getHeader().getTimeSeconds());
            return;
        }
        
        for (TransactionOutput output : tx.getOutputs()) {
            try {
                Script scriptPubKey = output.getScriptPubKey();
                NameScript ns = new NameScript(scriptPubKey);
                // Always save the coinbase, because it lets us identify that we've received the contents of the block, even if it has no name_anyupdate operations.
                // TODO: maybe save a null reference instead of the actual coinbase tx, since this would cut down on memory usage very slightly.
                if(tx.isCoinBase() || ( ns.isNameOp() && ns.isAnyUpdate() ) ) {
                    log.debug("NameDB temporarily storing name transaction until it gets more confirmations.");
                    pendingBlockTransactions.put(block.getHeader().getHash(), tx);
                }
            } catch (ScriptException e) {
                // Our threat model is lightweight SPV, which means we
                // don't attempt to reject a blockchain due to a single
                // invalid transaction.  As such, if we see a
                // ScriptException, we just discard the transaction
                // (and log a warning) rather than rejecting the block.
                log.warn("Error checking TransactionOutput for name_anyupdate script!", e);
                continue;
            }
        }
    }
    
    // TODO: add optional FilteredBlock support
    @Override
    public boolean notifyTransactionIsInBlock(Sha256Hash txHash, StoredBlock block, AbstractBlockChain.NewBlockType blockType, int relativityOffset) throws VerificationException {
        return false;
    }
}
