package io.bitcoinsv.bsvcl.net.protocol.handlers.discovery

import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import io.bitcoinsv.bsvcl.net.P2PConfig
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.bsvcl.net.protocol.events.control.InitialPeersLoadedEvent
import io.bitcoinsv.bsvcl.net.protocol.handlers.blacklist.BlacklistHandler
import io.bitcoinsv.bsvcl.net.P2P
import io.bitcoinsv.bsvcl.net.P2PBuilder
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicReference

/**
 * Testing Class for the Discovery Handler (Happy path/scenarios)
 */
class DiscoverySeedTest extends Specification {
    /**
     * We check that on startup, the Discovery Handler successfully loads the initial set of Peers from th DNS List
     */
    def "Testing Initial Peers loaded from DNS"() {
        given:
            // We set up the Configuration to use the Peers obtained by looking up the hardcoded DNS
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))

            DiscoveryHandlerConfig discoveryConfig = config.getDiscoveryConfig().toBuilder()
                .discoveryMethod(DiscoveryHandlerConfig.DiscoveryMethod.DNS)
                .build()
            P2P server = new P2PBuilder("testing")
                    .config(config)
                    .config(discoveryConfig)
                    .config(P2PConfig.builder().listeningPort(0).listening(true).build())
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // We store the Event triggered when the initial Peers are loaded:
            AtomicReference<InitialPeersLoadedEvent> event = new AtomicReference<>()
            server.EVENTS.PEERS.INITIAL_PEERS_LOADED.forEach({ e ->
                println(" >>> EVENT DETECTED: Initial Set of Peers loaded " + e.toString())
                event.set(e)
            })

        when:
            try {
                println("Starting P2P ...")
                server.start()
                Thread.sleep(30000) // Raise this timeout if the DNS are poor and take long to find peers
                println("P2P Stopping...")
                server.initiateStop()
                server.join()
                println("P2P Stopped.")
            } catch (Throwable e) {
                e.printStackTrace()
            }
        then:
            event.get() != null
            event.get().numPeersLoaded > 0
    }
}
