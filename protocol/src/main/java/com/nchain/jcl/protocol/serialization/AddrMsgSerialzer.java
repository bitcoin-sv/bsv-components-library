package com.nchain.jcl.protocol.serialization;



import com.nchain.jcl.protocol.messages.*;
import com.nchain.jcl.protocol.serialization.common.*;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2009-2010 Satoshi Nakamoto
 * Copyright (c) 2009-2016 The Bitcoin Core developers
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 01/08/2019 13:22
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
