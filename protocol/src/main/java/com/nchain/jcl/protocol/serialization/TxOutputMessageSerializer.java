package com.nchain.jcl.protocol.serialization;

import com.nchain.jcl.protocol.messages.TxOutputMessage;
import com.nchain.jcl.protocol.messages.VarIntMsg;
import com.nchain.jcl.protocol.serialization.common.*;
import com.nchain.jcl.tools.bytes.HEX;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 *
 * @date 01/10/2019
 *
 * A Serializer for instance of {@Link TxOutputMessage} messages
 */
public class TxOutputMessageSerializer implements MessageSerializer<TxOutputMessage> {

    private static TxOutputMessageSerializer instance;

    // Reference to singleton instances used during serialization/Deserialization. Defined here for performance
    private static VarIntMsgSerializer      varIntMsgSerializer     = VarIntMsgSerializer.getInstance();

    private TxOutputMessageSerializer() { }

    public static TxOutputMessageSerializer getInstance(){
        if(instance == null) {
            synchronized (TxOutputMessageSerializer.class) {
                instance = new TxOutputMessageSerializer();
            }
        }

        return instance;
    }

    @Override
    public TxOutputMessage deserialize(DeserializerContext context, ByteArrayReader byteReader) {

        byteReader.waitForBytes(8);
        long txValue = byteReader.readInt64LE();
        int scriptLen = (int) varIntMsgSerializer.deserialize(context, byteReader).getValue();

        byteReader.waitForBytes(scriptLen);
        byte[] pk_script = byteReader.read(scriptLen);

        return TxOutputMessage.builder().txValue(txValue).pk_script(pk_script).build();
    }

    @Override
    public void serialize(SerializerContext context, TxOutputMessage message, ByteArrayWriter byteWriter) {
        byteWriter.writeUint64LE(message.getTxValue());
        varIntMsgSerializer.serialize(context, message.getPk_script_length(), byteWriter);
        byteWriter.write(message.getPk_script());
    }
}
