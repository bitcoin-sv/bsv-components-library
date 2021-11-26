package com.nchain.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import com.nchain.jcl.net.network.PeerAddress;
import com.nchain.jcl.net.protocol.messages.common.Message;

import java.io.Serializable;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A NetMshAddr is not a fully Bitcoin Message itself, but it's a structure that is reused by different other
 * messages in the Bitcoin P2P. When a network address is needed somewhere, this structure is used.
 * Network addresses are not prefixed with a timestamp in the version message.
 *
 * Structure of the Message:
 *
 *  - field: "time" (4 bytes) unit32_t
 *    the Time (version >= 31402). Not present in version message.
 *
 *  - field: "services" (8 bytes) uint64_t
 *    same service(s) listed in the Version Message
 *
 *  - field: "IPv6/4" (16 bytes) char[16]
 *    IPv6 address. Network byte order. The original client only supported IPv4 and only read the last 4 bytes
 *    to get the IPv4 address. However, the IPv4 address is written into the message as a 16 byte IPv4-mapped
 *    IPv6 address
 *    (12 bytes 00 00 00 00 00 00 00 00 00 00 FF FF, followed by the 4 bytes of the IPv4 address).
 *
 *  - field: "getPort" (2 bytes) uint16_t
 *    getPort number, network byte order
 */
public final class NetAddressMsg extends Message implements Serializable {
    // An NetAddressMsg has a length of 30 Bytes or 26 bytes, depending whether it has a timestamp or not.
    public static final int MESSAGE_LENGTH = 30;
    public static final int MESSAGE_LENGTH_NO_TIMESTAMP = 26;

    public static final String MESSAGE_TYPE = "netAddress";

    private final Long timestamp;
    private final PeerAddress address;
    private final long services;

    // Constructor
    protected NetAddressMsg(Long timestamp, long services, PeerAddress address) {
        this.timestamp = timestamp;
        this.services = services;
        this.address = address;
        init();
    }

    @Override
    protected long calculateLength() {
        long length = timestamp != null ? MESSAGE_LENGTH : MESSAGE_LENGTH_NO_TIMESTAMP;
        return length;
    }

    @Override
    protected void validateMessage() {}

    @Override
    public String getMessageType()  { return MESSAGE_TYPE;}
    public Long getTimestamp()      { return this.timestamp; }
    public PeerAddress getAddress() { return this.address; }
    public long getServices()       { return this.services; }

    @Override
    public String toString() {
        return "NetAddressMsg(timestamp=" + this.getTimestamp() + ", address=" + this.getAddress() + ", services=" + this.getServices() + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(timestamp, address, services);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        NetAddressMsg other = (NetAddressMsg) obj;
        return Objects.equal(this.timestamp, other.timestamp)
                && Objects.equal(this.address, other.address)
                && Objects.equal(this.services, other.services);
    }

    public static NetAddressMsgBuilder builder() {
        return new NetAddressMsgBuilder();
    }


    public NetAddressMsgBuilder toBuilder() {
        return new NetAddressMsgBuilder()
                        .timestamp(this.timestamp)
                        .services(this.services)
                        .address(this.address);
    }

    /**
     * Builder
     */
    public static class NetAddressMsgBuilder  {
        private Long timestamp;
        private long services;
        private PeerAddress address;

        public NetAddressMsgBuilder() {}

        public NetAddressMsg.NetAddressMsgBuilder timestamp(Long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public NetAddressMsg.NetAddressMsgBuilder services(long services) {
            this.services = services;
            return this;
        }

        public NetAddressMsg.NetAddressMsgBuilder address(PeerAddress address) {
            this.address = address;
            return this;
        }

        public NetAddressMsg build() {
            return new NetAddressMsg(timestamp, services, address);
        }
    }
}
