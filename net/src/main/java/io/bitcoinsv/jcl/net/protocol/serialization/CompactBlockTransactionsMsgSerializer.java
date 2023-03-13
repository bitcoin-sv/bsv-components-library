package io.bitcoinsv.jcl.net.protocol.serialization;

import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;
import io.bitcoinsv.jcl.net.protocol.messages.CompactBlockTransactionsMsg;
import io.bitcoinsv.jcl.net.protocol.messages.HashMsg;
import io.bitcoinsv.jcl.net.protocol.messages.TxMsg;
import io.bitcoinsv.jcl.net.protocol.messages.VarIntMsg;
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;

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
        if (startTxIndex.getValue() == 1) {
            coinbase = Optional.of(TxMsgSerializer.getInstance().deserialize(context, byteReader));
        }
        byte[] transactionIds = deserializeTransactionIds(context, byteReader);
        return CompactBlockTransactionsMsg.builder().blockHash(blockHash).startTxIndex(startTxIndex)
                .coinbaseTransaction(coinbase).transactionIds(transactionIds).build();
    }

    /**
     * Deserializes the chunk of transaction ids.
     * @param context
     * @param byteReader
     * @return Returns transaction ids as a byte array.
     */
    protected byte[] deserializeTransactionIds(DeserializerContext context, ByteArrayReader byteReader) {
        VarIntMsg numberOfTransactions = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);
        return byteReader.read((int)numberOfTransactions.getValue() * Sha256Hash.LENGTH);
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
        if (message.getStartTxIndex().getValue() == 1) {
            TxMsgSerializer.getInstance().serialize(context, message.getCoinbaseTransaction().get(), byteWriter);
        }
        VarIntMsgSerializer.getInstance().serialize(context, message.getNumberOfTransactions(), byteWriter);
        byteWriter.write(message.getTransactionIds());
    }
}