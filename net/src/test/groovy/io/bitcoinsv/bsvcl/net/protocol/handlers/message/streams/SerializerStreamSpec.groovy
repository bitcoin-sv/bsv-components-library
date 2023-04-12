package io.bitcoinsv.bsvcl.net.protocol.handlers.message.streams

import io.bitcoinsv.bsvcl.net.network.PeerAddress
import io.bitcoinsv.bsvcl.net.network.streams.PeerOutputStream
import io.bitcoinsv.bsvcl.net.network.streams.StreamDataEvent
import io.bitcoinsv.bsvcl.net.network.streams.StreamState
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.bsvcl.net.protocol.handlers.message.streams.serializer.SerializerStream
import io.bitcoinsv.bsvcl.net.protocol.messages.HeaderMsg
import io.bitcoinsv.bsvcl.net.protocol.messages.NetAddressMsg
import io.bitcoinsv.bsvcl.net.protocol.messages.VarStrMsg
import io.bitcoinsv.bsvcl.net.protocol.messages.VersionMsg
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg
import io.bitcoinsv.bsvcl.net.network.streams.PeerStreamInOutSimulator
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext
import spock.lang.Ignore
import spock.lang.Specification

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Ignore
class SerializerStreamSpec extends Specification {

    public static long REF_BODY_TIMESTAMP = 1563391320
    public static final long REF_BODY_START_HEIGHT = 50
    public static final String REF_BODY_USER_AGENT = "/bitcoinj-sv:0.9.0/"
    public static final int REF_BODY_PORT = 8333
    public static final PeerAddress REF_BODY_ADDRESS = new PeerAddress(InetAddress.getByName("localhost"), REF_BODY_PORT)


    /** Definition of a Destination connected to a Peer */

    class PeerDestination extends PeerStreamInOutSimulator<ByteArrayReader> {
        private PeerAddress peerAddress;
        PeerDestination(ExecutorService executor,  PeerAddress peerAddress) {
            super(peerAddress, executor)
            this.peerAddress = peerAddress
        }

        StreamState getState() { return null} // Not used now...
        PeerAddress getPeerAddress() { return peerAddress}
        List<StreamDataEvent<ByteArrayReader>> transform(StreamDataEvent<ByteArrayReader> data) {
          return Arrays.asList(data)
        }
    }

    /**
     * We test that the transformation function works fine, and each message sent is serialized correctly into a ByteReader.
     *
     * NOTE: For the Stream to work property we need to provide an ExecutorService with just one Thread
     * (single Thread), otherwise we cannot guarantee that the results sent by the Output Stream are
     * coming out in the same order as we send them)
     */
    def "Testing Transformation Function with a Bitcoin Version Message"() {
        given:
            // general test config
            ExecutorService executor = Executors.newSingleThreadExecutor()
            VarStrMsg userAgentMsg = VarStrMsg.builder().str(REF_BODY_USER_AGENT).build();
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))

            // We create a VERSION Message:
            NetAddressMsg body_addr = NetAddressMsg.builder()
                    .address(REF_BODY_ADDRESS)
                    .build()
            VersionMsg versionMsg = VersionMsg.builder()
                    .version(config.getBasicConfig().protocolVersion)
                    .timestamp(REF_BODY_TIMESTAMP)
                    .user_agent(userAgentMsg)
                    .start_height(REF_BODY_START_HEIGHT)
                    .addr_from(body_addr)
                    .addr_recv(body_addr)
                    .relay(true)
                    .build()

            // We Build the whole Message:
            HeaderMsg header = HeaderMsg.builder()
                .command(versionMsg.getMessageType())
                .length((int) versionMsg.getLengthInBytes())
                .magic(config.getBasicConfig().getMagicPackage())
                .build();

            // Now we calculate its checksum and inject it back:
            long checksum = BitcoinMsgSerializerImpl.getInstance().calculateChecksum(config.getBasicConfig(), versionMsg);
            header = header.toBuilder().checksum(checksum).build()

            BitcoinMsg<VersionMsg> messageToSent = new BitcoinMsg<>(header, versionMsg);

            // This is the Message that will be received. It will be populated by a callback:
            BitcoinMsg<VersionMsg> messageReceived;

            // We send the message:
            // We crate our Destination,and some callbacks to deserialize our message
        DeserializerContext deserializerContext = DeserializerContext.builder()
                .protocolBasicConfig(config.getBasicConfig())
                .insideVersionMsg(true)
                .maxBytesToRead(messageToSent.getLengthInBytes())
                .build()
            PeerOutputStream<ByteArrayReader> destination = new PeerDestination(executor, REF_BODY_ADDRESS)
            destination.onData({ e -> messageReceived = BitcoinMsgSerializerImpl.getInstance().deserialize(deserializerContext, e.getData()) })

            // We create our Output Stream:
            PeerOutputStream<BitcoinMsg> myOutputStream = new SerializerStream(destination, config.getMessageConfig())

        when:
            myOutputStream.send(new StreamDataEvent<BitcoinMsg>(messageToSent))

            // We wait a little bit until all te data has passed through the InputStream:
            Thread.sleep(1000)

        then:
            messageToSent == messageReceived
    }


}
