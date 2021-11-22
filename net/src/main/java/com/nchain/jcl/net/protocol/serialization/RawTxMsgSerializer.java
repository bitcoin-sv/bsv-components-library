package com.nchain.jcl.net.protocol.serialization;


import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.nchain.jcl.net.protocol.messages.*;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer for instance of {@Link RawTxMsg} messages
 */
public class RawTxMsgSerializer extends RawMsgSerializer<RawTxMsg> {
    private static RawTxMsgSerializer instance;
    private static HashFunction hashFunction = Hashing.sha256();

    private RawTxMsgSerializer() {}

    public static RawTxMsgSerializer getInstance(){
        if(instance == null) {
            synchronized (RawTxMsgSerializer.class) {
                instance = new RawTxMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public RawTxMsg buildRawMsg(byte[] content) {
        return new RawTxMsg(content, 0); // checksum ZERO
    }

}
