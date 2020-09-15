package com.nchain.jcl.net.integration.protocol.handlers.handshake

import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.net.protocol.events.MinHandshakedPeersLostEvent
import com.nchain.jcl.net.protocol.events.MinHandshakedPeersReachedEvent
import com.nchain.jcl.net.protocol.handlers.blacklist.BlacklistHandler
import com.nchain.jcl.net.protocol.wrapper.P2P
import com.nchain.jcl.net.protocol.wrapper.P2PBuilder
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Integration Test Class for the Handshake Handler.
 * The handshakes performed in this class are performed with real Peers int the P2P Network.
 *
 * INTERNET CONNECTION IS NEEDED FOR THIS TEST.
 */
class HandshakeOKTest extends Specification {

    /**
     * In this Test, we connect to the P2P Network and check that the service connects automatically
     * with other Peers in the Network and the related events are triggered properly:
     *
     * - The initial set of Peers is loaded right after starting the Service. This is done by the Discovery Handler
     * - The service keeps connecting to Peers and starting the Handshake protocol with them.
     * - At some point in time, the MIN number of Handshaked Peers is reached.
     * - At some point in time, the MAX number of Handshaked Peers is reached.
     *
     *  Then we wait, and then we STOP the Service. At that moment the service will start dropping connections, and:
     *  - At some point, the MIN number of Handshaked Peers is Lost.
     *
     */
    def "Testing Handshake Range Peers"() {
        given:
            final int MIN_PEERS = 3
            final int MAX_PEERS = 6

            // We set the Default Config:
            ProtocolConfig config = new ProtocolBSVMainConfig()

            // We disable the Handlers we do NOT need for this test
            P2P server = new P2PBuilder("testing")
                    .config(config)
                    .randomPort()
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .minPeers(MIN_PEERS)
                    .maxPeers(MAX_PEERS)
                    .build()

            // We keep track of different things:
            // Number of Peers Connected: (low-level: connected does not mean handshaked, it only means that
            // the socket connection has been established:
            AtomicInteger numPeersCurrentlyConnected = new AtomicInteger()
            AtomicInteger numPeersConnections = new AtomicInteger()

            // Number of Peers handshaked:
            AtomicInteger numPeersCurrentlyHandshaked = new AtomicInteger()
            AtomicInteger numPeersHandshakes = new AtomicInteger()

            AtomicReference<MinHandshakedPeersReachedEvent> minHandshakedReachedEvent = new AtomicReference<>()
            AtomicReference<MinHandshakedPeersLostEvent> minHandshakedLostEvent = new AtomicReference<>()

            server.EVENTS.PEERS.HANDSHAKED.forEach({ e ->
                numPeersCurrentlyHandshaked.incrementAndGet()
                numPeersHandshakes.incrementAndGet()
            })
            server.EVENTS.PEERS.HANDSHAKED_DISCONNECTED.forEach({ e ->
                numPeersCurrentlyHandshaked.decrementAndGet()
            })

            server.EVENTS.PEERS.CONNECTED.forEach({e ->
                numPeersCurrentlyConnected.incrementAndGet()
                numPeersConnections.incrementAndGet()
            })
            server.EVENTS.PEERS.DISCONNECTED.forEach({e ->
                numPeersCurrentlyConnected.decrementAndGet()
            })

            server.EVENTS.PEERS.HANDSHAKED_MIN_REACHED.forEach({e -> minHandshakedReachedEvent.set(e)})
            server.EVENTS.PEERS.HANDSHAKED_MIN_LOST.forEach({e -> minHandshakedLostEvent.set(e)})

        when:
            server.startServer()
            Thread.sleep(2000)
            // The Service will start connecting to the Peers and handshaking with them.
            // The connection will stop at the moment we have MAX_PEER handshaked. At that moment, the service will
            // disconnect from any other additional Pees that he might have handshaked after that, so the number of
            // Peers handshaked remains at MAX_PEER.
            println(" >>> CHECKING NUMBER OF PEERS HANDSHAKED: " + numPeersHandshakes.get())
            boolean connection_stable = numPeersHandshakes.get() == MAX_PEERS
            println(" >>> STOPPING...")
            server.stop()
        then:
            // We check that at some pint in time, we've reached the MIN and MAX Peers to handshake:
            minHandshakedReachedEvent.get() != null
            minHandshakedReachedEvent.get() != null
            // we check
            connection_stable
    }
}