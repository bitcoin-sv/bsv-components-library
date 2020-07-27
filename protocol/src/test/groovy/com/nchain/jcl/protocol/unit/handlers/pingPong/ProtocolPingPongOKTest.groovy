package com.nchain.jcl.protocol.unit.handlers.pingPong

import com.nchain.jcl.protocol.config.ProtocolConfig
import com.nchain.jcl.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.protocol.handlers.blacklist.BlacklistHandler
import com.nchain.jcl.protocol.handlers.discovery.DiscoveryHandler
import com.nchain.jcl.protocol.handlers.pingPong.PingPongHandlerConfig
import com.nchain.jcl.protocol.wrapper.P2P
import com.nchain.jcl.protocol.wrapper.P2PBuilder
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * Testing class for the "Happy" scenarios in the Ping/Pong P2P
 */
class ProtocolPingPongOKTest extends Specification {

    /**
     * We test that the Ping/Pong protocol is working fine. "Working fine" means that the Ping/Pong protocol is
     * actually triggered (PING and PON messages are exchanged) bewen 2 peers when they¡ve been inactive for some
     * period of time.
     */
    def "Testing Ping-Pong OK"() {
        given:
            // This is the inactivity period well use in this test. It will be fed into the configuration of both
            // the Serve and the client.
            Duration inactivityTimeout = Duration.ofMillis(500)

            // This is the time we are going to artificially WAIT, to make sure that the "inactivity" timeout is reached
            // ad the Ping/Pong protocol actually starts. NOTE: If this time is not Long enough, the Ping/Pong protocol
            // might not start. If it's too LONG, it might be triggered more than once.
            Duration waitingTime = Duration.ofMillis(inactivityTimeout.toMillis() * 5) // 3 times as much

            // Server Definition:
            ProtocolConfig serverConfig = new ProtocolBSVMainConfig().toBuilder()
                .port(0)
                .build()

            PingPongHandlerConfig serverPingConfig = serverConfig.getPingPongConfig()
                    .toBuilder()
                    .inactivityTimeout(inactivityTimeout.toMillis())
                    .build()

            // We disable the Handlers we dont need for this Test:
            P2P server = new P2PBuilder("server")
                    .config(serverConfig)
                    .config(serverPingConfig)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // Client Definition:
            ProtocolConfig clientConfig = new ProtocolBSVMainConfig().toBuilder()
                    .port(0)
                    .build()

            PingPongHandlerConfig clientPingConfig = clientConfig.getPingPongConfig()
                    .toBuilder()
                    .inactivityTimeout(inactivityTimeout.toMillis())
                    .build()
            // We disable the Handlers we dont need for this Test:
            P2P client = new P2PBuilder("client")
                    .config(clientConfig)
                    .config(clientPingConfig)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // We keep track of the PING and PONG Messages exchanged:
            AtomicInteger numPingReceivedByServer = new AtomicInteger(0)
            AtomicInteger numPongReceivedByServer = new AtomicInteger(0)

            AtomicInteger numPingReceivedByClient = new AtomicInteger(0)
            AtomicInteger numPongReceivedByClient = new AtomicInteger(0)

            server.EVENTS.MSGS.PING.forEach({ e -> numPingReceivedByServer.incrementAndGet()})
            server.EVENTS.MSGS.PONG.forEach({ e -> numPongReceivedByServer.incrementAndGet()})

            client.EVENTS.MSGS.PING.forEach({ e -> numPingReceivedByClient.incrementAndGet()})
            client.EVENTS.MSGS.PONG.forEach({ e -> numPongReceivedByClient.incrementAndGet()})

        when:

            // We start both and connect them:
            server.startServer()
            client.start()

            Thread.sleep(100)
            // We connect both together. This will trigger the HANDSHAKE protocol automatically.
            client.REQUESTS.PEERS.connect(server.getPeerAddress()).submit()
            Thread.sleep(waitingTime.toMillis())
            // At this moment, the Ping/Pong protocol must have been triggered at least once...
            server.stop()
            client.stop()

        then:
            // We check that the Ping/Pong protocol has been triggered at LEAST ONCE. In order to check it, we verify
            // that both Server and client hae exchange the respective PING and PON messages.
            numPingReceivedByServer.get() > 0
            numPongReceivedByServer.get() > 0
            numPingReceivedByClient.get() > 0
            numPongReceivedByClient.get() > 0
    }
}
