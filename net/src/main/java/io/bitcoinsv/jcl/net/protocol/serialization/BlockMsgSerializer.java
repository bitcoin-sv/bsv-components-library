/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.serialization;


import io.bitcoinsv.jcl.net.protocol.messages.BlockMsg;
import io.bitcoinsv.jcl.net.protocol.messages.TxMsg;
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReaderOptimized;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;
import io.bitcoinj.core.Utils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;


/**
 * @author i.fernandez @nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * * A Serializer for {@link BlockMsg} messages
 */
public class BlockMsgSerializer implements MessageSerializer<BlockMsg> {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BlockMsgSerializer.class);

    private BlockMsgSerializer() { }

    /**
     * Returns the instance of this Serializer (Singleton)
     */
    public static BlockMsgSerializer getInstance() {
        return new BlockMsgSerializer();
    }

    @Override
    public BlockMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {

        // We wrap the reader around an ByteArrayReaderOptimized, which works faster than a regular ByteArrayReader
        // (its also more expensive in terms of memory, but it usually pays off):
        ByteArrayReader reader = byteReader;
        if (!(byteReader instanceof ByteArrayReaderOptimized)) {
            reader = new ByteArrayReaderOptimized(byteReader);
        }

        // First we deserialize the Block Header:
        var blockHeader = BlockHeaderMsgSerializer.getInstance().deserialize(context, byteReader);

        // The transactions are taken from the Block Body...since the Block Header has been already extracted
        // from the "byteReader", the information remaining in there are the Block Transactions...

        // We are logging the Deserialization process. But we only log every 1% of progress, so no we do some Math
        // to calculate how many Txs we need to process before we log them...

        String blockHash = Utils.HEX.encode(blockHeader.getHash().getHashBytes());
        int numTxs = (int) blockHeader.getTransactionCount().getValue();
        int percentageLog = 1; // 1%
        int numTxsForEachLog = (percentageLog * numTxs) / 100;
        int accumulateNumTxs = 0;

        List<TxMsg> transactionMsgList = new ArrayList<>();
        for (int i = 0; i < numTxs; i++) {
            if ((accumulateNumTxs++) > numTxsForEachLog) {
                accumulateNumTxs = 0;
                int progress = (i * 100) / numTxs;
                log.trace("Deserializing Block " + blockHash + " " + progress + "% Done...");
            }
            transactionMsgList.add(TxMsgSerializer.getInstance().deserialize(context, reader));
        }
        return BlockMsg.builder().blockHeader(blockHeader).transactionMsgs(transactionMsgList).build();
    }

    @Override
    public void serialize(SerializerContext context, BlockMsg blockMsg, ByteArrayWriter byteWriter) {
        BlockHeaderMsgSerializer.getInstance().serialize(context, blockMsg.getBlockHeader(), byteWriter);
        for (TxMsg transactionMsg : blockMsg.getTransactionMsg()) {
            TxMsgSerializer.getInstance().serialize(context, transactionMsg, byteWriter);
        }
    }
}
