package io.bitcoinsv.bsvcl.net.protocol.serialization;


import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.bsvcl.net.protocol.messages.BlockHeaderMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.BlockMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.RawBlockMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.RawTxMsg;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter;
import io.bitcoinsv.bsvcl.common.bytes.IReader;
import io.bitcoinsv.bsvcl.common.serialization.TransactionSerializerUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;


/**
 * @author i.fernandez @nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * * A Serializer for {@link BlockMsg} messages
 */
public class RawBlockMsgSerializer implements MessageSerializer<RawBlockMsg> {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(RawBlockMsgSerializer.class);

    private RawBlockMsgSerializer() { }

    /**
     * Returns the instance of this Serializer (Singleton)
     */
    public static RawBlockMsgSerializer getInstance() {
        return new RawBlockMsgSerializer();
    }

    @Override
    public RawBlockMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {

        // First we deserialize the Block Header:
        BlockHeaderMsg blockHeaderMsg = deserializeHeader(context, byteReader);

        // The transactions are taken from the Block Body...since the Block Header has been already extracted
        // from the "byteReader", the information remaining in there are the Block Transactions...

        long numTxs = blockHeaderMsg.getTransactionCount().getValue();
        List<RawTxMsg> txs = new ArrayList<>();

        while (txs.size() < numTxs) {
            RawTxMsg tx = deserializeNextTx(context, byteReader);
            txs.add(tx);
        }

        return RawBlockMsg.builder().blockHeader(blockHeaderMsg).txs(txs).build();
    }

    public BlockHeaderMsg deserializeHeader(DeserializerContext context, ByteArrayReader byteArrayReader){
        return BlockHeaderMsgSerializer.getInstance().deserialize(context, byteArrayReader);
    }

    /**
     * It deserializes a single Tx of this block from the Byte Array Reader, and returns it
     */
    public static RawTxMsg deserializeNextTx(DeserializerContext context, IReader byteReader) {
        var txBytes = TransactionSerializerUtils.deserializeNextTx(byteReader);
        return new RawTxMsg(txBytes, 0);
    }

    @Override
    public void serialize(SerializerContext context, RawBlockMsg blockRawMsg, ByteArrayWriter byteWriter) {
        BlockHeaderMsgSerializer.getInstance().serialize(context, blockRawMsg.getBlockHeader(), byteWriter);
        blockRawMsg.getTxs().forEach(t -> {
            byteWriter.write(t.getContent());
        });
    }
}
