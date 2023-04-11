package io.bitcoinsv.bsvcl.net.protocol.wrapper.receivers;

import com.google.common.base.Preconditions;
import io.bitcoinsv.bsvcl.net.protocol.handlers.message.MessageHandler;
import io.bitcoinsv.bsvcl.net.protocol.handlers.message.MessageHandlerConfig;
import io.bitcoinsv.bsvcl.net.protocol.messages.BlockHeaderMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.RawTxMsg;
import io.bitcoinsv.bsvcl.net.protocol.serialization.BlockHeaderMsgSerializer;
import io.bitcoinsv.bsvcl.net.protocol.serialization.RawBlockMsgSerializer;
import io.bitcoinsv.bsvcl.net.protocol.serialization.RawTxMsgSerializer;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.bsvcl.common.bigObjects.receivers.*;
import io.bitcoinsv.bsvcl.net.protocol.wrapper.P2P;
import io.bitcoinsv.bsvcl.common.bigObjects.BigCollectionChunk;
import io.bitcoinsv.bsvcl.common.bigObjects.BigCollectionChunkImpl;
import io.bitcoinsv.bsvcl.common.bigObjects.stores.ObjectSerializer;
import io.bitcoinsv.bsvcl.common.bigObjects.stores.ObjectStore;
import io.bitcoinsv.bsvcl.common.bigObjects.stores.ObjectStoreCMap;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.common.bytes.ByteArrayWriter;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This component implements a Big Object Receiver specialized in Raw Blocks. A Row Block is made up of:
 *  - A Header (BlockHeaderMsg)
 *  - A list of RAW Txs (RawTxMsg)
 *
 *  This receiver receives the blocks from the network. It uses a P2P Service which carries oput all the Network
 *  Connectivity. So once the P2P starts downloading Blocks, all the partial "pieces" of blocks are stored internally
 *  by this component, and it offers "hooks" you can use to get notified about several Events, like when a Block has
 *  been completely received, or a CHUNK oif Txs arived, or when the SOURCE of a Block has changed (so gthe downloading
 *  of that Block is resdet).
 *
 *  So the flow is:
 *  1 - Create an configure a P2P Instance
 *  2 - Create an instance an instance of this class, passing a refefrence to the previous P2P serevice.
 *  3 - Start the P2P Service and request some blocks to download.
 *
 */
public class RawBlockReceiver {


    /** Block Header Serializer */
    static class BlockHeaderSerializer implements ObjectSerializer<BlockHeaderMsg> {
        @Override
        public void serialize(BlockHeaderMsg object, ByteArrayWriter writer) {
            BlockHeaderMsgSerializer.getInstance().serialize(serContext, object, writer);
        }
        @Override
        public BlockHeaderMsg deserialize(ByteArrayReader reader) {
            return BlockHeaderMsgSerializer.getInstance().deserialize(desContext, reader);
        }
    }

    /** RawTx Serializer */
    static class TxSerializer implements ObjectSerializer<RawTxMsg> {
        @Override
        public void serialize(RawTxMsg object, ByteArrayWriter writer) {
            RawTxMsgSerializer.getInstance().serialize(serContext, object, writer);
        }
        @Override
        public RawTxMsg deserialize(ByteArrayReader reader) {
            return RawBlockMsgSerializer.getInstance().deserializeNextTx(desContext, reader);
        }
    }

    // Parameters for the Store to save Block Headers:
    private static final long  HEADERS_MAX_ENTRIES = 100_000; // 100K Blocks Max!!
    private static final int   AVG_HEADER_KEY_SIZE = "0000000000000000014bfe17eefb5ceb82d283bfa4cb515946124378c36800cb".getBytes(StandardCharsets.UTF_8).length;
    private static final int   AVG_HEADER_VALUE_SIZE = 81;

    // Parameters for the Txs Receiver:
    private static final long  TXS_EACH_FILE = 100_000; //100K Txs in each CMap file on disk
    private static final int   AVG_TXS_KEY_SIZE = "0000000000000000014bfe17eefb5ceb82d283bfa4cb515946124378c36800cb".getBytes(StandardCharsets.UTF_8).length + 2;
    private static final int   AVG_TXS_VALUE_SIZE = 1_0000; // 1 KB each Tx in avg

    // Serializer Context are defined one, for performance sake:
    private static SerializerContext serContext = SerializerContext.builder().build();
    private static DeserializerContext desContext = DeserializerContext.builder().build();

    // Underlying Receiver that does all the work:
    private BigObjectHeaderPlusReceiver<BlockHeaderMsg, RawTxMsg> receiver;

    // Event Streamer:
    public BigObjectHeaderPlusReceiverEventStreamer EVENTS;

    /**
     * Constructor.
     * @param receiverId    Receiver Id. Internally it will be used to create a Folder where the underlying
     *                      implementation will store the Blocks until they are removed,.
     * @param p2p           P2P Service
     */
    public RawBlockReceiver(String receiverId, P2P p2p) {

        // TODO: FIXME!! This config should be possible to get by 2p.getProtocolConfig().getMessageConfig()...
        Preconditions.checkArgument(((MessageHandlerConfig) p2p.getHandler(MessageHandler.HANDLER_ID).getConfig()).isRawTxsEnabled(),
                "This Receiver can only work if 'RawTxs' is enabled in the P2P Service");

        // We initialize a Store for the Headers:
        ObjectStore<BlockHeaderMsg> headersStore = new ObjectStoreCMap<>(
                p2p.getRuntimeConfig(), receiverId,
                new BlockHeaderSerializer(),
                AVG_HEADER_KEY_SIZE,
                AVG_HEADER_VALUE_SIZE,
                HEADERS_MAX_ENTRIES);

        BigCollectionReceiver<RawTxMsg> txsReceiver = new BigCollectionReceiverCMap<>(
                p2p.getRuntimeConfig(),
                receiverId,
                new TxSerializer(),
                AVG_TXS_KEY_SIZE,
                HEADERS_MAX_ENTRIES,
                AVG_TXS_VALUE_SIZE,
                TXS_EACH_FILE);

        // And now we initialize the whole Receiver (HEADER + TXs):
        this.receiver = new BigObjectHeaderPlusReceiverImpl<>(headersStore, txsReceiver, true);

        // We feed the receiver with the Events from the P2P Service:
        p2p.EVENTS.BLOCKS.BLOCK_HEADER_DOWNLOADED.forEach(e -> {
            String blockHash = e.getBtcMsg().getBody().getBlockHeader().getHash().getBytes().toString();
            String source = e.getPeerAddress().toString();
            this.receiver.registerHeader(blockHash, e.getBtcMsg().getBody().getBlockHeader(), source);
            this.receiver.registerNumTotalItems(blockHash, e.getBtcMsg().getBody().getBlockHeader().getTransactionCount().getValue(), source);
        });

        p2p.EVENTS.BLOCKS.BLOCK_RAW_TXS_DOWNLOADED.forEach(e -> {
            String blockHash = e.getBtcMsg().getBody().getBlockHeader().getHash().getBytes().toString();
            String source = e.getPeerAddress().toString();
            BigCollectionChunk<RawTxMsg> chunk = new BigCollectionChunkImpl<>(
                    e.getBtcMsg().getBody().getTxs(),
                    (int) e.getBtcMsg().getBody().getTxsOrderNumber().getValue());
            this.receiver.registerIncomingItems(blockHash, chunk, source);
        });

        // We initialize the Event Streamer of this Class:
        this.EVENTS = ((BigObjectHeaderPlusReceiverImpl<BlockHeaderMsg, RawTxMsg>) this.receiver).EVENTS;
    }

    public void start() { this.receiver.start(); }
    public void stop() { this.receiver.stop();}
    public void destroy() { this.receiver.destroy();}

    /**
     * Returns an Iterator over the CHUNKS of Txs of the block given, if already downloaded.
     * You can use this once you received a OBJECT_RECEIVED Event
     */
    public Iterator<BigCollectionChunk<RawTxMsg>> getTxs(String blockHash) {
        return this.receiver.getCollectionChunks(blockHash);
    }

    /** Removes a Block and all its Txs */
    public void remove(String blockHash) {
        this.receiver.remove(blockHash);
    }

    /** Returns the Header of the Block given, if already downloaded */
    public BlockHeaderMsg getHeader(String blockHash) {
        return this.receiver.getHeader(blockHash);
    }

    /** Removes and clear all the info stored by this Receiver */
    public void clear() {
        this.receiver.clear();
    }

}