package com.nchain.jcl.protocol.unit.serialization.streams

import com.nchain.jcl.network.PeerAddress
import com.nchain.jcl.network.streams.PeerStreamInfo
import com.nchain.jcl.protocol.config.ProtocolConfig
import com.nchain.jcl.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.protocol.messages.HeaderMsg
import com.nchain.jcl.protocol.messages.NetAddressMsg
import com.nchain.jcl.protocol.messages.VarStrMsg
import com.nchain.jcl.protocol.messages.VersionMsg
import com.nchain.jcl.protocol.messages.common.BitcoinMsg
import com.nchain.jcl.protocol.serialization.VersionMsgSerializer
import com.nchain.jcl.protocol.serialization.common.BitcoinMsgSerializerImpl
import com.nchain.jcl.protocol.serialization.common.DeserializerContext
import com.nchain.jcl.protocol.serialization.common.SerializerContext
import com.nchain.jcl.protocol.streams.SerializerStream
import com.nchain.jcl.tools.bytes.ByteArrayReader
import com.nchain.jcl.tools.bytes.ByteArrayWriter
import com.nchain.jcl.tools.bytes.ByteTools
import com.nchain.jcl.tools.crypto.Sha256
import com.nchain.jcl.tools.streams.*
import spock.lang.Specification

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SerializerStreamSpec extends Specification {

    public static long REF_BODY_TIMESTAMP = 1563391320
    public static final long REF_BODY_START_HEIGHT = 50
    public static final String REF_BODY_USER_AGENT = "/bitcoinj-sv:0.9.0/"
    public static final int REF_BODY_PORT = 8333
    public static final PeerAddress REF_BODY_ADDRESS = new PeerAddress(InetAddress.getByName("localhost"), REF_BODY_PORT)


    /** Definition of a Destination connected to a Peer */

    class PeerDestination extends OutputStreamDestinationImpl<ByteArrayReader> implements PeerStreamInfo {
        private PeerAddress peerAddress;
        PeerDestination(ExecutorService executor,  PeerAddress peerAddress) {
            super(executor)
            this.peerAddress = peerAddress
        }
        StreamState getState() { return null} // Not used now...
        PeerAddress getPeerAddress() { return peerAddress}
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
            //general test config
            ExecutorService executor = Executors.newSingleThreadExecutor()
            VarStrMsg userAgentMsg = VarStrMsg.builder().str(REF_BODY_USER_AGENT).build();
            ProtocolConfig config = new ProtocolBSVMainConfig()
            DeserializerContext deserializerContext = DeserializerContext.builder().protocolBasicConfig(config.getBasicConfig()).build()
            SerializerContext serializerContext = SerializerContext.builder().protocolBasicConfig(config.getBasicConfig()).build()

            // generate a serialized version message
            NetAddressMsg body_addr = NetAddressMsg.builder()
                    .address(REF_BODY_ADDRESS)
                    .timestamp(0)
                    .build();
            VersionMsg versionMsg = VersionMsg.builder()
                    .version(config.getBasicConfig().protocolVersion)
                    .timestamp(REF_BODY_TIMESTAMP)
                    .user_agent(userAgentMsg)
                    .start_height(REF_BODY_START_HEIGHT)
                    .addr_from(body_addr)
                    .addr_recv(body_addr)
                    .relay(true)
                    .build()

            // we need to serialize the message so we can calculate the checksum
            ByteArrayWriter versionByteWriter = new ByteArrayWriter();
            VersionMsgSerializer.getInstance().serialize(serializerContext, versionMsg, versionByteWriter);
            byte[] bodyBytes = versionByteWriter.reader().getFullContentAndClose()
            long checksum = ByteTools.readUint32(Sha256.hashTwice(bodyBytes));

            //generate the bitcoins header message with the calculated checksum
            HeaderMsg header = HeaderMsg.builder()
                    .command(versionMsg.getMessageType())
                    .length((int) versionMsg.getLengthInBytes())
                    .magic(config.getBasicConfig().getMagicPackage())
                    .checksum(checksum)
                    .build();

            //build the bitcoin message from the constructed header and version message
            BitcoinMsg<VersionMsg> sentMessage = new BitcoinMsg<>(header, versionMsg);
            //the received message will be stored here for comparison
            BitcoinMsg<VersionMsg> receivedMessage;


            // We crate our Destination,and some callbacks to deserialize our message

            OutputStreamDestination<ByteArrayReader> destination = new PeerDestination(executor, REF_BODY_ADDRESS)
            destination.onData({ e -> receivedMessage = BitcoinMsgSerializerImpl.getInstance().deserialize(deserializerContext, e.getData(), VersionMsg.MESSAGE_TYPE) })

            // We create our Output Stream:
            OutputStream<ByteArrayReader> myOutputStream = new SerializerStream(executor, destination, config.getBasicConfig())

        when:
            myOutputStream.send(new StreamDataEvent<BitcoinMsg>(sentMessage))

            // We wait a little bit until all te data has passed through the InputStream:
            Thread.sleep(1000)

        then:
            sentMessage == receivedMessage
    }


}
