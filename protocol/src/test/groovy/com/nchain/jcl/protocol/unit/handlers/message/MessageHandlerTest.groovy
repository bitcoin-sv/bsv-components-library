package com.nchain.jcl.protocol.unit.handlers.message

import com.nchain.jcl.network.PeerAddress
import com.nchain.jcl.network.config.NetworkConfig
import com.nchain.jcl.network.config.NetworkConfigDefault
import com.nchain.jcl.network.handlers.NetworkHandler
import com.nchain.jcl.network.handlers.NetworkHandlerImpl
import com.nchain.jcl.protocol.config.ProtocolBasicConfig
import com.nchain.jcl.protocol.config.ProtocolConfig
import com.nchain.jcl.protocol.config.provided.ProtocolBSVMainConfig
import com.nchain.jcl.protocol.events.MsgReceivedEvent
import com.nchain.jcl.protocol.events.PeerMsgReadyEvent
import com.nchain.jcl.protocol.handlers.message.MessageHandler
import com.nchain.jcl.protocol.handlers.message.MessageHandlerConfig
import com.nchain.jcl.protocol.handlers.message.MessageHandlerImpl
import com.nchain.jcl.protocol.messages.AddrMsg
import com.nchain.jcl.protocol.messages.common.BitcoinMsg
import com.nchain.jcl.protocol.streams.MessageStream
import com.nchain.jcl.protocol.unit.tools.MsgTest
import com.nchain.jcl.tools.config.RuntimeConfig
import com.nchain.jcl.tools.config.provided.RuntimeConfigDefault
import com.nchain.jcl.tools.events.EventBus
import com.nchain.jcl.tools.thread.ThreadUtils
import spock.lang.Specification

import java.util.concurrent.ExecutorService

/**
 * Testing class for the Message Handler
 */
class MessageHandlerTest extends Specification {


    /**
     * We test that a Ser and a Client connect to each other, and they exchange a Message, which is
     * Serialized/Deserialized properly by the Message Handler.
     *
     * NOTE: In this test we are using the low-level classes directly: The individual Handlers, the EventBus and
     * we wire them all together. In further tests we'll use the more convenient "protocol wrapper.
     */
    def "Test Message Serialization OK"() {
        given:
            // We capture the MessageStreams that are produced when Client and Server Connect to each other;
            // Since we can only use final Variables in lambda or closure statements, we use a List instead of
            // individual objects
            List<MessageStream> serverStream = new ArrayList<>()
            List<MessageStream> clientStream = new ArrayList<>()

            // We store the message exchange (sent from the client to the Server)
            List<BitcoinMsg<?>> msgs = new ArrayList<>()

            // Basic Configurations:
            RuntimeConfig runtimeConfig = new RuntimeConfigDefault()
            ProtocolConfig serverProtocolConfig = new ProtocolBSVMainConfig().toBuilder().port(0).build()
            ProtocolConfig clientProtocolConfig = new ProtocolBSVMainConfig().toBuilder().port(0).build()
            NetworkConfig networkConfig = new NetworkConfigDefault()

            // Server Configuration:
            String serverID = "server"
            ExecutorService serverExecutor = ThreadUtils.getSingleThreadExecutorService("ServerBus")
            EventBus serverBus = new EventBus(serverExecutor)
            serverBus.subscribe(MsgReceivedEvent.class, {e -> msgs.add(e.getBtcMsg())})

            NetworkHandler serverNetworkHandler = new NetworkHandlerImpl(serverID, runtimeConfig, networkConfig,
                PeerAddress.localhost(0))
            serverNetworkHandler.useEventBus(serverBus)

            MessageHandlerConfig serverMsgConfig = serverProtocolConfig.getMessageConfig()
            MessageHandler serverMsgHandler = new MessageHandlerImpl(serverID, runtimeConfig, serverMsgConfig)
            serverMsgHandler.useEventBus(serverBus)
            serverMsgHandler.init()

            serverBus.subscribe(PeerMsgReadyEvent.class,
                    {e -> serverStream.add(e.getStream())})

            // Client Configuration:
            String clientID = "client"
            ExecutorService clientExecutor = ThreadUtils.getSingleThreadExecutorService("ClientBus")
            EventBus clientBus = new EventBus(clientExecutor)

            NetworkHandler clientNetworkHandler = new NetworkHandlerImpl(clientID, runtimeConfig, networkConfig,
                PeerAddress.localhost(0))
            clientNetworkHandler.useEventBus(clientBus)

            MessageHandlerConfig clientMsgConfig = clientProtocolConfig.getMessageConfig()
            MessageHandler clientMsgHandler = new MessageHandlerImpl(clientID, runtimeConfig, clientMsgConfig)
            clientMsgHandler.useEventBus(clientBus)
            clientMsgHandler.init()

            clientBus.subscribe(PeerMsgReadyEvent.class,
                    {e -> clientStream.add(e.getStream())})

        when:
            // We start both and we connect them together...

            serverNetworkHandler.startServer()
            clientNetworkHandler.start()

            clientNetworkHandler.connect(serverNetworkHandler.getPeerAddress())

            // We wait a little bit, to make sure we have captured both Streams. now, we use one of them to send one
            // Message to the other...
            Thread.sleep(1000)

            // We send a Message from the client to the Server...
            BitcoinMsg<AddrMsg> msg = MsgTest.getAddrMsg()
            clientMsgHandler.send(serverNetworkHandler.getPeerAddress(), msg)

            Thread.sleep(1000)

            // We are Done...
            serverNetworkHandler.stop()
            clientNetworkHandler.stop()

        then:
            // We check that the Message has been captured, and its the same (After Deserialization) as the original
            // one:
            msgs.get(0).equals(msg)
    }
}
