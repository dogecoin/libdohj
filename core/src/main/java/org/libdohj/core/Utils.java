/**
 * Copyright 2011 Google Inc.
 * Copyright 2014 Andreas Schildbach
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
package org.libdohj.core;

import com.lambdaworks.crypto.SCrypt;
import org.bitcoinj.core.Sha256Hash;

import java.math.BigInteger;
import java.security.GeneralSecurityException;

/**
 *
 */
public class Utils {
    /**
     * Calculates the Scrypt hash of the given byte range.
     * The resulting hash is in small endian form.
     */
    public static byte[] scryptDigest(byte[] input) throws GeneralSecurityException {
        return SCrypt.scrypt(input, input, 1024, 1, 1, 32);
    }

    public static String formatAsHash(final BigInteger value) {
        final StringBuilder builder = new StringBuilder(value.toString(16));
        while (builder.length() < (Sha256Hash.LENGTH * 2)) {
            builder.insert(0, "0");
        }
        return builder.toString();
    }
}
