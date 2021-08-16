package io.bitcoinsv.jcl.net.protocol.serialization;


import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import io.bitcoinsv.jcl.net.protocol.messages.RawTxMsg;

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
        // We calculate the Hash, in human.readable format (reversed)
        // TESTING THE Guava Implementation:
        //Sha256Hash txHash =  Sha256Hash.wrapReversed(Sha256Hash.twiceOf(content).getBytes());
        //Sha256Hash txHash =  Sha256Hash.wrapReversed(hashFunction.hashBytes(hashFunction.hashBytes(content).asBytes()).asBytes());

        // We return the object
        return new RawTxMsg(content);
    }

}
