package io.bitcoinsv.bsvcl.net.protocol.handlers.pingPong

import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.bsvcl.net.protocol.handlers.blacklist.BlacklistHandler
import io.bitcoinsv.bsvcl.net.protocol.handlers.discovery.DiscoveryHandler
import io.bitcoinsv.bsvcl.net.protocol.handlers.pingPong.PingPongHandler
import io.bitcoinsv.bsvcl.net.protocol.handlers.pingPong.PingPongHandlerConfig
import io.bitcoinsv.bsvcl.net.protocol.wrapper.P2P
import io.bitcoinsv.bsvcl.net.protocol.wrapper.P2PBuilder
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
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
            // the server and the client.
            // After this inactivity time, the Ping/Pong protocol will start...
            Duration inactivityTimeout = Duration.ofMillis(100)

            // If a Peer does not reply to a Ping message after this response Time, the Ping/Pong will fail:
            Duration responseTimeout = Duration.ofMillis(1000)

            // Server Definition:
            ProtocolConfig serverConfig = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))

            PingPongHandlerConfig serverPingConfig = serverConfig.getPingPongConfig()
                .toBuilder()
                .inactivityTimeout(inactivityTimeout)
                .responseTimeout(responseTimeout)
                .build()

            // We disable the Handlers we dont need for this Test:
            P2P server = new P2PBuilder("server")
                    .config(serverConfig)
                    .config(serverPingConfig)
                    .useLocalhost()
                    .serverPort(0) // Random Port
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // Client Definition:
            // NOTE: We are going to REMOVE the Ping/Pong Handler from the Client, so this client will NOT reply to
            // PING Messages and therefore won't implement the Ping/Pong P2P
            // We disable the Handlers we dont need for this Test:

            ProtocolConfig clientConfig = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            P2P client = new P2PBuilder("client")
                    .config(clientConfig)
                    .useLocalhost()
                    .serverPort(0) // Random Port
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
            CountDownLatch disconnectLatch = new CountDownLatch(1)

            server.EVENTS.MSGS.PING.forEach({ e -> numPingReceivedByServer.incrementAndGet()})
            server.EVENTS.MSGS.PONG.forEach({ e -> numPongReceivedByServer.incrementAndGet()})
            server.EVENTS.PEERS.PINGPONG_FAILED.forEach({ e -> pingPongFailed.set(true)})
            server.EVENTS.PEERS.DISCONNECTED.forEach({ e -> disconnectLatch.countDown()})

            client.EVENTS.MSGS.PING.forEach({ e -> numPingReceivedByClient.incrementAndGet()})
            client.EVENTS.MSGS.PONG.forEach({ e -> numPongReceivedByClient.incrementAndGet()})
            client.EVENTS.PEERS.DISCONNECTED.forEach({ e -> disconnectLatch.countDown()})

        when:
            // We start both and connect them:
            server.startServer()
            client.start()
            server.awaitStarted(10, TimeUnit.SECONDS)
            client.awaitStarted(10, TimeUnit.SECONDS)

            println(" >>> CONNECTING TO THE SERVER...")
            // We connect both together. This will trigger the HANDSHAKE protocol automatically.
            client.REQUESTS.PEERS.connect(server.getPeerAddress()).submit()

            // The Ping/Pong protocol will be triggered at least once by the server, but the client won't reply to it,
            // so the server will detect that the timeout has expired and therefore disconnect from this client.
            var disconnected = disconnectLatch.await(10, TimeUnit.SECONDS)

            println(" >>> STOPPING...")
            server.stop()
            client.stop()
            server.awaitStopped()
            client.awaitStopped()

        then:
            // We check that the Ping/Pong protocol has been triggered at LEAST ONCE by the Server.
            disconnected
            numPingReceivedByServer.get() == 0
            numPongReceivedByServer.get() == 0
            numPingReceivedByClient.get() > 0
            numPongReceivedByClient.get() == 0
            pingPongFailed.get()
    }
}
