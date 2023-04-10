package io.bitcoinsv.jcl.net.unit.protocol.serialization.largeMsgs

import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.jcl.net.protocol.config.provided.ProtocolBSVMainConfig
import io.bitcoinsv.jcl.net.protocol.messages.PartialBlockHeaderMsg
import io.bitcoinsv.jcl.net.protocol.messages.PartialBlockTXsMsg
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.jcl.net.protocol.serialization.largeMsgs.BigBlockDeserializer
import io.bitcoinsv.jcl.net.unit.protocol.tools.MsgTest
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader
import io.bitcoinsv.jcl.tools.bytes.ByteArrayReaderOptimized
import io.bitcoinsv.bitcoinjsv.core.Utils
import spock.lang.Specification

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class BigBlockDeserializerTest extends Specification {

    /**
     * We test the a "Big" Block is deserializes properly and that the callbacks are triggered and we get notified
     * of the different parts of this Block (the header, and different multiple notifications of TXs
     */
    def "Testing Big-Block Deserialized syncronously"() {
        given:
            // We are using the Block defined in the MsgTest Utility class, that Block contains 2 TXs
            final int NUM_TXS = 2
            String BLOCK_HEX = MsgTest.BLOCK_BODY_HEX

            ProtocolConfig protocolConfig = new ProtocolBSVMainConfig()

            // We keep track of the info sent by the callbacks triggered:
            AtomicBoolean headerReceived = new AtomicBoolean(false)
            AtomicInteger numTXsReceived = new AtomicInteger()
            Set<Long> txIndexesReceived  = ConcurrentHashMap.newKeySet();
            AtomicBoolean errorThrown    = new AtomicBoolean(false)

        when:
            // Reader of the Block:
            ByteArrayReader reader = new ByteArrayReader(Utils.HEX.decode(BLOCK_HEX))

            // Deserializer set up:
            BigBlockDeserializer bigBlockDeserializer = new BigBlockDeserializer()
            bigBlockDeserializer.setPartialMsgSize(1) // very small threshold, so we get 1 chunk per each Tx

            // We feed the Deserializer with callbacks, so we get notified when Header and Txs are Deserialized:
            bigBlockDeserializer.onDeserialized({ e ->
                if (e.getData() instanceof PartialBlockHeaderMsg) headerReceived.set(true)
                else if (e.getData() instanceof PartialBlockTXsMsg) {
                    numTXsReceived.addAndGet(((PartialBlockTXsMsg) e.getData()).txs.size() )
                    txIndexesReceived.add(((PartialBlockTXsMsg) e.getData()).txsIndexNumber.value);
                } else errorThrown.set(true);
                println("Partial Msg (" + e.getData().getClass().getSimpleName() + ") received")
            })

            bigBlockDeserializer.onError({e ->
                errorThrown.set(true)
                println("ERROR Received: " + e.getException())
            })

            // We start deserializing:
            DeserializerContext deserializedContext = DeserializerContext.builder()
                .protocolBasicConfig(protocolConfig.getBasicConfig())
                .maxBytesToRead(reader.size())
                .build()
            bigBlockDeserializer.deserializeBody(deserializedContext, null, reader)

            println("End of Test.")

        then:
            // We check that we received the Header, the right number of TXs,a nd no errors have been thrown
            headerReceived.get()
            numTXsReceived.get() == NUM_TXS
            // The block contains 2 Txs and each Chunk contains ony 1 Tx, so the "indexes" of Txs received are 0, 1
            txIndexesReceived.size() == 2
            txIndexesReceived.contains(0L)
            txIndexesReceived.contains(1L)
            // No error is expected
            !errorThrown.get()
    }

}
