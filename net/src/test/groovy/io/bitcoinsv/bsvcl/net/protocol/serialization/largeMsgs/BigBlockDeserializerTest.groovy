package io.bitcoinsv.bsvcl.net.protocol.serialization.largeMsgs


import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bsvcl.net.protocol.tools.MsgTest
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

            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig protocolConfig = new io.bitcoinsv.bsvcl.net.protocol.config.provided.ProtocolBSVMainConfig()

            // We keep track of the info sent by the callbacks triggered:
            AtomicBoolean headerReceived = new AtomicBoolean(false)
            AtomicInteger numTXsReceived = new AtomicInteger()
            Set<Long> txIndexesReceived  = ConcurrentHashMap.newKeySet();
            AtomicBoolean errorThrown    = new AtomicBoolean(false)

        when:
            // Reader of the Block:
            ByteArrayReader reader = new ByteArrayReader(Utils.HEX.decode(BLOCK_HEX))

            // Deserializer set up:
            io.bitcoinsv.bsvcl.net.protocol.serialization.largeMsgs.BigBlockDeserializer bigBlockDeserializer = new io.bitcoinsv.bsvcl.net.protocol.serialization.largeMsgs.BigBlockDeserializer()
            bigBlockDeserializer.setPartialMsgSize(1) // very small threshold, so we get 1 chunk per each Tx

            // We feed the Deserializer with callbacks, so we get notified when Header and Txs are Deserialized:
            bigBlockDeserializer.onDeserialized({ e ->
                if (e.getData() instanceof io.bitcoinsv.bsvcl.net.protocol.messages.PartialBlockHeaderMsg) headerReceived.set(true)
                else if (e.getData() instanceof io.bitcoinsv.bsvcl.net.protocol.messages.PartialBlockTXsMsg) {
                    numTXsReceived.addAndGet(((io.bitcoinsv.bsvcl.net.protocol.messages.PartialBlockTXsMsg) e.getData()).txs.size() )
                    txIndexesReceived.add(((io.bitcoinsv.bsvcl.net.protocol.messages.PartialBlockTXsMsg) e.getData()).txsIndexNumber.value);
                } else errorThrown.set(true);
                println("Partial Msg (" + e.getData().getClass().getSimpleName() + ") received")
            })

            bigBlockDeserializer.onError({e ->
                errorThrown.set(true)
                println("ERROR Received: " + e.getException())
            })

            // We start deserializing:
            io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext deserializedContext = io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext.builder()
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
