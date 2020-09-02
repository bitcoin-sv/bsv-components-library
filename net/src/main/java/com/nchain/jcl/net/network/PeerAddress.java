package com.nchain.jcl.net.network;

import com.google.common.base.Objects;
import com.nchain.jcl.base.tools.files.CSVSerializable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * 01/07/2019
 *
 * A class that wraps the info to locate a Peer in the Blockchain Network.
 * This class is immutable and safe for MultiThreading
 */
public final class PeerAddress implements CSVSerializable {

    private String address;
    private InetAddress ip;
    private int port;
    private boolean verified;

    /**
     * Constructor
     */
    public PeerAddress(InetAddress ip, int port) {
        this.ip = ip;
        this.port = port;
        this.verified = true;
    }

    /**
     * Constructor
     */
    public PeerAddress(String address, int port) {
        this.address = address;
        this.port = port;
        this.verified = false;
    }

    /**
     * Returns an instance pointing to localhost and the getPort given
     */
    public static PeerAddress localhost(int port) throws UnknownHostException {
        return new PeerAddress(InetAddress.getByName("localhost"), port);
    }

    /**
     * Returns a PeerAddress from a raw representation in IPv4 format
     * @param addr  addr in int format
     * @param port  getPort number
     */
    public static PeerAddress fromRawIPv4(int addr, int port) throws UnknownHostException {
        byte[] v4addr = new byte[4];
        v4addr[0] = (byte) (0xFF & (addr));
        v4addr[1] = (byte) (0xFF & (addr >> 8));
        v4addr[2] = (byte) (0xFF & (addr >> 16));
        v4addr[3] = (byte) (0xFF & (addr >> 24));
        InetAddress ip = InetAddress.getByAddress(v4addr);
        return new PeerAddress(ip, port);
    }

    /**
     * Returns a new instance of PeerAddress, our of the HostName + getPort stored in the parameter
     * @param ip Contains the Host + getPort, like "123.32.33.43:8333"
     */
    public static PeerAddress fromIp(String ip) throws UnknownHostException {
        return fromIp(ip, true);
    }

    /**
     * Returns a new instance of PeerAddress, our of the HostName + getPort stored in the parameter
     * @param ip Contains the Host + getPort, like "123.32.33.43:8333"
     */
    public static PeerAddress fromIpNoVerified(String ip) throws UnknownHostException {
        return fromIp(ip, false);
    }

    /**
     * Returns a new instance of PeerAddress, our of the HostName + getPort stored in the parameter
     * @param ip Contains the Host + getPort, like "123.32.33.43:8333"
     */
    private static PeerAddress fromIp(String ip, boolean checkAddress) throws UnknownHostException {

        PeerAddress result = null;
        // we check the IP length:
        if (ip == null || ip.trim().length() == 0) throw new RuntimeException("ip string provided is null or empty");

        int portNumber;
        String hostAddress;

        int lastColonIndex = ip.lastIndexOf(":");
        int slashIndex = ip.indexOf("/");

        // We get the Port number (0 if no port defined):
        portNumber = lastColonIndex != -1 ?  Integer.parseInt(ip.substring(lastColonIndex + 1)) : 0;

        // We get the Address:
        hostAddress = slashIndex != -1 ? ip.substring(0, slashIndex) : ip.substring(0, lastColonIndex);

        // We check and create the PeerAddress:
        if (checkAddress) {
            InetAddress inetAddress = InetAddress.getByName(hostAddress);
            result = new PeerAddress(inetAddress, portNumber);
        } else {
            result = new PeerAddress(ip, portNumber);
        }
        return result;
    }

    /**
     * Returns an array of PeerAddress, out of the hostname provided. The getPort provided will be applied to all the
     * address obtained
     */
    public static PeerAddress[] fromHostName(String hostname, int port) throws UnknownHostException {
        InetAddress[] ips = InetAddress.getAllByName(hostname);
        if (ips == null) return null;

        List<PeerAddress> result = new ArrayList<>();
        for (InetAddress ip : ips) result.add(new PeerAddress(ip, port));
        return result.toArray(new PeerAddress[result.size()]);
    }

    // getter
    public InetAddress getIp() {
        return ip;
    }
    // getter
    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return toStringWithoutPort() + ":" + port;
    }

    public String toStringWithoutPort() {
        String addressStr = (ip != null) ? ip.toString() : address;
        return addressStr.startsWith("/") ? addressStr.substring(1) + addressStr : addressStr;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.ip, this.port);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PeerAddress)) return false;
        PeerAddress other = (PeerAddress) obj;
        return Objects.equal(this.port, other.port) && Objects.equal(this.ip, other.ip);
    }
    @Override
    public String toCSVLine() {
        return this.ip.toString() + CSVSerializable.SEPARATOR + this.port;
    }

    @Override
    public void fromCSVLine(String line) {
        if (line == null) return;
        try {
            StringTokenizer tokens = new StringTokenizer(line, CSVSerializable.SEPARATOR);
            String hostAddress = tokens.nextToken();
            String port = tokens.nextToken();
            this.ip = InetAddress.getByName(hostAddress + ":" + port);
            this.port = Integer.parseInt(port);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
