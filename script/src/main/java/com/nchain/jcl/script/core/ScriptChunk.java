package com.nchain.jcl.script.core;


import com.nchain.jcl.base.tools.bytes.HEX;
import lombok.Builder;
import java.util.Arrays;
import static com.nchain.jcl.script.core.ScriptOpCodes.*;
import com.google.common.base.Objects;
import lombok.Value;

import static com.google.common.base.Preconditions.checkState;


/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
@Builder(toBuilder = true)
@Value
public class ScriptChunk<C> {

    /** Definition of Standard SCript Chunks, so they can be reused rather than creating new instances of them */
    public static final ScriptChunk[] STANDARD_TRANSACTION_SCRIPT_CHUNKS = {
            ScriptChunk.builder().opcode(OP_DUP).startLocationInProgram(0).build(),
            ScriptChunk.builder().opcode(OP_HASH160).startLocationInProgram(1).build(),
            ScriptChunk.builder().opcode(OP_EQUALVERIFY).startLocationInProgram(23).build(),
            ScriptChunk.builder().opcode(OP_CHECKSIG).startLocationInProgram(24).build()
    };

    /** Operation to be executed. Opcodes are defined in {@link ScriptOpCodes}. */
    private int opcode;
    private ScriptData data;
    private int startLocationInProgram;


    /** user provided context object for attaching meta data to a ScriptChunk*/
    public final C context;


    public byte[] data() {
        return data == null ? null : data.data();
    }

    public boolean equalsOpCode(int opcode) {
        return opcode == this.opcode;
    }

    /**
     * If this chunk is a single byte of non-pushdata content (could be OP_RESERVED or some invalid Opcode)
     */
    public boolean isOpCode() {
        return opcode > OP_PUSHDATA4;
    }

    /**
     * Returns true if this chunk is pushdata content, including the single-byte pushdatas.
     */
    public boolean isPushData() {
        return opcode <= OP_16;
    }

    public int getStartLocationInProgram() {
        checkState(startLocationInProgram >= 0);
        return startLocationInProgram;
    }

    /** If this chunk is an OP_N opcode returns the equivalent integer value. */
    public int decodeOpN() {
        checkState(isOpCode());
        return decodeFromOpN(opcode);
    }

    /**
     * Called on a pushdata chunk, returns true if it uses the smallest possible way (according to BIP62) to push the data.
     */
    public boolean isShortestPossiblePushData() {
        checkState(isPushData());
        if (data() == null)
            return true;   // OP_N
        if (data.length() == 0)
            return opcode == OP_0;
        if (data.length() == 1) {
            byte b = data()[0];
            if (b >= 0x01 && b <= 0x10)
                return opcode == OP_1 + b - 1;
            if ((b & 0xFF) == 0x81)
                return opcode == OP_1NEGATE;
        }
        if (data.length() < OP_PUSHDATA1)
            return opcode == data.length();
        if (data.length() < 256)
            return opcode == OP_PUSHDATA1;
        if (data.length() < 65536)
            return opcode == OP_PUSHDATA2;

        // can never be used, but implemented for completeness
        return opcode == OP_PUSHDATA4;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (isOpCode()) {
            buf.append(getOpCodeName(opcode));
        } else if (data() != null) {
            // Data chunk
            buf.append(getPushDataName(opcode)).append("[").append(HEX.encode(data())).append("]");
        } else {
            // Small num
            buf.append(decodeFromOpN(opcode));
        }
        return buf.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScriptChunk other = (ScriptChunk) o;
        return opcode == other.opcode && startLocationInProgram == other.startLocationInProgram
                && Arrays.equals(data(), other.data());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(opcode, startLocationInProgram, Arrays.hashCode(data()));
    }
}
