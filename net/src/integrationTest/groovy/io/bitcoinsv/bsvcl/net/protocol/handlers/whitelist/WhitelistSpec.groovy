package io.bitcoinsv.bsvcl.net.protocol.handlers.whitelist

import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bsvcl.net.P2P
import io.bitcoinsv.bsvcl.net.P2PBuilder
import spock.lang.Ignore
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors
import java.util.stream.IntStream

/**
 * Testing scenarios for Whitelisting/Removing from Whitelist Remote Peers
 */
@Ignore
class WhitelistSpec extends Specification {

    /**
     * We launch the P2P Service, and keep track of the Peers we connect to.
     * Then we perform different operations:
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
            Set<InetAddress>  peersWhitelisted          = Collections.synchronizedSet(new HashSet<InetAddress>());
            Set<InetAddress>  peersRemovedFromWhitelist = Collections.synchronizedSet(new HashSet<InetAddress>());
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
            server.EVENTS.PEERS.WHITELISTED.forEach({ e ->
                println(" > Whitelisted peer: " + e.inetAddresses)
                peersWhitelisted.addAll(e.inetAddresses);
            })
            server.EVENTS.PEERS.REMOVED_FROM_WHITELIST.forEach({ e ->
                println(" > Peers removed from Whitelist: " + e.inetAddresses)
                peersRemovedFromWhitelist.addAll(e.inetAddresses)
            })

        when:
            // We start the Service. The Discovery Handler will load an initial set of Peers and the service will
            // automatically starts connecting to them...
            server.start()

            // We wait until we connect to MAX_PEERS:
            while (!maxPeersReached.get()) {Thread.sleep(500)}

            // Now We blacklist some of them, and we check that we disconnect from them, and the Events are triggered:
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
            Thread.sleep(100)

            // We check everything is fine:
            boolean peersBlacklistedOK = peersToBlacklist.stream().allMatch({ p -> peersBlacklisted.contains(p.ip) && peersDisconnected.contains(p)})

            // Now we Whitelist 2 peers: one has been previously blacklisted, the other not. Both should be whitelisted,
            // and one of them should be actually removed form the Blacklist List.....
            println("==============================================================================================")
            println(":: WHITELISTING SOME PEERS...")

            io.bitcoinsv.bsvcl.net.network.PeerAddress peerBlacklistedThenWhitelisted = peersToBlacklist.get(0);
            List<io.bitcoinsv.bsvcl.net.network.PeerAddress> peersToWhitelist = new ArrayList<io.bitcoinsv.bsvcl.net.network.PeerAddress>();
            peersToWhitelist.add(peerBlacklistedThenWhitelisted)                    // first peer to be blacklisted
            peersToWhitelist.add(peersConnected.get(peersConnected.size() - 1));    // Last connected peer

            peersToWhitelist.stream().forEach({ p ->
                println("Whitelisting " + p + "...")
                server.REQUESTS.PEERS.whitelist(p.getIp()).submit()
            });

            // We wait a bit, so events and handlers are triggered...
            Thread.sleep(100);

            // We check everything is fine:
            boolean peersWhitelistedOK = peersToWhitelist.stream().allMatch({ p -> peersToWhitelist.contains(p)})
            boolean peersRemovedFromBlacklistOK = peersRemovedFromBlacklist.contains(peerBlacklistedThenWhitelisted.getIp())

            // We clear The Blacklist and Whitelist Lists of Peers:
            println("==============================================================================================")
            println(":: CLEARING BLACKLIST AND WHITELIST...")
            server.REQUESTS.PEERS.clearBlacklist().submit();
            server.REQUESTS.PEERS.clearWhitelist().submit();

            // We wait a bit, so the event is triggered:
            Thread.sleep(100);

            // We check everything is fine:
            boolean blacklistClearOK = peersBlacklisted.size() == peersRemovedFromBlacklist.size()
            boolean whitelistClearOK = peersWhitelisted.size() == peersRemovedFromWhitelist.size()


            // And we are Done.
            server.stop()
            server.awaitStopped()

        then:
            peersBlacklistedOK
            peersWhitelistedOK
            peersRemovedFromBlacklistOK
            blacklistClearOK
            whitelistClearOK
    }
}
