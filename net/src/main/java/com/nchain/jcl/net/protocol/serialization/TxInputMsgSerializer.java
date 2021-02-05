package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.net.protocol.messages.TxInputMsg;
import com.nchain.jcl.net.protocol.messages.TxOutPointMsg;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer for instance of {@Link TxInputMessage} messages
 */
public class TxInputMsgSerializer implements MessageSerializer<TxInputMsg> {
    private static TxInputMsgSerializer instance;

    // Reference to singleton instances used during serialization/Deserialization. Defined here for performance
    private static TxOutPointMsgSerializer  txOutPointMsgSerializer = TxOutPointMsgSerializer.getInstance();
    private static VarIntMsgSerializer varIntMsgSerializer     = VarIntMsgSerializer.getInstance();

    private TxInputMsgSerializer() { }

    public static TxInputMsgSerializer getInstance(){
        if(instance == null) {
            synchronized (TxInputMsgSerializer.class) {
                instance = new TxInputMsgSerializer();
            }
        }

        return instance;
    }

    @Override
    public TxInputMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        TxOutPointMsg txOutPointMsg = txOutPointMsgSerializer.deserialize(context, byteReader);

        int scriptLen = (int) varIntMsgSerializer.deserialize(context, byteReader).getValue();

        byteReader.waitForBytes(scriptLen + 4);
        byte[] sig_script = byteReader.read(scriptLen);
        long sequence = byteReader.readUint32();

        return TxInputMsg.builder().pre_outpoint(txOutPointMsg).signature_script(sig_script).sequence(sequence).build();
    }

    @Override
    public void serialize(SerializerContext context, TxInputMsg message, ByteArrayWriter byteWriter) {
        txOutPointMsgSerializer.serialize(context, message.getPre_outpoint(),byteWriter);
        varIntMsgSerializer.serialize(context, message.getScript_length(), byteWriter);
        byte[] script = (message.getScript_length().getValue() > 0) ? message.getSignature_script() : new byte[]{};
        byteWriter.write(script);
        byteWriter.writeUint32LE(message.getSequence());
    }
}
