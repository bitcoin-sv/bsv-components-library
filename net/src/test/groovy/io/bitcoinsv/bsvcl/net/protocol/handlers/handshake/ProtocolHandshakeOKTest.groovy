package io.bitcoinsv.bsvcl.net.protocol.handlers.handshake

import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolBasicConfig
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.bsvcl.net.protocol.events.control.MinHandshakedPeersLostEvent
import io.bitcoinsv.bsvcl.net.protocol.events.control.MinHandshakedPeersReachedEvent
import io.bitcoinsv.bsvcl.net.protocol.handlers.blacklist.BlacklistHandler
import io.bitcoinsv.bsvcl.net.protocol.handlers.discovery.DiscoveryHandler
import io.bitcoinsv.bsvcl.net.protocol.handlers.pingPong.PingPongHandler
import io.bitcoinsv.bsvcl.net.P2P
import io.bitcoinsv.bsvcl.net.P2PBuilder
import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import spock.lang.Ignore
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
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))
            // We disable all the Handlers we don't need for this Test:
            P2P server = new P2PBuilder("server")
                    .config(config)
                    .serverPort(0) // Random Port
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()
            // We disable all the Handlers we don't need for this Test:
            P2P client = new P2PBuilder("client")
                    .config(config)
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
     * Event is triggered and another is also triggered when that number drops.
     */
    @Ignore
    def "testing Min and Max Handshakes Threshold"() {
        given:
            int MIN_PEERS = 2
            int MAX_PEERS = 3
            int NUM_CLIENTS = 4
            // Server Definition:
            // We change the Default P2P Configuration, to establish the parameters for this Test:

            ProtocolConfig protocolConfig = ProtocolConfigBuilder.get(new MainNetParams(Net.MAINNET))

            // We set up the MIN and MAX Peers
            ProtocolBasicConfig basicConfig = protocolConfig.basicConfig.toBuilder()
                    .minPeers(OptionalInt.of(MIN_PEERS))
                    .maxPeers(OptionalInt.of(MAX_PEERS))
                    .build()

            // We disable all the Handlers we don't need for this Test:
            P2P server = new P2PBuilder("server")
                    .config(protocolConfig)
                    .config(basicConfig)
                    .serverPort(0) // Random Port
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()

            // Clients Definitions: We store them in a List
            List<P2P> clients = new ArrayList<>()
            for (int i = 0; i < NUM_CLIENTS; i++) {
                // We disable all the Handlers we don't need for this Test:
                P2P client = new P2PBuilder("client-" + i)
                        .config(protocolConfig)
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

            server.EVENTS.PEERS.HANDSHAKED.forEach({ e -> println(e)})
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
            Thread.sleep(10000)
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

            Thread.sleep(100)
            // Now we stop them all
            println(" >> STOPPING ALL CLIENTS...")
            for (P2P client : clients) {
                client.stop()
            }
            Thread.sleep(100)
            server.stop()
        then:
            numLostEvents.get() == 2
            numReachedEvents.get() == 2
            reachedEvent.get() != null
            reachedEvent.get().getNumPeers() == MIN_PEERS
            lostEvent.get() != null
            lostEvent.get().getNumPeers() <= MIN_PEERS

    }
}
