package io.bitcoinsv.jcl.net.protocol.serialization;


import io.bitcoinsv.jcl.net.protocol.messages.BlockMsg;
import io.bitcoinsv.jcl.net.protocol.messages.RawBlockMsg;
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;
import org.slf4j.Logger;


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
        byte[] txs = byteReader.read(numBytesToRead);
        return RawBlockMsg.builder().blockHeader(blockHeader).txs(txs).build();
    }

    @Override
    public void serialize(SerializerContext context, RawBlockMsg blockRawMsg, ByteArrayWriter byteWriter) {
        BlockHeaderMsgSerializer.getInstance().serialize(context, blockRawMsg.getBlockHeader(), byteWriter);
        byteWriter.write(blockRawMsg.getTxs());
    }
}
