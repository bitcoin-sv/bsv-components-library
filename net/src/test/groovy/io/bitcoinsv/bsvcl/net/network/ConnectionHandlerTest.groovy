package io.bitcoinsv.bsvcl.net.network

import io.bitcoinsv.bsvcl.net.P2PConfig
import io.bitcoinsv.bsvcl.net.tools.P2PDefaultConfig
import io.bitcoinsv.bsvcl.net.network.events.PeerConnectedEvent
import io.bitcoinsv.bsvcl.net.network.events.PeerDisconnectedEvent
import io.bitcoinsv.bsvcl.net.network.events.PeerRejectedEvent
import io.bitcoinsv.bsvcl.common.config.RuntimeConfig
import io.bitcoinsv.bsvcl.common.config.provided.RuntimeConfigDefault
import io.bitcoinsv.bsvcl.common.events.EventBus
import io.bitcoinsv.bsvcl.common.files.FileUtilsBuilder
import io.bitcoinsv.bsvcl.common.thread.ThreadUtils
import groovy.util.logging.Slf4j
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

@Slf4j
class ConnectionHandlerTest extends Specification {

    /**
     * Testing a basic connection between 2 ConnectionHandlers. One works as a Sever (Accepting incoming Connections), the
     * other as a Client.
     */
    def "testing Server-Client Connection OK"() {
        given:
            // Each Connection Handler will run on one specific Thread.
            ExecutorService serverExecutor = ThreadUtils.getSingleThreadExecutorService("Server-EventBus-")
            ExecutorService clientExecutor = ThreadUtils.getSingleThreadExecutorService("Client-EventBus-")

            // Basic Configuration is the same for both of them...
            RuntimeConfig runtimeConfig = new RuntimeConfigDefault()
            runtimeConfig = runtimeConfig.toBuilder()
                    .fileUtils(new FileUtilsBuilder().build())
                    .build()
        P2PConfig networkConfig = new P2PDefaultConfig()

            // Each one has its own Event-Bus, for events and callbacks handling...
            EventBus serverEventBus = EventBus.builder().executor(serverExecutor).build()
            EventBus clientEventBus = EventBus.builder().executor(clientExecutor).build()

            // We initialize them:
        NetworkController server = new NetworkController("server", runtimeConfig, networkConfig, PeerAddress.localhost(0))
            server.useEventBus(serverEventBus)
            NetworkController client = new NetworkController("client", runtimeConfig, networkConfig, PeerAddress.localhost(0))
            client.useEventBus(clientEventBus)

            // We keep track of the events in these variables:
            AtomicBoolean serverConnected = new AtomicBoolean(false)
            AtomicBoolean serverDisconnected = new AtomicBoolean(false)

            AtomicBoolean clientConnected = new AtomicBoolean(false)
            AtomicBoolean clientDisconnected = new AtomicBoolean(false)

            CountDownLatch connectsLatch = new CountDownLatch(2)
            CountDownLatch disconnectsLatch = new CountDownLatch(2)

            // We provide some callbacks for the Server...
            serverEventBus.subscribe(PeerConnectedEvent.class, {e ->
                log.trace("EVENT > SERVER: CONNECTED TO " + e.getPeerAddress())
                serverConnected.set(true)
                connectsLatch.countDown()
            })
            serverEventBus.subscribe(PeerDisconnectedEvent.class, { e ->
                log.trace("EVENT > SERVER: DISCONNECTED FROM " + e.getPeerAddress())
                serverDisconnected.set(true)
                disconnectsLatch.countDown()
            })

            // we provide some callbacks for the Client
            clientEventBus.subscribe(PeerConnectedEvent.class, {e ->
                log.trace("EVENT > CLIENT: CONNECTED TO " + e.getPeerAddress())
                clientConnected.set(true)
                connectsLatch.countDown()
            })
            clientEventBus.subscribe(PeerDisconnectedEvent.class, { e ->
                log.trace("EVENT > CLIENT: DISCONNECTED FROM " + e.getPeerAddress())
                clientDisconnected.set(true)
                disconnectsLatch.countDown()
            })

        when:
            server.startServer()
            client.start()
            client.connect(server.getPeerAddress())
            connectsLatch.await()
            client.disconnect(server.getPeerAddress())
            disconnectsLatch.await()
            server.stop()
            client.stop()
            server.awaitStopped()
            client.awaitStopped()

        then:
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
            P2PConfig networkConfig = new P2PDefaultConfig()

            // Event-Bus, for events and callbacks handling...
            EventBus clientEventBus = EventBus.builder().executor(clientExecutor).build()

            // We initialize it:
            NetworkController client = new NetworkController("client", runtimeConfig, networkConfig, PeerAddress.localhost(0))
            client.useEventBus(clientEventBus)

            // We keep track of the events in these variables:

            AtomicBoolean clientRejected= new AtomicBoolean(false)
            CountDownLatch failureLatch = new CountDownLatch(1)

            // We provide some callbacks ...
            clientEventBus.subscribe(PeerRejectedEvent.class, { e ->
                PeerRejectedEvent rejectedEvent = (PeerRejectedEvent) e
                println("EVENT > CLIENT: CONNECTION REJECTED FROM " + rejectedEvent.getPeerAddress())
                if (rejectedEvent.getReason() == PeerRejectedEvent.RejectedReason.INTERNAL_ERROR) clientRejected.set(true)
                failureLatch.countDown()
            })

        when:

            client.start()
            client.connect(PeerAddress.fromIp("127.0.0.1:8100")) // dummy port
            failureLatch.await()
            client.stop()
            client.awaitStopped()

        then:
            clientRejected.get()
    }


}
