package com.nchain.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import com.nchain.jcl.net.protocol.messages.common.Message;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 *
 * A VersionMSg represent the first step in the handshake between tow Peers in the blockchain Network.
 * When a node creates an outgoing connection, it will immediately advertise its version. The remote
 * node will respond with its version. No further communication is possible until both peers have
 * exchanged their version.
 *
 * Structure of the BODY of Message:
 *
 *  - field: "version" (4 bytes) unit32_t
 *    Identifies connection version being used by the node
 *
 *  - field: "services" (8 bytes) uint64_t
 *    bitfield of features to be enabled for this connection
 *
 *  - field: "timestamp" (8 bytes) uint64_t
 *    standard UNIX timestamp in seconds
 *
 *  - field: "addr_recv" (26 bytes) NetAddressMsg
 *    The network address of the node receiving this message
 *
 *  Fields below require version ≥ 106
 *
 *  - field: "addr_from" (26 bytes) NetAddressMsg
 *    The network address of the node emitting this message
 *
 *  - field: "nonce" (8 bytes) uint64_t
 *    Node random nonce, randomly generated every time a version packet is sent. This nonce is
 *    used to detect connections to self.
 *
 *  - field: "getHandshakeUserAgent" (? bytes) VarStrMsg
 *    User Agent (0x00 if string is 0 bytes long)
 *
 *  - field: "start_height" (4 bytes) int32_t
 *    The last block received by the emitting node
 *
 *  Fields below require version ≥ 70001
 *
 *  - field: "isHandshakeUsingRelay" (1 bytes) bool
 *    Whether the remote peer should announce relayed transactions or not, see BIP 0037
 */
public final class VersionMsg extends Message {
    // The only field which a variable length in the Version Message is the "getHandshakeUserAgent" field.
    // The rest of the Message has a fixed length of 85 bytes.
    private static final int FIXED_MESSAGE_LENGTH = 84; // need to add the "getHandshakeUserAgent"  and RELAY length to this.
    public static final String MESSAGE_TYPE = "version";

    private final long version;
    private final long services;
    private final long timestamp;
    private final NetAddressMsg addr_recv;
    private final NetAddressMsg addr_from;
    private final long nonce;
    private final VarStrMsg user_agent;
    private final long start_height;
    private final Boolean relay;

    protected VersionMsg(long version, long services, long timestamp,
                         NetAddressMsg addr_recv, NetAddressMsg addr_from,
                         long nonce, VarStrMsg user_agent, long start_height, Boolean relay ) {
        this.version = version;
        this.services = services;
        this.timestamp = timestamp;
        this.addr_recv = addr_recv;
        this.addr_from = addr_from;
        this.nonce = nonce;
        this.user_agent = user_agent;
        this.start_height = start_height;
        this.relay = relay;
        init();
    }

    @Override
    protected long calculateLength() {
        long length = FIXED_MESSAGE_LENGTH;
        length += (user_agent != null) ? user_agent.getLengthInBytes(): 0;
        length += (relay != null) ? 1 : 0;
        return length;
    }

    @Override
    protected void validateMessage() {}

    @Override
    public String getMessageType()      { return MESSAGE_TYPE; }
    public long getVersion()            { return this.version; }
    public long getServices()           { return this.services; }
    public long getTimestamp()          { return this.timestamp; }
    public NetAddressMsg getAddr_recv() { return this.addr_recv; }
    public NetAddressMsg getAddr_from() { return this.addr_from; }
    public long getNonce()              { return this.nonce; }
    public VarStrMsg getUser_agent()    { return this.user_agent; }
    public long getStart_height()       { return this.start_height; }
    public Boolean getRelay()           { return this.relay; }

    public String toString() {
        return "VersionMsg(version=" + this.getVersion() + ", services=" + this.getServices() + ", timestamp=" + this.getTimestamp() + ", addr_recv=" + this.getAddr_recv() + ", addr_from=" + this.getAddr_from() + ", nonce=" + this.getNonce() + ", user_agent=" + this.getUser_agent() + ", start_height=" + this.getStart_height() + ", relay=" + this.getRelay() + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(version, services, timestamp, addr_recv, addr_from, nonce, user_agent, start_height, relay);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) { return false; }
        VersionMsg other = (VersionMsg) obj;
        return Objects.equal(this.version, other.version)
                && Objects.equal(this.services, other.services)
                && Objects.equal(this.timestamp, other.timestamp)
                && Objects.equal(this.addr_recv, other.addr_recv)
                && Objects.equal(this.addr_from, other.addr_from)
                && Objects.equal(this.nonce, other.nonce)
                && Objects.equal(this.user_agent, other.user_agent)
                && Objects.equal(this.start_height, other.start_height)
                && Objects.equal(this.relay, other.relay);
    }

    public static VersionMsgBuilder builder() {
        return new VersionMsgBuilder();
    }

    /**
     * Builder
     */
    public static class VersionMsgBuilder {
        private long version;
        private long services;
        private long timestamp;
        private NetAddressMsg addr_recv;
        private NetAddressMsg addr_from;
        private long nonce;
        private VarStrMsg user_agent;
        private long start_height;
        private Boolean relay;

        VersionMsgBuilder() {}

        public VersionMsg.VersionMsgBuilder version(long version) {
            this.version = version;
            return this;
        }

        public VersionMsg.VersionMsgBuilder services(long services) {
            this.services = services;
            return this;
        }

        public VersionMsg.VersionMsgBuilder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public VersionMsg.VersionMsgBuilder addr_recv(NetAddressMsg addr_recv) {
            this.addr_recv = addr_recv;
            return this;
        }

        public VersionMsg.VersionMsgBuilder addr_from(NetAddressMsg addr_from) {
            this.addr_from = addr_from;
            return this;
        }

        public VersionMsg.VersionMsgBuilder nonce(long nonce) {
            this.nonce = nonce;
            return this;
        }

        public VersionMsg.VersionMsgBuilder user_agent(VarStrMsg user_agent) {
            this.user_agent = user_agent;
            return this;
        }

        public VersionMsg.VersionMsgBuilder start_height(long start_height) {
            this.start_height = start_height;
            return this;
        }

        public VersionMsg.VersionMsgBuilder relay(Boolean relay) {
            this.relay = relay;
            return this;
        }

        public VersionMsg build() {
            return new VersionMsg(version, services, timestamp, addr_recv, addr_from, nonce, user_agent, start_height, relay);
        }
    }
}
