package com.nchain.jcl.net.protocol.wrapper;

import com.nchain.jcl.base.tools.events.Event;
import com.nchain.jcl.base.tools.events.EventBus;
import com.nchain.jcl.net.network.events.*;
import com.nchain.jcl.net.protocol.events.*;
import com.nchain.jcl.net.protocol.messages.*;
import com.nchain.jcl.net.network.handlers.NetworkHandlerState;
import com.nchain.jcl.net.protocol.handlers.blacklist.BlacklistHandlerState;
import com.nchain.jcl.net.protocol.handlers.block.BlockDownloaderHandlerState;
import com.nchain.jcl.net.protocol.handlers.discovery.DiscoveryHandlerState;
import com.nchain.jcl.net.protocol.handlers.handshake.HandshakeHandlerState;
import com.nchain.jcl.net.protocol.handlers.message.MessageHandlerState;
import com.nchain.jcl.net.protocol.handlers.pingPong.PingPongHandlerState;
import lombok.AllArgsConstructor;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-06-30 12:32
 *
 * This class provides utilities for the User to get a Stream of different Events coming from The P2P,
 * and subscribe to them.
 *
 * All the possible Events that the P2P provides are broken down into different categories, and for each category
 * the User can subscribe to ALL the events or only to some of them (some sub-categories are provided that only applied
 * to specific Events).
 */
@AllArgsConstructor
public class P2PEventStreamer {

    // The same EventBus that is used by the underlying P2P
    private EventBus eventBus;

    /**
     * EvenStreamer
     *
     * The base class for subscribing to an Event Type. Using this class, you can provide a Consumer/Event Handler,
     * which will get triggered every time an Event of that type is published in the EventBus. You can also provide
     * a "filter" which will be executed BEFORE your Consumer, so the Events can be filtered out. This filter is being
     * already used by some "Streamer" classes below, to automatically filter out some Event Types.
     * @param <E>
     */
    @AllArgsConstructor
    public class EventStreamer<E extends Event> {
        private Class<E> eventClass;
        private Predicate<E> filter;
        public void filter(Predicate<E> filter) {
            this.filter = filter;
        }
        public void forEach(Consumer<E> eventHandler) {
            Consumer<E> eventHandlerToSubscribe = eventHandler;
            if (filter != null) {
                eventHandlerToSubscribe = e -> {
                  if (filter.test(e))
                      eventHandler.accept(e);
                };
            }
            eventBus.subscribe(eventClass, eventHandlerToSubscribe);
        }
    }


    /**
     * A convenience class that provides Event Stramers for "general" Events, not related to Peers or Msgs..
     */
    public class GeneralEventStreamer {
        public final EventStreamer<NetStartEvent>   START   = new EventStreamer<>(NetStartEvent.class, null);
        public final EventStreamer<NetStopEvent>    STOP    = new EventStreamer<>(NetStopEvent.class, null);
    }
    /**
     * A Convenience class that provides EventStreamers for Events related to Peers.
     * Some Streamer are already defined as final instances, so they can be used already to subscribe to specific
     * Peer Events. The ALL Streamer, on the other hand, applies to all Events that have to do with Peers. To do
     * that, the ALL Streamer is linked to the generic "Event", and uses a Filter to filter in only those Events
     * related to Peers.
     */
    public class PeersEventStreamer {
        private final Predicate<Event> ALL_FILTER = e ->
                ((e instanceof PeerConnectedEvent)
                        || (e instanceof PeerDisconnectedEvent)
                        || (e instanceof PingPongFailedEvent)
                        || (e instanceof PeersBlacklistedEvent)
                        || (e instanceof PeersWhitelistedEvent)
                        || (e instanceof PeerHandshakedEvent)
                        || (e instanceof PeerHandshakedDisconnectedEvent)
                        || (e instanceof PeerHandshakeRejectedEvent)
                        || (e instanceof MinHandshakedPeersReachedEvent)
                        || (e instanceof MinHandshakedPeersLostEvent)
                        || (e instanceof InitialPeersLoadedEvent)
                );

        public final EventStreamer<Event>                       ALL             = new EventStreamer<Event>(Event.class, ALL_FILTER);
        public final EventStreamer<PeerConnectedEvent>          CONNECTED       = new EventStreamer<>(PeerConnectedEvent.class, null);
        public final EventStreamer<PeerDisconnectedEvent>       DISCONNECTED    = new EventStreamer<>(PeerDisconnectedEvent.class, null);
        public final EventStreamer<PingPongFailedEvent>         PINGPONG_FAILED = new EventStreamer<>(PingPongFailedEvent.class, null);
        public final EventStreamer<PeersBlacklistedEvent>       BLACKLISTED     = new EventStreamer<>(PeersBlacklistedEvent.class, null);
        public final EventStreamer<PeersWhitelistedEvent>       WHITELISTED     = new EventStreamer<>(PeersWhitelistedEvent.class, null);
        public final EventStreamer<PeerHandshakedEvent>         HANDSHAKED      = new EventStreamer<>(PeerHandshakedEvent.class, null);
        public final EventStreamer<PeerHandshakedDisconnectedEvent> HANDSHAKED_DISCONNECTED     = new EventStreamer<>(PeerHandshakedDisconnectedEvent.class, null);
        public final EventStreamer<PeerHandshakeRejectedEvent>      HANDSHAKED_REJECTED         = new EventStreamer<>(PeerHandshakeRejectedEvent.class, null);
        public final EventStreamer<MinHandshakedPeersReachedEvent>  HANDSHAKED_MIN_REACHED      = new EventStreamer<>(MinHandshakedPeersReachedEvent.class, null);
        public final EventStreamer<MinHandshakedPeersLostEvent>     HANDSHAKED_MIN_LOST         = new EventStreamer<>(MinHandshakedPeersLostEvent.class, null);
        public final EventStreamer<InitialPeersLoadedEvent>         INITIAL_PEERS_LOADED        = new EventStreamer<>(InitialPeersLoadedEvent.class, null);



    }

    /**
     * A convenience Class that provides EventStreamers for different kind of Messages.
     * All the incoming Messages are publishing using the same Event (MsgReceivedEvent), so those Streamers defined
     * below that only applied to specific messages are using internally a "filter", to filter in messages.
     */
    public class MsgsEventStreamer {
        private Predicate<MsgReceivedEvent> getFilterForMsgReceived(String msgType) {
            return e -> e.getBtcMsg().getHeader().getCommand().equalsIgnoreCase(msgType);
        }
        private Predicate<MsgSentEvent> getFilterForMsgSent(String msgType) {
            return e -> e.getBtcMsg().getHeader().getCommand().equalsIgnoreCase(msgType);
        }
        public final EventStreamer<MsgReceivedEvent> ALL            = new EventStreamer<>(MsgReceivedEvent.class, null);
        public final EventStreamer<MsgReceivedEvent> ADDR           = new EventStreamer<>(MsgReceivedEvent.class, getFilterForMsgReceived(AddrMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgReceivedEvent> BLOCK          = new EventStreamer<>(MsgReceivedEvent.class, getFilterForMsgReceived(BlockMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgReceivedEvent> FEE            = new EventStreamer<>(MsgReceivedEvent.class, getFilterForMsgReceived(FeeFilterMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgReceivedEvent> GETADDR        = new EventStreamer<>(MsgReceivedEvent.class, getFilterForMsgReceived(GetAddrMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgReceivedEvent> GETDATA        = new EventStreamer<>(MsgReceivedEvent.class, getFilterForMsgReceived(GetdataMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgReceivedEvent> INV            = new EventStreamer<>(MsgReceivedEvent.class, getFilterForMsgReceived(InvMessage.MESSAGE_TYPE));
        public final EventStreamer<MsgReceivedEvent> NOTFOUND       = new EventStreamer<>(MsgReceivedEvent.class, getFilterForMsgReceived(NotFoundMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgReceivedEvent> PING           = new EventStreamer<>(MsgReceivedEvent.class, getFilterForMsgReceived(PingMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgReceivedEvent> PONG           = new EventStreamer<>(MsgReceivedEvent.class, getFilterForMsgReceived(PongMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgReceivedEvent> REJECT         = new EventStreamer<>(MsgReceivedEvent.class, getFilterForMsgReceived(RejectMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgReceivedEvent> TX             = new EventStreamer<>(MsgReceivedEvent.class, getFilterForMsgReceived(TransactionMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgReceivedEvent> VERSION        = new EventStreamer<>(MsgReceivedEvent.class, getFilterForMsgReceived(VersionMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgReceivedEvent> VERSIONACK     = new EventStreamer<>(MsgReceivedEvent.class, getFilterForMsgReceived(VersionAckMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgReceivedEvent> GETHEADERS     = new EventStreamer<>(MsgReceivedEvent.class, getFilterForMsgReceived(GetHeadersMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgReceivedEvent> SENDHEADERS    = new EventStreamer<>(MsgReceivedEvent.class, getFilterForMsgReceived(SendHeadersMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgReceivedEvent> MEMPOOL        = new EventStreamer<>(MsgReceivedEvent.class, getFilterForMsgReceived(MemPoolMsg.MESSAGE_TYPE));

        public final EventStreamer<MsgSentEvent> ALL_SENT           = new EventStreamer<>(MsgSentEvent.class, null);
        public final EventStreamer<MsgSentEvent> ADDR_SENT          = new EventStreamer<>(MsgSentEvent.class, getFilterForMsgSent(AddrMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgSentEvent> BLOCK_SENT         = new EventStreamer<>(MsgSentEvent.class, getFilterForMsgSent(BlockMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgSentEvent> FEE_SENT           = new EventStreamer<>(MsgSentEvent.class, getFilterForMsgSent(FeeFilterMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgSentEvent> GETADDR_SENT       = new EventStreamer<>(MsgSentEvent.class, getFilterForMsgSent(GetAddrMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgSentEvent> GETDATA_SENT       = new EventStreamer<>(MsgSentEvent.class, getFilterForMsgSent(GetdataMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgSentEvent> INV_SENT           = new EventStreamer<>(MsgSentEvent.class, getFilterForMsgSent(InvMessage.MESSAGE_TYPE));
        public final EventStreamer<MsgSentEvent> NOTFOUND_SENT      = new EventStreamer<>(MsgSentEvent.class, getFilterForMsgSent(NotFoundMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgSentEvent> PING_SENT          = new EventStreamer<>(MsgSentEvent.class, getFilterForMsgSent(PingMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgSentEvent> PONG_SENT          = new EventStreamer<>(MsgSentEvent.class, getFilterForMsgSent(PongMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgSentEvent> REJECT_SENT        = new EventStreamer<>(MsgSentEvent.class, getFilterForMsgSent(RejectMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgSentEvent> TX_SENT            = new EventStreamer<>(MsgSentEvent.class, getFilterForMsgSent(TransactionMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgSentEvent> VERSION_SENT       = new EventStreamer<>(MsgSentEvent.class, getFilterForMsgSent(VersionMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgSentEvent> VERSIONACK_SENT    = new EventStreamer<>(MsgSentEvent.class, getFilterForMsgSent(VersionAckMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgSentEvent> GETHEADERS_SENT    = new EventStreamer<>(MsgSentEvent.class, getFilterForMsgSent(GetHeadersMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgSentEvent> SENDHEADERS_SENT   = new EventStreamer<>(MsgSentEvent.class, getFilterForMsgSent(SendHeadersMsg.MESSAGE_TYPE));
        public final EventStreamer<MsgSentEvent> MEMPOOL_SENT       = new EventStreamer<>(MsgSentEvent.class, getFilterForMsgSent(MemPoolMsg.MESSAGE_TYPE));
    }

    /**
     * A convenience class that provides EventStreamer for the States returned by the different Handlers used by the
     * P2P Class.
     */
    public class StateEventStreamer {
        private Predicate<HandlerStateEvent> getFilterForHandler(Class handlerStateClass) {
            return e -> handlerStateClass.isInstance(e.getState());
        }

        public final EventStreamer<HandlerStateEvent> ALL        = new EventStreamer<>(HandlerStateEvent.class, null);
        public final EventStreamer<HandlerStateEvent> NETWORK    = new EventStreamer<>(HandlerStateEvent.class, getFilterForHandler(NetworkHandlerState.class));
        public final EventStreamer<HandlerStateEvent> MESSAGES   = new EventStreamer<>(HandlerStateEvent.class, getFilterForHandler(MessageHandlerState.class));
        public final EventStreamer<HandlerStateEvent> HANDSHAKE  = new EventStreamer<>(HandlerStateEvent.class, getFilterForHandler(HandshakeHandlerState.class));
        public final EventStreamer<HandlerStateEvent> PINGPONG   = new EventStreamer<>(HandlerStateEvent.class, getFilterForHandler(PingPongHandlerState.class));
        public final EventStreamer<HandlerStateEvent> DISCOVERY  = new EventStreamer<>(HandlerStateEvent.class, getFilterForHandler(DiscoveryHandlerState.class));
        public final EventStreamer<HandlerStateEvent> BLACKLIST  = new EventStreamer<>(HandlerStateEvent.class, getFilterForHandler(BlacklistHandlerState.class));
        public final EventStreamer<HandlerStateEvent> BLOCKS     = new EventStreamer<>(HandlerStateEvent.class, getFilterForHandler(BlockDownloaderHandlerState.class));
    }

    /**
     * A convenience class that provides Event Streamer specific for Event triggered by the Block downloader Handler
     */
    public class BlockEventStreamer {
        public final EventStreamer<LiteBlockDownloadedEvent>    LITE_BLOCK_DOWNLOADED   = new EventStreamer<>(LiteBlockDownloadedEvent.class, null);
        public final EventStreamer<BlockDownloadedEvent>        BLOCK_DOWNLOADED        = new EventStreamer<>(BlockDownloadedEvent.class, null);
        public final EventStreamer<BlockDiscardedEvent>         BLOCK_DISCARDED         = new EventStreamer<>(BlockDiscardedEvent.class, null);
        public final EventStreamer<BlockHeaderDownloadedEvent>  BLOCK_HEADER_DOWNLOADED = new EventStreamer<>(BlockHeaderDownloadedEvent.class, null);
        public final EventStreamer<BlockTXsDownloadedEvent>     BLOCK_TXS_DOWNLOADED    = new EventStreamer<>(BlockTXsDownloadedEvent.class, null);
    }

    // Definition of the different built-in EventStreamer classes:
    public final GeneralEventStreamer   GENERAL = new GeneralEventStreamer();
    public final PeersEventStreamer     PEERS   = new PeersEventStreamer();
    public final MsgsEventStreamer      MSGS    = new MsgsEventStreamer();
    public final StateEventStreamer     STATE   = new StateEventStreamer();
    public final BlockEventStreamer     BLOCKS  = new BlockEventStreamer();
}
