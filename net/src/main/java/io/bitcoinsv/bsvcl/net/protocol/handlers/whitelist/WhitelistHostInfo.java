package io.bitcoinsv.bsvcl.net.protocol.handlers.whitelist;


import io.bitcoinsv.bsvcl.tools.files.CSVSerializable;
import org.slf4j.Logger;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.StringTokenizer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This class stores all the info that the Whitelist Handler needs to do its work. It sotres info for each HOST
 * (Not Peer, but a HOST. Only the IP address).
 */
public class WhitelistHostInfo implements CSVSerializable {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(WhitelistHostInfo.class);
    // Host IP Address:
    private InetAddress ip;
    // Timestamp when the Host has been whitelisted
    private LocalDateTime timestamp;

    /** Constructor */
    public WhitelistHostInfo(InetAddress ip, LocalDateTime timestamp) {
        this.ip = ip;
        this.timestamp = timestamp;
    }

    /** Constructor */
    public WhitelistHostInfo(InetAddress ip) {
        this(ip, LocalDateTime.now());
    }
    /** Constructor */
    public WhitelistHostInfo() {}

    public void reset()                 { this.timestamp = LocalDateTime.now(); }
    public InetAddress getIp()          { return this.ip;}
    public LocalDateTime getTimestamp() { return this.timestamp;}


    /** It prints the info of this Host, so it can be saved in a CSV file */
    @Override
    public String toCSVLine() {
        StringBuffer result = new StringBuffer();
        result.append(ip.getHostAddress()).append(CSVSerializable.SEPARATOR);
        result.append(timestamp.format(DateTimeFormatter.ISO_DATE_TIME)).append(CSVSerializable.SEPARATOR);
        return result.toString();
    }

    @Override
    public void fromCSVLine(String line) {
        if (line == null) return;
        try {
            reset();
            StringTokenizer tokens = new StringTokenizer(line, CSVSerializable.SEPARATOR);
            this.ip = InetAddress.getByName(tokens.nextToken());
            this.timestamp = LocalDateTime.parse(tokens.nextToken(), DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            log.error("Error Parsing line for CSV: " + line);
            throw new RuntimeException(e);
        }
    }

}
