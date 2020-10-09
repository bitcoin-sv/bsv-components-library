package com.nchain.jcl.script.serialization;

import com.nchain.jcl.base.tools.bytes.ByteTools;
import com.nchain.jcl.script.core.ScriptChunk;
import com.nchain.jcl.script.core.ScriptData;
import com.nchain.jcl.script.exception.ScriptParseException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.nchain.jcl.script.core.ScriptOpCodes.*;
import static com.google.common.base.Preconditions.*;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer for the ScriptChunk. As other Serialzers in JCL, its a stateless Singletong instance.
 */
public class ScriptChunkSerializer {

    private static ScriptChunkSerializer instance;

    private ScriptChunkSerializer() {}

    public static ScriptChunkSerializer getInstance() {
        if (instance == null) {
            synchronized (ScriptChunkSerializer.class) {
                instance = new ScriptChunkSerializer();
            }
        }
        return instance;
    }

    public ScriptChunk deserialize(int currentLocationInProgram, ByteArrayInputStream bis) {


        checkState(bis.available() > 0, "Input Stream empty, not possible to deserialize next ScriptChunk");
        ScriptChunk result ;

        int opcode = bis.read();
        long dataToRead = -1;
        if (opcode >= 0 && opcode < OP_PUSHDATA1) {
            // Read some bytes of data, where how many is the opcode value itself.
            dataToRead = opcode;
        } else if (opcode == OP_PUSHDATA1) {
            if (bis.available() < 1) throw new ScriptParseException("Unexpected end of script");
            dataToRead = bis.read();
        } else if (opcode == OP_PUSHDATA2) {
            // Read a short, then read that many bytes of data.
            if (bis.available() < 2) throw new ScriptParseException("Unexpected end of script");
            dataToRead = bis.read() | (bis.read() << 8);
        } else if (opcode == OP_PUSHDATA4) {
            // Read a uint32, then read that many bytes of data.
            // Though this is allowed, because its value cannot be > 520, it should never actually be used
            if (bis.available() < 4) throw new ScriptParseException("Unexpected end of script");
            dataToRead = ((long) bis.read()) | (((long) bis.read()) << 8) | (((long) bis.read()) << 16) | (((long) bis.read()) << 24);
        }

        // Now we know how much dara we are supposed to read form the Stream. We perform some checks...
        checkState(dataToRead == -1 || dataToRead <= bis.available(),
                "Push of data (lenght: " + dataToRead+ ")  element that is larger than remaining data (" + bis.available() + ")");


        ScriptData data = null;
        if (dataToRead != -1) {
            byte[] bytes = new byte[(int) dataToRead];
            checkState(dataToRead == 0 || bis.read(bytes, 0, (int) dataToRead) == dataToRead);
            data = ScriptData.builder().data(bytes).build();
        }
        result = ScriptChunk.builder().opcode(opcode).data(data).startLocationInProgram(currentLocationInProgram).build();
        return result;
    }

    public void serialize(ScriptChunk chunk, ByteArrayOutputStream bos) {
        final int MAX_NUMBER_IN_1_BYTE = 256;
        final int MAX_NUMBER_IN_2_BYTES = 65536;

        // if it's an OPCode, then it shouldn't have any data.
        checkState((chunk.isOpCode() && chunk.getData() == null) || !chunk.isOpCode());


        int opcode = chunk.getOpcode();
        if (chunk.isOpCode()) bos.write(opcode);
        else if (chunk.getData() != null) {
            // This is a PUSH OP Code (pushing some data into the Stack)
            ScriptData data = chunk.getData();

            // We perform some verifications, to check that the data size matches the opcode:
            boolean dataSizeCorrect = true;

            if (opcode < OP_PUSHDATA1) dataSizeCorrect = data.length() == opcode;
            else if (opcode == OP_PUSHDATA1) dataSizeCorrect = data.length() < MAX_NUMBER_IN_1_BYTE;
            else if (opcode == OP_PUSHDATA2) dataSizeCorrect = data.length() < MAX_NUMBER_IN_2_BYTES;
            else if (opcode != OP_PUSHDATA4) throw new RuntimeException("Unimplemented");

            checkState(dataSizeCorrect);

            serialize(chunk.getOpcode(), chunk.data(), bos);

        } else {
            bos.write(opcode); // smallNum
        }
    }

    /**
     * It serializes a Chunk of type Data It only relies on the Data, without giving the opcode. This means that this
     * method ASSUMES that the length of the Data is Coherent with the opCode provided. If the opCode is NOT provided,
     * it will be deduced base don the length of the data.
     *
     * To write an integer call writeBytes(out, Utils.reverseBytes(Utils.encodeMPI(val, false)));
     */
    public void serialize(Integer opCode, byte[] data, ByteArrayOutputStream bos) {
        try {
            final int MAX_NUMBER_IN_2_BYTES = 65536;
            checkState(data.length < MAX_NUMBER_IN_2_BYTES, "Unimplemented");

            int opCodeToWrite = (opCode != null) ? opCode : getOpCodeFromData(data);

            if (opCodeToWrite < OP_PUSHDATA1) {
                bos.write(opCodeToWrite);
            } else if (opCodeToWrite == OP_PUSHDATA1) {
                bos.write(OP_PUSHDATA1);
                bos.write(data.length);
            } else if (opCodeToWrite == OP_PUSHDATA2) {
                bos.write(OP_PUSHDATA2);
                bos.write(0xFF & (data.length));
                bos.write(0xFF & (data.length >> 8));
            } else {
                // We suppose here that its a OP_PUSHDATA4:
                bos.write(OP_PUSHDATA4);
                ByteTools.uint32ToByteStreamLE(data.length, bos);
            }

            // Now we write the actual Data:
            bos.write(data);

        } catch (IOException ioe) {
            throw new ScriptParseException(ioe);
        }
    }

    /**
     * This method "calculates" the opCode that must be used to push the data into the Stack, depending on the
     * Size of the data.
     */
    private int getOpCodeFromData(byte[] data) {
        final int MAX_NUMBER_IN_1_BYTE = 256;
        final int MAX_NUMBER_IN_2_BYTES = 65536;

        int opCode = data.length; // Default...
        if (data.length > OP_PUSHDATA1)
            if (data.length < MAX_NUMBER_IN_1_BYTE)         opCode = OP_PUSHDATA1;
            else if (data.length < MAX_NUMBER_IN_2_BYTES)   opCode = OP_PUSHDATA2;
            else                                            opCode = OP_PUSHDATA4;
        return opCode;
    }
}
