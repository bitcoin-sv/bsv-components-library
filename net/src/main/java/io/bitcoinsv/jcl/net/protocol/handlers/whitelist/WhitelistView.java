package io.bitcoinsv.jcl.net.protocol.handlers.whitelist;


import org.slf4j.Logger;
import java.net.InetAddress;
import java.time.LocalDateTime;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A read-only view of the Info that the WhitelistHandler keeps for each whitelisted peer.
 */
public class WhitelistView {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(WhitelistView.class);
    // Host IP Address:
    private InetAddress ip;
    private LocalDateTime timestamp;

    /** Constructor */
    public WhitelistView(InetAddress ip, LocalDateTime timestamp) {
        this.ip = ip;
        this.timestamp = timestamp;
    }

    /** Constructor */
    public WhitelistView(InetAddress ip) {
        this(ip, LocalDateTime.now());
    }
    /** Constructor */
    public WhitelistView() {}

    public void reset()                 { this.timestamp = LocalDateTime.now(); }
    public InetAddress getIp()          { return this.ip;}
    public LocalDateTime getTimestamp() { return this.timestamp;}


    /** A Conversion utility method */
    public static WhitelistView from(WhitelistHostInfo hostInfo) {
        return new WhitelistView(hostInfo.getIp(), hostInfo.getTimestamp());
    }
}
