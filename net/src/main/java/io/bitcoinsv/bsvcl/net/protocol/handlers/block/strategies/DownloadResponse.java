package io.bitcoinsv.bsvcl.net.protocol.handlers.block.strategies;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * It represents a Response to a PRevious "Request/proposal". It holds info about whether the request has been
 * accepted, or the reason why it has been not.
 */
public class DownloadResponse {

    // Note: A "announcer" Peer is a Peer that has "announced" a Block by sending previously an INV
    public enum DownloadResponseState {
        ASSIGNED,                       // Bock assigned to Peer (NORMAL CASE)
        TOO_MANY_FAILURES,              // Block NOT assigned: Peer has already tried this block before
        OTHER_PEER_WITH_EXCLUSIVITY,    // Block NOT assigned: Other Peer has exclusivity for this Block
        OTHER_PEER_WITH_PRIORITY,       // Block NOT assigned: Other Peer has higher priority for this Block
        OTHER_PEER_ANNOUNCER,           // Another Peer has announced this block so that Peer has higher Priority
        NO_ANNOUNCERS                   // The Block can ONLY be downloader from an Announcer, but this Peer is NOT
    }

    private DownloadRequest request;
    private DownloadResponseState state;

    public DownloadResponse(DownloadRequest request) {
        this.request = request;
        this.state = DownloadResponseState.ASSIGNED;
    }

    public DownloadResponse(DownloadRequest request, DownloadResponseState state) {
        this.request = request;
        this.state = state;
    }

    public DownloadRequest getRequest()     { return this.request;}
    public DownloadResponseState getState() { return this.state;}
    public boolean isAssigned()             { return this.state.equals(DownloadResponseState.ASSIGNED);}
}
