package com.nchain.jcl.net.protocol.serialization;

import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import com.nchain.jcl.net.protocol.messages.*;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author m.jose@nchain.com
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 *
 * @date 03/10/2019
 *
 * A Serializer for instance of {@Link TransactionMsg} messages
 */
public class TxMsgSerializer implements MessageSerializer<TxMsg> {
    private static TxMsgSerializer instance;

    // Reference to singleton instances used during serialization/Deserialization. Defined here for performance
    private static VarIntMsgSerializer          varIntMsgSerializer         = VarIntMsgSerializer.getInstance();
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
        byteReader.waitForBytes(4);
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
        byteReader.waitForBytes(4);
        long locktime = byteReader.readUint32();

        TxMsg.TxMsgBuilder txBuilder =  TxMsg.builder()
                .version(version)
                .tx_in(txInputMessage)
                .tx_out(txOutputMessage)
                .lockTime(locktime);


        //we don't want to calculate the hash when running within an optimized context
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
            HashMsg txHash =  HashMsg.builder().hash(
                    Sha256Wrapper.wrapReversed(
                            Sha256Wrapper.twiceOf(txBytes).getBytes()).getBytes())
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