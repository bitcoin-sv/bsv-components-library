package io.bitcoinsv.bsvcl.net.protocol.serialization;

import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.bsvcl.net.protocol.messages.CompactBlockMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.PrefilledTxMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.VarIntMsg;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class CompactBlockMsgSerializer implements MessageSerializer<CompactBlockMsg> {
    private static CompactBlockMsgSerializer instance;

    public static CompactBlockMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (CompactBlockMsgSerializer.class) {
                instance = new CompactBlockMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public CompactBlockMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        // First we deserialize the Block Header:
        var blockHeader = CompactBlockHeaderMsgSerializer.getInstance().deserialize(context, byteReader);

        long nonce = byteReader.readInt64LE();

        // read number of short transaction id and short transaction ids
        var shortIdsLength = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);
        var shortTxIds = new ArrayList<Long>( shortIdsLength.getValue()<0 || shortIdsLength.getValue()>Integer.MAX_VALUE ? -1 : (int)shortIdsLength.getValue() ); // NOTE: IllegalArgumentException will be thrown if length is too large or negative
        for (long i = 0; i < shortIdsLength.getValue(); i++) {
            shortTxIds.add(byteReader.readInt48LE());
        }

        // read number of prefilled transactions and transactions
        var prefilledTxnLength = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);
        var prefilledTransactions = new ArrayList<PrefilledTxMsg>( prefilledTxnLength.getValue()<0 || prefilledTxnLength.getValue()>Integer.MAX_VALUE ? -1 : (int)prefilledTxnLength.getValue() );
        for (long i = 0; i < prefilledTxnLength.getValue(); i++) {
            prefilledTransactions.add(PrefilledTxMsgSerializer.getInstance().deserialize(context, byteReader));
        }

        return CompactBlockMsg.builder()
            .header(blockHeader)
            .nonce(nonce)
            .shortTxIds(shortTxIds)
            .prefilledTransactions(prefilledTransactions)
            .build();
    }

    @Override
    public void serialize(SerializerContext context, CompactBlockMsg message, ByteArrayWriter byteWriter) {
        // write header
        CompactBlockHeaderMsgSerializer.getInstance().serialize(context, message.getHeader(), byteWriter);

        // write nonce
        byteWriter.writeUint64LE(message.getNonce());

        var shortTxIds = message.getShortTxIds();

        // write short transactions length
        var shortTxIdsLength = VarIntMsg.builder().value(shortTxIds.size()).build();
        VarIntMsgSerializer.getInstance().serialize(context, shortTxIdsLength, byteWriter);

        var buffer = ByteBuffer.allocate(6);
        shortTxIds.forEach(byteWriter::writeUint48LE);

        var prefilledTransactions = message.getPrefilledTransactions();

        // write short transactions length
        var txIdsLength = VarIntMsg.builder().value(prefilledTransactions.size()).build();
        VarIntMsgSerializer.getInstance().serialize(context, txIdsLength, byteWriter);

        prefilledTransactions.forEach(prefilledTransaction ->
            PrefilledTxMsgSerializer.getInstance().serialize(context, prefilledTransaction, byteWriter)
        );
    }
}
