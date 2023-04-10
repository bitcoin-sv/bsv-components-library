package io.bitcoinsv.bsvcl.net.protocol.handlers.block.strategies;

import io.bitcoinsv.bsvcl.net.network.PeerAddress;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * It represents a "Request/Proposal" for a Block to be downlaod by a specific Peer
 */
public class DownloadRequest {
    private PeerAddress peerAddress;
    private String blockHash;
    public DownloadRequest(PeerAddress peerAddress, String blockHash) {
        this.peerAddress = peerAddress;
        this.blockHash = blockHash;
    }
    public PeerAddress getPeerAddress() { return this.peerAddress;}
    public String getBlockHash()        { return this.blockHash;}
}
