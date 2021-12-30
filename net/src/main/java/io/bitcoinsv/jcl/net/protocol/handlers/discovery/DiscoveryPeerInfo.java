package io.bitcoinsv.jcl.net.protocol.handlers.discovery;


import io.bitcoinsv.jcl.net.network.PeerAddress;
import io.bitcoinsv.jcl.net.protocol.messages.VersionMsg;
import io.bitcoinsv.jcl.tools.files.CSVSerializable;
import io.bitcoinsv.jcl.tools.util.DateTimeUtils;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.util.StringTokenizer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Information about each Peer/Address needed by the Discovery ProtocolHandler
 */
public class DiscoveryPeerInfo implements CSVSerializable {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DiscoveryPeerInfo.class);
    // Peer Address
    private PeerAddress peerAddress;
    // Timestamps as it will be sent in a ADDR messages to other Peers
    private Long timestamp;

    // Variables set when this Peer is handsahked, and set to null when the connection to it is lost (but this record
    // is kept).
    private VersionMsg versionMsg;
    private LocalDateTime lastHandshakeTime;

    /** Constructor */
    public DiscoveryPeerInfo(PeerAddress peerAddress) {
        this(peerAddress, System.currentTimeMillis());
    }

    /** Constructor */
    public DiscoveryPeerInfo(PeerAddress peerAddress, Long timestamp) {
        this.peerAddress = peerAddress;
        this.timestamp = timestamp;
    }

    public DiscoveryPeerInfo() {}

    public void reset() {
        //this.timestamp = null;
        this.versionMsg = null;
    }

    public void updateHandshake(VersionMsg versionMsg) {
        this.versionMsg = versionMsg;
        this.lastHandshakeTime = DateTimeUtils.nowDateTimeUTC();
    }

    public void updateTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isHandshaked() {
        return (versionMsg != null);
    }

    @Override
    public String toCSVLine() {
        StringBuffer result = new StringBuffer();
        result.append(peerAddress).append(CSVSerializable.SEPARATOR);
        result.append(timestamp);
        return result.toString();
    }

    @Override
    public void fromCSVLine(String line) {
        try {
            reset();
            if (line == null) return;
            StringTokenizer tokens = new StringTokenizer(line, CSVSerializable.SEPARATOR);
            this.peerAddress = PeerAddress.fromIp(tokens.nextToken());
            this.timestamp = Long.parseLong(tokens.nextToken());
        } catch (Exception e) {
            log.error("Error Parsing line for CSV: " + line);
            throw new RuntimeException(e);
        }
    }

    public PeerAddress getPeerAddress()         { return this.peerAddress; }
    public Long getTimestamp()                  { return this.timestamp; }
    public VersionMsg getVersionMsg()           { return this.versionMsg; }
    public LocalDateTime getLastHandshakeTime() { return this.lastHandshakeTime; }
}
