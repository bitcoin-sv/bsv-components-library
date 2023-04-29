package io.bitcoinsv.bsvcl.net

import io.bitcoinsv.bitcoinjsv.params.MainNetParams
import io.bitcoinsv.bitcoinjsv.params.Net
import io.bitcoinsv.bitcoinjsv.params.RegTestParams
import io.bitcoinsv.bsvcl.net.P2PConfig
import io.bitcoinsv.bsvcl.net.network.NetworkController
import io.bitcoinsv.bsvcl.net.network.PeerAddress
import io.bitcoinsv.bsvcl.net.network.events.PeerConnectedEvent
import io.bitcoinsv.bsvcl.net.network.events.PeerDisconnectedEvent
import io.bitcoinsv.bsvcl.net.network.events.PeerRejectedEvent
import io.bitcoinsv.bsvcl.common.config.RuntimeConfig
import io.bitcoinsv.bsvcl.common.config.provided.RuntimeConfigDefault
import io.bitcoinsv.bsvcl.common.events.EventBus
import io.bitcoinsv.bsvcl.common.files.FileUtilsBuilder
import io.bitcoinsv.bsvcl.common.thread.ThreadUtils
import groovy.util.logging.Slf4j
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfig
import io.bitcoinsv.bsvcl.net.protocol.config.ProtocolConfigBuilder
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.TimeUnit

@Slf4j
class ConnectionHandlerTest extends Specification {

    /**
     * Testing a basic connection between 2 ConnectionHandlers. One works as a Sever (Accepting incoming Connections), the
     * other as a Client.
     */
    def "testing Server-Client Connection OK"() {
        given:
            // Basic Configuration is the same for both of them...
            RuntimeConfig runtimeConfig = new RuntimeConfigDefault()
            runtimeConfig = runtimeConfig.toBuilder()
                    .fileUtils(new FileUtilsBuilder().build())
                    .build()
            P2PConfig networkConfig = P2PConfig.builder().listeningPort(0).build()
            ProtocolConfig protocolConfig = ProtocolConfigBuilder.get(new RegTestParams(Net.REGTEST))

            // We initialize them:
            P2P server = new P2P("server", runtimeConfig, networkConfig.toBuilder().listening(true).build(), protocolConfig)
            P2P client = new P2P("client", runtimeConfig, networkConfig, protocolConfig)

            // We keep track of the events in these variables:
            AtomicBoolean serverConnected = new AtomicBoolean(false)
            AtomicBoolean serverDisconnected = new AtomicBoolean(false)

            AtomicBoolean clientConnected = new AtomicBoolean(false)
            AtomicBoolean clientDisconnected = new AtomicBoolean(false)

            CountDownLatch connectsLatch = new CountDownLatch(2)
            CountDownLatch disconnectsLatch = new CountDownLatch(2)

            // We provide some callbacks for the Server...
            server.eventBus.subscribe(PeerConnectedEvent.class, {e ->
                log.trace("EVENT > SERVER: CONNECTED TO " + e.getPeerAddress())
                serverConnected.set(true)
                connectsLatch.countDown()
            })
            server.eventBus.subscribe(PeerDisconnectedEvent.class, { e ->
                log.trace("EVENT > SERVER: DISCONNECTED FROM " + e.getPeerAddress())
                serverDisconnected.set(true)
                disconnectsLatch.countDown()
            })

            // we provide some callbacks for the Client
            client.eventBus.subscribe(PeerConnectedEvent.class, {e ->
                log.trace("EVENT > CLIENT: CONNECTED TO " + e.getPeerAddress())
                clientConnected.set(true)
                connectsLatch.countDown()
            })
            client.eventBus.subscribe(PeerDisconnectedEvent.class, { e ->
                log.trace("EVENT > CLIENT: DISCONNECTED FROM " + e.getPeerAddress())
                clientDisconnected.set(true)
                disconnectsLatch.countDown()
            })

        when:
            server.start()
            client.start()

            client.connect(server.getPeerAddress())
            boolean connected = connectsLatch.await(1, TimeUnit.SECONDS)
            client.disconnect(server.getPeerAddress())
            boolean disconnected = disconnectsLatch.await(1, TimeUnit.SECONDS)
            server.initiateStop()
            client.initiateStop()
            server.awaitStopped()
            client.awaitStopped()

        then:
            connected
            disconnected
            serverConnected.get()
            serverDisconnected.get()
            clientConnected.get()
            clientDisconnected.get()
    }

    /**
     * Testing that the right events are triggered when a connection is refused
     */
    def "testing Failed Connection - REFUSED"() {
        given:
            ExecutorService clientExecutor = ThreadUtils.getSingleThreadExecutorService("Client-EventBus-")

            // Basic Configuration...
            RuntimeConfig runtimeConfig = new RuntimeConfigDefault()
            P2PConfig networkConfig = P2PConfig.builder().build()

            // Event-Bus, for events and callbacks handling...
            EventBus clientEventBus = EventBus.builder().executor(clientExecutor).build()

            // Initialize it:
            NetworkController client = new NetworkController("client", runtimeConfig, networkConfig, PeerAddress.localhost(0),
                                                                clientEventBus)

            // Keep track of the events in these variables:
            AtomicBoolean clientRejected= new AtomicBoolean(false)
            CountDownLatch failureLatch = new CountDownLatch(1)

            // Provide some callbacks ...
            clientEventBus.subscribe(PeerRejectedEvent.class, { e ->
                PeerRejectedEvent rejectedEvent = (PeerRejectedEvent) e
                println("EVENT > CLIENT: CONNECTION REJECTED FROM " + rejectedEvent.getPeerAddress())
                if (rejectedEvent.getReason() == PeerRejectedEvent.RejectedReason.INTERNAL_ERROR) clientRejected.set(true)
                failureLatch.countDown()
            })

        when:

            client.start()
            client.openConnection(PeerAddress.fromIp("127.0.0.1:8100")) // dummy port
            boolean failed = failureLatch.await(5, TimeUnit.SECONDS)
            client.initiateStop()
            client.awaitStopped()

        then:
            failed
            clientRejected.get()
    }


}
