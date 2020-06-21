package com.nchain.jcl.protocol.serialization.largeMsgs;

import com.nchain.jcl.protocol.messages.BlockHeaderMsg;
import com.nchain.jcl.protocol.messages.PartialBlockHeaderMsg;
import com.nchain.jcl.protocol.messages.PartialBlockTXsMsg;
import com.nchain.jcl.protocol.messages.TransactionMsg;
import com.nchain.jcl.protocol.serialization.BlockHeaderMsgSerializer;
import com.nchain.jcl.protocol.serialization.TransactionMsgSerializer;
import com.nchain.jcl.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.HEX;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author j.bloggs@nchain.com
 * Copyright (c) 2009-2010 Satoshi Nakamoto
 * Copyright (c) 2009-2016 The Bitcoin Core developers
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-18 15:25
 */
@Slf4j
public class BigBlockDeserializer extends LargeMessageDeserializerImpl {

    private static final int TX_BATCH = 1000;

    public BigBlockDeserializer(ExecutorService executor) { super(executor); }

    public BigBlockDeserializer() { super(); }

    @Override
    public void deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        try {
            // We first deserialize the Blcok Header:
            log.trace("Deserializing the Block Header...");
            BlockHeaderMsg blockHeader = BlockHeaderMsgSerializer.getInstance().deserialize(context, byteReader);
            PartialBlockHeaderMsg partialBlockHeader = PartialBlockHeaderMsg.builder().blockHeader(blockHeader).build();
            notifyDeserialization(partialBlockHeader);

            // Now we Deserialize the Txs, in batches..
            log.trace("Deserializing TXs...");
            long numTxs = blockHeader.getTransactionCount().getValue();
            List<TransactionMsg> txList = new ArrayList<>();

            for (int i = 0; i < numTxs; i++) {
                txList.add(TransactionMsgSerializer.getInstance().deserialize(context, byteReader));
                if (i > 0 && i % TX_BATCH == 0) {
                    // We notify about a new Batch of TX Deserialized...
                    log.trace("Batch of " + TX_BATCH + " Txs deserialized.");
                    PartialBlockTXsMsg partialBlockTXs = PartialBlockTXsMsg.builder().txs(txList).build();
                    txList = new ArrayList<>();
                    notifyDeserialization(partialBlockTXs);
                }
            } // for...
            // In case we still have some TXs without being notified, we do it now...
            if (txList.size() > 0) notifyDeserialization(PartialBlockTXsMsg.builder().txs(txList).build());

        } catch (Exception e) {
            notifyError(e);
        }
    }
}
