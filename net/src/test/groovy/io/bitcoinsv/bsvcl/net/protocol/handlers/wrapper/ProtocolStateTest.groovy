package io.bitcoinsv.bsvcl.net.protocol.handlers.wrapper

import io.bitcoinsv.bsvcl.net.P2PConfig
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.bsvcl.net.protocol.handlers.blacklist.BlacklistHandler
import io.bitcoinsv.bsvcl.net.protocol.handlers.discovery.DiscoveryHandler
import io.bitcoinsv.bsvcl.net.protocol.handlers.message.MessageHandler
import io.bitcoinsv.bsvcl.net.protocol.handlers.pingPong.PingPongHandler
import io.bitcoinsv.bsvcl.net.P2P
import io.bitcoinsv.bsvcl.net.P2PBuilder
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import spock.lang.Ignore
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * Testing class using the P2P "wrapper", that includes all the network and protocol handlers within.
 */
class ProtocolStateTest extends Specification {

    /**
     * We est that the Events related to the P2P Handler States are triggered properly.
     * In this test we are configuring several states to be published at different frequencies. We verify that
     * any state has been published and that the number of notifications is different, since the frequencies
     * are also different.
     */
    @Ignore("state publication from network controller not controllable at the moment - todo")
    def "Testing Client/Server Msgs exchange"() {
        given:
            // We disable the Handlers we dont need for this Test:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams())
            P2P server = new P2PBuilder("server")
                    .config(config)
                    .config(P2PConfig.builder().listeningPort(0).build())
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
//                    .publishState(NetworkHandler.HANDLER_ID, Duration.ofMillis(500))
                    .publishState(MessageHandler.HANDLER_ID, Duration.ofMillis(100))
                    .build()
            // We disable the Handlers we dont need for this Test:
            P2P client = new P2PBuilder("client")
                    .config(config)
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // We keep track of the States being published:
            AtomicInteger statesNetwork = new AtomicInteger()
            AtomicInteger statesMessages = new AtomicInteger()
            server.EVENTS.STATE.NETWORK.forEach({ e -> statesNetwork.incrementAndGet()})
            server.EVENTS.STATE.MESSAGES.forEach({ e -> statesMessages.incrementAndGet()})

        when:
            server.startServer()
            client.start()

            Thread.sleep(1000)
            client.REQUESTS.PEERS.connect(server.getPeerAddress()).submit()

            Thread.sleep(1000)
            client.REQUESTS.PEERS.disconnect(server.getPeerAddress()).submit()

            Thread.sleep(1000)

            server.initiateStop()
            client.initiateStop()

            Thread.sleep(1000)
        then:
            statesNetwork.get() > 0
            statesMessages.get() > 0
            statesMessages.get() > statesNetwork.get()
    }
}
