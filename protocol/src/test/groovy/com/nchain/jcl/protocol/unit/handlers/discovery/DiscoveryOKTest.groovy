package com.nchain.jcl.protocol.unit.handlers.discovery

import com.nchain.jcl.protocol.config.ProtocolConfig
import com.nchain.jcl.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.protocol.events.InitialPeersLoadedEvent
import com.nchain.jcl.protocol.handlers.blacklist.BlacklistHandler
import com.nchain.jcl.protocol.handlers.discovery.DiscoveryHandlerConfig
import com.nchain.jcl.protocol.wrapper.P2P
import com.nchain.jcl.protocol.wrapper.P2PBuilder
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicReference

/**
 * Testing Class for the Discovery Handler (Happy path/scenarios)
 */
class DiscoveryOKTest extends Specification {

    /**
     * We check that on startup, the Discovery handler successfully loads the initial set of Peers from the CSV
     * fie, located in the working folder.
     * NOTE: This tests assumes that there is a file named "BSV[mainNet]-discovery-handler-seed.csv" in the
     * test/resources/jcl folder.
     */
    def "Testing Initial Peers loaded from CSV"() {
        given:
            // We set up the Configuration to use the Peers hardcoded in a CSV file:
            ProtocolConfig config = new ProtocolBSVMainConfig()

            DiscoveryHandlerConfig discoveryConfig = config.getDiscoveryConfig()
                    .toBuilder()
                    .discoveryMethod(DiscoveryHandlerConfig.DiscoveryMethod.PEERS)
                    .build()

        P2P server = new P2PBuilder("testing")
                    .config(config)
                    .config(discoveryConfig)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // We store the Event triggered when the initial Peers are loaded:
            AtomicReference<InitialPeersLoadedEvent> event = new AtomicReference<>()
            server.EVENTS.PEERS.INITIAL_PEERS_LOADED.forEach({ e -> event.set(e)})

        when:
            server.startServer()
            Thread.sleep(500)
            server.stop()
        then:
            event.get() != null
            event.get().numPeersLoaded > 0

    }

    /**
     * We check that on startup, the Discovery Handler successfully loads the initial set of Peers from a
     */
    def "Testing Initial Peers loaded from DNS"() {
        given:
            // We set up the Configuration to use the Peers obtained by looking up oin the hardcoded DNS
            ProtocolConfig config = new ProtocolBSVMainConfig()
            DiscoveryHandlerConfig discoveryConfig = config.getDiscoveryConfig().toBuilder()
                .discoveryMethod(DiscoveryHandlerConfig.DiscoveryMethod.DNS)
                .build()
            P2P server = new P2PBuilder("testing")
                    .config(config)
                    .config(discoveryConfig)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // We store the Event triggered when the initial Peers are loaded:
            AtomicReference<InitialPeersLoadedEvent> event = new AtomicReference<>()
            server.EVENTS.PEERS.INITIAL_PEERS_LOADED.forEach({ e -> event.set(e)})

        when:
            server.startServer()
            Thread.sleep(1000)
            server.stop()
        then:
            event.get() != null
            event.get().numPeersLoaded > 0
    }
}
