package io.bitcoinsv.bsvcl.net.protocol.handlers.blacklist;


import io.bitcoinsv.bsvcl.net.network.events.PeersBlacklistedEvent;
import io.bitcoinsv.bsvcl.common.files.CSVSerializable;
import io.bitcoinsv.bsvcl.common.util.DateTimeUtils;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.StringTokenizer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class stores all the info that the Blacklist Handler needs to do its work. It sotres info for each HOST
 * (Not Peer, but a HOST. Only the IP address). If a Host is blacklisted, ALL the Peers using that IP (but with
 * different port) will be disconnected.
 */
public class BlacklistHostInfo implements CSVSerializable {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BlacklistHostInfo.class);
    // Host IP Address:
    private InetAddress ip;
    // If blacklisted, these variables stores the reason and the time when it was blacklisted:
    private PeersBlacklistedEvent.BlacklistReason blacklistReason;
    private LocalDateTime blacklistTimestamp;
    private LocalDateTime expirationTime;

    // We keep track of the number of times this Host has reached specific scenarios:
    private int numFailedHandshakes;
    private int numFailedPingPongs;
    private int numConnRejections;
    private int numSerializationErrors;


    /** Constructor */
    public BlacklistHostInfo(InetAddress ip) {
        this.ip = ip;
    }

    public BlacklistHostInfo() {}

    public void reset() {
        this.blacklistReason = null;
        this.blacklistTimestamp = null;
        this.numConnRejections = 0;
        this.numFailedHandshakes = 0;
        this.numFailedPingPongs = 0;
        this.numSerializationErrors = 0;
    }

    // Convenience methods:
    public boolean isBlacklisted()          { return blacklistReason != null; }
    public void addFailedHandshakes()       { numFailedHandshakes++;}
    public void addFailedPingPongs()        { numFailedPingPongs++;}
    public void addConnRejections()         { numConnRejections++;}
    public void addSerializationErrors()    { numSerializationErrors++;}

    public void blacklist(PeersBlacklistedEvent.BlacklistReason reason, Optional<Duration> duration) {
        this.blacklistReason = reason;
        this.blacklistTimestamp = DateTimeUtils.nowDateTimeUTC();
        this.expirationTime = duration.isPresent()
                ? this.blacklistTimestamp.plus(duration.get())
                : null;
    }

    // TODO: CAREFUL WITH UTC AND TIMEZONES!!
    public boolean isBlacklistExpired() {
        if (!isBlacklisted()) return false;
        if (expirationTime == null) return false;
        return DateTimeUtils.nowDateTimeUTC().isAfter(expirationTime);
    }

    public void removeFromBacklist() {
        // We reset the field according to the Reason provided:
        switch (blacklistReason) {
            case CONNECTION_REJECTED: {
                this.numConnRejections = 0;
                break;
            }
            case SERIALIZATION_ERROR: {
                this.numSerializationErrors = 0;
                break;
            }
            case FAILED_HANDSHAKE: {
                this.numFailedHandshakes = 0;
                break;
            }
            case PINGPONG_TIMEOUT: {
                this.numFailedPingPongs = 0;
                break;
            }
        }
        this.blacklistReason = null;
        this.blacklistTimestamp = null;
    }

    /** It prints the info of this Host, so it can be saved in a CSV file */
    @Override
    public String toCSVLine() {
        StringBuffer result = new StringBuffer();
        result.append(ip.getHostAddress()).append(CSVSerializable.SEPARATOR);
        result.append(blacklistTimestamp.format(DateTimeFormatter.ISO_DATE_TIME)).append(CSVSerializable.SEPARATOR);
        result.append(blacklistReason);
        return result.toString();
    }

    @Override
    public void fromCSVLine(String line) {
        if (line == null) return;
        try {
            reset();
            StringTokenizer tokens = new StringTokenizer(line, CSVSerializable.SEPARATOR);
            this.ip = InetAddress.getByName(tokens.nextToken());
            this.blacklistTimestamp = LocalDateTime.parse(tokens.nextToken(), DateTimeFormatter.ISO_DATE_TIME);
            this.blacklistReason = PeersBlacklistedEvent.BlacklistReason.valueOf(tokens.nextToken());
        } catch (Exception e) {
            log.error("Error Parsing line for CSV: " + line);
            throw new RuntimeException(e);
        }
    }

    public InetAddress getIp()                                          { return this.ip; }
    public PeersBlacklistedEvent.BlacklistReason getBlacklistReason()   { return this.blacklistReason; }
    public LocalDateTime getBlacklistTimestamp()                        { return this.blacklistTimestamp; }
    public LocalDateTime getExpirationTime()                            { return this.expirationTime; }
    public int getNumFailedHandshakes()                                 { return this.numFailedHandshakes; }
    public int getNumFailedPingPongs()                                  { return this.numFailedPingPongs; }
    public int getNumConnRejections()                                   { return this.numConnRejections; }
    public int getNumSerializationErrors()                              { return this.numSerializationErrors; }

    public void setIp(InetAddress ip)                                                       { this.ip = ip; }
    public void setBlacklistReason(PeersBlacklistedEvent.BlacklistReason blacklistReason)   { this.blacklistReason = blacklistReason; }
    public void setBlacklistTimestamp(LocalDateTime blacklistTimestamp)                     { this.blacklistTimestamp = blacklistTimestamp; }
    public void setNumFailedHandshakes(int numFailedHandshakes)                             { this.numFailedHandshakes = numFailedHandshakes; }
    public void setNumFailedPingPongs(int numFailedPingPongs)                               { this.numFailedPingPongs = numFailedPingPongs; }
    public void setNumConnRejections(int numConnRejections)                                 { this.numConnRejections = numConnRejections; }
    public void setNumSerializationErrors(int numSerializationErrors)                       { this.numSerializationErrors = numSerializationErrors; }
}
