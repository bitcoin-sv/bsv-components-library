package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.net.protocol.messages.BlockMsg;
import com.nchain.jcl.net.protocol.messages.RawBlockMsg;
import com.nchain.jcl.net.protocol.messages.RawTxMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.tools.serialization.BitcoinSerializerUtils;
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

        int numBytesToRead = (int) (context.getMaxBytesToRead() - blockHeader.getLengthInBytes());
        long totalBytesRemaining = numBytesToRead;


        //record each tx in this batch
        List<RawTxMsg> txs = new ArrayList<>();

        while (totalBytesRemaining > 0) {
            int totalBytesInTx = 0;

            //deserialize tx
            totalBytesInTx += 4; //version

            //input count
            long inputCount =  BitcoinSerializerUtils.deserializeVarIntWithoutExtraction(byteReader, totalBytesInTx);
            totalBytesInTx += BitcoinSerializerUtils.getVarIntSizeInBytes(inputCount);

            //calculate total bytes in txInput
            for (int i = 0; i < inputCount; i++) {
                totalBytesInTx += 36; //outpoint;

                //script length
                long scriptLen = BitcoinSerializerUtils.deserializeVarIntWithoutExtraction(byteReader, totalBytesInTx);
                totalBytesInTx += BitcoinSerializerUtils.getVarIntSizeInBytes(scriptLen);

                //script
                totalBytesInTx += scriptLen;

                totalBytesInTx += 4; //sequence
            }

            long outputCount = BitcoinSerializerUtils.deserializeVarIntWithoutExtraction(byteReader, totalBytesInTx);
            totalBytesInTx += BitcoinSerializerUtils.getVarIntSizeInBytes(outputCount);

            for (int i = 0; i < outputCount; i++) {
                totalBytesInTx += 8; //value

                //script length
                long scriptLen = BitcoinSerializerUtils.deserializeVarIntWithoutExtraction(byteReader, totalBytesInTx);
                totalBytesInTx += BitcoinSerializerUtils.getVarIntSizeInBytes(scriptLen);

                //script
                totalBytesInTx += scriptLen;
            }

            totalBytesInTx += 4; //lock time


            txs.add(new RawTxMsg(byteReader.read(totalBytesInTx), 0)); // checksum ZERO

            totalBytesRemaining -= totalBytesInTx;
        }

        return RawBlockMsg.builder().blockHeader(blockHeader).txs(txs).build();
    }

    @Override
    public void serialize(SerializerContext context, RawBlockMsg blockRawMsg, ByteArrayWriter byteWriter) {
        BlockHeaderMsgSerializer.getInstance().serialize(context, blockRawMsg.getBlockHeader(), byteWriter);
        blockRawMsg.getTxs().forEach(t -> {
            byteWriter.write(t.getContent());
        });
    }
}
