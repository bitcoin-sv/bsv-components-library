package com.nchain.jcl.protocol.serialization;


import com.nchain.jcl.protocol.messages.VarIntMsg;
import com.nchain.jcl.protocol.serialization.common.*;
import com.nchain.jcl.tools.bytes.ByteTools;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-07-16
 *
 * A Serializer for {@link VarIntMsg} messages
 */
public class VarIntMsgSerializer implements MessageSerializer<VarIntMsg> {

    private static VarIntMsgSerializer instance;

    private VarIntMsgSerializer() { }

    public static VarIntMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (VarIntMsgSerializer.class) {
                instance = new VarIntMsgSerializer();
            }
        }
        return instance;
    }


    public VarIntMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        long value;

        byteReader.waitForBytes(1);

        int firstByte = 0xFF & byteReader.read();

        // We calculate how to read the value from the byte Array.
        // the size in bytes used to store the value will be calculated automatically by the Builder later on:
        if (firstByte < 253){
            value = firstByte;
        } else if (firstByte == 253){
            byteReader.waitForBytes(2);
            value = (0xFF & byteReader.read()) | ((0xFF & byteReader.read()) << 8);
        } else if (firstByte == 254) {
            byteReader.waitForBytes(4);
            value = byteReader.readUint32();
        } else {
            byteReader.waitForBytes(8);
            value = byteReader.readInt64LE();
        }

        // We assign the value to the builder:
        return VarIntMsg.builder().value(value).build();
    }

    @Override
    public void serialize(SerializerContext context, VarIntMsg message, ByteArrayWriter byteWriter) {
        byte[] bytesToWrite;
        long value = message.getValue();
        int sizeInBytes = (int) message.getLengthInBytes();
        switch (sizeInBytes) {
            case 1: {
                bytesToWrite = new byte[]{(byte) value};
                break;
            }
            case 3: {
                bytesToWrite = new byte[]{(byte) 253, (byte) (value), (byte) (value >> 8)};
                break;
            }
            case 5: {
                bytesToWrite = new byte[5];
                bytesToWrite[0] = (byte) 254;
                ByteTools.uint32ToByteArrayLE(value, bytesToWrite, 1);
                break;
            }
            default: {
                bytesToWrite = new byte[9];
                bytesToWrite[0] = (byte) 255;
                ByteTools.uint64ToByteArrayLE(value, bytesToWrite, 1);
                break;
            }
        } // switch

        byteWriter.write(bytesToWrite);
    }
}
