package com.nchain.jcl.net.protocol.serialization;

import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import com.nchain.jcl.net.protocol.messages.BlockHeaderEnrichedMsg;
import com.nchain.jcl.net.protocol.messages.HashMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;

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
        return null;
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


        //TODO ::Need to add coinbaseMerkleProof and coinbaseTx
    }

    private byte[] getBytesHash(HashMsg prevBlockHash) {
        return Sha256Wrapper.wrapReversed(prevBlockHash.getHashBytes()).getBytes();
    }
}
