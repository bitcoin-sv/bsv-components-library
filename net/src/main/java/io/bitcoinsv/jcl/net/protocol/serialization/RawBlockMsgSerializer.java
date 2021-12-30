package io.bitcoinsv.jcl.net.protocol.serialization;


import io.bitcoinsv.jcl.net.protocol.messages.BlockMsg;
import io.bitcoinsv.jcl.net.protocol.messages.RawBlockMsg;
import io.bitcoinsv.jcl.net.protocol.messages.RawTxMsg;
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;
import io.bitcoinsv.jcl.tools.serialization.BitcoinSerializerUtils;
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
        var blockHeader = BlockHeaderMsgSerializer.getInstance().deserialize(context, byteReader);

        // The transactions are taken from the Block Body...since the Block Header has been already extracted
        // from the "byteReader", the information remaining in there are the Block Transactions...

        long numTxs = blockHeader.getTransactionCount().getValue();
        List<RawTxMsg> txs = new ArrayList<>();

        while (txs.size() < numTxs) {
            RawTxMsg tx = deserializeNextTx(context, byteReader);
            txs.add(tx);
        }

        return RawBlockMsg.builder().blockHeader(blockHeader).txs(txs).build();
    }

    /**
     * It deserializes a single Tx of this block from the Byte Array Reader, and returns it
     */
    public RawTxMsg deserializeNextTx(DeserializerContext context, ByteArrayReader byteReader) {
        RawTxMsg result = null;

        // We need to locate the position in this Reader that marks te end of the Txs, and then we just "extract" all
        // the bytes from the beginning and up to that point:
        int numBytesInTx = 0;

        // Version
        numBytesInTx += 4;

        // input count
        long inputCount =  BitcoinSerializerUtils.deserializeVarIntWithoutExtraction(byteReader, numBytesInTx);
        numBytesInTx += BitcoinSerializerUtils.getVarIntSizeInBytes(inputCount);

        // txInputs
        for (int i = 0; i < inputCount; i++) {
            // output
            numBytesInTx += 36;
            //script length
            long scriptLen = BitcoinSerializerUtils.deserializeVarIntWithoutExtraction(byteReader, numBytesInTx);
            numBytesInTx += BitcoinSerializerUtils.getVarIntSizeInBytes(scriptLen);
            // script
            numBytesInTx += scriptLen;
            // sequence
            numBytesInTx += 4;
        }

        // output count
        long outputCount = BitcoinSerializerUtils.deserializeVarIntWithoutExtraction(byteReader, numBytesInTx);
        numBytesInTx += BitcoinSerializerUtils.getVarIntSizeInBytes(outputCount);

        // txOutputs
        for (int i = 0; i < outputCount; i++) {
            // Value
            numBytesInTx += 8;
            //script length
            long scriptLen = BitcoinSerializerUtils.deserializeVarIntWithoutExtraction(byteReader, numBytesInTx);
            numBytesInTx += BitcoinSerializerUtils.getVarIntSizeInBytes(scriptLen);

            //script
            numBytesInTx += scriptLen;
        }
        // locktime
        numBytesInTx += 4;

        result = new RawTxMsg(byteReader.read(numBytesInTx), 0); // checksum ZERO
        return result;

    }

    @Override
    public void serialize(SerializerContext context, RawBlockMsg blockRawMsg, ByteArrayWriter byteWriter) {
        BlockHeaderMsgSerializer.getInstance().serialize(context, blockRawMsg.getBlockHeader(), byteWriter);
        blockRawMsg.getTxs().forEach(t -> {
            byteWriter.write(t.getContent());
        });
    }
}
