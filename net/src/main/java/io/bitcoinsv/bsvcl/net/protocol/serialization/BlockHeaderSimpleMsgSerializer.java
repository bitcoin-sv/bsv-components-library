package io.bitcoinsv.bsvcl.net.protocol.serialization;

import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.bsvcl.net.protocol.messages.BlockHeaderSimpleMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayWriter;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;

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

        // Since this Hash is stored in a Field that is NOT part of the real message and
        // its only a convenience field, we are storing it in the human-readable way (reversed)
        Sha256Hash hash = Sha256Hash.wrapReversed(Sha256Hash.twiceOf(blockHeaderBytes).getBytes());

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