package io.bitcoinsv.bsvcl.net.unit.protocol.handlers.discovery

import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.bsvcl.net.protocol.events.control.InitialPeersLoadedEvent
import io.bitcoinsv.bsvcl.net.protocol.handlers.blacklist.BlacklistHandler
import io.bitcoinsv.bsvcl.net.protocol.handlers.discovery.DiscoveryHandlerConfig
import io.bitcoinsv.bsvcl.net.protocol.wrapper.P2P
import io.bitcoinsv.bsvcl.net.protocol.wrapper.P2PBuilder
import io.bitcoinsv.bsvcl.common.config.RuntimeConfig
import io.bitcoinsv.bsvcl.common.config.provided.RuntimeConfigDefault
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
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
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))

            // Since the initial peers are in a CSV in our classpath, we need to use a "RuntimeConfig" specifying a
            // classpath, so we instead of using the Default, we use an specific one
            RuntimeConfig runtimeConfig = new RuntimeConfigDefault(this.getClass().getClassLoader());

            DiscoveryHandlerConfig discoveryConfig = config.getDiscoveryConfig()
                    .toBuilder()
                    .discoveryMethod(DiscoveryHandlerConfig.DiscoveryMethod.PEERS)
                    .build()

            P2P server = new P2PBuilder("testing")
                    .config(runtimeConfig)
                    .config(config)
                    .config(discoveryConfig)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // We store the Event triggered when the initial Peers are loaded:
            AtomicReference<InitialPeersLoadedEvent> event = new AtomicReference<>()
            server.EVENTS.PEERS.INITIAL_PEERS_LOADED.forEach({ e -> event.set(e)})

        when:
            println("Starting P2P ...")
            server.startServer()
            Thread.sleep(500)
            println("P2P Stopping...")
            server.stop()
            println("P2P Stopped.")
        then:
            event.get() != null
            event.get().numPeersLoaded > 0

    }

    /**
     * We check that on startup, the Discovery Handler successfully loads the initial set of Peers from th DNS List
     */
    def "Testing Initial Peers loaded from DNS"() {
        given:
            // We set up the Configuration to use the Peers obtained by looking up oin the hardcoded DNS
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))

            DiscoveryHandlerConfig discoveryConfig = config.getDiscoveryConfig().toBuilder()
                .discoveryMethod(DiscoveryHandlerConfig.DiscoveryMethod.DNS)
                .build()
            P2P server = new P2PBuilder("testing")
                    .config(config)
                    .config(discoveryConfig)
                    .serverPort(0) // Random Port
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
                server.startServer()
                Thread.sleep(30000) // Raise this timeout if the DNS are poor and take long to find peers
                println("P2P Stopping...")
                server.stop()
                println("P2P Stopped.")
            } catch (Throwable e) {
                e.printStackTrace()
            }
        then:
            event.get() != null
            event.get().numPeersLoaded > 0
    }
}
