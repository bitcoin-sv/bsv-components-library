package com.nchain.jcl.protocol.serialization;


import com.nchain.jcl.protocol.messages.TxInputMessage;
import com.nchain.jcl.protocol.messages.TxOutPointMsg;
import com.nchain.jcl.protocol.messages.VarIntMsg;
import com.nchain.jcl.protocol.serialization.common.*;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.tools.bytes.HEX;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 *
 * @date 26/09/2019
 * A Serializer for instance of {@Link TxInputMessage} messages
 */
public class TxInputMessageSerializer implements MessageSerializer<TxInputMessage> {
    private static TxInputMessageSerializer instance;

    // Reference to singleton instances used during serialization/Deserialization. Defined here for performance
    private static TxOutPointMsgSerializer  txOutPointMsgSerializer = TxOutPointMsgSerializer.getInstance();
    private static VarIntMsgSerializer      varIntMsgSerializer     = VarIntMsgSerializer.getInstance();

    private TxInputMessageSerializer() { }

    public static TxInputMessageSerializer getInstance(){
        if(instance == null) {
            synchronized (TxInputMessageSerializer.class) {
                instance = new TxInputMessageSerializer();
            }
        }

        return instance;
    }

    @Override
    public TxInputMessage deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        TxOutPointMsg txOutPointMsg = txOutPointMsgSerializer.deserialize(context, byteReader);

        int scriptLen = (int) varIntMsgSerializer.deserialize(context, byteReader).getValue();

        byteReader.waitForBytes(scriptLen + 4);
        byte[] sig_script = byteReader.read(scriptLen);
        long sequence = byteReader.readUint32();

        return TxInputMessage.builder().pre_outpoint(txOutPointMsg).signature_script(sig_script).sequence(sequence).build();
    }

    @Override
    public void serialize(SerializerContext context, TxInputMessage message, ByteArrayWriter byteWriter) {
        txOutPointMsgSerializer.serialize(context, message.getPre_outpoint(),byteWriter);
        varIntMsgSerializer.serialize(context, message.getScript_length(), byteWriter);
        byteWriter.write(message.getSignature_script());
        byteWriter.writeUint32LE(message.getSequence());
    }
}
