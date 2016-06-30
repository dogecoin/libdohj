/*
 * Copyright 2016 Jeremy Rand.
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

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;

// TODO: Document this.

// identity is used for things like Tor stream isolation
public interface NameLookupByBlockHash {
    
    public Transaction getNameTransaction(String name, Sha256Hash blockHash, String identity) throws Exception;
    
}
