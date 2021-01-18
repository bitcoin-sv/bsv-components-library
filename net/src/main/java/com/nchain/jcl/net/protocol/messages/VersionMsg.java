package com.nchain.jcl.net.protocol.messages;

import com.nchain.jcl.net.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;


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
@Value
@EqualsAndHashCode
public class VersionMsg extends Message {
    // The only field which a variable length in the Version Message is the "getHandshakeUserAgent" field.
    // The rest of the Message has a fixed length of 85 bytes.
    private static int FIXED_MESSAGE_LENGTH = 84; // need to add the "getHandshakeUserAgent"  and RELAY length to this.
    public static final String MESSAGE_TYPE = "version";

    private long version;
    private long services;
    private long timestamp;
    private NetAddressMsg addr_recv;
    private NetAddressMsg addr_from;
    private long nonce;
    private VarStrMsg user_agent;
    private long start_height;
    private Boolean relay;

    @Builder
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
    public String getMessageType() { return MESSAGE_TYPE; }


    @Override
    protected long calculateLength() {
        long length = FIXED_MESSAGE_LENGTH;
        length += (user_agent != null) ? user_agent.getLengthInBytes(): 0;
        length += (relay != null) ? 1 : 0;
        return length;
    }

    @Override
    protected void validateMessage() {}
}
