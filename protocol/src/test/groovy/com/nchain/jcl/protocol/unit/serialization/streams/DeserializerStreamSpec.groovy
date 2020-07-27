package com.nchain.jcl.protocol.unit.serialization.streams

import com.nchain.jcl.protocol.config.ProtocolConfig
import com.nchain.jcl.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.protocol.messages.VersionAckMsg
import com.nchain.jcl.protocol.messages.common.BitcoinMsg
import com.nchain.jcl.protocol.streams.DeserializerStream
import com.nchain.jcl.protocol.unit.tools.MsgTest
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.HEX
import com.nchain.jcl.tools.config.RuntimeConfig
import com.nchain.jcl.tools.config.provided.RuntimeConfigDefault
import com.nchain.jcl.tools.streams.StreamDataEvent
import spock.lang.Specification

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

/**
 * Testing class for small use cases for the Deserializer
 */
class DeserializerStreamSpec extends Specification {

    /**
     * We test that an EMPTY Message (a Message with HEAD but NOT BODY) is deserialized properly.
     */
    def "Testing Deserializer Empry Messages"() {
        given:
            RuntimeConfig   runtimeConfig = new RuntimeConfigDefault()
            ProtocolConfig protocolConfig = new ProtocolBSVMainConfig()
            ExecutorService executor = Executors.newSingleThreadExecutor()

            // We configure the Streams: The Source and the Deserializer:
            MsgTest.DummyPeerStreamSource source = MsgTest.getDummyStreamSource()
            DeserializerStream stream = new DeserializerStream(executor, source, runtimeConfig, protocolConfig.getBasicConfig())

            // We store the Msg after being Deserialized:
            AtomicReference<BitcoinMsg> msgReceived = new AtomicReference<>()
            stream.onData({ e -> msgReceived.set(e.getData())})

            // This is the BitcoinMSg that we are sending (in byteArray format) to the Deserializer through the
            // Source. We kee an instance here we we can compare that the original version and the deserialized one
            // are identical:
            BitcoinMsg<VersionAckMsg> msg = MsgTest.getVersionAckMsg()
        when:
            ByteArrayReader reader = new ByteArrayReader(HEX.decode(MsgTest.VERSION_ACK_HEX))
            source.send(new StreamDataEvent<ByteArrayReader>(reader))
            // We need to wait a bit, for the events to be triggered...
            Thread.sleep(100)
        then:
            // We check the Deserialized Msg is the same as the original one:
            msgReceived.get().equals(msg)
    }
}
