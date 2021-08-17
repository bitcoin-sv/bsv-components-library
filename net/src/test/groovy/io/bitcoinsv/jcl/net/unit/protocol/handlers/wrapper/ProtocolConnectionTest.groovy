package io.bitcoinsv.jcl.net.unit.protocol.handlers.wrapper


import io.bitcoinj.params.MainNetParams
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.jcl.net.protocol.handlers.blacklist.BlacklistHandler
import io.bitcoinsv.jcl.net.protocol.handlers.discovery.DiscoveryHandler
import io.bitcoinsv.jcl.net.protocol.handlers.handshake.HandshakeHandler
import io.bitcoinsv.jcl.net.protocol.handlers.pingPong.PingPongHandler
import io.bitcoinsv.jcl.net.protocol.wrapper.P2P
import io.bitcoinsv.jcl.net.protocol.wrapper.P2PBuilder
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger

/**
 * Testing class using the P2P "wrapper", that includes all the network and protocol handlers within.
 */
class ProtocolConnectionTest extends Specification {

    /**
     * We test that 2 protocolHandlers can connect to each other, and the Events are triggered properly
     */
    def "Testing Client/Server communication"() {
        given:
            // Server Definition:
        ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams())
            // We disable all the Handlers we don't need for this Test:
        P2P server = new P2PBuilder("server")
                    .config(config)
                    .serverPort(0) // Random Port
                    .excludeHandler(HandshakeHandler.HANDLER_ID)
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()
            // Client definition:
            // We disable all the Handlers we don't need for this Test:
            P2P client = new P2PBuilder("client")
                    .config(config)
                    .excludeHandler(HandshakeHandler.HANDLER_ID)
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // we listen to the Connect/Disconnect Events:
            AtomicInteger numConnections = new AtomicInteger()
            AtomicInteger numDisconnections = new AtomicInteger()

            server.EVENTS.PEERS.CONNECTED.forEach({ e -> numConnections.incrementAndGet()})
            client.EVENTS.PEERS.CONNECTED.forEach({ e -> numConnections.incrementAndGet()})
            server.EVENTS.PEERS.DISCONNECTED.forEach({ e -> numDisconnections.incrementAndGet()})
            client.EVENTS.PEERS.DISCONNECTED.forEach({ e -> numDisconnections.incrementAndGet()})


        when:
            server.startServer()
            client.start()

            Thread.sleep(100)
            client.REQUESTS.PEERS.connect(server.getPeerAddress()).submit()

            Thread.sleep(100)
            client.REQUESTS.PEERS.disconnect(server.getPeerAddress()).submit()

            Thread.sleep(100)

            server.stop()
            client.stop()

        then:
            // We check that the Events hae been triggered right:
            numConnections.get() == 2
            numDisconnections.get() == 2
    }
}
