package io.bitcoinsv.bsvcl.net.protocol.handlers.blacklist


import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bsvcl.net.P2P
import io.bitcoinsv.bsvcl.net.P2PBuilder
import spock.lang.Ignore
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors
import java.util.stream.IntStream

/**
 * Testing scenarios for Blacklisting/Removing from Blacklist Remote Peers
 */
@Ignore
class BlacklistSpec extends Specification {

    /**
     * We launch the P2P Service, and keep track of the Peers we connect to.
     * Then we perform differnet operations:
     * - We blacklist some Peers.
     * - We remove from the Blacklist the ONE of previous peer.
     * - We Clear the Blacklist
     *
     */
    def "testing blacklisting/removing from Blacklist"() {
        given:
            // We limit the number of Peer we want to connect to:
            final int MIN_PEERS = 5
            final int MAX_PEERS = 10

            // We set up the configuration
            io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig config = io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder.get(new MainNetParams()).toBuilder()
                    .minPeers(MIN_PEERS)
                    .maxPeers(MAX_PEERS)
                    .build()

            // We extends the DiscoveryHandler Config, in case DNS's are not working properly:
            io.bitcoinsv.bsvcl.net.protocol.handlers.discovery.DiscoveryHandlerConfig discoveryConfig = io.bitcoinsv.bsvcl.net.integration.utils.IntegrationUtils.getDiscoveryHandlerConfigMainnet(config.getDiscoveryConfig())

        P2P server = new P2PBuilder("testing")
                    .config(config)
                    .config(discoveryConfig)
                    .serverPort(0) // Random Port
                    .excludeHandler(io.bitcoinsv.bsvcl.net.protocol.handlers.block.BlockDownloaderHandler.HANDLER_ID)
                    .build()

            // We keep track of the Peer we connect to:
            List<io.bitcoinsv.bsvcl.net.network.PeerAddress> peersConnected            = Collections.synchronizedList(new ArrayList<io.bitcoinsv.bsvcl.net.network.PeerAddress>())
            List<io.bitcoinsv.bsvcl.net.network.PeerAddress> peersDisconnected         = Collections.synchronizedList(new ArrayList<io.bitcoinsv.bsvcl.net.network.PeerAddress>())
            Set<InetAddress>  peersBlacklisted          = Collections.synchronizedSet(new HashSet<InetAddress>());
            Set<InetAddress>  peersRemovedFromBlacklist = Collections.synchronizedSet(new HashSet<InetAddress>());
            AtomicBoolean     maxPeersReached           = new AtomicBoolean()

            server.EVENTS.PEERS.HANDSHAKED_MAX_REACHED.forEach({ e ->
                maxPeersReached.set(true)
                println(" > Max number of Peers reached: " + e.numPeers + " peers.")
            })
            server.EVENTS.PEERS.HANDSHAKED.forEach({e ->
                println(" > Connected to " + e.peerAddress)
                peersConnected.add(e.peerAddress)
            })
            server.EVENTS.PEERS.DISCONNECTED.forEach({e ->
                println(" > Disconnected from " + e.peerAddress)
                peersDisconnected.add(e.peerAddress)
            })
            server.EVENTS.PEERS.BLACKLISTED.forEach({ e ->
                println(" > Blacklisted peer: " + e.inetAddresses)
                peersBlacklisted.addAll(e.inetAddresses.keySet());
            })
            server.EVENTS.PEERS.REMOVED_FROM_BLACKLIST.forEach({ e ->
                println(" > Peer removed from blacklist: " + e.inetAddresses)
                peersRemovedFromBlacklist.addAll(e.inetAddresses)
            })

        when:
            // We start the Service. The Discovery Handler will load an initial set of Peers and the service will
            // automatically starts connecting to them...
            server.start()

            // We wait until we connect to MAX_PEERS:
            while (!maxPeersReached.get()) {Thread.sleep(500)}

            // -----------------------------------------------------------------------------------------------------
            // We blacklist some of them, and we check that we disconnect from them, and the Events are triggered:
            println("==============================================================================================")
            println(":: BLACKLISTING SOME PEERS...")

            int NUM_PEERS_TO_BLACKLIST = 3
            List<io.bitcoinsv.bsvcl.net.network.PeerAddress> peersToBlacklist = IntStream
                    .range(0, NUM_PEERS_TO_BLACKLIST)
                    .mapToObj({i -> peersConnected.get(i)})
                    .collect(Collectors.toList())

            for (io.bitcoinsv.bsvcl.net.network.PeerAddress peerAddress : peersToBlacklist) {
                println("Blacklisting " + peerAddress + "...")
                server.REQUESTS.PEERS.blacklist(peerAddress.getIp()).submit()
            }

            // We wait a bit, so events and handlers are triggered...
            Thread.sleep(1000)
            boolean peersBlacklistedOK = peersToBlacklist.stream().allMatch({ p -> peersDisconnected.contains(p)})


            // -----------------------------------------------------------------------------------------------------
            // We remove from the Blacklist one of the Peers previously blacklisted. The P2P service should have enough
            // Peers already so it won't try to connect to more peers just yet, but the "RemoveFromBlacklist"" event
            // will be triggered.
            println("==============================================================================================")
            println(":: REMOVING ONE PEER FORM THE BLACKLIST...")
            io.bitcoinsv.bsvcl.net.network.PeerAddress peerToRemove = peersToBlacklist.get(0)
            println("Removing " + peerToRemove + " from the Blacklist...")
            server.REQUESTS.PEERS.removeFromBlacklist(peerToRemove.ip).submit();
            // We wait a bit, so events and handlers are triggered...
            Thread.sleep(100)
            boolean singlePeerRemoved = peersRemovedFromBlacklist.contains(peerToRemove.ip)

            // -----------------------------------------------------------------------------------------------------
            // We Clear the blacklist. Several "RemoveFromBlacklist"" Events should be triggered at this point.
            // These events should contain references to the Peers previously blacklisted, but they might also contain
            // references to other Peers that have been blacklisted by the P2P service on startup. So here we just
            // check that at least the Peers removed from the blacklist include the ones manually blacklisted in this test.
            println("==============================================================================================")
            println(":: CLEARING BLACKLIST...")
            server.REQUESTS.PEERS.clearBlacklist().submit()
            // We wait a bit, so events and handlers are triggered...
            Thread.sleep(100)
            boolean blacklistCleared =  peersToBlacklist.stream().allMatch({ p -> peersRemovedFromBlacklist.contains(p.ip)})

            // And we are Done.
            server.initiateStop()
            server.awaitStopped()

        then:
            peersBlacklistedOK
            singlePeerRemoved
            blacklistCleared
    }
}
