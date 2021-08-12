package com.nchain.jcl.net.protocol.wrapper;

import com.nchain.jcl.net.network.events.*;
import com.nchain.jcl.net.protocol.events.control.*;
import com.nchain.jcl.net.protocol.events.data.*;
import com.nchain.jcl.net.network.handlers.NetworkHandlerState;
import com.nchain.jcl.net.protocol.handlers.blacklist.BlacklistHandlerState;
import com.nchain.jcl.net.protocol.handlers.block.BlockDownloaderHandlerState;
import com.nchain.jcl.net.protocol.handlers.discovery.DiscoveryHandlerState;
import com.nchain.jcl.net.protocol.handlers.handshake.HandshakeHandlerState;
import com.nchain.jcl.net.protocol.handlers.message.MessageHandlerState;
import com.nchain.jcl.net.protocol.handlers.pingPong.PingPongHandlerState;
import com.nchain.jcl.tools.events.EventBus;
import com.nchain.jcl.tools.events.EventStreamer;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class provides utilities for the User to get a Stream of different Events coming from The P2P,
 * and subscribe to them.
 *
 * All the possible Events that the P2P provides are broken down into different categories, and for each category
 * the User can subscribe to ALL the events or only to some of them (some sub-categories are provided that only applied
 * to specific Events).
 */
public class P2PEventStreamer {

    // The same EventBus that is used by the underlying P2P
    private EventBus eventBus;

    /**
     * A convenience class that provides Event Stramers for "general" Events, not related to Peers or Msgs..
     */
    public class GeneralEventStreamer {
        public final EventStreamer<NetStartEvent> START   = new EventStreamer<>(eventBus, NetStartEvent.class);
        public final EventStreamer<NetStopEvent>  STOP    = new EventStreamer<>(eventBus, NetStopEvent.class);
    }

    /**
     * A Convenience class that provides EventStreamers for Events related to Peers.
     * Some Streamer are already defined as final instances, so they can be used already to subscribe to specific
     * Peer Events. The ALL Streamer, on the other hand, applies to all Events that have to do with Peers. To do
     * that, the ALL Streamer is linked to the generic "Event", and uses a Filter to filter in only those Events
     * related to Peers.
     */
    public class PeersEventStreamer {
        public final EventStreamer<PeerConnectedEvent>          CONNECTED       = new EventStreamer<>(eventBus, PeerConnectedEvent.class);
        public final EventStreamer<PeerDisconnectedEvent>       DISCONNECTED    = new EventStreamer<>(eventBus, PeerDisconnectedEvent.class);
        public final EventStreamer<PingPongFailedEvent>         PINGPONG_FAILED = new EventStreamer<>(eventBus, PingPongFailedEvent.class);
        public final EventStreamer<PeersBlacklistedEvent>       BLACKLISTED     = new EventStreamer<>(eventBus, PeersBlacklistedEvent.class);
        public final EventStreamer<PeersWhitelistedEvent>       WHITELISTED     = new EventStreamer<>(eventBus, PeersWhitelistedEvent.class);
        public final EventStreamer<PeerHandshakedEvent>         HANDSHAKED      = new EventStreamer<>(eventBus, PeerHandshakedEvent.class);
        public final EventStreamer<PeerHandshakedDisconnectedEvent> HANDSHAKED_DISCONNECTED     = new EventStreamer<>(eventBus, PeerHandshakedDisconnectedEvent.class);
        public final EventStreamer<PeerHandshakeRejectedEvent>      HANDSHAKED_REJECTED         = new EventStreamer<>(eventBus, PeerHandshakeRejectedEvent.class);
        public final EventStreamer<MinHandshakedPeersReachedEvent>  HANDSHAKED_MIN_REACHED      = new EventStreamer<>(eventBus, MinHandshakedPeersReachedEvent.class);
        public final EventStreamer<MaxHandshakedPeersReachedEvent>  HANDSHAKED_MAX_REACHED      = new EventStreamer<>(eventBus, MaxHandshakedPeersReachedEvent.class);
        public final EventStreamer<MinHandshakedPeersLostEvent>     HANDSHAKED_MIN_LOST         = new EventStreamer<>(eventBus, MinHandshakedPeersLostEvent.class);
        public final EventStreamer<InitialPeersLoadedEvent>         INITIAL_PEERS_LOADED        = new EventStreamer<>(eventBus, InitialPeersLoadedEvent.class);
        public final EventStreamer<PeerRejectedEvent>               PEER_REJECTED               = new EventStreamer<>(eventBus, PeerRejectedEvent.class);

    }

    /**
     * A convenience Class that provides EventStreamers for different kind of Messages.
     * All the incoming Messages are publishing using the same Event (MsgReceivedEvent), so those Streamers defined
     * below that only applied to specific messages are using internally a "filter", to filter in messages.
     */
    public class MsgsEventStreamer {

        public final EventStreamer<MsgReceivedEvent>                            ALL                 = new EventStreamer<>(eventBus, MsgReceivedEvent.class);
        public final EventStreamer<VersionMsgReceivedEvent>                     VERSION             = new EventStreamer<>(eventBus, VersionMsgReceivedEvent.class);
        public final EventStreamer<VersionAckMsgReceivedEvent>                  VERSIONACK          = new EventStreamer<>(eventBus, VersionAckMsgReceivedEvent.class);
        public final EventStreamer<AddrMsgReceivedEvent>                        ADDR                = new EventStreamer<>(eventBus, AddrMsgReceivedEvent.class);
        public final EventStreamer<BlockMsgReceivedEvent>                       BLOCK               = new EventStreamer<>(eventBus, BlockMsgReceivedEvent.class);
        public final EventStreamer<CompactBlockMsgReceivedEvent>                CMPCTBLOCK          = new EventStreamer<>(eventBus, CompactBlockMsgReceivedEvent.class);
        public final EventStreamer<SendCompactBlockMsgReceivedEvent>            SENDCMPCT           = new EventStreamer<>(eventBus, SendCompactBlockMsgReceivedEvent.class);
        public final EventStreamer<GetBlockTxnMsgReceivedEvent>                 GETBLOCKTXN         = new EventStreamer<>(eventBus, GetBlockTxnMsgReceivedEvent.class);
        public final EventStreamer<BlockTxnMsgReceivedEvent>                    BLOCKTXN            = new EventStreamer<>(eventBus, BlockTxnMsgReceivedEvent.class);
        public final EventStreamer<FeeMsgReceivedEvent>                         FEE                 = new EventStreamer<>(eventBus, FeeMsgReceivedEvent.class);
        public final EventStreamer<GetAddrMsgReceivedEvent>                     GETADDR             = new EventStreamer<>(eventBus, GetAddrMsgReceivedEvent.class);
        public final EventStreamer<GetDataMsgReceivedEvent>                     GETDATA             = new EventStreamer<>(eventBus, GetDataMsgReceivedEvent.class);
        public final EventStreamer<InvMsgReceivedEvent>                         INV                 = new EventStreamer<>(eventBus, InvMsgReceivedEvent.class);
        public final EventStreamer<NotFoundMsgReceivedEvent>                    NOTFOUND            = new EventStreamer<>(eventBus, NotFoundMsgReceivedEvent.class);
        public final EventStreamer<PingMsgReceivedEvent>                        PING                = new EventStreamer<>(eventBus, PingMsgReceivedEvent.class);
        public final EventStreamer<PongMsgReceivedEvent>                        PONG                = new EventStreamer<>(eventBus, PongMsgReceivedEvent.class);
        public final EventStreamer<RejectMsgReceivedEvent>                      REJECT              = new EventStreamer<>(eventBus, RejectMsgReceivedEvent.class);
        public final EventStreamer<TxMsgReceivedEvent>                          TX                  = new EventStreamer<>(eventBus, TxMsgReceivedEvent.class);
        public final EventStreamer<GetHeadersMsgReceivedEvent>                  GETHEADERS          = new EventStreamer<>(eventBus, GetHeadersMsgReceivedEvent.class);
        public final EventStreamer<SendHeadersMsgReceivedEvent>                 SENDHEADERS         = new EventStreamer<>(eventBus, SendHeadersMsgReceivedEvent.class);
        public final EventStreamer<HeadersMsgReceivedEvent>                     HEADERS             = new EventStreamer<>(eventBus, HeadersMsgReceivedEvent.class);
        public final EventStreamer<MempoolMsgReceivedEvent>                     MEMPOOL             = new EventStreamer<>(eventBus, MempoolMsgReceivedEvent.class);
        public final EventStreamer<GetHeadersEnMsgReceivedEvent>                GETHEADERSEN        = new EventStreamer<>(eventBus, GetHeadersEnMsgReceivedEvent.class);
        public final EventStreamer<PartialBlockTxnDownloadedEvent>              PARTIAL_BLOCKTXN    = new EventStreamer<>(eventBus, PartialBlockTxnDownloadedEvent.class);


        public final EventStreamer<RawTxMsgReceivedEvent>                   TX_RAW          = new EventStreamer<>(eventBus, RawTxMsgReceivedEvent.class);


        public final EventStreamer<MsgSentEvent>                            ALL_SENT            = new EventStreamer<>(eventBus, MsgSentEvent.class);
        public final EventStreamer<VersionMsgSentEvent>                     VERSION_SENT        = new EventStreamer<>(eventBus, VersionMsgSentEvent.class);
        public final EventStreamer<VersionAckMsgSentEvent>                  VERSIONACK_SENT     = new EventStreamer<>(eventBus, VersionAckMsgSentEvent.class);
        public final EventStreamer<AddrMsgSentEvent>                        ADDR_SENT           = new EventStreamer<>(eventBus, AddrMsgSentEvent.class);
        public final EventStreamer<BlockMsgSentEvent>                       BLOCK_SENT          = new EventStreamer<>(eventBus, BlockMsgSentEvent.class);
        public final EventStreamer<CompactBlockMsgSentEvent>                CMPCTBLOCK_SENT     = new EventStreamer<>(eventBus, CompactBlockMsgSentEvent.class);
        public final EventStreamer<GetBlockTxnMsgSentEvent>                 GETBLOCKTXN_SENT    = new EventStreamer<>(eventBus, GetBlockTxnMsgSentEvent.class);
        public final EventStreamer<BlockTxnMsgSentEvent>                    BLOCKTXN_SENT       = new EventStreamer<>(eventBus, BlockTxnMsgSentEvent.class);
        public final EventStreamer<SendCompactBlockMsgSentEvent>            SENDCMPCT_SENT      = new EventStreamer<>(eventBus, SendCompactBlockMsgSentEvent.class);
        public final EventStreamer<FeeMsgSentEvent>                         FEE_SENT            = new EventStreamer<>(eventBus, FeeMsgSentEvent.class);
        public final EventStreamer<GetAddrMsgSentEvent>                     GETADDR_SENT        = new EventStreamer<>(eventBus, GetAddrMsgSentEvent.class);
        public final EventStreamer<GetDataMsgSentEvent>                     GETDATA_SENT        = new EventStreamer<>(eventBus, GetDataMsgSentEvent.class);
        public final EventStreamer<InvMsgSentEvent>                         INV_SENT            = new EventStreamer<>(eventBus, InvMsgSentEvent.class);
        public final EventStreamer<NotFoundMsgSentEvent>                    NOTFOUND_SENT       = new EventStreamer<>(eventBus, NotFoundMsgSentEvent.class);
        public final EventStreamer<PingMsgSentEvent>                        PING_SENT           = new EventStreamer<>(eventBus, PingMsgSentEvent.class);
        public final EventStreamer<PongMsgSentEvent>                        PONG_SENT           = new EventStreamer<>(eventBus, PongMsgSentEvent.class);
        public final EventStreamer<RejectMsgSentEvent>                      REJECT_SENT         = new EventStreamer<>(eventBus, RejectMsgSentEvent.class);
        public final EventStreamer<TxMsgSentEvent>                          TX_SENT             = new EventStreamer<>(eventBus, TxMsgSentEvent.class);

        public final EventStreamer<GetHeadersMsgSentEvent>          GETHEADERS_SENT = new EventStreamer<>(eventBus, GetHeadersMsgSentEvent.class);
        public final EventStreamer<SendHeadersMsgSentEvent>         SENDHEADERS_SENT= new EventStreamer<>(eventBus, SendHeadersMsgSentEvent.class);
        public final EventStreamer<HeadersMsgSentEvent>             HEADERS_SENT    = new EventStreamer<>(eventBus, HeadersMsgSentEvent.class);
        public final EventStreamer<MempoolMsgSentEvent>             MEMPOOL_SENT    = new EventStreamer<>(eventBus, MempoolMsgSentEvent.class);
        public final EventStreamer<GetHeadersEnMsgSentEvent>        GETHEADERSEN_SENT  = new EventStreamer<>(eventBus, GetHeadersEnMsgSentEvent.class);
    }

    /**
     * A convenience class that provides EventStreamer for the States returned by the different Handlers used by the
     * P2P Class.
     */
    public class StateEventStreamer {
        private Predicate<HandlerStateEvent> getFilterForHandler(Class handlerStateClass) {
            return e -> handlerStateClass.isInstance(e.getState());
        }

        public final EventStreamer<HandlerStateEvent> ALL        = new EventStreamer<>(eventBus, HandlerStateEvent.class);
        public final EventStreamer<HandlerStateEvent> NETWORK    = new EventStreamer<>(eventBus, HandlerStateEvent.class, getFilterForHandler(NetworkHandlerState.class));
        public final EventStreamer<HandlerStateEvent> MESSAGES   = new EventStreamer<>(eventBus, HandlerStateEvent.class, getFilterForHandler(MessageHandlerState.class));
        public final EventStreamer<HandlerStateEvent> HANDSHAKE  = new EventStreamer<>(eventBus, HandlerStateEvent.class, getFilterForHandler(HandshakeHandlerState.class));
        public final EventStreamer<HandlerStateEvent> PINGPONG   = new EventStreamer<>(eventBus, HandlerStateEvent.class, getFilterForHandler(PingPongHandlerState.class));
        public final EventStreamer<HandlerStateEvent> DISCOVERY  = new EventStreamer<>(eventBus, HandlerStateEvent.class, getFilterForHandler(DiscoveryHandlerState.class));
        public final EventStreamer<HandlerStateEvent> BLACKLIST  = new EventStreamer<>(eventBus, HandlerStateEvent.class, getFilterForHandler(BlacklistHandlerState.class));
        public final EventStreamer<HandlerStateEvent> BLOCKS     = new EventStreamer<>(eventBus, HandlerStateEvent.class, getFilterForHandler(BlockDownloaderHandlerState.class));
    }

    /**
     * A convenience class that provides Event Streamer specific for Event triggered by the Block downloader Handler
     */
    public class BlockEventStreamer {
        public final EventStreamer<LiteBlockDownloadedEvent>    LITE_BLOCK_DOWNLOADED       = new EventStreamer<>(eventBus, LiteBlockDownloadedEvent.class);
        public final EventStreamer<BlockDownloadedEvent>        BLOCK_DOWNLOADED            = new EventStreamer<>(eventBus, BlockDownloadedEvent.class);
        public final EventStreamer<BlockDiscardedEvent>         BLOCK_DISCARDED             = new EventStreamer<>(eventBus, BlockDiscardedEvent.class);
        public final EventStreamer<BlockHeaderDownloadedEvent>  BLOCK_HEADER_DOWNLOADED     = new EventStreamer<>(eventBus, BlockHeaderDownloadedEvent.class);
        public final EventStreamer<BlockTXsDownloadedEvent>     BLOCK_TXS_DOWNLOADED        = new EventStreamer<>(eventBus, BlockTXsDownloadedEvent.class);
        public final EventStreamer<BlockRawDataDownloadedEvent> BLOCK_RAW_DATA_DOWNLOADED = new EventStreamer<>(eventBus, BlockRawDataDownloadedEvent.class);
        public final EventStreamer<BlockRawTXsDownloadedEvent> BLOCK_RAW_TXS_DOWNLOADED = new EventStreamer<>(eventBus, BlockRawTXsDownloadedEvent.class);
    }

    /**
     * A convenience class that provides Event Stramers for ANY Event
     */
    public class GenericEventStreamer {
        private EventBus eventBus;
        public GenericEventStreamer(EventBus eventBus) {
            this.eventBus = eventBus;
        }
        public void forEach(Class<? extends P2PEvent> eventClass, Consumer<? extends P2PEvent> eventHandler) {
            eventBus.subscribe(eventClass, eventHandler);
        }
    }

    // Definition of the different built-in EventStreamer classes:
    private final GenericEventStreamer  GENERIC;
    public final GeneralEventStreamer   GENERAL;
    public final PeersEventStreamer     PEERS;
    public final MsgsEventStreamer      MSGS;
    public final StateEventStreamer     STATE;
    public final BlockEventStreamer     BLOCKS ;

    /** It allows to subscribe to any Event specified as a parameter */
    public void forEach(Class<? extends P2PEvent> eventClass, Consumer<? extends P2PEvent> eventHandler) {
        GENERIC.forEach(eventClass, eventHandler);
    }

    /** Constructor */
    public P2PEventStreamer(EventBus eventBus) {
        this.eventBus   = eventBus;
        this.GENERIC    = new GenericEventStreamer(eventBus);
        this.GENERAL    = new GeneralEventStreamer();
        this.PEERS      = new PeersEventStreamer();
        this.MSGS       = new MsgsEventStreamer();
        this.STATE      = new StateEventStreamer();
        this.BLOCKS     = new BlockEventStreamer();
    }
}
