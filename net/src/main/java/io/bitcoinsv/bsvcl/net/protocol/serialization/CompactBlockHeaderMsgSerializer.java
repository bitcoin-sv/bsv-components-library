package io.bitcoinsv.bsvcl.net.protocol.serialization;


import io.bitcoinsv.bsvcl.net.protocol.serialization.common.DeserializerContext;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.MessageSerializer;
import io.bitcoinsv.bsvcl.net.protocol.serialization.common.SerializerContext;
import io.bitcoinsv.bsvcl.net.protocol.messages.BlockHeaderMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.CompactBlockHeaderMsg;
import io.bitcoinsv.bsvcl.net.protocol.messages.HashMsg;
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.bsvcl.tools.bytes.ByteArrayWriter;
import io.bitcoinsv.bitcoinjsv.core.Sha256Hash;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * <p>
 * A Serializer for {@link BlockHeaderMsg}
 */
public class CompactBlockHeaderMsgSerializer implements MessageSerializer<CompactBlockHeaderMsg> {

    protected static final int HEADER_LENGTH = 80; // Block header length (up to the "nonce" field, included)

    private static CompactBlockHeaderMsgSerializer instance;

    public static CompactBlockHeaderMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (CompactBlockHeaderMsgSerializer.class) {
                instance = new CompactBlockHeaderMsgSerializer();
            }
        }
        return instance;
    }

    protected CompactBlockHeaderMsgSerializer() {
    }

    @Override
    public CompactBlockHeaderMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {

        byte[] blockHeaderBytes = byteReader.read(HEADER_LENGTH);

        HashMsg hash = HashMsg.builder().hash(
            Sha256Hash.wrap(
                Sha256Hash.twiceOf(blockHeaderBytes).getBytes()).getBytes()
        ).build();

        // String blockHashStr = HEX.encode(hash.getHashBytes());

        // We create a Reader on the Header Bytes, since we need those values again now to serialize the
        // whole Header...
        ByteArrayReader headerReader = new ByteArrayReader(blockHeaderBytes);

        // Now we de-serialize the Block.
        // These values are taken from the Block Header...

        long version = headerReader.readUint32();
        HashMsg prevBlockHash = HashMsg.builder().hash(HashMsgSerializer.getInstance().deserialize(context, headerReader).getHashBytes()).build();
        HashMsg merkleRoot = HashMsg.builder().hash(HashMsgSerializer.getInstance().deserialize(context, headerReader).getHashBytes()).build();
        long creationTime = headerReader.readUint32();
        long difficultyTarget = headerReader.readUint32();
        long nonce = headerReader.readUint32();

        // We return the Header
        return CompactBlockHeaderMsg.builder()
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
    public void serialize(SerializerContext context, CompactBlockHeaderMsg message, ByteArrayWriter byteWriter) {
        byteWriter.writeUint32LE(message.getVersion());
        byteWriter.write(message.getPrevBlockHash().getHashBytes());
        byteWriter.write(message.getMerkleRoot().getHashBytes());
        byteWriter.writeUint32LE(message.getCreationTimestamp());
        byteWriter.writeUint32LE(message.getDifficultyTarget());
        byteWriter.writeUint32LE(message.getNonce());
    }
}
