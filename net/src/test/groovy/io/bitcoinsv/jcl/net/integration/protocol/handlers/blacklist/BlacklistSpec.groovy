/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.integration.protocol.handlers.blacklist


import io.bitcoinj.params.MainNetParams
import io.bitcoinsv.jcl.net.network.PeerAddress
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.jcl.net.protocol.handlers.block.BlockDownloaderHandler
import io.bitcoinsv.jcl.net.protocol.wrapper.P2P
import io.bitcoinsv.jcl.net.protocol.wrapper.P2PBuilder
import spock.lang.Specification

/**
 * Testing scenarios for Blacklisting Remote Peers
 */
class BlacklistSpec extends Specification {

    /**
     * We launch the P2P Service, and keep track of the Peers we connect to. Then we blacklist one of them, and we
     * verify that the service actually disconnects from it.
     */
    def "testing a Remote Peer and check we disconnect from it"() {
        given:
            // We limit the number of Peer we want to connect to:
            final int MIN_PEERS = 15
            final int MAX_PEERS = 20

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

        when:
            // We start the Service. The Discovery Handler will load an initial set of Peers and the service will
            // automatically starts connecting to them...
            server.start()
            Thread.sleep(20000)

            // At this point, the service would be already connected to the MAX_PEERS.
            // We blacklist ONE of them (the first one)...
            PeerAddress peerToBlacklist = peersConnected.get(0)

            println("Blacklisting " + peerToBlacklist + "...")
            server.REQUESTS.PEERS.blacklist(peerToBlacklist).submit()

            // We wait a bit, during this time the Peer should be Blacklisted and Disconnected...
            Thread.sleep(1000)
            boolean peerDisconnected = peersDisconnected.contains(peerToBlacklist)

            // And we are Done.
            server.stop()

        then:
            peerDisconnected
    }
}
