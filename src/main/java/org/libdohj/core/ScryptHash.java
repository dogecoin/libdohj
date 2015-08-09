/**
 * Copyright 2015 Ross Nicoll
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

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Sha256Hash;

/**
 * Scrypt hash. Currently extends Sha256Hash (so no real type safety is provided),
 * but in time the two classes should have a common superclass rather than one
 * extending the other directly.
 */
public class ScryptHash extends Sha256Hash {

    public ScryptHash(byte[] rawHashBytes) {
        super(rawHashBytes);
    }
    
    public ScryptHash(String hexString) {
        super(hexString);
    }
}
