package io.bitcoinsv.jcl.net.protocol.serialization.common;


import com.nchain.jcl.net.protocol.messages.*;
import com.nchain.jcl.net.protocol.serialization.*;
import io.bitcoinsv.jcl.net.protocol.messages.*;
import io.bitcoinsv.jcl.net.protocol.serialization.*;
import io.bitcoinsv.jcl.net.protocol.serialization.largeMsgs.BigBlockDeserializer;
import io.bitcoinsv.jcl.net.protocol.serialization.largeMsgs.BigBlockRawDeserializer;
import io.bitcoinsv.jcl.net.protocol.serialization.largeMsgs.BigBlockTxnDeserializer;
import io.bitcoinsv.jcl.net.protocol.serialization.largeMsgs.LargeMessageDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * A registry of all the MessageBuilder and MessageSerializers developed. If you want to use the
 * {@link BitcoinMsgSerializer} to serialize/deserialize a new type of Message, make sure that the
 * Builders and Serializer Classes for that Message are registered here.
 */
public class MsgSerializersFactory {

    // Regular Message Serializers:
    private static final Map<String, MessageSerializer> serializers = new HashMap<>();

    // Raw Message Serializers:
    private static final Map<String, MessageSerializer> rawSerializers = new HashMap<>();

    // Indicates if some Serializer have benn overwriten with their RAW Versions:
    private static boolean RAW_SERIALIZERS_ENABLED = false;

    static {

        // We register the message Serializers:
        // Only COMPLETE Message Serializers need to be registered here, but no harm in registering all
        // of them though

        serializers.put(HeaderMsg.MESSAGE_TYPE.toUpperCase(), HeaderMsgSerializer.getInstance());
        serializers.put(NetAddressMsg.MESSAGE_TYPE.toUpperCase(), NetAddressMsgSerializer.getInstance());
        serializers.put(VarIntMsg.MESSAGE_TYPE.toUpperCase(), VarIntMsgSerializer.getInstance());
        serializers.put(VarStrMsg.MESSAGE_TYPE.toUpperCase(), VarStrMsgSerializer.getinstance());
        serializers.put(VersionAckMsg.MESSAGE_TYPE.toUpperCase(), VersionAckMsgSerializer.getInstance());
        serializers.put(VersionMsg.MESSAGE_TYPE.toUpperCase(), VersionMsgSerializer.getInstance());
        serializers.put(RejectMsg.MESSAGE_TYPE.toUpperCase(), RejectMsgSerializer.getInstance());
        serializers.put(PingMsg.MESSAGE_TYPE.toUpperCase(), PingMsgSerializer.getInstance());
        serializers.put(PongMsg.MESSAGE_TYPE.toUpperCase(), PongMsgSerializer.getInstance());
        serializers.put(GetAddrMsg.MESSAGE_TYPE.toUpperCase(), GetAddrMsgSerializer.getInstance());
        serializers.put(AddrMsg.MESSAGE_TYPE.toUpperCase(), AddrMsgSerialzer.getInstance());
        serializers.put(InventoryVectorMsg.MESSAGE_TYPE.toUpperCase(), InventoryVectorMsgSerializer.getInstance());
        serializers.put(InvMessage.MESSAGE_TYPE.toUpperCase(), InvMsgSerializer.getInstance());
        serializers.put(GetdataMsg.MESSAGE_TYPE.toUpperCase(), GetdataMsgSerializer.getInstance());
        serializers.put(NotFoundMsg.MESSAGE_TYPE.toUpperCase(), NotFoundMsgSerilaizer.getInstance());
        serializers.put(GetBlocksMsg.MESSAGE_TYPE.toUpperCase(), GetblocksMsgSerializer.getInstance());
        serializers.put(GetHeadersMsg.MESSAGE_TYPE.toUpperCase(), GetHeadersMsgSerializer.getInstance());
        serializers.put(BaseGetDataAndHeaderMsg.MESSAGE_TYPE.toUpperCase(), BaseGetDataAndHeaderMsgSerializer.getInstance());
        serializers.put(TxOutPointMsg.MESSAGE_TYPE.toUpperCase(), TxOutPointMsgSerializer.getInstance());
        serializers.put(TxInputMsg.MESSAGE_TYPE.toUpperCase(), TxInputMsgSerializer.getInstance());
        serializers.put(TxMsg.MESSAGE_TYPE.toUpperCase(), TxMsgSerializer.getInstance());
        serializers.put(BlockMsg.MESSAGE_TYPE.toUpperCase(), BlockMsgSerializer.getInstance());
        serializers.put(CompactBlockHeaderMsg.MESSAGE_TYPE.toUpperCase(), CompactBlockHeaderMsgSerializer.getInstance());
        serializers.put(BlockHeaderMsg.MESSAGE_TYPE.toUpperCase(), BlockHeaderMsgSerializer.getInstance());
        serializers.put(FeeFilterMsg.MESSAGE_TYPE.toUpperCase(), FeeFilterMsgSerializer.getInstance());
        serializers.put(HeadersMsg.MESSAGE_TYPE.toUpperCase(), HeadersMsgSerializer.getInstance());
        serializers.put(MemPoolMsg.MESSAGE_TYPE.toUpperCase(), MemPoolMsgSerializer.getInstance());
        serializers.put(SendHeadersMsg.MESSAGE_TYPE.toUpperCase(), SendHeadersMsgSerializer.getInstance());
        serializers.put(GetHeadersEnMsg.MESSAGE_TYPE.toUpperCase(), GetHeadersEnMsgSerializer.getInstance());
        serializers.put(BlockHeaderEnMsg.MESSAGE_TYPE.toUpperCase(), BlockHeaderEnMsgSerializer.getInstance());
        serializers.put(HeadersEnMsg.MESSAGE_TYPE.toUpperCase(), HeadersEnMsgSerializer.getInstance());
        serializers.put(PrefilledTxMsg.MESSAGE_TYPE.toUpperCase(), PrefilledTxMsgSerializer.getInstance());
        serializers.put(CompactBlockMsg.MESSAGE_TYPE.toUpperCase(), CompactBlockMsgSerializer.getInstance());
        serializers.put(SendCompactBlockMsg.MESSAGE_TYPE.toUpperCase(), SendCompactBlockMsgSerializer.getInstance());
        serializers.put(GetBlockTxnMsg.MESSAGE_TYPE.toUpperCase(), GetBlockTxnMsgSerializer.getInstance());
        serializers.put(BlockTxnMsg.MESSAGE_TYPE.toUpperCase(), BlockTxnMsgSerializer.getInstance());

        rawSerializers.put(RawTxMsg.MESSAGE_TYPE.toUpperCase(), RawTxMsgSerializer.getInstance());
        rawSerializers.put(RawBlockMsg.MESSAGE_TYPE.toUpperCase(), RawBlockMsgSerializer.getInstance());
    }

    private MsgSerializersFactory() {
    }

    /**
     * Returns a Serializer of the message specify by its COMMAND
     */
    public static MessageSerializer getSerializer(String command) {
        return serializers.get(command.toUpperCase());
    }

    /**
     * We overwrite regular Serializers with their raw versions
     */
    public static void enableRawSerializers() {
        RAW_SERIALIZERS_ENABLED = true;
        rawSerializers.entrySet().forEach(entry -> serializers.put(entry.getKey(), entry.getValue()));
    }

    /**
     * It returns an instance of a Deserializer for Large Messages. The Deserializers for Large Messages have STATE
     * (they need to store the callbacks that will be triggered when different parts of the Message are deserialized)
     * , so that means that they are NOT singletons. We need to create one new instance every time we need to
     * deserialize a message
     *
     * @param command Message Type to Deserialize
     */
    public static LargeMessageDeserializer getLargeMsgDeserializer(String command, int minBytesPerSec) {
        LargeMessageDeserializer result = null;

        // We need to instantiate each Serializer manually, based on the COMMAND and whether the RAW versions of
        // the serializers are enabled or not.
        // NOTE: WE use the Constructor without arguments, that means that the Batches returned by these serializers
        // will be triggered in this same Thread ina blocking way, but that's all right since the LargeDeserializers
        // are already running in their own Thread.

        if (command.equalsIgnoreCase(BlockMsg.MESSAGE_TYPE)) {
            result = (RAW_SERIALIZERS_ENABLED) ? new BigBlockRawDeserializer() : new BigBlockDeserializer();
        } else if (command.equalsIgnoreCase(BlockTxnMsg.MESSAGE_TYPE)) {
            result = new BigBlockTxnDeserializer();
        } else {
            System.out.println("SHOULD NEVER HAPPEN: command=" + command);
        }

        if (result != null) {
            result.setMinSpeedBytesPerSec(minBytesPerSec);
        }

        return result;
    }

    /**
     * Indicates if there is a Serializer register for this Message.
     */
    public static boolean hasSerializerFor(String command, boolean onlyForLargeMessages) {
        boolean result = (!onlyForLargeMessages) ? serializers.containsKey(command.toUpperCase()) : (getLargeMsgDeserializer(command, 0) != null);
        return result;
    }

}
