package com.nchain.jcl.network.unit.handlers

import com.nchain.jcl.network.PeerAddress
import com.nchain.jcl.network.config.NetworkConfig
import com.nchain.jcl.network.config.NetworkConfigDefault
import com.nchain.jcl.network.events.PeerConnectedEvent
import com.nchain.jcl.network.events.PeerDisconnectedEvent
import com.nchain.jcl.network.events.PeerRejectedEvent
import com.nchain.jcl.network.handlers.NetworkHandlerImpl
import com.nchain.jcl.network.handlers.NetworkHandler
import com.nchain.jcl.tools.config.RuntimeConfig
import com.nchain.jcl.tools.config.provided.RuntimeConfigDefault
import com.nchain.jcl.tools.events.EventBus
import com.nchain.jcl.tools.thread.ThreadUtils
import groovy.util.logging.Slf4j
import spock.lang.Specification

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
            NetworkConfig networkConfig = new NetworkConfigDefault()

            // Each one has its own Event-Bus, for events and callbacks handling...
            EventBus serverEventBus = EventBus.builder().executor(serverExecutor).build()
            EventBus clientEventBus = EventBus.builder().executor(clientExecutor).build()

            // We initialize them:
            NetworkHandler server = new NetworkHandlerImpl("server", runtimeConfig, networkConfig, PeerAddress.localhost(8333))
            server.useEventBus(serverEventBus)
            NetworkHandler client = new NetworkHandlerImpl("client", runtimeConfig, networkConfig, PeerAddress.localhost(8111))
            client.useEventBus(clientEventBus)

            // We keep track of the events in these variabes:
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
            // TODO: NOTS ABOUT THE ADDRESSES
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
            NetworkConfig networkConfig = new NetworkConfigDefault()

            // Event-Bus, for events and callbacks handling...
            EventBus clientEventBus = EventBus.builder().executor(clientExecutor).build()

            // We initialize it:
            NetworkHandler client = new NetworkHandlerImpl("client", runtimeConfig, networkConfig, PeerAddress.localhost(8111))
            client.useEventBus(clientEventBus)

            // We keep track of the events in these variabes:

            AtomicBoolean clientRejected= new AtomicBoolean(false)

            // We provide some callbacks ...
            clientEventBus.subscribe(PeerRejectedEvent.class, { e ->
                log.trace("EVENT > CLIENT: CONNECTION REJECTED FROM " + e.getPeerAddress())
                if (e.getReason() == PeerRejectedEvent.RejectedReason.INTERNAL_ERROR) clientRejected.set(true)
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
