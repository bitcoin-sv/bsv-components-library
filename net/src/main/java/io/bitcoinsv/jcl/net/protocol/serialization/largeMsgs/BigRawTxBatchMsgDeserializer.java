package io.bitcoinsv.jcl.net.protocol.serialization.largeMsgs;


import com.google.common.base.Preconditions;
import io.bitcoinsv.jcl.net.protocol.messages.HeaderMsg;
import io.bitcoinsv.jcl.net.protocol.serialization.RawTxBatchMsgSerializer;
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import org.slf4j.Logger;

import java.util.concurrent.ExecutorService;

public class BigRawTxBatchMsgDeserializer extends LargeMessageDeserializerImpl {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BigRawTxBatchMsgDeserializer.class);

    /**
     * Constructor
     */
    public BigRawTxBatchMsgDeserializer(ExecutorService executor) {
        super(executor);
    }

    /**
     * Constructor. Callbacks will be blocking
     */
    public BigRawTxBatchMsgDeserializer() {
        super();
    }

    @Override
    public void deserializeBody(DeserializerContext context, HeaderMsg headerMsg, ByteArrayReader byteReader) {
        try {
            // Sanity Check:
            Preconditions.checkState(super.partialMsgSize != null, "The Size of partial Msgs must be defined before using a Large Deserializer");

            var msg = RawTxBatchMsgSerializer.getInstance().deserialize(context, byteReader);
            notifyDeserialization(msg);
        } catch (Exception e) {
            e.printStackTrace();
            notifyError(e);
        }
    }
}