package com.nchain.jcl.net.protocol.handlers.message;

import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.events.data.BlockHeaderDownloadedEvent;
import com.nchain.jcl.net.protocol.events.data.BlockTXsDownloadedEvent;
import com.nchain.jcl.net.protocol.events.data.*;
import com.nchain.jcl.net.protocol.messages.*;
import com.nchain.jcl.net.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.net.protocol.messages.common.Message;
import com.nchain.jcl.tools.events.Event;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * It creates Events to publishd to the Bus, based on the bitcoin Message we are processing.
 */
public class EventFactory {

    /** It creates the Event to be published to the Bus after we process an incoming Message */
    public static MsgReceivedEvent buildIncomingEvent(PeerAddress peerAddress, BitcoinMsg<? extends Message> btcMsg) {
        MsgReceivedEvent result = new MsgReceivedEvent<>(peerAddress, btcMsg);
        Message body = btcMsg.getBody();

        if (body instanceof VersionMsg)                 result = new VersionMsgReceivedEvent(peerAddress, (BitcoinMsg<VersionMsg>) btcMsg);
        else if (body instanceof VersionAckMsg)         result = new VersionAckMsgReceivedEvent(peerAddress, (BitcoinMsg<VersionAckMsg>) btcMsg);
        else if (body instanceof AddrMsg)               result = new AddrMsgReceivedEvent(peerAddress, (BitcoinMsg<AddrMsg>) btcMsg);
        else if (body instanceof BlockMsg)              result = new BlockMsgReceivedEvent(peerAddress, (BitcoinMsg<BlockMsg>) btcMsg);
        else if (body instanceof FeeFilterMsg)          result = new FeeMsgReceivedEvent(peerAddress, (BitcoinMsg<FeeFilterMsg>) btcMsg);
        else if (body instanceof GetAddrMsg)            result = new GetAddrMsgReceivedEvent(peerAddress, (BitcoinMsg<GetAddrMsg>) btcMsg);
        else if (body instanceof GetdataMsg)            result = new GetDataMsgReceivedEvent(peerAddress, (BitcoinMsg<GetdataMsg>) btcMsg);
        else if (body instanceof InvMessage)            result = new InvMsgReceivedEvent(peerAddress, (BitcoinMsg<InvMessage>) btcMsg);
        else if (body instanceof NotFoundMsg)           result = new NotFoundMsgReceivedEvent(peerAddress, (BitcoinMsg<NotFoundMsg>) btcMsg);
        else if (body instanceof PingMsg)               result = new PingMsgReceivedEvent(peerAddress, (BitcoinMsg<PingMsg>) btcMsg);
        else if (body instanceof PongMsg)               result = new PongMsgReceivedEvent(peerAddress, (BitcoinMsg<PongMsg>) btcMsg);
        else if (body instanceof RejectMsg)             result = new RejectMsgReceivedEvent(peerAddress, (BitcoinMsg<RejectMsg>) btcMsg);
        else if (body instanceof TxMsg)                 result = new TxMsgReceivedEvent(peerAddress, (BitcoinMsg<TxMsg>) btcMsg);
        else if (body instanceof GetHeadersMsg)         result = new GetHeadersMsgReceivedEvent(peerAddress, (BitcoinMsg<GetHeadersMsg>) btcMsg);
        else if (body instanceof SendHeadersMsg)        result = new SendHeadersMsgReceivedEvent(peerAddress, (BitcoinMsg<SendHeadersMsg>) btcMsg);
        else if (body instanceof HeadersMsg)            result = new HeadersMsgReceivedEvent(peerAddress, (BitcoinMsg<HeadersMsg>) btcMsg);
        else if (body instanceof MemPoolMsg)            result = new MempoolMsgReceivedEvent(peerAddress, (BitcoinMsg<MemPoolMsg>) btcMsg);
        else if (body instanceof GetHeadersEnMsg)       result = new GetHeadersEnMsgReceivedEvent(peerAddress, (BitcoinMsg<GetHeadersEnMsg>) btcMsg);
        else if (body instanceof PartialBlockHeaderMsg) result = new BlockHeaderDownloadedEvent(peerAddress, (BitcoinMsg<PartialBlockHeaderMsg>) btcMsg);
        else if (body instanceof PartialBlockTXsMsg)    result = new BlockTXsDownloadedEvent(peerAddress, (BitcoinMsg<PartialBlockTXsMsg>) btcMsg);
        else if (body instanceof PartialBlockRawTxMsg)  result = new BlockRawTXsDownloadedEvent(peerAddress, (BitcoinMsg<PartialBlockRawTxMsg>) btcMsg);
        else if (body instanceof RawTxMsg)              result = new RawTxMsgReceivedEvent(peerAddress, (BitcoinMsg<RawTxMsg>) btcMsg);
        else if (body instanceof RawTxBlockMsg)           result = new RawBlockMsgReceivedEvent(peerAddress, (BitcoinMsg<RawTxBlockMsg>) btcMsg);
        else if (body instanceof CompactBlockMsg)       result = new CompactBlockMsgReceivedEvent(peerAddress, (BitcoinMsg<CompactBlockMsg>) btcMsg);
        else if (body instanceof SendCompactBlockMsg)   result = new SendCompactBlockMsgReceivedEvent(peerAddress, (BitcoinMsg<SendCompactBlockMsg>) btcMsg);
        else if (body instanceof GetBlockTxnMsg)        result = new GetBlockTxnMsgReceivedEvent(peerAddress, (BitcoinMsg<GetBlockTxnMsg>) btcMsg);
        else if (body instanceof BlockTxnMsg)           result = new BlockTxnMsgReceivedEvent(peerAddress, (BitcoinMsg<BlockTxnMsg>) btcMsg);
        else if (body instanceof RawTxMsg)              result = new RawTxMsgReceivedEvent(peerAddress, (BitcoinMsg<RawTxMsg>) btcMsg);
        else if (body instanceof PartialBlockTxnMsg)    result = new PartialBlockTxnDownloadedEvent(peerAddress, (BitcoinMsg<PartialBlockTxnMsg>) btcMsg);

        return result;
    }

    /** It creates the Event to be published to the Bus after we process an outcoming Message */
    public static  Event buildOutcomingEvent(PeerAddress peerAddress, BitcoinMsg<? extends Message> btcMsg) {
        Event result = new MsgReceivedEvent<>(peerAddress, btcMsg);
        Message body = btcMsg.getBody();

        if (body instanceof VersionMsg)                 result = new VersionMsgSentEvent(peerAddress, (BitcoinMsg<VersionMsg>) btcMsg);
        else if (body instanceof VersionAckMsg)         result = new VersionAckMsgSentEvent(peerAddress, (BitcoinMsg<VersionAckMsg>) btcMsg);
        else if (body instanceof AddrMsg)               result = new AddrMsgSentEvent(peerAddress, (BitcoinMsg<AddrMsg>) btcMsg);
        else if (body instanceof BlockMsg)              result = new BlockMsgSentEvent(peerAddress, (BitcoinMsg<BlockMsg>) btcMsg);
        else if (body instanceof FeeFilterMsg)          result = new FeeMsgSentEvent(peerAddress, (BitcoinMsg<FeeFilterMsg>) btcMsg);
        else if (body instanceof GetAddrMsg)            result = new GetAddrMsgSentEvent(peerAddress, (BitcoinMsg<GetAddrMsg>) btcMsg);
        else if (body instanceof GetdataMsg)            result = new GetDataMsgSentEvent(peerAddress, (BitcoinMsg<GetdataMsg>) btcMsg);
        else if (body instanceof InvMessage)            result = new InvMsgSentEvent(peerAddress, (BitcoinMsg<InvMessage>) btcMsg);
        else if (body instanceof NotFoundMsg)           result = new NotFoundMsgSentEvent(peerAddress, (BitcoinMsg<NotFoundMsg>) btcMsg);
        else if (body instanceof PingMsg)               result = new PingMsgSentEvent(peerAddress, (BitcoinMsg<PingMsg>) btcMsg);
        else if (body instanceof PongMsg)               result = new PongMsgSentEvent(peerAddress, (BitcoinMsg<PongMsg>) btcMsg);
        else if (body instanceof RejectMsg)             result = new RejectMsgSentEvent(peerAddress, (BitcoinMsg<RejectMsg>) btcMsg);
        else if (body instanceof TxMsg)                 result = new TxMsgSentEvent(peerAddress, (BitcoinMsg<TxMsg>) btcMsg);
        else if (body instanceof GetHeadersMsg)         result = new GetHeadersMsgSentEvent(peerAddress, (BitcoinMsg<GetHeadersMsg>) btcMsg);
        else if (body instanceof SendHeadersMsg)        result = new SendHeadersMsgSentEvent(peerAddress, (BitcoinMsg<SendHeadersMsg>) btcMsg);
        else if (body instanceof HeadersMsg)            result = new HeadersMsgSentEvent(peerAddress, (BitcoinMsg<HeadersMsg>) btcMsg);
        else if (body instanceof MemPoolMsg)            result = new MempoolMsgSentEvent(peerAddress, (BitcoinMsg<MemPoolMsg>) btcMsg);
        else if (body instanceof GetHeadersEnMsg)       result = new GetHeadersEnMsgSentEvent(peerAddress, (BitcoinMsg<GetHeadersEnMsg>) btcMsg);
        else if (body instanceof SendCompactBlockMsg)   result = new SendCompactBlockMsgSentEvent(peerAddress, (BitcoinMsg<SendCompactBlockMsg>) btcMsg);
        else if (body instanceof CompactBlockMsg)       result = new CompactBlockMsgSentEvent(peerAddress, (BitcoinMsg<CompactBlockMsg>) btcMsg);
        else if (body instanceof GetBlockTxnMsg)        result = new GetBlockTxnMsgSentEvent(peerAddress, (BitcoinMsg<GetBlockTxnMsg>) btcMsg);
        else if (body instanceof BlockTxnMsg)           result = new BlockTxnMsgSentEvent(peerAddress, (BitcoinMsg<BlockTxnMsg>) btcMsg);

        return result;
    }
}
