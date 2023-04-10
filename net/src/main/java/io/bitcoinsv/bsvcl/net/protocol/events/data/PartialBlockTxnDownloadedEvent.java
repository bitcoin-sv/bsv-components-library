package io.bitcoinsv.bsvcl.net.protocol.events.data;

import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.protocol.messages.PartialBlockTxnMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.common.BitcoinMsg;
import io.bitcoinsv.bsvcl.net.network.PeerAddress;

/**
 * @author j.pomer@nchain.com
 * Copyright (c) 2018-2021 nChain Ltd
 */
public final class PartialBlockTxnDownloadedEvent extends MsgReceivedEvent<PartialBlockTxnMsg> {
    public PartialBlockTxnDownloadedEvent(PeerAddress peerAddress, BitcoinMsg<PartialBlockTxnMsg> blockTxsMsg) {
        super(peerAddress, blockTxsMsg);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode());
    }
}
