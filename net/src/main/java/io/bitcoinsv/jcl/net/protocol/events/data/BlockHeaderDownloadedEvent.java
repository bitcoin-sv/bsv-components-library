package io.bitcoinsv.jcl.net.protocol.events.data;


import com.google.common.base.Objects;
import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.PartialBlockHeaderMsg;
import io.bitcoinsv.jcl.net.protocol.messages.common.BitcoinMsg;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-29 13:21
 *
 * An Event triggered when a Block Header has been downloaded.
 */
public final class BlockHeaderDownloadedEvent extends MsgReceivedEvent<PartialBlockHeaderMsg> {

    public BlockHeaderDownloadedEvent(PeerAddress peerAddress, BitcoinMsg<PartialBlockHeaderMsg> blockHeaderMsg) {
        super(peerAddress, blockHeaderMsg);
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
