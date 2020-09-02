package com.nchain.jcl.net.protocol.serialization;

import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.base.tools.bytes.HEX;
import com.nchain.jcl.net.protocol.messages.BlockHeaderMsg;
import com.nchain.jcl.net.protocol.messages.BlockMsg;
import com.nchain.jcl.net.protocol.messages.TransactionMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;


/**
 * @author i.fernandez @nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 01/10/2019 15:37
 * <p>
 * * A Serializer for {@link BlockMsg} messages
 */
@Slf4j
public class BlockMsgSerializer implements MessageSerializer<BlockMsg> {

    private BlockMsgSerializer() { }

    /**
     * Returns the instance of this Serializer (Singleton)
     */
    public static BlockMsgSerializer getInstance() {
        return new BlockMsgSerializer();
    }

    @Override
    public BlockMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {

        // First we deserialize the Block Header:
        BlockHeaderMsg blockHeader = BlockHeaderMsgSerializer.getInstance().deserialize(context, byteReader);

        // The transactions are taken from the Block Body...since the Block Header has been already extracted
        // from the "byteReader", the information remaining in there are the Block Transactions...

        // We are logging the Deserialization process. But we only log every 1% of progress, so no we do some Math
        // to calculate how many Txs we need to process before we log them...

        String blockHash = HEX.encode(blockHeader.getHash().getHashBytes());
        int numTxs = (int) blockHeader.getTransactionCount().getValue();
        int percentageLog = 1; // 1%
        int numTxsForEachLog = (percentageLog * numTxs) / 100;
        int accumulateNumTxs = 0;

        List<TransactionMsg> transactionMsgList = new ArrayList<>();
        for (int i = 0; i < numTxs; i++) {
            if ((accumulateNumTxs++) > numTxsForEachLog) {
                accumulateNumTxs = 0;
                int progress = (i * 100) / numTxs;
                log.trace("Deserializing Block " + blockHash + " " + progress + "% Done...");
            }
            transactionMsgList.add(TransactionMsgSerializer.getInstance().deserialize(context, byteReader));
        }
        return BlockMsg.builder().blockHeader(blockHeader).transactionMsgs(transactionMsgList).build();
    }

    @Override
    public void serialize(SerializerContext context, BlockMsg blockMsg, ByteArrayWriter byteWriter) {
        BlockHeaderMsgSerializer.getInstance().serialize(context, blockMsg.getBlockHeader(), byteWriter);
        for (TransactionMsg transactionMsg : blockMsg.getTransactionMsg()) {
            TransactionMsgSerializer.getInstance().serialize(context, transactionMsg, byteWriter);
        }
    }
}
