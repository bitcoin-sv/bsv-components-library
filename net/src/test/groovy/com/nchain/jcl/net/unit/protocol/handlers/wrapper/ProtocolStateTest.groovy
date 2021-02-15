package com.nchain.jcl.net.unit.protocol.handlers.wrapper

import com.nchain.jcl.net.network.handlers.NetworkHandler
import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfigBuilder
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.net.protocol.handlers.blacklist.BlacklistHandler
import com.nchain.jcl.net.protocol.handlers.discovery.DiscoveryHandler
import com.nchain.jcl.net.protocol.handlers.message.MessageHandler
import com.nchain.jcl.net.protocol.handlers.pingPong.PingPongHandler
import com.nchain.jcl.net.protocol.wrapper.P2P
import com.nchain.jcl.net.protocol.wrapper.P2PBuilder
import io.bitcoinj.params.MainNetParams
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
    def "Testing Client/Server Msgs exchange"() {
        given:
            // We disable the Handlers we dont need for this Test:
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams())
            P2P server = new P2PBuilder("server")
                    .config(config)
                    .serverPort(0) // Random Port
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .publishState(NetworkHandler.HANDLER_ID, Duration.ofMillis(500))
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

            server.stop()
            client.stop()

            Thread.sleep(1000)
        then:
            statesNetwork.get() > 0
            statesMessages.get() > 0
            statesMessages.get() > statesNetwork.get()
    }
}
