package com.nchain.jcl.net.protocol.serialization.largeMsgs;


import com.nchain.jcl.net.protocol.messages.BlockHeaderMsg;
import com.nchain.jcl.net.protocol.messages.PartialBlockHeaderMsg;
import com.nchain.jcl.net.protocol.messages.PartialBlockTXsMsg;
import com.nchain.jcl.net.protocol.messages.TxMsg;
import com.nchain.jcl.net.protocol.serialization.BlockHeaderMsgSerializer;
import com.nchain.jcl.net.protocol.serialization.TxMsgSerializer;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An implementation of Big Blocks Deserializer. Its based on the LargeMessageDeserializerImpl, do the general
 * behaviour consists of deserializing "small" parts of the Block and notify them using the convenience methods
 * "notify" provided vby the parent Class. Those notification will trigger callbacks that previously must have been
 * fed byt he client of this class.
 */
public class BigBlockDeserializer extends LargeMessageDeserializerImpl {

    // The TX are Deserialized and notified in batches:
    private static final int TX_BATCH = 10000;
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BigBlockDeserializer.class);

    // Once the Block Header is deserialzed, we keep a reference here, since we include it as well when we
    // deserialze each set of TXs:
    private BlockHeaderMsg blockHeader;

    /** Constructor */
    public BigBlockDeserializer(ExecutorService executor) { super(executor); }

    /** Constructor. Callbacks will be blocking */
    public BigBlockDeserializer() { super(); }

    @Override
    public void deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        try {
            // We first deserialize the Block Header:
            log.trace("Deserializing the Block Header...");
            blockHeader = BlockHeaderMsgSerializer.getInstance().deserialize(context, byteReader);
            PartialBlockHeaderMsg partialBlockHeader = PartialBlockHeaderMsg.builder().blockHeader(blockHeader).build();
            notifyDeserialization(partialBlockHeader);

            // Now we Deserialize the Txs, in batches..
            log.trace("Deserializing TXs...");
            long numTxs = blockHeader.getTransactionCount().getValue();
            List<TxMsg> txList = new ArrayList<>();

            // Order of each batch of Txs within the Block
            long txsOrderNumber = 0;

            // We keep track of some statistics:
            int totalTxsSize = 0;
            Instant deserializingTime = Instant.now();

            for (int i = 0; i < numTxs; i++) {
                TxMsg txMsg = TxMsgSerializer.getInstance().deserialize(context, byteReader);
                txList.add(txMsg);
                totalTxsSize += txMsg.getLengthInBytes();
                if (i > 0 && i % TX_BATCH == 0) {
                    // We notify about a new Batch of TX Deserialized...
                    log.trace("Batch of " + TX_BATCH + " Txs deserialized :: "
                            + totalTxsSize + " bytes, "
                            + Duration.between(deserializingTime, Instant.now()).toMillis() + " milissecs...");
                    PartialBlockTXsMsg partialBlockTXs = PartialBlockTXsMsg.builder()
                            .blockHeader(blockHeader)
                            .txs(txList)
                            .txsOrdersNumber(txsOrderNumber)
                            .build();
                    txList = new ArrayList<>();
                    notifyDeserialization(partialBlockTXs);

                    // We reset the counters for logging...
                    totalTxsSize = 0;
                    deserializingTime = Instant.now();
                    txsOrderNumber++;
                }
            } // for...
            // In case we still have some TXs without being notified, we do it now...
            if (txList.size() > 0)
                notifyDeserialization(PartialBlockTXsMsg.builder()
                        .blockHeader(blockHeader)
                        .txs(txList)
                        .txsOrdersNumber(txsOrderNumber)
                        .build());

        } catch (Exception e) {
            notifyError(e);
        }
    }
}
