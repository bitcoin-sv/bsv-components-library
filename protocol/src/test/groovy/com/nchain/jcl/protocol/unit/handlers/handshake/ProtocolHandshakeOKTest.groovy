package com.nchain.jcl.protocol.unit.handlers.handshake

import com.nchain.jcl.protocol.config.ProtocolConfig
import com.nchain.jcl.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.protocol.events.MinHandshakedPeersLostEvent
import com.nchain.jcl.protocol.events.MinHandshakedPeersReachedEvent
import com.nchain.jcl.protocol.handlers.blacklist.BlacklistHandler
import com.nchain.jcl.protocol.handlers.discovery.DiscoveryHandler
import com.nchain.jcl.protocol.handlers.pingPong.PingPongHandler
import com.nchain.jcl.protocol.wrapper.P2P
import com.nchain.jcl.protocol.wrapper.P2PBuilder
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Testing class for the Handshake Handler ("happy" scenarios, where the Handshake is successfully performed)
 */
class ProtocolHandshakeOKTest extends Specification {

    /**
     * We test a valid Handshake between a server and a Client
     */
    def "testing Handshake OK"() {
        given:
            // Server and Client Definition:
            // We disable all the Handlers we don't need for this Test:
            P2P server = new P2PBuilder("server")
                    .randomPort()
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()
            // We disable all the Handlers we don't need for this Test:
            P2P client = new P2PBuilder("client")
                    .randomPort()
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // we Define the Listener for the events and keep track of them:
            AtomicBoolean serverHandshaked = new AtomicBoolean()
            AtomicBoolean clientHandshaked = new AtomicBoolean()
            server.EVENTS.PEERS.HANDSHAKED.forEach({ e -> serverHandshaked.set(true)})
            client.EVENTS.PEERS.HANDSHAKED.forEach({ e -> clientHandshaked.set(true)})

        when:
            server.startServer()
            client.start()

            Thread.sleep(100)

            client.REQUESTS.PEERS.connect(server.getPeerAddress()).submit()
            Thread.sleep(1000)

            server.stop()
            client.stop()

        then:
            // We check that each on of them (Server and client) have received and triggered a Handshake)
            serverHandshaked.get()
            clientHandshaked.get()
    }

    /**
     * We connect a set of Clients and a Server, and we test that when we reach the minimum number of Peers, an
     * Event is triggeredna and another is also triggered when that number drops.
     */
    def "testing Min and Max Handshakes Threshold"() {
        given:
            int MIN_PEERS = 2
            int MAX_PEERS = 3
            int NUM_CLIENTS = 4
            // Server Definition:
            // We change the Default P2P Configuration, to stablish the parameters for this Test:
            ProtocolConfig protocolConfig = new ProtocolBSVMainConfig()

            // We disable all the Handlers we don't need for this Test:
            P2P server = new P2PBuilder("server")
                    .config(protocolConfig)
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .minPeers(MIN_PEERS)
                    .maxPeers(MAX_PEERS)
                    .build()

            // Clients Definitions: We store them in a List
            List<P2P> clients = new ArrayList<>()
            for (int i = 0; i < NUM_CLIENTS; i++) {
                // We disable all the Handlers we don't need for this Test:
                P2P client = new P2PBuilder("client-" + i)
                        .randomPort()
                        .excludeHandler(PingPongHandler.HANDLER_ID)
                        .excludeHandler(DiscoveryHandler.HANDLER_ID)
                        .excludeHandler(BlacklistHandler.HANDLER_ID)
                        .build()
                clients.add(client)
            } // for...

            // We keep track of the Events:
            AtomicInteger numReachedEvents = new AtomicInteger(0)
            AtomicInteger numLostEvents = new AtomicInteger(0)
            AtomicReference<MinHandshakedPeersReachedEvent> reachedEvent = new AtomicReference<>()
            AtomicReference<MinHandshakedPeersLostEvent> lostEvent = new AtomicReference<>()

            server.EVENTS.PEERS.HANDSHAKED_MIN_REACHED.forEach({ e ->
                println(" >> Event: Min Handshaked Peers Reached (" + e.getNumPeers() + " peers)")
                numReachedEvents.incrementAndGet()
                reachedEvent.set(e)
            })
            server.EVENTS.PEERS.HANDSHAKED_MIN_LOST.forEach({ e ->
                println(" >> Event: Min Handshaked Peers Lost (" + e.getNumPeers() + " peers)")
                numLostEvents.incrementAndGet()
                lostEvent.set(e)
            })

        when:
            // We start the Server...
            println(" >> STARTING...")
            server.startServer()

            // We start and connect all the clients to the Server:
            for (P2P client : clients) {
                client.start()
                client.REQUESTS.PEERS.connect(server.getPeerAddress()).submit()
            }
            println(" >> CLIENTS CONNECTING TO SERVER.")

            // We do a little waiting, to make sure al the handshakes have been performed.
            // At his point, the "MinHandshakedPeersReachedEvent" must have been reached...
            Thread.sleep(500)
            println(" >> CLIENTS DISCONNECTING FROM SERVER...")

            // Now we disconnect the clients from the Server one by one. At some point, the
            // MinHandshakedPeersLostEvent wil be triggered:
            for (P2P client: clients) {
                client.REQUESTS.PEERS.disconnect(server.getPeerAddress()).submit()
                Thread.sleep(100)
            }

            // Now we connect the Clients to the Server again...
            Thread.sleep(500)
            println(" >> CLIENTS CONNECTING TO SERVER AGAIN...")
            for (P2P client : clients) {
              client.REQUESTS.PEERS.connect(server.getPeerAddress()).submit()
            }

            Thread.sleep(500)
            // Now we stop them all CHECK EVENTS HERE:
            server.stop()
            for (P2P client : clients) {
                client.stop()
            }
        then:
            numLostEvents.get() == 2
            numReachedEvents.get() == 2
            reachedEvent.get() != null
            reachedEvent.get().getNumPeers() == MAX_PEERS
            lostEvent.get() != null
            lostEvent.get().getNumPeers() == MIN_PEERS

    }
}
