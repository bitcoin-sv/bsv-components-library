/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.serialization;


import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.jcl.net.protocol.messages.TxOutputMsg;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer for instance of {@Link TxOutputMessage} messages
 */
public class TxOutputMsgSerializer implements MessageSerializer<TxOutputMsg> {

    private static TxOutputMsgSerializer instance;

    // Reference to singleton instances used during serialization/Deserialization. Defined here for performance
    private static VarIntMsgSerializer varIntMsgSerializer     = VarIntMsgSerializer.getInstance();

    private TxOutputMsgSerializer() { }

    public static TxOutputMsgSerializer getInstance(){
        if(instance == null) {
            synchronized (TxOutputMsgSerializer.class) {
                instance = new TxOutputMsgSerializer();
            }
        }

        return instance;
    }

    @Override
    public TxOutputMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {

        long txValue = byteReader.readInt64LE();
        int scriptLen = (int) varIntMsgSerializer.deserialize(context, byteReader).getValue();
        byte[] pk_script = byteReader.read(scriptLen);

        return TxOutputMsg.builder().txValue(txValue).pk_script(pk_script).build();
    }

    @Override
    public void serialize(SerializerContext context, TxOutputMsg message, ByteArrayWriter byteWriter) {
        byteWriter.writeUint64LE(message.getTxValue());
        varIntMsgSerializer.serialize(context, message.getPk_script_length(), byteWriter);
        byte[] script = (message.getPk_script_length().getValue() > 0) ? message.getPk_script() : new byte[]{};
        byteWriter.write(script);
    }
}
