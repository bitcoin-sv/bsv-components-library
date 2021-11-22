package com.nchain.jcl.net.protocol.serialization;

import com.nchain.jcl.net.protocol.messages.BlockHeaderSimpleMsg;
import com.nchain.jcl.net.protocol.messages.HashMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;
import io.bitcoinj.core.Sha256Hash;

/**
 * @author m.fletcher@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This is the same as the regular BH serializer, except it doesn't include tx count.
 */
public class BlockHeaderSimpleMsgSerializer implements MessageSerializer<BlockHeaderSimpleMsg> {

    protected static final int HEADER_LENGTH = 80; // Block header length (up to the "nonce" field, included)

    private static BlockHeaderSimpleMsgSerializer instance;

    protected BlockHeaderSimpleMsgSerializer() {
    }

    public static BlockHeaderSimpleMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (BlockHeaderSimpleMsgSerializer.class) {
                instance = new BlockHeaderSimpleMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public BlockHeaderSimpleMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {

        byte[] blockHeaderBytes = byteReader.read(HEADER_LENGTH);

        HashMsg hash = HashMsg.builder().hash(
            Sha256Hash.wrap(
                Sha256Hash.twiceOf(blockHeaderBytes).getBytes()).getBytes()
        ).build();

        ByteArrayReader headerReader = new ByteArrayReader(blockHeaderBytes);

        long version = headerReader.readUint32();
        HashMsg prevBlockHash = HashMsg.builder().hash(HashMsgSerializer.getInstance().deserialize(context, headerReader).getHashBytes()).build();
        HashMsg merkleRoot = HashMsg.builder().hash(HashMsgSerializer.getInstance().deserialize(context, headerReader).getHashBytes()).build();
        long creationTime = headerReader.readUint32();
        long difficultyTarget = headerReader.readUint32();
        long nonce = headerReader.readUint32();

        return BlockHeaderSimpleMsg.builder()
            .hash(hash)
            .version(version)
            .prevBlockHash(prevBlockHash)
            .merkleRoot(merkleRoot)
            .creationTimestamp(creationTime)
            .difficultyTarget(difficultyTarget)
            .nonce(nonce)
            .build();
    }

    @Override
    public void serialize(SerializerContext context, BlockHeaderSimpleMsg message, ByteArrayWriter byteWriter) {
        byteWriter.writeUint32LE(message.getVersion());
        byteWriter.write(message.getPrevBlockHash().getHashBytes());
        byteWriter.write(message.getMerkleRoot().getHashBytes());
        byteWriter.writeUint32LE(message.getCreationTimestamp());
        byteWriter.writeUint32LE(message.getDifficultyTarget());
        byteWriter.writeUint32LE(message.getNonce());
    }

}