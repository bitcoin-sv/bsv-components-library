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

/**
 * @author m.jose@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 *  A Serializer for {@link BlockHeaderEnrichedMsg} messages
 */
public class BlockHeaderEnMsgSerializer implements MessageSerializer<BlockHeaderEnrichedMsg> {

    private static BlockHeaderEnMsgSerializer instance;

    private BlockHeaderEnMsgSerializer() { }

    public static  BlockHeaderEnMsgSerializer getInstance() {
        if( instance == null) {
            synchronized (BlockHeaderEnMsgSerializer.class) {
                instance = new BlockHeaderEnMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public BlockHeaderEnrichedMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {
        byteReader.waitForBytes(4);
        long version = byteReader.readUint32();

        BaseGetDataAndHeaderMsgSerializer baseGetDataAndHeaderMsgSerializer =  BaseGetDataAndHeaderMsgSerializer.getInstance();
        HashMsg prevBlockHash = baseGetDataAndHeaderMsgSerializer.readHashMsg(context, byteReader);
        HashMsg merkleRoot = baseGetDataAndHeaderMsgSerializer.readHashMsg(context, byteReader);

        byteReader.waitForBytes(BlockHeaderEnrichedMsg.TIMESTAMP_LENGTH+BlockHeaderEnrichedMsg.NONCE_LENGTH+BlockHeaderEnrichedMsg.NBITS_LENGTH+BlockHeaderEnrichedMsg.TX_CNT);
        long creationTime = byteReader.readUint32();
        long difficultyTarget = byteReader.readUint32();
        long nonce = byteReader.readUint32();
        long txCount = byteReader.readInt64LE();

        byteReader.waitForBytes(1);
        boolean noMoreHeaders = byteReader.readBoolean();

        byteReader.waitForBytes(1);
        boolean hasCoinbaseData = byteReader.readBoolean();

        BlockHeaderEnrichedMsg blockHeaderEnrichedMsg  ;
        if(hasCoinbaseData) {
            List hashes = new ArrayList<byte[]>();
            for(int i=0; i < txCount ; i++ ){
                hashes.add(baseGetDataAndHeaderMsgSerializer.readHashMsg(context, byteReader));
            }

            VarIntMsg txLengthValue = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);
            int coinBaseTxLength = (int) txLengthValue.getValue();

            byteReader.waitForBytes(coinBaseTxLength);
            byte[]  coinbaseTxBytes=  byteReader.get(coinBaseTxLength);

            //creating a coinbaseTX which is not part of the message ,
            TxMsg tx =  TxMsgSerializer.getInstance().deserialize(context, new ByteArrayReader(coinbaseTxBytes));

            VarStrMsg coinbase = VarStrMsgSerializer.getinstance().deserialize(new ByteArrayReader(coinbaseTxBytes),
                    coinbaseTxBytes.length);

            blockHeaderEnrichedMsg = BlockHeaderEnrichedMsg.builder()
                    .version(version)
                    .prevBlockHash(prevBlockHash).merkleRoot(merkleRoot).creationTimestamp(creationTime)
                    .nBits(difficultyTarget).nonce(nonce).transactionCount(txCount).hasCoinbaseData(hasCoinbaseData)
                    .noMoreHeaders(noMoreHeaders).coinbaseTX(tx).coinbase(coinbase).coinbaseMerkleProof(hashes).build();
        }  else {
            blockHeaderEnrichedMsg = BlockHeaderEnrichedMsg.builder()
                    .version(version)
                    .prevBlockHash(prevBlockHash).merkleRoot(merkleRoot).creationTimestamp(creationTime)
                    .nBits(difficultyTarget).nonce(nonce).transactionCount(txCount).hasCoinbaseData(hasCoinbaseData)
                    .noMoreHeaders(noMoreHeaders).build();
        }

        return blockHeaderEnrichedMsg;
    }


    @Override
    public void serialize(SerializerContext context, BlockHeaderEnrichedMsg message, ByteArrayWriter byteWriter) {
        byteWriter.writeUint32LE(message.getVersion());
        byteWriter.write(getBytesHash(message.getPrevBlockHash()));
        byteWriter.write(getBytesHash(message.getMerkleRoot()));
        byteWriter.writeUint32LE(message.getCreationTimestamp());
        byteWriter.writeUint32LE(message.getNBits());
        byteWriter.writeUint32LE(message.getNonce());

        // We write the "nTx" field. Long 8 Bytes
        byteWriter.writeUint64LE(message.getTransactionCount());

        byteWriter.writeBoolean(message.isHasCoinbaseData());
        byteWriter.writeBoolean(message.isNoMoreHeaders());

        // We are not using the HashMsgSerializer for serialize as
        // We have to flip it around, as it's been read off the wire in little endian.
        List<HashMsg> hashes = message.getCoinbaseMerkleProof();
        for(HashMsg merkleProofHash:hashes) {
            byteWriter.write(getBytesHash(merkleProofHash));
        }

        VarStrMsgSerializer.getinstance().serialize(context,message.getCoinbase(), byteWriter);
    }

    private byte[] getBytesHash(HashMsg hashMsg) {
        return Sha256Wrapper.wrapReversed(hashMsg.getHashBytes()).getBytes();
    }
}
