package org.libdohj.params;

import org.bitcoinj.core.AltcoinBlock;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Util;
import org.libdohj.core.AltcoinSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

class BlockLoader {
    private final NetworkParameters params;
    public BlockLoader(final NetworkParameters params) {
        this.params = params;
    }

    protected Map<String, AltcoinBlock> loadBlocks(final String[][] blocks) throws IOException {
        final AltcoinSerializer serializer = (AltcoinSerializer)params.getDefaultSerializer();
        final Map<String, AltcoinBlock> loadedBlocks = new HashMap<>();
        for (String[] row: blocks) {
            final InputStream stream = getClass().getResourceAsStream(row[0]);
            if (stream == null) {
                throw new IOException("Failed to find resource " + row[0]);
            }
            final byte[] payload = Util.getBytes(stream);
            final AltcoinBlock block = (AltcoinBlock)serializer.makeBlock(payload);
            assertEquals(row[1], block.getHashAsString());
            loadedBlocks.put(block.getHashAsString(), block);
        }
        return loadedBlocks;
    }

    /**
     * Load all the block headers found in the given filename.
     */
    protected LinkedHashMap<String, AltcoinBlock> loadAllHeaders(final String filename, final int headerSize) throws IOException {
        final AltcoinSerializer serializer = (AltcoinSerializer)params.getDefaultSerializer();
        final LinkedHashMap<String, AltcoinBlock> loadedBlocks = new LinkedHashMap<>();
        final InputStream stream = getClass().getResourceAsStream(filename);
        if (stream == null) {
            throw new IOException("Failed to find resource " + filename);
        }
        final byte[] payload = new byte[headerSize];
        while (stream.read(payload) == headerSize) {
            final AltcoinBlock block = (AltcoinBlock) serializer.makeBlock(payload);
            loadedBlocks.put(block.getHashAsString(), block);
        }
        return loadedBlocks;
    }
}