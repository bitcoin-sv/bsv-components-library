package io.bitcoinsv.jcl.net.unit.network.handlers

import io.bitcoinsv.jcl.net.network.PeerAddress
import io.bitcoinsv.jcl.net.network.config.NetworkConfig
import io.bitcoinsv.jcl.net.network.config.provided.NetworkDefaultConfig
import io.bitcoinsv.jcl.net.network.events.PeerConnectedEvent
import io.bitcoinsv.jcl.net.network.events.PeerDisconnectedEvent
import io.bitcoinsv.jcl.net.network.events.PeerRejectedEvent
import io.bitcoinsv.jcl.net.network.handlers.NetworkHandler
import io.bitcoinsv.jcl.net.network.handlers.NetworkHandlerImpl
import io.bitcoinsv.jcl.tools.config.RuntimeConfig
import io.bitcoinsv.jcl.tools.config.provided.RuntimeConfigDefault
import io.bitcoinsv.jcl.tools.events.EventBus
import io.bitcoinsv.jcl.tools.files.FileUtilsBuilder
import io.bitcoinsv.jcl.tools.thread.ThreadUtils
import groovy.util.logging.Slf4j
import spock.lang.Ignore
import spock.lang.Specification

import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

@Slf4j
@Ignore("This test is not working yet. It needs to be fixed.")  // todo: fix this test
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
            NetworkConfig networkConfig = new NetworkDefaultConfig()

            // Each one has its own Event-Bus, for events and callbacks handling...
            EventBus serverEventBus = EventBus.builder().executor(serverExecutor).build()
            EventBus clientEventBus = EventBus.builder().executor(clientExecutor).build()

            // We initialize them:
            NetworkHandler server = new NetworkHandlerImpl("server", runtimeConfig, networkConfig, PeerAddress.localhost(0))
            server.useEventBus(serverEventBus)
            NetworkHandler client = new NetworkHandlerImpl("client", runtimeConfig, networkConfig, PeerAddress.localhost(0))
            client.useEventBus(clientEventBus)

            // We keep track of the events in these variables:
            AtomicBoolean serverConnected = new AtomicBoolean(false)
            AtomicBoolean serverDisconnected = new AtomicBoolean(false)

            AtomicBoolean clientConnected = new AtomicBoolean(false)
            AtomicBoolean clientDisconnected = new AtomicBoolean(false)

            // We provide some callbacks for the Server...
            serverEventBus.subscribe(PeerConnectedEvent.class, {e ->
                log.trace("EVENT > SERVER: CONNECTED TO " + e.getPeerAddress())
                serverConnected.set(true)
            })
            serverEventBus.subscribe(PeerDisconnectedEvent.class, { e ->
                log.trace("EVENT > SERVER: DISCONNECTED FROM " + e.getPeerAddress())
                serverDisconnected.set(true)
            })

            // we provide some callbacks for the Client
            clientEventBus.subscribe(PeerConnectedEvent.class, {e ->
                log.trace("EVENT > CLIENT: CONNECTED TO " + e.getPeerAddress())
                clientConnected.set(true)
            })
            clientEventBus.subscribe(PeerDisconnectedEvent.class, { e ->
                log.trace("EVENT > CLIENT: DISCONNECTED FROM " + e.getPeerAddress())
                clientDisconnected.set(true)
            })

        when:
            // TODO: NOTES ABOUT THE ADDRESSES
            server.startServer()
            client.start()
            client.connect(server.getPeerAddress())
            Thread.sleep(1_000)
            client.disconnect(server.getPeerAddress())
            Thread.sleep(1_000)
            server.stop()
            client.stop()
            Thread.sleep(1_000)
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
            NetworkConfig networkConfig = new NetworkDefaultConfig()

            // Event-Bus, for events and callbacks handling...
            EventBus clientEventBus = EventBus.builder().executor(clientExecutor).build()

            // We initialize it:
            NetworkHandler client = new NetworkHandlerImpl("client", runtimeConfig, networkConfig, PeerAddress.localhost(0))
            client.useEventBus(clientEventBus)

            // We keep track of the events in these variables:

            AtomicBoolean clientRejected= new AtomicBoolean(false)

            // We provide some callbacks ...
            clientEventBus.subscribe(PeerRejectedEvent.class, { e ->
                PeerRejectedEvent rejectedEvent = (PeerRejectedEvent) e;
                println("EVENT > CLIENT: CONNECTION REJECTED FROM " + rejectedEvent.getPeerAddress())
                if (rejectedEvent.getReason() == PeerRejectedEvent.RejectedReason.INTERNAL_ERROR) clientRejected.set(true)
            })

        when:

            client.start()
            client.connect(PeerAddress.fromIp("127.0.0.1:8100")) // dummy port
            Thread.sleep(1_000)
            client.stop()
        then:
            clientRejected.get()
    }


}
