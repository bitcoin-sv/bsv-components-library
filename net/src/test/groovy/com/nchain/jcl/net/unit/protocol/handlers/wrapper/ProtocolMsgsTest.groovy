package com.nchain.jcl.net.unit.protocol.handlers.wrapper

import com.nchain.jcl.net.protocol.config.ProtocolConfig
import com.nchain.jcl.net.protocol.config.ProtocolConfigBuilder
import com.nchain.jcl.net.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.net.protocol.handlers.blacklist.BlacklistHandler
import com.nchain.jcl.net.protocol.handlers.discovery.DiscoveryHandler
import com.nchain.jcl.net.protocol.handlers.handshake.HandshakeHandler
import com.nchain.jcl.net.protocol.handlers.pingPong.PingPongHandler
import com.nchain.jcl.net.protocol.messages.AddrMsg
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg
import com.nchain.jcl.net.unit.protocol.tools.MsgTest
import com.nchain.jcl.net.protocol.wrapper.P2P
import com.nchain.jcl.net.protocol.wrapper.P2PBuilder
import io.bitcoinj.params.MainNetParams
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
            ProtocolConfig config = ProtocolConfigBuilder.get(new MainNetParams())
            // We disable all the Handlers we don't need for this Test:
            P2P server = new P2PBuilder("server")
                    .config(config)
                    .serverPort(0) // Random Port
                    .excludeHandler(HandshakeHandler.HANDLER_ID)
                    .excludeHandler(PingPongHandler.HANDLER_ID)
                    .excludeHandler(DiscoveryHandler.HANDLER_ID)
                    .excludeHandler(BlacklistHandler.HANDLER_ID)
                    .build()
            P2P client = new P2PBuilder("client")
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

            Thread.sleep(100)
            BitcoinMsg<AddrMsg> msg = MsgTest.getAddrMsg();
            for (int i = 0; i < NUM_MSGS; i++) {
                println(" >> SENDING ADDR MSG...")
                client.REQUESTS.MSGS.send(server.getPeerAddress(), msg).submit()
            }

            // NOTE: Here, we wait a little bit before we close the connection If we don't wait enough, the Socket
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
