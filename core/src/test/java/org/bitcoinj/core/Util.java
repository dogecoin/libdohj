package org.bitcoinj.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Util {

    public static byte[] getBytes(InputStream inputStream) throws IOException {
        assert null != inputStream;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int numberRead;
        byte[] data = new byte[BUFFER_SIZE];

        while ((numberRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, numberRead);
        }

        buffer.flush();

        return buffer.toByteArray();
    }
    private static final int BUFFER_SIZE = 1024;
    
}
