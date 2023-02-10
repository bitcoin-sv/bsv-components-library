package io.bitcoinsv.jcl.net.protocol.serialization;

import io.bitcoinsv.jcl.net.protocol.messages.CompactBlockTransactionsMsg;
import io.bitcoinsv.jcl.net.protocol.messages.CompactTransactionMsg;
import io.bitcoinsv.jcl.net.protocol.messages.HashMsg;
import io.bitcoinsv.jcl.net.protocol.messages.TxMsg;
import io.bitcoinsv.jcl.net.protocol.messages.VarIntMsg;
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A Serializer for {@link CompactBlockTransactionsMsg} messages
 */
public class CompactBlockTransactionsMsgSerializer implements MessageSerializer<CompactBlockTransactionsMsg> {

    private static CompactBlockTransactionsMsgSerializer instance;

    private CompactBlockTransactionsMsgSerializer() {
    }

    public static CompactBlockTransactionsMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (CompactBlockTransactionsMsgSerializer.class) {
                instance = new CompactBlockTransactionsMsgSerializer();
            }
        }
        return instance;
    }

    /**
     * Deserializes the message
     * @param context                   Serializer Context
     * @param byteReader                Wrapper for the Byte Array Source
     * @return CompactBlockTransactionsMsg instance
     */
    @Override
    public CompactBlockTransactionsMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        HashMsg blockHash = HashMsgSerializer.getInstance().deserialize(context, byteReader);
        VarIntMsg startTxIndex = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);
        Optional<TxMsg> coinbase = Optional.empty();
        if (startTxIndex.getValue() == 0) {
            coinbase = Optional.of(TxMsgSerializer.getInstance().deserialize(context, byteReader));
        }
        List<CompactTransactionMsg> compactTransactions = deserializeList(context, byteReader);
        return CompactBlockTransactionsMsg.builder().blockHash(blockHash).startTxIndex(startTxIndex)
                .coinbaseTransaction(coinbase).compactTransactions(compactTransactions).build();
    }

    /**
     * Deserializes the list of CompactTransactionMsg
     * @param context
     * @param byteReader
     * @return
     */
    protected List<CompactTransactionMsg> deserializeList(DeserializerContext context, ByteArrayReader byteReader) {
        VarIntMsg numberOfTransactions = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);
        List<CompactTransactionMsg> compactTransactions = new ArrayList<>((int)numberOfTransactions.getValue());
        CompactTransactionMsgSerializer compactTransactionMsgSerializer = CompactTransactionMsgSerializer.getInstance();
        for (int i = 0; i < numberOfTransactions.getValue(); ++i) {
            CompactTransactionMsg compactTransaction = compactTransactionMsgSerializer.deserialize(context, byteReader);
            compactTransactions.add(compactTransaction);
        }
        return compactTransactions;
    }

    /**
     * Serializes CompactBlockTransactionsMsg into P2P message body.
     * @param context                   Serializer Context
     * @param message                   Message to Serialize
     * @param byteWriter                Result of the Serialization
     */
    @Override
    public void serialize(SerializerContext context, CompactBlockTransactionsMsg message, ByteArrayWriter byteWriter) {
        HashMsgSerializer.getInstance().serialize(context, message.getBlockHash(), byteWriter);
        VarIntMsgSerializer.getInstance().serialize(context, message.getStartTxIndex(), byteWriter);
        if (message.getStartTxIndex().getValue() == 0) {
            TxMsgSerializer.getInstance().serialize(context, message.getCoinbaseTransaction().get(), byteWriter);
        }
        VarIntMsgSerializer.getInstance().serialize(context, message.getNumberOfTransactions(), byteWriter);
        serializeList(context, message.getCompactTransactions(), byteWriter);
    }

    /**
     * Serializes the list of CompactTransactionMsg
     *
     * @param context
     * @param compactTransactions
     * @param byteWriter
     */
    protected void serializeList(SerializerContext context, List<CompactTransactionMsg> compactTransactions, ByteArrayWriter byteWriter) {
        for (CompactTransactionMsg compactTransaction  : compactTransactions) {
            CompactTransactionMsgSerializer.getInstance().serialize(context, compactTransaction, byteWriter);
        }
    }
}