package io.bitcoinsv.bsvcl.net.protocol.serialization;

import io.bitcoinsv.bitcoinjsv.core.Utils;
import io.bitcoinsv.bsvcl.net.protocol.messages.RawTxBatchMsg;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter;

import java.util.ArrayList;
import java.util.List;

public class RawTxBatchMsgSerializer implements MessageSerializer<RawTxBatchMsg> {
    private static RawTxBatchMsgSerializer instance;

    private RawTxBatchMsgSerializer() {
    }

    public static RawTxBatchMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (RawTxBatchMsgSerializer.class) {
                instance = new RawTxBatchMsgSerializer();
            }
        }

        return instance;
    }

    @Override
    public RawTxBatchMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        var count = byteReader.readUint32();
        List<byte[]> txs = new ArrayList<>((int) count);

        for (int i = 0; i < count; i++) {
            long numOfBytes = byteReader.readUint32();
            txs.add(byteReader.read((int) numOfBytes));
        }

        return new RawTxBatchMsg(txs, Utils.EMPTY_BYTE_ARRAY, 0);
    }

    @Override
    public void serialize(SerializerContext context, RawTxBatchMsg message, ByteArrayWriter byteWriter) {
        byteWriter.writeUint32LE(message.getTxs().size());

        for (byte[] tx : message.getTxs()) {
            byteWriter.writeUint32LE(tx.length);
            byteWriter.write(tx);
        }
    }
}