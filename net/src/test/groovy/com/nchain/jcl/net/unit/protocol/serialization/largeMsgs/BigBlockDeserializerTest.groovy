package com.nchain.jcl.net.unit.protocol.serialization.largeMsgs

import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.net.protocol.messages.PartialBlockHeaderMsg
import com.nchain.jcl.net.protocol.messages.PartialBlockTXsMsg
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext
import com.nchain.jcl.net.protocol.serialization.largeMsgs.BigBlockDeserializer
import com.nchain.jcl.net.unit.protocol.tools.MsgTest
import com.nchain.jcl.base.tools.bytes.ByteArrayReader
import com.nchain.jcl.base.tools.bytes.ByteArrayReaderOptimized
import com.nchain.jcl.base.tools.bytes.HEX
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class BigBlockDeserializerTest extends Specification {


    /**
     * We test the a "Big" Message is deserializes properly and that the callbacks are triggered and we deserialize notified
     * of the different parts of this Block (the header, and diferent multiple notifications of TXs
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
            DeserializerContext deserializedContext = DeserializerContext.builder().protocolBasicConfig(protocolConfig.getBasicConfig()).build()
            ByteArrayReader reader = new ByteArrayReader(HEX.decode(BLOCK_HEX))
            ByteArrayReader optimizedReader = new ByteArrayReaderOptimized(reader)
            BigBlockDeserializer bigBlockDeserializer = new BigBlockDeserializer()

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
            bigBlockDeserializer.deserialize(deserializedContext, optimizedReader)
            println("End of Test.")

        then:
            // We check that we received the Header, the right number of TXs,a nd no errors have been thrown
            headerReceived.get()
            numTXsReceived.get() == NUM_TXS
            !errorThrown.get()
    }

}
