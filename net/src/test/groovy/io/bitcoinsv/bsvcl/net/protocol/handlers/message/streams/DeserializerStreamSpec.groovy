package io.bitcoinsv.bsvcl.net.protocol.handlers.message.streams

import io.bitcoinsv.bsvcl.net.network.streams.StreamDataEvent
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.bsvcl.net.protocol.handlers.message.streams.deserializer.Deserializer
import io.bitcoinsv.bsvcl.net.protocol.handlers.message.streams.deserializer.DeserializerConfig
import io.bitcoinsv.bsvcl.net.protocol.handlers.message.streams.deserializer.DeserializerStream
import io.bitcoinsv.bsvcl.net.protocol.messages.VersionAckMsg
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg
import io.bitcoinsv.bsvcl.net.protocol.tools.MsgTest
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader
import io.bitcoinsv.bsvcl.common.config.RuntimeConfig
import io.bitcoinsv.bsvcl.common.config.provided.RuntimeConfigDefault

import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import spock.lang.Ignore
import spock.lang.Specification

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

/**
 * Testing class for small use cases for the Deserializer
 */
@Ignore
class DeserializerStreamSpec extends Specification {

    /**
     * We test that an EMPTY Message (a Message with HEAD but NOT BODY) is deserialized properly.
     */
    def "Testing Deserializer Empty Messages"() {
        given:
            RuntimeConfig runtimeConfig = new RuntimeConfigDefault()
            ProtocolConfig protocolConfig = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            ExecutorService eventBusExecutor = Executors.newSingleThreadExecutor()
            ExecutorService dedicatedConnExecutor = Executors.newSingleThreadExecutor()

            // We configure the Streams: The Source and the Deserializer:
            MsgTest.DummyPeerStreamSource source = MsgTest.getDummyStreamSource()
            Deserializer deserializer = new Deserializer(runtimeConfig, DeserializerConfig.builder().build())
            DeserializerStream stream = new DeserializerStream(eventBusExecutor, source, runtimeConfig, protocolConfig.getMessageConfig(), deserializer, dedicatedConnExecutor)

            // We store the Msg after being Deserialized:
            AtomicReference<BitcoinMsg> msgReceived = new AtomicReference<>()
            stream.onData({ e -> msgReceived.set(e.getData())})

            // This is the BitcoinMSg that we are sending (in byteArray format) to the Deserializer through the
            // Source. We kee an instance here we we can compare that the original version and the deserialized one
            // are identical:
            BitcoinMsg<VersionAckMsg> msg = MsgTest.getVersionAckMsg()
        when:
            ByteArrayReader reader = new ByteArrayReader(Utils.HEX.decode(MsgTest.VERSION_ACK_HEX))
            source.send(new StreamDataEvent<ByteArrayReader>(reader))
            // We need to wait a bit, for the events to be triggered...
            Thread.sleep(100)
        then:
            // We check the Deserialized Msg is the same as the original one:
            msgReceived.get().equals(msg)
    }
}
