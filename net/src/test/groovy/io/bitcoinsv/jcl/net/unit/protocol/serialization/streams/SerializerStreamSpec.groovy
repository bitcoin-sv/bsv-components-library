/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.unit.protocol.serialization.streams


import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter

import io.bitcoinsv.bitcoinjsv.core.Sha256Hash
import io.bitcoinsv.bitcoinjsv.core.Utils
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import io.bitcoinsv.jcl.net.network.PeerAddress
import io.bitcoinsv.jcl.net.network.streams.PeerOutputStream
import io.bitcoinsv.jcl.net.network.streams.StreamDataEvent
import io.bitcoinsv.jcl.net.network.streams.StreamState
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.jcl.net.protocol.messages.HeaderMsg
import io.bitcoinsv.jcl.net.protocol.messages.NetAddressMsg
import io.bitcoinsv.jcl.net.protocol.messages.VarStrMsg
import io.bitcoinsv.jcl.net.protocol.messages.VersionMsg
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg
import io.bitcoinsv.jcl.net.protocol.serialization.VersionMsgSerializer
import io.bitcoinsv.jcl.net.protocol.serialization.common.BitcoinMsgSerializerImpl
import io.bitcoinsv.jcl.net.protocol.serialization.common.DeserializerContext
import io.bitcoinsv.jcl.net.protocol.serialization.common.SerializerContext
import io.bitcoinsv.jcl.net.protocol.streams.serializer.SerializerStream
import io.bitcoinsv.jcl.net.unit.network.streams.PeerStreamInOutSimulator
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
            //general test config
            ExecutorService executor = Executors.newSingleThreadExecutor()
        VarStrMsg userAgentMsg = VarStrMsg.builder().str(REF_BODY_USER_AGENT).build();
        ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
        DeserializerContext deserializerContext = DeserializerContext.builder().protocolBasicConfig(config.getBasicConfig()).build()
        SerializerContext serializerContext = SerializerContext.builder().protocolBasicConfig(config.getBasicConfig()).build()

            // generate a serialized version message
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

            // we need to serialize the message so we can calculate the checksum
            ByteArrayWriter versionByteWriter = new ByteArrayWriter();
        VersionMsgSerializer.getInstance().serialize(serializerContext, versionMsg, versionByteWriter);
            byte[] bodyBytes = versionByteWriter.reader().getFullContentAndClose()
            long checksum = Utils.readUint32(Sha256Hash.hashTwice(bodyBytes), 0);

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

        PeerOutputStream<ByteArrayReader> destination = new PeerDestination(executor, REF_BODY_ADDRESS)
            destination.onData({ e -> receivedMessage = BitcoinMsgSerializerImpl.getInstance().deserialize(deserializerContext, e.getData(), VersionMsg.MESSAGE_TYPE) })

            // We create our Output Stream:
            PeerOutputStream<BitcoinMsg> myOutputStream = new SerializerStream(executor, destination, config.getBasicConfig())

        when:
            myOutputStream.send(new StreamDataEvent<BitcoinMsg>(sentMessage))

            // We wait a little bit until all te data has passed through the InputStream:
            Thread.sleep(1000)

        then:
            sentMessage == receivedMessage
    }


}
