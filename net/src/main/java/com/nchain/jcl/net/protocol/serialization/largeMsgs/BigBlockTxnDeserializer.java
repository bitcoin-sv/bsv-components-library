package com.nchain.jcl.net.protocol.serialization.largeMsgs;


import com.nchain.jcl.net.protocol.messages.HeaderMsg;
import com.nchain.jcl.net.protocol.messages.PartialBlockTxnMsg;
import com.nchain.jcl.net.protocol.messages.TxMsg;
import com.nchain.jcl.net.protocol.serialization.HashMsgSerializer;
import com.nchain.jcl.net.protocol.serialization.TxMsgSerializer;
import com.nchain.jcl.net.protocol.serialization.VarIntMsgSerializer;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static java.util.Optional.ofNullable;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 */
public class BigBlockTxnDeserializer extends LargeMessageDeserializerImpl {

    // The TX are Deserialized and notified in batches:
    private static final int TX_BATCH = 10_000;

    public BigBlockTxnDeserializer() {
    }

    public BigBlockTxnDeserializer(ExecutorService executor) {
        super(executor);
    }

    @Override
    public void deserializeBody(DeserializerContext context, HeaderMsg headerMsg, ByteArrayReader byteReader) {
        var blockHash = HashMsgSerializer.getInstance().deserialize(context, byteReader);
        var numOfTxs = VarIntMsgSerializer.getInstance().deserialize(context, byteReader).getValue();

        context.setCalculateHashes(true);

        long batchSize = ofNullable(context.getBatchSize()).orElse(TX_BATCH);

        int order = 0;

        List<TxMsg> transactions = new ArrayList<>();
        for (int i = 0; i < numOfTxs; i++) {
            transactions.add(TxMsgSerializer.getInstance().deserialize(context, byteReader));

            if (transactions.size() == batchSize) {
                notifyDeserialization(
                    PartialBlockTxnMsg.builder()
                        .headerMsg(headerMsg)
                        .blockHash(blockHash)
                        .transactions(new ArrayList<>(transactions))
                        .order(order)
                        .build()
                );

                order++;
                transactions.clear();
            }
        }

        if (!transactions.isEmpty()) {
            notifyDeserialization(
                PartialBlockTxnMsg.builder()
                    .blockHash(blockHash)
                    .transactions(transactions)
                    .order(order)
                    .build()
            );
        }
    }
}
