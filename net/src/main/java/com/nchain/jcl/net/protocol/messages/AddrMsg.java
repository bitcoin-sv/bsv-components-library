package com.nchain.jcl.net.protocol.messages;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.nchain.jcl.net.protocol.messages.common.Message;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.List;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Addr Message provide information on known nodes of the network.
 * The size of message  depend whether its being used within a VERSION Message or not.Starting version 31402, addresses are
 * prefixed with a timestamp. If no timestamp is present, the addresses should not be relayed to other peers,
 * unless it is indeed confirmed they are up.
 *
 * Structure of the BODY of Message:
 *
 *  - field: "count" (1+ bytes) 	var_int
 *   Number of address entries (max: 1000)
 *
 * - field: "addr_list" (30*count) array of net_addr
 *   Address of other nodes on the network.
 *
 */
@Value
@EqualsAndHashCode
public class AddrMsg extends Message {
    private static final long MAX_ADDRESSES = 1000;
    public static final String MESSAGE_TYPE = "addr";

    private VarIntMsg count;
    private ImmutableList<NetAddressMsg> addrList;


    /**
     * Creates the AddrMsg Object.Use the corresponding builder to create the instance.
     *
     * @param addrList
     */
    @Builder
    protected AddrMsg( List<NetAddressMsg> addrList) {
        this.addrList = ImmutableList.copyOf(addrList);
        this.count = VarIntMsg.builder().value(this.addrList.size()).build();
        init();
    }

    @Override
    protected long calculateLength() {
        long lengthInBytes  = count.getLengthInBytes();

        for (NetAddressMsg netAddressMsg : addrList) {
            lengthInBytes += netAddressMsg.getLengthInBytes();
        }
        return lengthInBytes;
    }

    @Override
    protected void validateMessage() {
        Preconditions.checkArgument(count.getValue() <= MAX_ADDRESSES, "Address message too large.");
        Preconditions.checkArgument(count.getValue() ==  addrList.size(), "Address message list and count value are not same.");
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }
}
