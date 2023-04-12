package io.bitcoinsv.bsvcl.net.protocol.handlers.handshake

import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigImpl
import io.bitcoinsv.bsvcl.net.protocol.events.control.PeerHandshakeRejectedEvent
import io.bitcoinsv.bsvcl.net.protocol.handlers.blacklist.BlacklistHandler
import io.bitcoinsv.bsvcl.net.protocol.handlers.discovery.DiscoveryHandler
import io.bitcoinsv.bsvcl.net.protocol.handlers.pingPong.PingPongHandler
import io.bitcoinsv.bsvcl.net.protocol.tools.MsgTest
import io.bitcoinsv.bsvcl.net.protocol.wrapper.P2P
import io.bitcoinsv.bsvcl.net.protocol.wrapper.P2PBuilder
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Testing class for the "Bad" Scenarios, where the Handshake is rejected for some reason
 */
class ProtocolHandshakeFailedTest extends Specification {

    /**
     * We test that the Handshake fails if the clients uses a wrong "version" number
     */
    def "Failed Handshaked-Wrong Version"() {
        given:

            // Server Definition:
            ProtocolConfig protocolConfig = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))

            // We disable all the Handlers we don't need for this Test:
            P2P server = new P2PBuilder("server")
                    .config(protocolConfig)
                    .useLocalhost()
                    .serverPort(0) // Random Port
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // Client Definition:
            // First we obtain the Basic BSV Configuration
            // Then we set the changes:
            // - a random Port ("0")
            // - a wrong protocolNumber.

            ProtocolConfig wrongConfig = ((ProtocolConfigImpl) protocolConfig).toBuilder()
                .port(0)
                .basicConfig(protocolConfig.getBasicConfig().toBuilder().protocolVersion(0).build())
                .build()

            // We disable all the Handlers we don't need for this Test:
            P2P client = new P2PBuilder("client")
                    .config(wrongConfig)
                    .useLocalhost()
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // we Define the Listener for the events and keep track of them:
            AtomicBoolean serverHandshaked = new AtomicBoolean()
            AtomicBoolean clientHandshaked = new AtomicBoolean()
            AtomicReference<PeerHandshakeRejectedEvent> clientRejectedEvent   = new AtomicReference<>()

            server.EVENTS.PEERS.HANDSHAKED.forEach({ e -> serverHandshaked.set(true)})
            server.EVENTS.PEERS.HANDSHAKED_REJECTED.forEach({ e -> clientRejectedEvent.set(e)})
            client.EVENTS.PEERS.HANDSHAKED.forEach({ e -> clientHandshaked.set(true)})

        when:
            server.startServer()
            client.start()
            Thread.sleep(100)
            client.REQUESTS.PEERS.connect(server.getPeerAddress()).submit()
            Thread.sleep(1000)
            server.stop()
            client.stop()
            println("CLIENT THREAD INFO:")
            println(client.getEventBus().getStatus())
            println("SERVER THREAD INFO:")
            println(server.getEventBus().getStatus())
        then:
            // We check that each there has been no handshake
            !serverHandshaked.get()
            !clientHandshaked.get()
            // we check that the Server has rejected the handshake proposed by the client, with the right reason
            clientRejectedEvent.get() != null
            clientRejectedEvent.get().reason == PeerHandshakeRejectedEvent.HandshakedRejectedReason.WRONG_VERSION
    }

    /**
     * We test that the HAndshake is rejected when a banned "user_agent" has been used
     */
    def "Failed Handshaked-Wrong UserAgent"() {
        given:

            // Server Definition:
            ProtocolConfig protocolConfig = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            // We disable all the Handlers we don't need for this Test:
            P2P server = new P2PBuilder("server")
                    .config(protocolConfig)
                    .useLocalhost()
                    .serverPort(0) // Random Port
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // Client Definition:
            // We change the "User Agent" used by the Client, to use an incorrect one (in the protocolBSVMainConfig
            // class, any user_agent containing "ABC" and some other patterns are blacklisted)

            HandshakeHandlerConfig handshakeConfig = protocolConfig.getHandshakeConfig().toBuilder()
                .userAgent("Bitcoin ABC")
                .build()
            // We disable all the Handlers we don't need for this Test:
            P2P client = new P2PBuilder("client")
                    .config(protocolConfig)
                    .config(handshakeConfig)
                    .useLocalhost()
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // we Define the Listener for the events and keep track of them:
            AtomicBoolean serverHandshaked = new AtomicBoolean()
            AtomicBoolean clientHandshaked = new AtomicBoolean()
            AtomicReference<PeerHandshakeRejectedEvent> clientRejectedEvent   = new AtomicReference<>()

            server.EVENTS.PEERS.HANDSHAKED.forEach({ e -> serverHandshaked.set(true)})
            server.EVENTS.PEERS.HANDSHAKED_REJECTED.forEach({ e -> clientRejectedEvent.set(e)})
            client.EVENTS.PEERS.HANDSHAKED.forEach({ e -> clientHandshaked.set(true)})

        when:
            server.startServer()
            client.start()
            Thread.sleep(100)
            client.REQUESTS.PEERS.connect(server.getPeerAddress()).submit()
            Thread.sleep(1000)
            server.stop()
            client.stop()

        then:
            // We check that each there has been no handshake
            !serverHandshaked.get()
            !clientHandshaked.get()
            // we check that the Server has rejected the handshake proposed by the client, with the right reason
            clientRejectedEvent.get() != null
            clientRejectedEvent.get().reason == PeerHandshakeRejectedEvent.HandshakedRejectedReason.WRONG_USER_AGENT
    }

    /**
     * We test that the Handshake is rejected when an extra message is sent (like an extra VersionAckMsg, in this
     * case). NOTE: In this particular scenario, the Handshake is properly stablished, and THEN it's rejected (when
     * the extra VersionAckMsg is sent).
     */
    def "Failed Handshaked-Duplicated ACK"() {
        given:
            // Server and Client Definition:
         ProtocolConfig protocolConfig = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            // We disable all the Handlers we don't need for this Test:
            P2P server = new P2PBuilder("server")
                    .config(protocolConfig)
                    .useLocalhost()
                    .serverPort(0) // Random Port
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()
            // We disable all the Handlers we don't need for this Test:
            P2P client = new P2PBuilder("client")
                    .config(protocolConfig)
                    .useLocalhost()
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // we Define the Listener for the events and keep track of them:
            AtomicBoolean serverHandshaked = new AtomicBoolean()
            AtomicBoolean clientHandshaked = new AtomicBoolean()
            AtomicReference<PeerHandshakeRejectedEvent> clientRejectedEvent   = new AtomicReference<>()
            server.EVENTS.PEERS.HANDSHAKED.forEach({ e -> serverHandshaked.set(true)})
            server.EVENTS.PEERS.HANDSHAKED_REJECTED.forEach({ e -> clientRejectedEvent.set(e)})
            client.EVENTS.PEERS.HANDSHAKED.forEach({ e -> clientHandshaked.set(true)})

        when:
            server.startServer()
            client.start()
            Thread.sleep(100)
            client.REQUESTS.PEERS.connect(server.getPeerAddress()).submit()
            Thread.sleep(1000)
            // At his moment, the handshake must have been stablished.
            // Now we send and additional VersionAck Msg, which will cause the handshake to be rejected
            client.REQUESTS.MSGS.send(server.getPeerAddress(), MsgTest.getVersionAckMsg()).submit()
            Thread.sleep(100)
            server.stop()
            client.stop()

        then:
            // We check that each on of them (Server and client) have received and triggered a Handshake)
            serverHandshaked.get()
            clientHandshaked.get()
            clientRejectedEvent.get() != null
            clientRejectedEvent.get().reason == PeerHandshakeRejectedEvent.HandshakedRejectedReason.PROTOCOL_MSG_DUPLICATE
    }
}
