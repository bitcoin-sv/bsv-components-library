package io.bitcoinsv.jcl.net.protocol.messages;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.bitcoinsv.jcl.net.protocol.messages.common.BodyMessage;

import java.io.Serializable;
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
public final class AddrMsg extends BodyMessage implements Serializable {

    private static final long MAX_ADDRESSES = 1000;
    public static final String MESSAGE_TYPE = "addr";

    private final VarIntMsg count;
    private final ImmutableList<NetAddressMsg> addrList;

    /**
     * Creates the AddrMsg Object.Use the corresponding byteArray to create the instance.
     */
    protected AddrMsg( List<NetAddressMsg> addrList, byte[]extraBytes, long checksum) {
        super(extraBytes, checksum);
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
    public String getMessageType()                      { return MESSAGE_TYPE; }
    public VarIntMsg getCount()                         { return this.count; }
    public ImmutableList<NetAddressMsg> getAddrList()   { return this.addrList; }

    @Override
    public String toString() {
        return "AddrMsg(count=" + this.getCount() + ", addrList=" + this.getAddrList() + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), addrList, count);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) { return false;}
        AddrMsg other = (AddrMsg) obj;
        return Objects.equal(this.addrList, other.addrList)
                && Objects.equal(this.count, other.count);
    }

    @Override
    public AddrMsgBuilder toBuilder() {
        return new AddrMsgBuilder(super.extraBytes, super.checksum).addrList(this.addrList);
    }

    public static AddrMsgBuilder builder() {
        return new AddrMsgBuilder();
    }

    /**
     * Builder
     */
    public static class AddrMsgBuilder extends BodyMessageBuilder {
        private List<NetAddressMsg> addrList;

        AddrMsgBuilder() {}
        AddrMsgBuilder(byte[] extraBytes, long checksum) { super(extraBytes, checksum);}

        public AddrMsg.AddrMsgBuilder addrList(List<NetAddressMsg> addrList) {
            this.addrList = addrList;
            return this;
        }

        public AddrMsg build() {
            return new AddrMsg(addrList, super.extraBytes, super.checksum);
        }
    }
}
