/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.serialization;

import io.bitcoinsv.jcl.net.protocol.messages.CompactBlockMsg;
import io.bitcoinsv.jcl.net.protocol.messages.PrefilledTxMsg;
import io.bitcoinsv.jcl.net.protocol.messages.VarIntMsg;
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;

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
        var shortTxIds = new ArrayList<Long>((short) shortIdsLength.getValue());
        for (int i = 0; i < shortIdsLength.getValue(); i++) {
            shortTxIds.add(byteReader.readInt48LE());
        }

        // read number of prefilled transactions and transactions
        var prefilledTxnLength = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);
        var prefilledTransactions = new ArrayList<PrefilledTxMsg>((short) prefilledTxnLength.getValue());
        for (int i = 0; i < prefilledTxnLength.getValue(); i++) {
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
