package io.bitcoinsv.jcl.net.protocol.wrapper;

import io.bitcoinsv.jcl.net.network.events.*;
import io.bitcoinsv.jcl.net.protocol.events.control.*;
import io.bitcoinsv.jcl.net.protocol.events.data.*;
import io.bitcoinsv.jcl.net.network.handlers.NetworkHandlerState;
import io.bitcoinsv.jcl.net.protocol.handlers.blacklist.BlacklistHandlerState;
import io.bitcoinsv.jcl.net.protocol.handlers.block.BlockDownloaderHandlerState;
import io.bitcoinsv.jcl.net.protocol.handlers.discovery.DiscoveryHandlerState;
import io.bitcoinsv.jcl.net.protocol.handlers.handshake.HandshakeHandlerState;
import io.bitcoinsv.jcl.net.protocol.handlers.message.MessageHandlerState;
import io.bitcoinsv.jcl.net.protocol.handlers.pingPong.PingPongHandlerState;
import io.bitcoinsv.jcl.tools.events.EventBus;
import io.bitcoinsv.jcl.tools.events.EventStreamer;

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

    // Default Number of Threads to use depending on the Event type:
    private static final int DEFAULT_NUM_THREADS_GENERAL    = 1;
    private static final int DEFAULT_NUM_THREADS_GENERIC    = 50; // ONLY FOR TESTING
    private static final int DEFAULT_NUM_THREADS_PEERS      = 10;
    private static final int DEFAULT_NUM_THREADS_MSGS       = 10;
    private static final int DEFAULT_NUM_THREADS_STATE      = 1;
    private static final int DEFAULT_NUM_THREADS_BLOCK      = 5;

    // The same EventBus that is used by the underlying P2P
    private EventBus eventBus;

    // An specific Bus for the Handler State Events:
    private EventBus stateEventBus;

    /** Base class for the Event Streamers */
    public class BaseEventStreamer {
        protected int numThreads;
        public BaseEventStreamer(int numThreads) { this.numThreads = numThreads;}
    }

    /**
     * A convenience class that provides Event Streamers for "general" Events, not related to Peers or Msgs..
     */
    public class GeneralEventStreamer extends BaseEventStreamer {
        public GeneralEventStreamer(int numThreads) { super(numThreads);}
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
    public class PeersEventStreamer extends BaseEventStreamer {
        public PeersEventStreamer(int numThreads) { super(numThreads);}
        public final EventStreamer<PeerConnectedEvent>              CONNECTED                   = new EventStreamer<>(eventBus, PeerConnectedEvent.class);
        public final EventStreamer<PeerDisconnectedEvent>           DISCONNECTED                = new EventStreamer<>(eventBus, PeerDisconnectedEvent.class);
        public final EventStreamer<PingPongFailedEvent>             PINGPONG_FAILED             = new EventStreamer<>(eventBus, PingPongFailedEvent.class);
        public final EventStreamer<PeersBlacklistedEvent>           BLACKLISTED                 = new EventStreamer<>(eventBus, PeersBlacklistedEvent.class);
        public final EventStreamer<PeersRemovedFromBlacklistEvent>  REMOVED_FROM_BLACKLIST      = new EventStreamer<>(eventBus, PeersRemovedFromBlacklistEvent.class);
        public final EventStreamer<PeersWhitelistedEvent>           WHITELISTED                 = new EventStreamer<>(eventBus, PeersWhitelistedEvent.class);
        public final EventStreamer<PeersRemovedFromWhitelistEvent>  REMOVED_FROM_WHITELIST      = new EventStreamer<>(eventBus, PeersRemovedFromWhitelistEvent.class);
        public final EventStreamer<PeerHandshakedEvent>             HANDSHAKED                  = new EventStreamer<>(eventBus, PeerHandshakedEvent.class);
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
    public class MsgsEventStreamer extends BaseEventStreamer{
        public MsgsEventStreamer(int numThreads) { super(numThreads);}
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
        public final EventStreamer<RawTxMsgReceivedEvent>                       TX_RAW              = new EventStreamer<>(eventBus, RawTxMsgReceivedEvent.class);
        public final EventStreamer<GetHeadersMsgReceivedEvent>                  GETHEADERS          = new EventStreamer<>(eventBus, GetHeadersMsgReceivedEvent.class);
        public final EventStreamer<SendHeadersMsgReceivedEvent>                 SENDHEADERS         = new EventStreamer<>(eventBus, SendHeadersMsgReceivedEvent.class);
        public final EventStreamer<HeadersMsgReceivedEvent>                     HEADERS             = new EventStreamer<>(eventBus, HeadersMsgReceivedEvent.class);
        public final EventStreamer<MempoolMsgReceivedEvent>                     MEMPOOL             = new EventStreamer<>(eventBus, MempoolMsgReceivedEvent.class);
        public final EventStreamer<GetHeadersEnMsgReceivedEvent>                GETHEADERSEN        = new EventStreamer<>(eventBus, GetHeadersEnMsgReceivedEvent.class);
        public final EventStreamer<PartialBlockTxnDownloadedEvent>              PARTIAL_BLOCKTXN    = new EventStreamer<>(eventBus, PartialBlockTxnDownloadedEvent.class);
        public final EventStreamer<TxsBatchMsgReceivedEvent>                    TX_BATCH            = new EventStreamer<>(eventBus, TxsBatchMsgReceivedEvent.class);
        public final EventStreamer<RawTxsBatchMsgReceivedEvent>                 TX_RAW_BATCH        = new EventStreamer<>(eventBus, RawTxsBatchMsgReceivedEvent.class);

        public final EventStreamer<MsgSentEvent>                                ALL_SENT            = new EventStreamer<>(eventBus, MsgSentEvent.class);
        public final EventStreamer<VersionMsgSentEvent>                         VERSION_SENT        = new EventStreamer<>(eventBus, VersionMsgSentEvent.class);
        public final EventStreamer<VersionAckMsgSentEvent>                      VERSIONACK_SENT     = new EventStreamer<>(eventBus, VersionAckMsgSentEvent.class);
        public final EventStreamer<AddrMsgSentEvent>                            ADDR_SENT           = new EventStreamer<>(eventBus, AddrMsgSentEvent.class);
        public final EventStreamer<BlockMsgSentEvent>                           BLOCK_SENT          = new EventStreamer<>(eventBus, BlockMsgSentEvent.class);
        public final EventStreamer<CompactBlockMsgSentEvent>                    CMPCTBLOCK_SENT     = new EventStreamer<>(eventBus, CompactBlockMsgSentEvent.class);
        public final EventStreamer<GetBlockTxnMsgSentEvent>                     GETBLOCKTXN_SENT    = new EventStreamer<>(eventBus, GetBlockTxnMsgSentEvent.class);
        public final EventStreamer<BlockTxnMsgSentEvent>                        BLOCKTXN_SENT       = new EventStreamer<>(eventBus, BlockTxnMsgSentEvent.class);
        public final EventStreamer<SendCompactBlockMsgSentEvent>                SENDCMPCT_SENT      = new EventStreamer<>(eventBus, SendCompactBlockMsgSentEvent.class);
        public final EventStreamer<FeeMsgSentEvent>                             FEE_SENT            = new EventStreamer<>(eventBus, FeeMsgSentEvent.class);
        public final EventStreamer<GetAddrMsgSentEvent>                         GETADDR_SENT        = new EventStreamer<>(eventBus, GetAddrMsgSentEvent.class);
        public final EventStreamer<GetDataMsgSentEvent>                         GETDATA_SENT        = new EventStreamer<>(eventBus, GetDataMsgSentEvent.class);
        public final EventStreamer<InvMsgSentEvent>                             INV_SENT            = new EventStreamer<>(eventBus, InvMsgSentEvent.class);
        public final EventStreamer<NotFoundMsgSentEvent>                        NOTFOUND_SENT       = new EventStreamer<>(eventBus, NotFoundMsgSentEvent.class);
        public final EventStreamer<PingMsgSentEvent>                            PING_SENT           = new EventStreamer<>(eventBus, PingMsgSentEvent.class);
        public final EventStreamer<PongMsgSentEvent>                            PONG_SENT           = new EventStreamer<>(eventBus, PongMsgSentEvent.class);
        public final EventStreamer<RejectMsgSentEvent>                          REJECT_SENT         = new EventStreamer<>(eventBus, RejectMsgSentEvent.class);
        public final EventStreamer<TxMsgSentEvent>                              TX_SENT             = new EventStreamer<>(eventBus, TxMsgSentEvent.class);
        public final EventStreamer<GetHeadersMsgSentEvent>                      GETHEADERS_SENT     = new EventStreamer<>(eventBus, GetHeadersMsgSentEvent.class);
        public final EventStreamer<SendHeadersMsgSentEvent>                     SENDHEADERS_SENT    = new EventStreamer<>(eventBus, SendHeadersMsgSentEvent.class);
        public final EventStreamer<HeadersMsgSentEvent>                         HEADERS_SENT        = new EventStreamer<>(eventBus, HeadersMsgSentEvent.class);
        public final EventStreamer<MempoolMsgSentEvent>                         MEMPOOL_SENT        = new EventStreamer<>(eventBus, MempoolMsgSentEvent.class);
        public final EventStreamer<GetHeadersEnMsgSentEvent>                    GETHEADERSEN_SENT   = new EventStreamer<>(eventBus, GetHeadersEnMsgSentEvent.class);
        public final EventStreamer<CompactBlockTransactionsMsgReceivedEvent>    CMPCTBLCKTXS        = new EventStreamer<>(eventBus, CompactBlockTransactionsMsgReceivedEvent.class);
        public final EventStreamer<CompactBlockTransactionsMsgSentEvent>        CMPCTBLCKTXS_SENT   = new EventStreamer<>(eventBus, CompactBlockTransactionsMsgSentEvent.class);
    }

    /**
     * A convenience class that provides EventStreamer for the States returned by the different Handlers used by the
     * P2P Class.
     */
    public class StateEventStreamer extends BaseEventStreamer {
        public StateEventStreamer(int numThreads) { super(numThreads);}
        private Predicate<HandlerStateEvent> getFilterForHandler(Class handlerStateClass) {
            return e -> handlerStateClass.isInstance(e.getState());
        }

        public final EventStreamer<HandlerStateEvent> ALL        = new EventStreamer<>(stateEventBus, HandlerStateEvent.class);
        public final EventStreamer<HandlerStateEvent> NETWORK    = new EventStreamer<>(stateEventBus, HandlerStateEvent.class, getFilterForHandler(NetworkHandlerState.class));
        public final EventStreamer<HandlerStateEvent> MESSAGES   = new EventStreamer<>(stateEventBus, HandlerStateEvent.class, getFilterForHandler(MessageHandlerState.class));
        public final EventStreamer<HandlerStateEvent> HANDSHAKE  = new EventStreamer<>(stateEventBus, HandlerStateEvent.class, getFilterForHandler(HandshakeHandlerState.class));
        public final EventStreamer<HandlerStateEvent> PINGPONG   = new EventStreamer<>(stateEventBus, HandlerStateEvent.class, getFilterForHandler(PingPongHandlerState.class));
        public final EventStreamer<HandlerStateEvent> DISCOVERY  = new EventStreamer<>(stateEventBus, HandlerStateEvent.class, getFilterForHandler(DiscoveryHandlerState.class));
        public final EventStreamer<HandlerStateEvent> BLACKLIST  = new EventStreamer<>(stateEventBus, HandlerStateEvent.class, getFilterForHandler(BlacklistHandlerState.class));
        public final EventStreamer<HandlerStateEvent> BLOCKS     = new EventStreamer<>(stateEventBus, HandlerStateEvent.class, getFilterForHandler(BlockDownloaderHandlerState.class));
    }

    /**
     * A convenience class that provides Event Streamer specific for Event triggered by the Block downloader Handler
     */
    public class BlockEventStreamer extends BaseEventStreamer {
        public BlockEventStreamer(int numThreads) { super(numThreads);}
        public final EventStreamer<LiteBlockDownloadedEvent>    LITE_BLOCK_DOWNLOADED       = new EventStreamer<>(eventBus, LiteBlockDownloadedEvent.class);
        public final EventStreamer<BlockDownloadedEvent>        BLOCK_DOWNLOADED            = new EventStreamer<>(eventBus, BlockDownloadedEvent.class);
        public final EventStreamer<BlockDiscardedEvent>         BLOCK_DISCARDED             = new EventStreamer<>(eventBus, BlockDiscardedEvent.class);
        public final EventStreamer<BlockHeaderDownloadedEvent>  BLOCK_HEADER_DOWNLOADED     = new EventStreamer<>(eventBus, BlockHeaderDownloadedEvent.class);
        public final EventStreamer<BlockTXsDownloadedEvent>     BLOCK_TXS_DOWNLOADED        = new EventStreamer<>(eventBus, BlockTXsDownloadedEvent.class);
        public final EventStreamer<BlockRawTXsDownloadedEvent>  BLOCK_RAW_TXS_DOWNLOADED    = new EventStreamer<>(eventBus, BlockRawTXsDownloadedEvent.class);
    }

    /**
     * A convenience class that provides Event Streamers for ANY Event
     */
    public class GenericEventStreamer extends BaseEventStreamer {
        public GenericEventStreamer(int numThreads) {
            super(numThreads);
        }
        public void forEach(Class<P2PEvent> eventClass, Consumer<P2PEvent> eventHandler) {
            eventBus.subscribe(eventClass, eventHandler);
            if (HandlerStateEvent.class.isAssignableFrom(eventClass)) {
                stateEventBus.subscribe(eventClass, eventHandler);
            }
        }
    }

    // Methods to returns builtin EventStreams with a custom number of Threads:
    public GenericEventStreamer GENERIC(int numThreads) { return new GenericEventStreamer(numThreads);}
    public GeneralEventStreamer GENERAL(int numThreads) { return new GeneralEventStreamer(numThreads);}
    public PeersEventStreamer   PEERS(int numThreads)   { return new PeersEventStreamer(numThreads);}
    public MsgsEventStreamer    MSGS(int numThreads)    { return new MsgsEventStreamer(numThreads);}
    public StateEventStreamer   STATE(int numThreads)   { return new StateEventStreamer(numThreads);}
    public BlockEventStreamer   BLOCKS(int numThreads)  { return new BlockEventStreamer(numThreads);}

    // Definition of the different built-in EventStreamer classes with default number of threads:
    private final GenericEventStreamer  GENERIC;
    public final GeneralEventStreamer   GENERAL;
    public final PeersEventStreamer     PEERS;
    public final MsgsEventStreamer      MSGS;
    public final StateEventStreamer     STATE;
    public final BlockEventStreamer     BLOCKS ;

    /** It allows to subscribe to any Event specified as a parameter */
    public void forEach(Class<P2PEvent> eventClass, Consumer<P2PEvent> eventHandler) {
        GENERIC.forEach(eventClass, eventHandler);
    }

    /** Constructor */
    public P2PEventStreamer(EventBus eventBus, EventBus stateEventBus) {
        this.eventBus       = eventBus;
        this.stateEventBus  = stateEventBus;


        this.GENERIC    = new GenericEventStreamer(DEFAULT_NUM_THREADS_GENERIC);
        this.GENERAL    = new GeneralEventStreamer(DEFAULT_NUM_THREADS_GENERAL);
        this.PEERS      = new PeersEventStreamer(DEFAULT_NUM_THREADS_PEERS);
        this.MSGS       = new MsgsEventStreamer(DEFAULT_NUM_THREADS_MSGS);
        this.STATE      = new StateEventStreamer(DEFAULT_NUM_THREADS_STATE);
        this.BLOCKS     = new BlockEventStreamer(DEFAULT_NUM_THREADS_BLOCK);
    }
}