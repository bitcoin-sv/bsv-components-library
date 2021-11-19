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

    // Default Number of Threads to use depending on the Event type:
    private static final int DEFAULT_NUM_THREADS_GENERAL    = 1;
    private static final int DEFAULT_NUM_THREADS_GENERIC    = 50; // ONLY FOR TESTING
    private static final int DEFAULT_NUM_THREADS_PEERS      = 1;
    private static final int DEFAULT_NUM_THREADS_MSGS       = 50;
    private static final int DEFAULT_NUM_THREADS_STATE      = 10;
    private static final int DEFAULT_NUM_THREADS_BLOCK      = 10;

    // The same EventBus that is used by the underlying P2P
    private EventBus eventBus;


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
        public final EventStreamer<NetStartEvent> START   = new EventStreamer<>(eventBus, NetStartEvent.class, numThreads);
        public final EventStreamer<NetStopEvent>  STOP    = new EventStreamer<>(eventBus, NetStopEvent.class, numThreads);
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
        public final EventStreamer<PeerConnectedEvent>          CONNECTED       = new EventStreamer<>(eventBus, PeerConnectedEvent.class, numThreads);
        public final EventStreamer<PeerDisconnectedEvent>       DISCONNECTED    = new EventStreamer<>(eventBus, PeerDisconnectedEvent.class, numThreads);
        public final EventStreamer<PingPongFailedEvent>         PINGPONG_FAILED = new EventStreamer<>(eventBus, PingPongFailedEvent.class, numThreads);
        public final EventStreamer<PeersBlacklistedEvent>       BLACKLISTED     = new EventStreamer<>(eventBus, PeersBlacklistedEvent.class, numThreads);
        public final EventStreamer<PeersWhitelistedEvent>       WHITELISTED     = new EventStreamer<>(eventBus, PeersWhitelistedEvent.class, numThreads);
        public final EventStreamer<PeerHandshakedEvent>         HANDSHAKED      = new EventStreamer<>(eventBus, PeerHandshakedEvent.class, numThreads);
        public final EventStreamer<PeerHandshakedDisconnectedEvent> HANDSHAKED_DISCONNECTED     = new EventStreamer<>(eventBus, PeerHandshakedDisconnectedEvent.class, numThreads);
        public final EventStreamer<PeerHandshakeRejectedEvent>      HANDSHAKED_REJECTED         = new EventStreamer<>(eventBus, PeerHandshakeRejectedEvent.class, numThreads);
        public final EventStreamer<MinHandshakedPeersReachedEvent>  HANDSHAKED_MIN_REACHED      = new EventStreamer<>(eventBus, MinHandshakedPeersReachedEvent.class, numThreads);
        public final EventStreamer<MaxHandshakedPeersReachedEvent>  HANDSHAKED_MAX_REACHED      = new EventStreamer<>(eventBus, MaxHandshakedPeersReachedEvent.class, numThreads);
        public final EventStreamer<MinHandshakedPeersLostEvent>     HANDSHAKED_MIN_LOST         = new EventStreamer<>(eventBus, MinHandshakedPeersLostEvent.class, numThreads);
        public final EventStreamer<InitialPeersLoadedEvent>         INITIAL_PEERS_LOADED        = new EventStreamer<>(eventBus, InitialPeersLoadedEvent.class, numThreads);
        public final EventStreamer<PeerRejectedEvent>               PEER_REJECTED               = new EventStreamer<>(eventBus, PeerRejectedEvent.class, numThreads);

    }

    /**
     * A convenience Class that provides EventStreamers for different kind of Messages.
     * All the incoming Messages are publishing using the same Event (MsgReceivedEvent), so those Streamers defined
     * below that only applied to specific messages are using internally a "filter", to filter in messages.
     */
    public class MsgsEventStreamer extends BaseEventStreamer{
        public MsgsEventStreamer(int numThreads) { super(numThreads);}
        public final EventStreamer<MsgReceivedEvent>                            ALL                 = new EventStreamer<>(eventBus, MsgReceivedEvent.class, numThreads);
        public final EventStreamer<VersionMsgReceivedEvent>                     VERSION             = new EventStreamer<>(eventBus, VersionMsgReceivedEvent.class, numThreads);
        public final EventStreamer<VersionAckMsgReceivedEvent>                  VERSIONACK          = new EventStreamer<>(eventBus, VersionAckMsgReceivedEvent.class, numThreads);
        public final EventStreamer<AddrMsgReceivedEvent>                        ADDR                = new EventStreamer<>(eventBus, AddrMsgReceivedEvent.class, numThreads);
        public final EventStreamer<BlockMsgReceivedEvent>                       BLOCK               = new EventStreamer<>(eventBus, BlockMsgReceivedEvent.class, numThreads);
        public final EventStreamer<CompactBlockMsgReceivedEvent>                CMPCTBLOCK          = new EventStreamer<>(eventBus, CompactBlockMsgReceivedEvent.class, numThreads);
        public final EventStreamer<SendCompactBlockMsgReceivedEvent>            SENDCMPCT           = new EventStreamer<>(eventBus, SendCompactBlockMsgReceivedEvent.class, numThreads);
        public final EventStreamer<GetBlockTxnMsgReceivedEvent>                 GETBLOCKTXN         = new EventStreamer<>(eventBus, GetBlockTxnMsgReceivedEvent.class, numThreads);
        public final EventStreamer<BlockTxnMsgReceivedEvent>                    BLOCKTXN            = new EventStreamer<>(eventBus, BlockTxnMsgReceivedEvent.class, numThreads);
        public final EventStreamer<FeeMsgReceivedEvent>                         FEE                 = new EventStreamer<>(eventBus, FeeMsgReceivedEvent.class, numThreads);
        public final EventStreamer<GetAddrMsgReceivedEvent>                     GETADDR             = new EventStreamer<>(eventBus, GetAddrMsgReceivedEvent.class, numThreads);
        public final EventStreamer<GetDataMsgReceivedEvent>                     GETDATA             = new EventStreamer<>(eventBus, GetDataMsgReceivedEvent.class, numThreads);
        public final EventStreamer<InvMsgReceivedEvent>                         INV                 = new EventStreamer<>(eventBus, InvMsgReceivedEvent.class, numThreads);
        public final EventStreamer<NotFoundMsgReceivedEvent>                    NOTFOUND            = new EventStreamer<>(eventBus, NotFoundMsgReceivedEvent.class, numThreads);
        public final EventStreamer<PingMsgReceivedEvent>                        PING                = new EventStreamer<>(eventBus, PingMsgReceivedEvent.class, numThreads);
        public final EventStreamer<PongMsgReceivedEvent>                        PONG                = new EventStreamer<>(eventBus, PongMsgReceivedEvent.class, numThreads);
        public final EventStreamer<RejectMsgReceivedEvent>                      REJECT              = new EventStreamer<>(eventBus, RejectMsgReceivedEvent.class, numThreads);
        public final EventStreamer<TxMsgReceivedEvent>                          TX                  = new EventStreamer<>(eventBus, TxMsgReceivedEvent.class, numThreads);
        public final EventStreamer<RawTxMsgReceivedEvent>                       TX_RAW              = new EventStreamer<>(eventBus, RawTxMsgReceivedEvent.class, numThreads);
        public final EventStreamer<GetHeadersMsgReceivedEvent>                  GETHEADERS          = new EventStreamer<>(eventBus, GetHeadersMsgReceivedEvent.class, numThreads);
        public final EventStreamer<SendHeadersMsgReceivedEvent>                 SENDHEADERS         = new EventStreamer<>(eventBus, SendHeadersMsgReceivedEvent.class, numThreads);
        public final EventStreamer<HeadersMsgReceivedEvent>                     HEADERS             = new EventStreamer<>(eventBus, HeadersMsgReceivedEvent.class, numThreads);
        public final EventStreamer<MempoolMsgReceivedEvent>                     MEMPOOL             = new EventStreamer<>(eventBus, MempoolMsgReceivedEvent.class, numThreads);
        public final EventStreamer<GetHeadersEnMsgReceivedEvent>                GETHEADERSEN        = new EventStreamer<>(eventBus, GetHeadersEnMsgReceivedEvent.class, numThreads);
        public final EventStreamer<PartialBlockTxnDownloadedEvent>              PARTIAL_BLOCKTXN    = new EventStreamer<>(eventBus, PartialBlockTxnDownloadedEvent.class, numThreads);
        public final EventStreamer<TxsBatchMsgReceivedEvent>                    TX_BATCH            = new EventStreamer<>(eventBus, TxsBatchMsgReceivedEvent.class, numThreads);
        public final EventStreamer<RawTxsBatchMsgReceivedEvent>                 TX_RAW_BATCH        = new EventStreamer<>(eventBus, RawTxsBatchMsgReceivedEvent.class, numThreads);

        public final EventStreamer<MsgSentEvent>                                ALL_SENT            = new EventStreamer<>(eventBus, MsgSentEvent.class, numThreads);
        public final EventStreamer<VersionMsgSentEvent>                         VERSION_SENT        = new EventStreamer<>(eventBus, VersionMsgSentEvent.class, numThreads);
        public final EventStreamer<VersionAckMsgSentEvent>                      VERSIONACK_SENT     = new EventStreamer<>(eventBus, VersionAckMsgSentEvent.class, numThreads);
        public final EventStreamer<AddrMsgSentEvent>                            ADDR_SENT           = new EventStreamer<>(eventBus, AddrMsgSentEvent.class, numThreads);
        public final EventStreamer<BlockMsgSentEvent>                           BLOCK_SENT          = new EventStreamer<>(eventBus, BlockMsgSentEvent.class, numThreads);
        public final EventStreamer<CompactBlockMsgSentEvent>                    CMPCTBLOCK_SENT     = new EventStreamer<>(eventBus, CompactBlockMsgSentEvent.class, numThreads);
        public final EventStreamer<GetBlockTxnMsgSentEvent>                     GETBLOCKTXN_SENT    = new EventStreamer<>(eventBus, GetBlockTxnMsgSentEvent.class, numThreads);
        public final EventStreamer<BlockTxnMsgSentEvent>                        BLOCKTXN_SENT       = new EventStreamer<>(eventBus, BlockTxnMsgSentEvent.class, numThreads);
        public final EventStreamer<SendCompactBlockMsgSentEvent>                SENDCMPCT_SENT      = new EventStreamer<>(eventBus, SendCompactBlockMsgSentEvent.class, numThreads);
        public final EventStreamer<FeeMsgSentEvent>                             FEE_SENT            = new EventStreamer<>(eventBus, FeeMsgSentEvent.class, numThreads);
        public final EventStreamer<GetAddrMsgSentEvent>                         GETADDR_SENT        = new EventStreamer<>(eventBus, GetAddrMsgSentEvent.class, numThreads);
        public final EventStreamer<GetDataMsgSentEvent>                         GETDATA_SENT        = new EventStreamer<>(eventBus, GetDataMsgSentEvent.class, numThreads);
        public final EventStreamer<InvMsgSentEvent>                             INV_SENT            = new EventStreamer<>(eventBus, InvMsgSentEvent.class, numThreads);
        public final EventStreamer<NotFoundMsgSentEvent>                        NOTFOUND_SENT       = new EventStreamer<>(eventBus, NotFoundMsgSentEvent.class, numThreads);
        public final EventStreamer<PingMsgSentEvent>                            PING_SENT           = new EventStreamer<>(eventBus, PingMsgSentEvent.class, numThreads);
        public final EventStreamer<PongMsgSentEvent>                            PONG_SENT           = new EventStreamer<>(eventBus, PongMsgSentEvent.class, numThreads);
        public final EventStreamer<RejectMsgSentEvent>                          REJECT_SENT         = new EventStreamer<>(eventBus, RejectMsgSentEvent.class, numThreads);
        public final EventStreamer<TxMsgSentEvent>                              TX_SENT             = new EventStreamer<>(eventBus, TxMsgSentEvent.class, numThreads);
        public final EventStreamer<GetHeadersMsgSentEvent>                      GETHEADERS_SENT     = new EventStreamer<>(eventBus, GetHeadersMsgSentEvent.class, numThreads);
        public final EventStreamer<SendHeadersMsgSentEvent>                     SENDHEADERS_SENT    = new EventStreamer<>(eventBus, SendHeadersMsgSentEvent.class, numThreads);
        public final EventStreamer<HeadersMsgSentEvent>                         HEADERS_SENT        = new EventStreamer<>(eventBus, HeadersMsgSentEvent.class, numThreads);
        public final EventStreamer<MempoolMsgSentEvent>                         MEMPOOL_SENT        = new EventStreamer<>(eventBus, MempoolMsgSentEvent.class, numThreads);
        public final EventStreamer<GetHeadersEnMsgSentEvent>                    GETHEADERSEN_SENT   = new EventStreamer<>(eventBus, GetHeadersEnMsgSentEvent.class, numThreads);
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

        public final EventStreamer<HandlerStateEvent> ALL        = new EventStreamer<>(eventBus, HandlerStateEvent.class, numThreads);
        public final EventStreamer<HandlerStateEvent> NETWORK    = new EventStreamer<>(eventBus, HandlerStateEvent.class, getFilterForHandler(NetworkHandlerState.class), numThreads);
        public final EventStreamer<HandlerStateEvent> MESSAGES   = new EventStreamer<>(eventBus, HandlerStateEvent.class, getFilterForHandler(MessageHandlerState.class), numThreads);
        public final EventStreamer<HandlerStateEvent> HANDSHAKE  = new EventStreamer<>(eventBus, HandlerStateEvent.class, getFilterForHandler(HandshakeHandlerState.class), numThreads);
        public final EventStreamer<HandlerStateEvent> PINGPONG   = new EventStreamer<>(eventBus, HandlerStateEvent.class, getFilterForHandler(PingPongHandlerState.class), numThreads);
        public final EventStreamer<HandlerStateEvent> DISCOVERY  = new EventStreamer<>(eventBus, HandlerStateEvent.class, getFilterForHandler(DiscoveryHandlerState.class), numThreads);
        public final EventStreamer<HandlerStateEvent> BLACKLIST  = new EventStreamer<>(eventBus, HandlerStateEvent.class, getFilterForHandler(BlacklistHandlerState.class), numThreads);
        public final EventStreamer<HandlerStateEvent> BLOCKS     = new EventStreamer<>(eventBus, HandlerStateEvent.class, getFilterForHandler(BlockDownloaderHandlerState.class), numThreads);
    }

    /**
     * A convenience class that provides Event Streamer specific for Event triggered by the Block downloader Handler
     */
    public class BlockEventStreamer extends BaseEventStreamer {
        public BlockEventStreamer(int numThreads) { super(numThreads);}
        public final EventStreamer<LiteBlockDownloadedEvent>    LITE_BLOCK_DOWNLOADED       = new EventStreamer<>(eventBus, LiteBlockDownloadedEvent.class, numThreads);
        public final EventStreamer<BlockDownloadedEvent>        BLOCK_DOWNLOADED            = new EventStreamer<>(eventBus, BlockDownloadedEvent.class, numThreads);
        public final EventStreamer<BlockDiscardedEvent>         BLOCK_DISCARDED             = new EventStreamer<>(eventBus, BlockDiscardedEvent.class, numThreads);
        public final EventStreamer<BlockHeaderDownloadedEvent>  BLOCK_HEADER_DOWNLOADED     = new EventStreamer<>(eventBus, BlockHeaderDownloadedEvent.class, numThreads);
        public final EventStreamer<BlockTXsDownloadedEvent>     BLOCK_TXS_DOWNLOADED        = new EventStreamer<>(eventBus, BlockTXsDownloadedEvent.class, numThreads);
        public final EventStreamer<BlockRawTXsDownloadedEvent>  BLOCK_RAW_TXS_DOWNLOADED    = new EventStreamer<>(eventBus, BlockRawTXsDownloadedEvent.class, numThreads);
    }

    /**
     * A convenience class that provides Event Streamers for ANY Event
     */
    public class GenericEventStreamer extends BaseEventStreamer {
        private EventBus eventBus;
        public GenericEventStreamer(int numThreads, EventBus eventBus) {
            super(numThreads);
            this.eventBus = eventBus;
        }
        public void forEach(Class<? extends P2PEvent> eventClass, Consumer<? extends P2PEvent> eventHandler) {
            eventBus.subscribe(eventClass, eventHandler);
        }
    }

    // Methods to returns builtin EventStreams with a custom number of Threads:
    public GenericEventStreamer GENERIC(int numThreads) { return new GenericEventStreamer(numThreads, eventBus);}
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
    public void forEach(Class<? extends P2PEvent> eventClass, Consumer<? extends P2PEvent> eventHandler) {
        GENERIC.forEach(eventClass, eventHandler);
    }

    /** Constructor */
    public P2PEventStreamer(EventBus eventBus) {
        this.eventBus   = eventBus;
        this.GENERIC    = new GenericEventStreamer(DEFAULT_NUM_THREADS_GENERIC, eventBus);
        this.GENERAL    = new GeneralEventStreamer(DEFAULT_NUM_THREADS_GENERAL);
        this.PEERS      = new PeersEventStreamer(DEFAULT_NUM_THREADS_PEERS);
        this.MSGS       = new MsgsEventStreamer(DEFAULT_NUM_THREADS_MSGS);
        this.STATE      = new StateEventStreamer(DEFAULT_NUM_THREADS_STATE);
        this.BLOCKS     = new BlockEventStreamer(DEFAULT_NUM_THREADS_BLOCK);
    }
}
