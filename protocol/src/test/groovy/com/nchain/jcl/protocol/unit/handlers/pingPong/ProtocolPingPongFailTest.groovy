package com.nchain.jcl.protocol.unit.handlers.pingPong

import com.nchain.jcl.protocol.config.ProtocolConfig
import com.nchain.jcl.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.protocol.handlers.blacklist.BlacklistHandler
import com.nchain.jcl.protocol.handlers.discovery.DiscoveryHandler
import com.nchain.jcl.protocol.handlers.pingPong.PingPongHandler
import com.nchain.jcl.protocol.handlers.pingPong.PingPongHandlerConfig
import com.nchain.jcl.protocol.wrapper.P2P
import com.nchain.jcl.protocol.wrapper.P2PBuilder
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Testing class for the "Bad" scenarios in the Ping/Pong P2P
 */
class ProtocolPingPongFailTest extends Specification {

    /**
     * We test that a Peer is disconnected if it fails to fullfill the Ping/Pong P2P
     */
    def "Testing Ping-Pong Fail"() {
        given:
            // This is the inactivity period well use in this test. It will be fed into the configuration of both
            // the Serve and the client.
            // After this inactivity time, the Ping/Pong protocol will start...
            Duration inactivityTimeout = Duration.ofMillis(100)

            // If a Peer does not reply to a Ping message after this response Time, the Ping/Pong will fail:
            Duration responseTimeout = Duration.ofMillis(1000)

            // This is the time we are going to artificially WAIT, to make sure that the "inactivity" timeout is reached
            // ad the Ping/Pong protocol actually starts. NOTE: If this time is not Long enough, the Ping/Pong protocol
            // might not start. If it's too LONG, it might be triggered more than once.
            Duration waitingTime = Duration.ofMillis(responseTimeout.toMillis() * 5) // 3 times as much

            // Server Definition:
            ProtocolConfig serverConfig = new ProtocolBSVMainConfig()

            PingPongHandlerConfig serverPingConfig = serverConfig.getPingPongConfig()
                .toBuilder()
                .inactivityTimeout(inactivityTimeout.toMillis())
                .responseTimeout(responseTimeout.toMillis())
                .build()

            // We disable the Handlers we dont need for this Test:
            P2P server = new P2PBuilder("server")
                    .config(serverConfig)
                    .config(serverPingConfig)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // Client Definition:
            // NOTE: We are going to REMOVE the Ping/Pong Handler from the Client, so this client will NOT reply to
            // PING Messages and therefore won't implement the Ping/Pong P2P
            // We disable the Handlers we dont need for this Test:
            P2P client = new P2PBuilder("client")
                    .randomPort()
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // We keep track of the PING and PONG Messages exchanged:
            AtomicInteger numPingReceivedByServer = new AtomicInteger(0)
            AtomicInteger numPongReceivedByServer = new AtomicInteger(0)
            AtomicBoolean pingPongFailed = new AtomicBoolean()

            AtomicInteger numPingReceivedByClient = new AtomicInteger(0)
            AtomicInteger numPongReceivedByClient = new AtomicInteger(0)

            server.EVENTS.MSGS.PING.forEach({ e -> numPingReceivedByServer.incrementAndGet()})
            server.EVENTS.MSGS.PONG.forEach({ e -> numPongReceivedByServer.incrementAndGet()})
            server.EVENTS.PEERS.PINGPONG_FAILED.forEach({ e -> pingPongFailed.set(true)})

            client.EVENTS.MSGS.PING.forEach({ e -> numPingReceivedByClient.incrementAndGet()})
            client.EVENTS.MSGS.PONG.forEach({ e -> numPongReceivedByClient.incrementAndGet()})

        when:
            // We start both and connect them:
            server.startServer()
            client.start()

            Thread.sleep(100)
            println(" >>> CONNECTING TO THE SERVER...")
            // We connect both together. This will trigger the HANDSHAKE protocol automatically.
            client.REQUESTS.PEERS.connect(server.getPeerAddress()).submit()
            println(" >>> WAITING UNTIL PING/PONG TIMEOUT...")
            Thread.sleep(waitingTime.toMillis())
            // At this moment, the Ping/Pong protocol must have been triggered at least once by the Server, but the
            // Client didn't replay to it, so the Server must have detected that the timeout has expired and therefore
            // requested a disconnection from this Client.
            Thread.sleep(5000)
            println(" >>> STOPPING...")
            server.stop()
            client.stop()

        then:
            // We check that the Ping/Pong protocol has been triggered at LEAST ONCE by the Server.
            numPingReceivedByServer.get() == 0
            numPongReceivedByServer.get() == 0
            numPingReceivedByClient.get() > 0
            numPongReceivedByClient.get() == 0
            pingPongFailed.get()
    }
}
