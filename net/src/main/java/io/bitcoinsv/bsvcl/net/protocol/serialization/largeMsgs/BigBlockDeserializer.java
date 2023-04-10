package io.bitcoinsv.bsvcl.net.protocol.serialization.largeMsgs;


import com.google.common.base.Preconditions;
import io.bitcoinsv.bsvcl.net.protocol.messages.*;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.protocol.messages.*;
import io.bitcoinsv.bsvcl.net.protocol.serialization.BlockHeaderMsgSerializer;
import io.bitcoinsv.bsvcl.net.protocol.serialization.TxMsgSerializer;
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayReader;
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
 * An implementation of Big Blocks Deserializer. Its based on the LargeMessageDeserializerImpl, so the general
 * behaviour consists of deserializing "small" parts of the Block and notify them using the convenience methods
 * "notify" provided by the parent Class. Those notifications will trigger callbacks that previously must have been
 * fed by the client of this class.
 */
public class BigBlockDeserializer extends LargeMessageDeserializerImpl {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BigBlockDeserializer.class);

    // Once the Block Header is deserialized, we keep a reference here, since we include it as well when we
    // deserialize each set of TXs:
    private BlockHeaderMsg blockHeader;

    /** Constructor */
    public BigBlockDeserializer(ExecutorService executor) {
        super(executor); }

    /** Constructor. Callbacks will be blocking */
    public BigBlockDeserializer() { super(); }

    @Override
    public void deserializeBody(DeserializerContext context, HeaderMsg headerMsg, ByteArrayReader byteReader) {
        try {
            // Sanity Check:
            Preconditions.checkState(super.partialMsgSize != null, "The Size of partial Msgs must be defined before using a Large Deserializer");

            // We first deserialize the Block Header:
            log.trace("Deserializing the Block Header...");
            blockHeader = BlockHeaderMsgSerializer.getInstance().deserialize(context, byteReader);
            PartialBlockHeaderMsg partialBlockHeader = PartialBlockHeaderMsg.builder()
                    .headerMsg(headerMsg)
                    .blockHeader(blockHeader)
                    .txsSizeInBytes(context.getMaxBytesToRead() - blockHeader.getLengthInBytes())
                    .blockTxsFormat(PartialBlockHeaderMsg.BlockTxsFormat.DESERIALIZED)
                    .build();
            notifyDeserialization(partialBlockHeader);

            // Now we Deserialize the Txs, in batches..
            log.trace("Deserializing TXs...");
            long numTxs = blockHeader.getTransactionCount().getValue();
            List<TxMsg> txList = new ArrayList<>();

            // Order of each batch of Txs within the Block
            long txsOrderNumber = 0;

            // Index of the FIRST Tx in this Chunk within the Block
            long txsIndexNumber = 0;

            // We keep track of some values:
            int currentBatchSize = 0;
            Instant deserializingTime = Instant.now();

            for (int i = 0; i < numTxs; i++) {
                TxMsg txMsg = TxMsgSerializer.getInstance().deserialize(context, byteReader);
                currentBatchSize += txMsg.getLengthInBytes();
                txList.add(txMsg);
                if (currentBatchSize > super.partialMsgSize) {
                    // We notify about a new Batch of TX Deserialized...
                    log.trace("Batch of " + txList.size() + " Txs deserialized :: "
                            + currentBatchSize + " bytes, "
                            + Duration.between(deserializingTime, Instant.now()).toMillis() + " milissecs...");
                    PartialBlockTXsMsg partialBlockTXs = PartialBlockTXsMsg.builder()
                            .blockHeader(blockHeader)
                            .txs(txList)
                            .txsOrdersNumber(txsOrderNumber)
                            .txsIndexNumber(txsIndexNumber)
                            .build();

                    notifyDeserialization(partialBlockTXs);

                    // We reset the counters...
                    currentBatchSize = 0;
                    deserializingTime = Instant.now();
                    txsOrderNumber++;
                    txsIndexNumber += txList.size();

                    // we move on for the next Chunk:
                    txList = new ArrayList<>();
                }
            } // for...
            // In case we still have some TXs without being notified, we do it now...
            if (txList.size() > 0)
                notifyDeserialization(PartialBlockTXsMsg.builder()
                        .blockHeader(blockHeader)
                        .txs(txList)
                        .txsOrdersNumber(txsOrderNumber)
                        .txsIndexNumber(txsIndexNumber)
                        .build());

        } catch (Exception e) {
            e.printStackTrace();
            notifyError(e);
        }
    }
}
