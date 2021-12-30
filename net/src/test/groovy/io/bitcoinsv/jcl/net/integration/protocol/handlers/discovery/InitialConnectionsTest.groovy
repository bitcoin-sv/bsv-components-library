package io.bitcoinsv.jcl.net.integration.protocol.handlers.discovery

import io.bitcoinsv.jcl.net.network.PeerAddress
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.jcl.net.protocol.handlers.blacklist.BlacklistHandler
import io.bitcoinsv.jcl.net.protocol.handlers.discovery.DiscoveryHandlerConfig
import io.bitcoinsv.jcl.net.protocol.wrapper.P2P
import io.bitcoinsv.jcl.net.protocol.wrapper.P2PBuilder
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Integration Test Class for the Discovery Handler Initial Connections Process.
 *
 * If the "initialConnections" property is defined in the configuration, then those Peers will be
 * the FIrSt ones to connect to. f more Pers are needed after that to reach maxPeers, then the
 * Node-Discovery Algorithm will be used as usual.
 *
 * INTERNET CONNECTION IS NEEDED FOR THIS TEST.
 */
class InitialConnectionsTest extends Specification {

    /**
     * We check that the initialConnections have preference over other Peers found by the regular Node-discovery
     * Algorithm. So we define some initial Peers and launch the P2P Service, and then we test that we actually
     * connected to those Peers before others.
     *
     * NOTE: A accurate testing here is tricky, since those Peers defined in the configuration might not be online.
     * So we rely on statistics: We define a high enough number of initial Peers, and then we check that those Peers
     * are connected BEFORE other Peers.
     * Example:
     *  - We define a set of 5 initial Peers
     *  - We launch the P2P Service and listen to all the "PeerConnectedEvent"
     *  - When we reach 10 "PeerConnectedEvents", we count how many of them belong to the initial Peers, and how many
     *    doo NOT (so they have been found by the Node-Discovery Alg)
     */
    def "Testing POOL Renew Job"() {
        given:
            // Initial Set of Peers:
            final List<PeerAddress> initialPeers = Arrays.asList(
                    PeerAddress.fromIp("209.97.181.106:8333"),
                    PeerAddress.fromIp("47.91.95.186:8333"),
                    PeerAddress.fromIp("144.76.117.158:8333"),
                    PeerAddress.fromIp("39.103.148.33:8333"),
                    PeerAddress.fromIp("206.189.81.233:8333")
            )

            // Number of Peers we want to Connect in TOTAL:
            final int MAX_PEERS = 10

            // Percentage of "initialPeers" that are connected when we reach MAX_PEERS.
            // For example, if percentage is 80, and we define 5 initial Peers, then the test will be a
            // SUCCESS if by the time we connect to MAX_PEERS, 4 of them are from "initialPeers"
            // (80% of 5 initial Peers = 4)
            final int PERCENTAGE_SUCCESS = 80

            // We set up the configuration
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams()).toBuilder()
                    .maxPeers(MAX_PEERS)
                    .minPeers(MAX_PEERS - 2) // 2 peers margin
                    .build()

            // We set up the frequency for the "pool" renewing Job and  disable the "handshake" renewing job:
            DiscoveryHandlerConfig discoveryConfig = config.getDiscoveryConfig().toBuilder()
                .addInitialConnections(initialPeers)
                .build()

            P2P server = new P2PBuilder("testing")
                    .config(config)
                    .config(discoveryConfig)
                    .serverPort(0) // Random Port
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // We keep track of the PeerConnected Events:
            AtomicInteger numPeersConnected = new AtomicInteger()
            AtomicInteger numInitialPeersConnected = new AtomicInteger()
            AtomicBoolean testSuccess = new AtomicBoolean(false);

            server.EVENTS.PEERS.CONNECTED.forEach({e ->
                numPeersConnected.incrementAndGet();
                if (initialPeers.contains(e.getPeerAddress())) {
                    println("Initial Peer Connected:" + e.getPeerAddress());
                    numInitialPeersConnected.incrementAndGet();
                } else {
                    println("Peer Connected:" + e.getPeerAddress());
                }
            })

        when:
            // We start the Service. The Discovery Handler will load an initial set of Peers and the service will
            // automatically starts connecting to them...
            server.start()
            while (numPeersConnected.get() < MAX_PEERS) {
                Thread.sleep(10)
            }
            println("Connected to " + MAX_PEERS + ". (Just connected, we do not care about handshake)");

            int percentageOfInitialPeers = (numInitialPeersConnected.get() * 100 / initialPeers.size());
            println("Percentage of Initial Peers connected: " + percentageOfInitialPeers + "% (" + PERCENTAGE_SUCCESS + " % needed)");
            if (percentageOfInitialPeers >= PERCENTAGE_SUCCESS) {
                testSuccess.set(true);
            }
            server.stop()

        then:
            testSuccess.get()
    }
}
