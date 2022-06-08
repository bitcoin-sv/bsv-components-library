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

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class BigBlockDeserializerTest extends Specification {


    /**
     * We test the a "Big" Message is deserializes properly and that the callbacks are triggered and we get notified
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
            AtomicBoolean errorThrown = new AtomicBoolean(false)

        when:
            // We configure the Deserializer and feed it with callbacks, so we deserialize notified when its different parts are
            // Deserialized:

            ByteArrayReader reader = new ByteArrayReader(Utils.HEX.decode(BLOCK_HEX))
            ByteArrayReader optimizedReader = new ByteArrayReaderOptimized(reader)
            BigBlockDeserializer bigBlockDeserializer = new BigBlockDeserializer()
            bigBlockDeserializer.setPartialMsgSize(1_000_000) // 1MB

        DeserializerContext deserializedContext = DeserializerContext.builder()
                .protocolBasicConfig(protocolConfig.getBasicConfig())
                .maxBytesToRead(reader.size())
                .build()

            bigBlockDeserializer.onDeserialized({ e ->
                if (e.getData() instanceof PartialBlockHeaderMsg) headerReceived.set(true)
                else if (e.getData() instanceof PartialBlockTXsMsg) {
                    numTXsReceived.addAndGet(((PartialBlockTXsMsg) e.getData()).txs.size() )
                } else errorThrown.set(true);
                println("Partial Msg (" + e.getData().getClass().getSimpleName() + ") received")
            })
            bigBlockDeserializer.onError({e ->
                errorThrown.set(true)
                println("ERROR Received: " + e.getException())
            })

            bigBlockDeserializer.deserializeBody(deserializedContext, null, optimizedReader)
            println("End of Test.")

        then:
            // We check that we received the Header, the right number of TXs,a nd no errors have been thrown
            headerReceived.get()
            numTXsReceived.get() == NUM_TXS
            !errorThrown.get()
    }

}
