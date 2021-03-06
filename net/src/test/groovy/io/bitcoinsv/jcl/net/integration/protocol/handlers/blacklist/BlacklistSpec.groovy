package io.bitcoinsv.jcl.net.integration.protocol.handlers.blacklist

import io.bitcoinsv.jcl.net.network.PeerAddress
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.jcl.net.protocol.handlers.block.BlockDownloaderHandler
import io.bitcoinsv.jcl.net.protocol.wrapper.P2P
import io.bitcoinsv.jcl.net.protocol.wrapper.P2PBuilder
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors
import java.util.stream.IntStream

/**
 * Testing scenarios for Blacklisting/Whitelisting Remote Peers
 */
class BlacklistSpec extends Specification {

    /**
     * We launch the P2P Service, and keep track of the Peers we connect to.
     * Then we perform differnet operations:
     * - We blacklist some Peers.
     * - We whitelist the ONE of previous peer.
     * - We Clear the Blacklist
     *
     */
    def "testing blacklisting/whitelisting"() {
        given:
            // We limit the number of Peer we want to connect to:
            final int MIN_PEERS = 5
            final int MAX_PEERS = 10

            // We set up the configuration
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams()).toBuilder()
                    .minPeers(MIN_PEERS)
                    .maxPeers(MAX_PEERS)
                    .build()

            P2P server = new P2PBuilder("testing")
                    .config(config)
                    .serverPort(0) // Random Port
                    .excludeHandler(BlockDownloaderHandler.HANDLER_ID)
                    .build()

            // We keep track of the Peer we connect to:
            List<PeerAddress> peersConnected = Collections.synchronizedList(new ArrayList<PeerAddress>())
            List<PeerAddress> peersDisconnected = Collections.synchronizedList(new ArrayList<PeerAddress>())
            Set<InetAddress>  peersBlacklisted = Collections.synchronizedSet(new HashSet<InetAddress>());
            Set<InetAddress>  peersWhitelisted = Collections.synchronizedSet(new HashSet<InetAddress>());
            AtomicBoolean     maxPeersReached = new AtomicBoolean()

            server.EVENTS.PEERS.HANDSHAKED_MAX_REACHED.forEach({ e ->
                maxPeersReached.set(true)
                println(" > Max number of Peers reached: " + e.numPeers + " peers.")
            })
            server.EVENTS.PEERS.HANDSHAKED.forEach({e ->
                println(" > Connected to " + e.peerAddress)
                peersConnected.add(e.peerAddress)
                peersDisconnected.remove(e.peerAddress)
            })
            server.EVENTS.PEERS.DISCONNECTED.forEach({e ->
                println(" > Disconnected from " + e.peerAddress)
                peersDisconnected.add(e.peerAddress)
                peersConnected.remove(e.peerAddress)
            })
            server.EVENTS.PEERS.BLACKLISTED.forEach({ e ->
                println(" > Blacklisted peer: " + e.inetAddresses)
                peersBlacklisted.addAll(e.inetAddresses.keySet());
                peersWhitelisted.removeAll(e.inetAddresses.keySet());
            })
            server.EVENTS.PEERS.WHITELISTED.forEach({ e ->
                println(" > Whitelisted peer: " + e.inetAddresses)
                peersWhitelisted.addAll(e.inetAddresses)
                peersBlacklisted.removeAll(e.inetAddresses);
            })

        when:
            // We start the Service. The Discovery Handler will load an initial set of Peers and the service will
            // automatically starts connecting to them...
            server.start()
            Thread.sleep(20000)

            // We wait until we connect to MAX_PEERS:
            while (!maxPeersReached.get()) {Thread.sleep(500)}

            // -----------------------------------------------------------------------------------------------------
            // We blacklist some of them, and we check that we disconnect from them, and the Events are triggered:
            println("==============================================================================================")
            println(":: BLACKLISTING SOME PEERS...")

            int NUM_PEERS_TO_BLACKLIST = 3
            List<PeerAddress> peersToBlacklist = IntStream
                    .range(0, NUM_PEERS_TO_BLACKLIST)
                    .mapToObj({i -> peersConnected.get(i)})
                    .collect(Collectors.toList())

            for (PeerAddress peerAddress : peersToBlacklist) {
                println("Blacklisting " + peerAddress + "...")
                server.REQUESTS.PEERS.blacklist(peerAddress.getIp()).submit()
            }

            // We wait a bit, during this time all the Peers should be Blacklisted and Disconnected...
            Thread.sleep(1000)
            boolean peersBlacklistedOK = peersToBlacklist.stream().allMatch({ p -> peersDisconnected.contains(p)})


            // -----------------------------------------------------------------------------------------------------
            // We whitelist one of the Peers previously blacklisted. The P2P service should have enough Peers already
            // so it won't try to connect to more peers just yet, but the Whitelist event will be triggered.
            println("==============================================================================================")
            println(":: WHITELISTING ONE PEER...")
            PeerAddress peerToWhitelist = peersToBlacklist.get(0)
            server.REQUESTS.PEERS.whitelist(peerToWhitelist.ip).submit();
            // We wait a bit, so the event is triggered:
            Thread.sleep(100)
            boolean singlePeerWhitelisted = peersWhitelisted.contains(peerToWhitelist.ip) && !peersBlacklisted.contains(peerToWhitelist.getIp())

            // -----------------------------------------------------------------------------------------------------
            // We Clear the blacklist. Several whitelist Events should be triggered at this point. These events should
            // contain references to the Peers previously blacklisted, but they might also contain references to other
            // Peers that have been blacklisted by the P2P service on startup. So here we just check that at least
            // the Peers whitelisted include the ones manually blacklisted in this test.
            println("==============================================================================================")
            println(":: CLEARING BLACKLIST...")
            server.REQUESTS.PEERS.clearBlacklist().submit()
            // We wait a bit so the events are triggered:
            Thread.sleep(100)
            boolean blacklistCleared =  peersToBlacklist.stream().allMatch({ p -> !peersBlacklisted.contains(p.ip)}) &&
                                        peersToBlacklist.stream().allMatch({ p -> peersWhitelisted.contains(p.ip)})

            // And we are Done.
            server.stop()

        then:
            peersBlacklistedOK
            singlePeerWhitelisted
            blacklistCleared
    }
}
