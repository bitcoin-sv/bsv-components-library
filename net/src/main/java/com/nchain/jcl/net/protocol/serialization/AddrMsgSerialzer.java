package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.net.protocol.messages.AddrMsg;
import com.nchain.jcl.net.protocol.messages.NetAddressMsg;
import com.nchain.jcl.net.protocol.messages.VarIntMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;

import java.util.ArrayList;
import java.util.List;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer for  {@link AddrMsg} messages
 */
public class AddrMsgSerialzer implements MessageSerializer<AddrMsg> {

    private static AddrMsgSerialzer instance;


    private AddrMsgSerialzer() { }

    /** Returns the instance of this Serializer (Singleton) */
    public static AddrMsgSerialzer getInstance() {
        if (instance == null) {
            synchronized (AddrMsgSerialzer.class) {
                instance = new AddrMsgSerialzer();
            }
        }
        return instance;
    }

    @Override
    public AddrMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {

        // "count" field:
        VarIntMsg count = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);

        // List of addresses:
        List<NetAddressMsg> netAddressMsgs = new ArrayList<>() ;
        for (int i = 0; i < count.getValue(); i++) {
            NetAddressMsg addressMsg = NetAddressMsgSerializer.getInstance().deserialize(context, byteReader);
            netAddressMsgs.add(addressMsg);
        } // for...

        // We build the object:
        AddrMsg addrMsg =  AddrMsg.builder().addrList(netAddressMsgs).build();

        return addrMsg;
    }

    @Override
    public void serialize(SerializerContext context, AddrMsg message, ByteArrayWriter byteWriter) {
        VarIntMsgSerializer.getInstance().serialize(context, message.getCount(), byteWriter);
        for (NetAddressMsg addressMsg: message.getAddrList()) {
            NetAddressMsgSerializer.getInstance().serialize(context, addressMsg, byteWriter);
        }
    }
}
