package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.net.protocol.messages.*;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;
import io.bitcoinj.core.Sha256Hash;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer for instance of {@Link TransactionMsg} messages
 */
public class TxMsgSerializer implements MessageSerializer<TxMsg> {
    private static TxMsgSerializer instance;

    // Reference to singleton instances used during serialization/Deserialization. Defined here for performance
    private static VarIntMsgSerializer varIntMsgSerializer         = VarIntMsgSerializer.getInstance();
    private static TxInputMsgSerializer txInputMessageSerializer    = TxInputMsgSerializer.getInstance();
    private static TxOutputMsgSerializer txOutputMessageSerializer   = TxOutputMsgSerializer.getInstance();

    private TxMsgSerializer() {}

    public static TxMsgSerializer getInstance(){
        if(instance == null) {
            synchronized (TxMsgSerializer.class) {
                instance = new TxMsgSerializer();
            }
        }

        return instance;
    }

    // TODO: CHECK PERFORMANCE WHEN CALCULATING THE HASH!!!!!!!!
    @Override
    public TxMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {

        // We deserialize the Tx the usual way...
        long version = byteReader.readUint32();
        VarIntMsg txInCount = varIntMsgSerializer.deserialize(context, byteReader);
        int txInCountValue = (int) txInCount.getValue();
        List<TxInputMsg> txInputMessage = new ArrayList<>();

        for(int i =0 ; i< txInCountValue; i++) {
            txInputMessage.add(txInputMessageSerializer.deserialize(context,byteReader));
        }

        VarIntMsg txOutCount = varIntMsgSerializer.deserialize(context, byteReader);
        int txOutCountValue = (int) txOutCount.getValue();
        List<TxOutputMsg> txOutputMessage = new ArrayList<>();

        for(int i =0 ; i< txOutCountValue; i++) {
            txOutputMessage.add(txOutputMessageSerializer.deserialize(context, byteReader));
        }
        long locktime = byteReader.readUint32();

        TxMsg.TxMsgBuilder txBuilder =  TxMsg.builder()
                .version(version)
                .tx_in(txInputMessage)
                .tx_out(txOutputMessage)
                .lockTime(locktime);


        // We only calculate the Hash if it¡s specified.
        if (context.isCalculateHashes()) {
            // To calculate the TX Hash, we need to Serialize the TX itself...
            SerializerContext serializerContext = SerializerContext.builder()
                    .protocolBasicConfig(context.getProtocolBasicConfig())
                    .insideVersionMsg(context.isInsideVersionMsg())
                    .build();
            ByteArrayWriter writer = new ByteArrayWriter();

            TxMsg tx = txBuilder.build();
            serialize(serializerContext, tx, writer);

            byte[] txBytes = writer.reader().getFullContentAndClose();
            // Since this Hash is stored in a Field that is NOT part of the real message and
            // its only a convenience field, we are storing it in the human-readable way (reversed)
            HashMsg txHash =  HashMsg.builder().hash(
                    Sha256Hash.wrapReversed(
                            Sha256Hash.twiceOf(txBytes).getBytes()).getBytes())
                    .build();
            txBuilder.hash(Optional.of(txHash));
        } else txBuilder.hash(Optional.empty());

        TxMsg result = txBuilder.build();
        return result;
    }

    @Override
    public void serialize(SerializerContext context, TxMsg message, ByteArrayWriter byteWriter) {
        byteWriter.writeUint32LE(message.getVersion());
        varIntMsgSerializer.serialize(context, message.getTx_in_count(), byteWriter);
        for(TxInputMsg txInputMessage: message.getTx_in()) {
            txInputMessageSerializer.serialize(context, txInputMessage, byteWriter);
        }
        VarIntMsgSerializer.getInstance().serialize(context, message.getTx_out_count(), byteWriter);
        for(TxOutputMsg txOutputMessage: message.getTx_out()){
            txOutputMessageSerializer.serialize(context,txOutputMessage, byteWriter);
        }
        byteWriter.writeUint32LE(message.getLockTime());
    }
}
