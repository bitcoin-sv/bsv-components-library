/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.unit.protocol.handlers.wrapper


import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.jcl.net.network.config.NetworkConfig
import io.bitcoinsv.jcl.net.network.config.provided.NetworkDefaultConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.jcl.net.protocol.config.ProtocolConfigBuilder
import io.bitcoinsv.jcl.net.protocol.handlers.blacklist.BlacklistHandler
import io.bitcoinsv.jcl.net.protocol.handlers.discovery.DiscoveryHandler
import io.bitcoinsv.jcl.net.protocol.handlers.handshake.HandshakeHandler
import io.bitcoinsv.jcl.net.protocol.handlers.pingPong.PingPongHandler
import io.bitcoinsv.jcl.net.protocol.messages.AddrMsg
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg
import io.bitcoinsv.jcl.net.protocol.wrapper.P2P
import io.bitcoinsv.jcl.net.protocol.wrapper.P2PBuilder
import io.bitcoinsv.jcl.net.unit.protocol.tools.MsgTest
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger

/**
 * Testing class using the P2P "wrapper", that includes all the network and protocol handlers within.
 */
class ProtocolMsgsTest extends Specification {

    /**
     * We test that 2 different P2P Handlers can connect and exchange a message, and the events are triggered properly
     */
    def "Testing Client/Server Msgs exchange"() {
        given:
            // Server and client configuration:

            // Time we wait for a Connection between Server and client (in millisecs).
            // The connection should be pretty fast. But the default URL for a Server/Client has been recently changed to
            // "0.0.0.0" instead of "127.0.0.1", and that might add some delay in the network communication, so we
            // increase the timeout now...
            final int CONN_TIMEOUT = 5000; // crazy long

            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams())

            NetworkConfig netConfig = new NetworkDefaultConfig().toBuilder()
                .timeoutSocketConnection(OptionalInt.of(CONN_TIMEOUT))
                .build();
                // We disable all the Handlers we don't need for this Test:
            P2P server = new P2PBuilder("server")
                        .config(netConfig)
                        .config(config)
                        .serverPort(0) // Random Port
                        .excludeHandler(HandshakeHandler.HANDLER_ID)
                        .excludeHandler(PingPongHandler.HANDLER_ID)
                        .excludeHandler(DiscoveryHandler.HANDLER_ID)
                        .excludeHandler(BlacklistHandler.HANDLER_ID)
                        .build()
            P2P client = new P2PBuilder("client")
                        .config(netConfig)
                        .config(config)
                        .excludeHandler(HandshakeHandler.HANDLER_ID)
                        .excludeHandler(PingPongHandler.HANDLER_ID)
                        .excludeHandler(DiscoveryHandler.HANDLER_ID)
                        .excludeHandler(BlacklistHandler.HANDLER_ID)
                        .build()

            // we listen to the Connect/Disconnect Events:
            int NUM_MSGS = 3
            AtomicInteger numConnections = new AtomicInteger()
            AtomicInteger numDisconnections = new AtomicInteger()
            AtomicInteger numMsgs = new AtomicInteger()

            server.EVENTS.PEERS.CONNECTED.forEach({ e -> numConnections.incrementAndGet()})
            client.EVENTS.PEERS.CONNECTED.forEach({ e -> numConnections.incrementAndGet()})
            server.EVENTS.PEERS.DISCONNECTED.forEach({ e -> numDisconnections.incrementAndGet()})
            client.EVENTS.PEERS.DISCONNECTED.forEach({ e -> numDisconnections.incrementAndGet()})
            server.EVENTS.MSGS.ADDR.forEach({ e -> numMsgs.incrementAndGet()})

        when:
            server.startServer()
            client.start()

            Thread.sleep(100)
            client.REQUESTS.PEERS.connect(server.getPeerAddress()).submit()

            Thread.sleep(CONN_TIMEOUT)
            BitcoinMsg<AddrMsg> msg = MsgTest.getAddrMsg();
            for (int i = 0; i < NUM_MSGS; i++) {
                println(" >> SENDING ADDR MSG...")
                client.REQUESTS.MSGS.send(server.getPeerAddress(), msg).submit()
            }

            // NOTE: Here, we wait a little bit before we closeAndClear the connection If we don't wait enough, the Socket
            // between the Client and Server will be closed before the message can be serialized/Deserialized and
            // travel between them
            Thread.sleep(200)
            println(" >>> DISCONNECTING FROM THE SERVER...")
            client.REQUESTS.PEERS.disconnect(server.getPeerAddress()).submit()

            Thread.sleep(100)
            println(" >>> STOPPING...")
            server.stop()
            client.stop()

        then:
            // We check that the Events have been triggered right:
            numConnections.get() == 2
            numDisconnections.get() == 2
            numMsgs.get() == NUM_MSGS
    }
}
