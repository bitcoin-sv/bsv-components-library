package com.nchain.jcl.protocol.serialization.common;



import com.nchain.jcl.protocol.messages.common.BitcoinMsg;
import com.nchain.jcl.protocol.messages.common.Message;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-07-14
 *
 * A serializer for {@link BitcoinMsg}
 */
public class BitcoinMsgSerializerKillableImpl<M extends Message> extends BitcoinMsgSerializerImpl implements BitcoinMsgSerializer {

    private MessageSerializer<M> bodySerializer;

    // Constructor
    private BitcoinMsgSerializerKillableImpl() {}

    public static <M extends Message> BitcoinMsgSerializerKillableImpl getKillableInstance() {
        return new BitcoinMsgSerializerKillableImpl<M>();
    }

    @Override
    protected MessageSerializer<M> getBodySerializer(String command) {
        bodySerializer = MsgSerializersFactory.getSerializer(command);
        return bodySerializer;
    }

    @Override
    public void kill() { if (bodySerializer != null) bodySerializer.kill();}

    @Override
    public boolean isKillable() { return true; }
}
