package com.nchain.jcl.net.protocol.serialization;


import com.nchain.jcl.net.protocol.messages.BlockHeaderMsg;
import com.nchain.jcl.net.protocol.messages.HashMsg;
import com.nchain.jcl.net.protocol.messages.VarIntMsg;
import com.nchain.jcl.net.protocol.serialization.common.DeserializerContext;
import com.nchain.jcl.net.protocol.serialization.common.MessageSerializer;
import com.nchain.jcl.net.protocol.serialization.common.SerializerContext;
import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;
import io.bitcoinj.core.Sha256Hash;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer for {@link BlockHeaderMsg}
 */
public class BlockHeaderMsgSerializer implements MessageSerializer<BlockHeaderMsg> {

    private static BlockHeaderMsgSerializer instance;
    private static final int HEADER_LENGTH = 80; // Block header length (up to the "nonce" field, included)


    private BlockHeaderMsgSerializer() { }

    /**
     * Returns the instance of this Serializer (Singleton)
     */
    public static BlockHeaderMsgSerializer getInstance() {
        if (instance == null) {
            synchronized (BlockHeaderMsgSerializer.class) {
                instance = new BlockHeaderMsgSerializer();
            }
        }
        return instance;
    }

    @Override
    public BlockHeaderMsg deserialize(DeserializerContext context, ByteArrayReader byteReader) {

        byteReader.waitForBytes(HEADER_LENGTH);
        byte[] blockHeaderBytes = byteReader.read(HEADER_LENGTH);

        HashMsg hash =  HashMsg.builder().hash(
                Sha256Hash.wrap(
                        Sha256Hash.twiceOf(blockHeaderBytes).getBytes()).getBytes())
                .build();

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
        VarIntMsg varIntTransactionCount = VarIntMsgSerializer.getInstance().deserialize(context, byteReader);
        int transactionCount = (int) varIntTransactionCount.getValue();

        // We return the Header

        return BlockHeaderMsg.builder()
                .hash(hash)
                .version(version)
                .prevBlockHash(prevBlockHash)
                .merkleRoot(merkleRoot)
                .creationTimestamp(creationTime)
                .difficultyTarget(difficultyTarget)
                .nonce(nonce)
                .transactionCount(transactionCount)
                .build();

    }

    @Override
    public void serialize(SerializerContext context, BlockHeaderMsg message, ByteArrayWriter byteWriter) {
        byteWriter.writeUint32LE(message.getVersion());
        byteWriter.write(message.getPrevBlockHash().getHashBytes());
        byteWriter.write(message.getMerkleRoot().getHashBytes());
        byteWriter.writeUint32LE(message.getCreationTimestamp());
        byteWriter.writeUint32LE(message.getDifficultyTarget());
        byteWriter.writeUint32LE(message.getNonce());
        VarIntMsgSerializer.getInstance().serialize(context, message.getTransactionCount(), byteWriter);
    }

}
