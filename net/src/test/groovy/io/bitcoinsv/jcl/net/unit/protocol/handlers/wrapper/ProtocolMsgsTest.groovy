package io.bitcoinsv.jcl.net.unit.protocol.handlers.wrapper

import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.jcl.net.protocol.handlers.blacklist.BlacklistHandler
import io.bitcoinsv.jcl.net.protocol.handlers.discovery.DiscoveryHandler
import io.bitcoinsv.jcl.net.protocol.handlers.handshake.HandshakeHandler
import io.bitcoinsv.jcl.net.protocol.handlers.pingPong.PingPongHandler
import io.bitcoinsv.jcl.net.protocol.messages.AddrMsg
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg
import io.bitcoinsv.jcl.net.unit.protocol.tools.MsgTest
import io.bitcoinsv.jcl.net.protocol.wrapper.P2P
import io.bitcoinsv.jcl.net.protocol.wrapper.P2PBuilder
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger

/**
 * Testing class using the P2P "wrapper", that includes all the network and protocol handlers within.
 */
class ProtocolMsgsTest extends Specification {

    /**
     * We test that 2 different P2P Handlers can connect and exchange a message, and the events are triggered properly
     */
    def "Testing Client/Server Msgs exchange"() {
        given:
            // Server and client configuration:

            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams())

                // We disable all the Handlers we don't need for this Test:
            P2P server = new P2PBuilder("server")
                    .config(config)
                    .useLocalhost()
                    .serverPort(0) // Random Port
                    //.excludeHandler(HandshakeHandler.HANDLER_ID)
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()
            P2P client = new P2PBuilder("client")
                    .config(config)
                    .useLocalhost()
                    //.excludeHandler(HandshakeHandler.HANDLER_ID)
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // we listen to the Connect/Disconnect Events:
            int NUM_MSGS = 3
            AtomicInteger numConnections = new AtomicInteger()
            AtomicInteger numDisconnections = new AtomicInteger()
            AtomicInteger numMsgs = new AtomicInteger()

            server.EVENTS.PEERS.CONNECTED.forEach({ e ->
                println("> Server :: peer connected: " + e.peerAddress)
            })
            client.EVENTS.PEERS.CONNECTED.forEach({ e ->
                println("> Client :: peer connected: " + e.peerAddress)
            })
            server.EVENTS.PEERS.HANDSHAKED.forEach({ e ->
                numConnections.incrementAndGet()
                println("> Server :: peer handshaked: " + e.peerAddress)
            })
            client.EVENTS.PEERS.HANDSHAKED.forEach({ e ->
                numConnections.incrementAndGet()
                println("> Client :: peer handshaked: " + e.peerAddress)
            })
            server.EVENTS.PEERS.DISCONNECTED.forEach({ e ->
                numDisconnections.incrementAndGet()
                println("> Server :: peer disconnected: " + e.peerAddress)
            })
            client.EVENTS.PEERS.DISCONNECTED.forEach({ e ->
                numDisconnections.incrementAndGet()
                println("> Client :: peer disconnected: " + e.peerAddress)
            })
            server.EVENTS.MSGS.ADDR.forEach({ e ->
                numMsgs.incrementAndGet()
            })

        when:
            server.startServer()
            client.start()

            Thread.sleep(100)
            client.REQUESTS.PEERS.connect(server.getPeerAddress()).submit()

            // We wait until the Handshake is done
            while (numConnections.get() < 2) {
                Thread.sleep(10)
            }

            // We send a few messages from the Client to the Server:
            BitcoinMsg<AddrMsg> msg = MsgTest.getAddrMsg();
            for (int i = 0; i < NUM_MSGS; i++) {
                println(" >> SENDING ADDR MSG...")
                client.REQUESTS.MSGS.send(server.getPeerAddress(), msg).submit()
            }

            // NOTE: Here, we wait a little bit before we close the connection If we don't wait enough, the Socket
            // between the Client and Server will be closed before the message can be serialized/Deserialized and
            // travel between them
            Thread.sleep(100)
            println(" >>> DISCONNECTING FROM THE SERVER...")
            client.REQUESTS.PEERS.disconnect(server.getPeerAddress()).submit()

            Thread.sleep(100)
            println(" >>> STOPPING...")
            server.stop()
            client.stop()

        then:
            // We check that the Events have been triggered right:
            numConnections.get() == 2
            numDisconnections.get() >= 2
            numMsgs.get() == NUM_MSGS
    }
}
