package io.bitcoinsv.bsvcl.net.protocol.handlers.discovery


import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import spock.lang.Ignore
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Integration Test Class for the Discovery Handler Initial Connections Process.
 *
 * If the "initialConnections" property is defined in the configuration, then those Peers will be
 * the first ones to connect to. If more peers are needed after that to reach maxPeers, then the
 * Node-Discovery Algorithm will be used as usual.
 *
 * INTERNET CONNECTION IS NEEDED FOR THIS TEST.
 */
@Ignore("This is an integration test")  // todo: set up integration tests
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
    // NOTE:
    // We ignore this test since it depends on the Peers specified in the code so it might get outdated pretty soon
    // if those peers are not available anymore...
    @Ignore
    def "Testing POOL Renew Job"() {
        given:
            // Initial Set of Peers:
            final List<io.bitcoinsv.bsvcl.net.network.PeerAddress> initialPeers = Arrays.asList(
                    io.bitcoinsv.bsvcl.net.network.PeerAddress.fromIp("209.97.181.106:8333"),
                    io.bitcoinsv.bsvcl.net.network.PeerAddress.fromIp("47.91.95.186:8333"),
                    io.bitcoinsv.bsvcl.net.network.PeerAddress.fromIp("144.76.117.158:8333"),
                    io.bitcoinsv.bsvcl.net.network.PeerAddress.fromIp("39.103.148.33:8333"),
                    io.bitcoinsv.bsvcl.net.network.PeerAddress.fromIp("206.189.81.233:8333")
            )

            // Number of Peers we want to Connect in TOTAL:
            final int MAX_PEERS = 10

            // Percentage of "initialPeers" that are connected when we reach MAX_PEERS.
            // For example, if percentage is 80, and we define 5 initial Peers, then the test will be a
            // SUCCESS if by the time we connect to MAX_PEERS, 4 of them are from "initialPeers"
            // (80% of 5 initial Peers = 4)
            final int PERCENTAGE_SUCCESS = 60

            // We set up the configuration
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams()).toBuilder()
                    .maxPeers(MAX_PEERS)
                    .minPeers(MAX_PEERS - 2) // 2 peers margin
                    .build()

            // We set up the frequency for the "pool" renewing Job and  disable the "handshake" renewing job:
            io.bitcoinsv.bsvcl.net.protocol.handlers.discovery.DiscoveryHandlerConfig discoveryConfig = config.getDiscoveryConfig().toBuilder()
                .addInitialConnections(initialPeers)
                .build()

            io.bitcoinsv.bsvcl.net.protocol.wrapper.P2P server = new io.bitcoinsv.bsvcl.net.protocol.wrapper.P2PBuilder("testing")
                    .config(config)
                    .config(discoveryConfig)
                    .serverPort(0) // Random Port
                    .excludeHandler(io.bitcoinsv.bsvcl.net.protocol.handlers.blacklist.BlacklistHandler.HANDLER_ID)
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
