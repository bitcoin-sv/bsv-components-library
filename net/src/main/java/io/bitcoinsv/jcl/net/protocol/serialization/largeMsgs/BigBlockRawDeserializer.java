package io.bitcoinsv.jcl.net.protocol.serialization.largeMsgs;


import io.bitcoinsv.jcl.net.protocol.messages.BlockHeaderMsg;
import io.bitcoinsv.jcl.net.protocol.messages.PartialBlockHeaderMsg;
import io.bitcoinsv.jcl.net.protocol.messages.PartialBlockRawTXsMsg;
import io.bitcoinsv.jcl.net.protocol.serialization.BlockHeaderMsgSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
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
public class BigBlockRawDeserializer extends LargeMessageDeserializerImpl {

    // Size of each Chunk of TXs in byte array format:
    private static final int TX_CHUNK_SIZE = 10_000_000;    // 10 MB

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BigBlockRawDeserializer.class);

    // Once the Block Header is deserialzed, we keep a reference here, since we include it as well when we
    // deserialize each set of TXs:
    private BlockHeaderMsg blockHeader;

    /** Constructor */
    public BigBlockRawDeserializer(ExecutorService executor) {
        super(executor);
    }

    /** Constructor. Callbacks will be blocking */
    public BigBlockRawDeserializer() {
        super(null);
    }

    @Override
    public void deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        try {
            // We update the reader:
            adjustReaderSpeed(byteReader);

            // We first deserialize the Block Header:
            log.trace("Deserializing the Block Header...");
            blockHeader = BlockHeaderMsgSerializer.getInstance().deserialize(context, byteReader);
            PartialBlockHeaderMsg partialBlockHeader = PartialBlockHeaderMsg.builder()
                    .blockHeader(blockHeader)
                    .txsSizeInBytes(context.getMaxBytesToRead() - blockHeader.getLengthInBytes())
                    .blockTxsFormat(PartialBlockHeaderMsg.BlockTxsFormat.RAW)
                    .build();
            notifyDeserialization(partialBlockHeader);

            // Now we Deserialize the Txs, in batches..
            log.trace("Deserializing TXs...");
            long txsBytesSize = context.getMaxBytesToRead() - blockHeader.getLengthInBytes();
            long txsBytesRemaining = txsBytesSize;
            int numChunks = (int) Math.ceil((double) txsBytesSize / TX_CHUNK_SIZE);

            // Order of each batch of Txs within the Block
            long txsOrderNumber = 0;

            // We keep track of some statistics:
            Instant deserializingTime = Instant.now();

            for (int i = 0; i < numChunks; i++) {
                int numBytesToRead = (int) Math.min(txsBytesRemaining, TX_CHUNK_SIZE);
                byte[] chunk = byteReader.read(numBytesToRead);

                // We notify about a new Batch of TX Deserialized...
                log.trace("Batch of " + TX_CHUNK_SIZE + " bytes of Txs deserialized :: "
                        + Duration.between(deserializingTime, Instant.now()).toMillis() + " milissecs...");
                PartialBlockRawTXsMsg partialBlockRawTXs = PartialBlockRawTXsMsg.builder()
                        .blockHeader(blockHeader)
                        .txs(chunk)
                        .txsOrdersNumber(txsOrderNumber)
                        .build();
                notifyDeserialization(partialBlockRawTXs);

                // We reset the counters for logging...
                txsBytesRemaining -= numBytesToRead;
                deserializingTime = Instant.now();
                txsOrderNumber++;

            } // for...

            // We reset the reader as it was before..
            resetReaderSpeed(byteReader);

        } catch (Exception e) {
            notifyError(e);
        }
    }
}
