package io.bitcoinsv.jcl.net.protocol.handlers.blacklist;

import io.bitcoinsv.jcl.net.network.events.PeersBlacklistedEvent;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * BlackListView is data class used for exposing blacklisted peers.
 */
public class BlacklistView {

    private InetAddress ip;
    private PeersBlacklistedEvent.BlacklistReason blacklistReason;
    private Date blacklistDate;
    private Date expirationDate;

    public BlacklistView(InetAddress ip, PeersBlacklistedEvent.BlacklistReason blacklistReason,
                         LocalDateTime blacklistTimestamp, LocalDateTime blacklistExpiration) {
        this.ip = ip;
        this.blacklistReason = blacklistReason;
        this.blacklistDate = (blacklistTimestamp != null) ? localDateTimeToDate(blacklistTimestamp) : null;
        this.expirationDate = (blacklistExpiration != null) ? localDateTimeToDate(blacklistExpiration) : null;
    }

    private Date localDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public InetAddress getIp() {
        return ip;
    }

    public PeersBlacklistedEvent.BlacklistReason getBlacklistReason() {
        return blacklistReason;
    }

    public Date getBlacklistDate() {
        return blacklistDate;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }
}
