package io.bitcoinsv.jcl.net.protocol.serialization;

import io.bitcoinsv.jcl.net.protocol.messages.DatarefTxMsg;
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;

/**
 * Copyright (c) 2024 nChain Ltd
 * <br>
 * A Serializer for {@link DatarefTxMsg} messages
 *
 * @author nChain Ltd
 */
public class DatarefTxMsgSerializer implements MessageSerializer<DatarefTxMsg> {

    private static DatarefTxMsgSerializer instance;

    // Constructor
    private DatarefTxMsgSerializer() { }

    /** Returns an instance of this Serializer (Singleton) */
    public static DatarefTxMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (DatarefTxMsgSerializer.class) {
                instance = new DatarefTxMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public DatarefTxMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        var txMsg = TxMsgSerializer.getInstance().deserialize(context, byteReader);
        var merkleProofMsg = MerkleProofMsgSerializer.getInstance().deserialize(context, byteReader);

        return DatarefTxMsg.builder()
                .txMsg(txMsg)
                .merkleProofMsg(merkleProofMsg)
                .build();
    }

    @Override
    public void serialize(SerializerContext context, DatarefTxMsg message, ByteArrayWriter byteWriter) {
        TxMsgSerializer.getInstance().serialize(context, message.getTxMsg(), byteWriter);
        MerkleProofMsgSerializer.getInstance().serialize(context, message.getMerkleProofMsg(), byteWriter);
    }
}
