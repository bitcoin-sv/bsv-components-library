package io.bitcoinsv.bsvcl.net.protocol.handlers.pingPong

import io.bitcoinsv.bsvcl.net.network.config.provided.NetworkDefaultConfig
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.bsvcl.net.protocol.handlers.blacklist.BlacklistHandler
import io.bitcoinsv.bsvcl.net.protocol.handlers.discovery.DiscoveryHandler
import io.bitcoinsv.bsvcl.net.P2P
import io.bitcoinsv.bsvcl.net.P2PBuilder
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * Testing class for the "Happy" scenarios in the Ping/Pong P2P
 */
class ProtocolPingPongOKTest extends Specification {

    /**
     * We test that the Ping/Pong protocol is working fine. "Working fine" means that the Ping/Pong protocol is
     * actually triggered (PING and PONG messages are exchanged) between 2 peers when they¡ve been inactive for some
     * period of time.
     */
    def "Testing Ping-Pong OK"() {
        given:
            // This is the inactivity period well use in this test. It will be fed into the configuration of both
            // the Server and the client.
            Duration inactivityTimeout = Duration.ofMillis(500)

            // This is the time we are going to artificially WAIT, to make sure that the "inactivity" timeout is reached
            // ad the Ping/Pong protocol actually starts. NOTE: If this time is not Long enough, the Ping/Pong protocol
            // might not start. If it's too LONG, it might be triggered more than once.
            Duration waitingTime = Duration.ofMillis(inactivityTimeout.toMillis() * 3) // 3 times as much

            // Server Definition:
            ProtocolConfig serverConfig = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET)).toBuilder().port(0).build()

            PingPongHandlerConfig serverPingConfig = serverConfig.getPingPongConfig()
                    .toBuilder()
                    .inactivityTimeout(inactivityTimeout)
                    .build()

            // We disable the Handlers we dont need for this Test:
            P2P server = new P2PBuilder("server")
                    .config(serverConfig)
                    .config(serverPingConfig)
                    .useLocalhost()
                    .config(new NetworkDefaultConfig().toBuilder().listeningPort(0).build())
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // Client Definition:
            ProtocolConfig clientConfig = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET)).toBuilder().port(0).build()

            PingPongHandlerConfig clientPingConfig = clientConfig.getPingPongConfig()
                    .toBuilder()
                    .inactivityTimeout(inactivityTimeout)
                    .build()
            // We disable the Handlers we dont need for this Test:
            P2P client = new P2PBuilder("client")
                    .config(clientConfig)
                    .config(clientPingConfig)
                    .useLocalhost()
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
            server.awaitStarted()
            client.awaitStarted()

            // We connect both together. This will trigger the HANDSHAKE protocol automatically.
            client.REQUESTS.PEERS.connect(server.getPeerAddress()).submit()
            println("We wait for " + waitingTime.toMillis() + " milliseconds...")
            Thread.sleep(waitingTime.toMillis())
            println("Waiting finished. Pings should have been triggered by now...")
            // At this moment, the Ping/Pong protocol must have been triggered at least once...
            server.stop()
            client.stop()
            server.awaitStopped()
            client.awaitStopped()
        then:
            // NOTE: The PING/PONG Starts after a Period of inactivity. So in this case, if both Server and Client are inactive
            // after some time, the ping/pong is triggered. BUT... The Ping/Pong will always start first in one of them, say
            // for example in the Server: the server then sends a PING to the Client. Meanwhile, the Client was about to start
            // the Ping/pong on its own, BUT when the PING from the Server arrived, this would RESET the counter in the client
            // side, so the "server" is not inactive anymore from the client standpoint, so the Ping/Pong TRIGGERED FROM THE
            // CLIENT SIDE doesn't happen in the end.
            // So in this tests, most of the time only one of the Ping/Pong processes will be triggered, and only in certain
            // HW the two of them will take place


            // We check that at least one of the Ping/pong process from any of the sides is performed:
            ((numPingReceivedByServer.get() > 0 && numPongReceivedByClient.get() > 0)
                    || (numPingReceivedByClient.get() > 0 && numPongReceivedByServer.get() > 0))

    }
}
