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

package org.libdohj.script;

import org.bitcoinj.script.*;

import static org.bitcoinj.script.ScriptOpCodes.*;

import java.util.ArrayList;
import java.util.List;

// TODO: review this

public class NameScript {

    public static final int OP_NAME_NEW = OP_1;
    public static final int OP_NAME_FIRSTUPDATE = OP_2;
    public static final int OP_NAME_UPDATE = OP_3;

    protected int op;
    
    protected ArrayList<ScriptChunk> args;
    
    protected Script address;

    public NameScript(Script baseScript) {
        op = OP_NOP;
        args = new ArrayList<ScriptChunk>();
        address = baseScript;
        
        ScriptChunk nameOp;
        int pc = 0;
        
        List<ScriptChunk> chunks = baseScript.getChunks();
        
        try {
            nameOp = chunks.get(pc);
        } catch (IndexOutOfBoundsException e) {
            return;
        }
        pc++;
        
        while(true) {
            ScriptChunk arg;
            
            try {
                arg = chunks.get(pc);
            } catch (IndexOutOfBoundsException e) {
                return;
            }
            pc++;
            
            if(arg.opcode == OP_DROP || arg.opcode == OP_2DROP || arg.opcode == OP_NOP) {
                break;
            }
            if( ! (arg.opcode >= 0 && arg.opcode <= OP_PUSHDATA4) ) {
                return;
            }
            
            args.add(arg);
        }
        
        // Move the pc to after any DROP or NOP.
        try {
            while(chunks.get(pc).opcode == OP_DROP || chunks.get(pc).opcode == OP_2DROP || chunks.get(pc).opcode == OP_NOP) {
                pc++;
            }
        } catch (IndexOutOfBoundsException e) {
        }
        
        /* Now, we have the args and the operation.  Check if we have indeed
        a valid name operation and valid argument counts.  Only now set the
        op and address members, if everything is valid.  */
        
        switch (nameOp.opcode) {
            case OP_NAME_NEW:
                if(args.size() != 1) {
                    return;
                }
                break;
            case OP_NAME_FIRSTUPDATE:
                if(args.size() != 3) {
                    return;
                }
                break;
            case OP_NAME_UPDATE:
                if(args.size() != 2) {
                    return;
                }
                break;
            default:
                return;
        }
        
        op = nameOp.opcode;
        
        ScriptBuilder addressBuilder = new ScriptBuilder();
        while(pc < chunks.size()) {
            addressBuilder.addChunk(chunks.get(pc));
            pc++;
        }
        address = addressBuilder.build();
    }
    
    public boolean isNameOp() {
        switch(op) {
            case OP_NAME_NEW:
            case OP_NAME_FIRSTUPDATE:
            case OP_NAME_UPDATE:
                return true;
            
            case OP_NOP:
                return false;
            
            default:
                throw new ScriptException(ScriptError.SCRIPT_ERR_BAD_OPCODE, "Invalid name op");
        }
    }
    
    public Script getAddress() {
        return address;
    }
    
    // TODO: getNameOp
    
    public boolean isAnyUpdate() {
        switch(op) {
            case OP_NAME_NEW:
                return false;
            
            case OP_NAME_FIRSTUPDATE:
            case OP_NAME_UPDATE:
                return true;
            
            default:
                throw new ScriptException(ScriptError.SCRIPT_ERR_DISABLED_OPCODE, "Not a name op");
        }
    }
    
    public ScriptChunk getOpName() {
        switch(op) {
            case OP_NAME_FIRSTUPDATE:
            case OP_NAME_UPDATE:
                return args.get(0);
            
            default:
                throw new ScriptException(ScriptError.SCRIPT_ERR_DISABLED_OPCODE, "Not an AnyUpdate op");
        }
    }
    
    public ScriptChunk getOpValue() {
        switch(op) {
            case OP_NAME_FIRSTUPDATE:
                return args.get(2);
            
            case OP_NAME_UPDATE:
                return args.get(1);
            
            default:
                throw new ScriptException(ScriptError.SCRIPT_ERR_DISABLED_OPCODE, "Not an AnyUpdate op");
        }
    }
    
    // TODO: getOpRand, getOpHash, isNameScript, buildNameNew, buildNameFirstupdate, buildNameUpdate
}