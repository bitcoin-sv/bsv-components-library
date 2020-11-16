package com.nchain.jcl.base.serialization;

import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.domain.bean.base.BlockHeaderBean;
import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class BlockHeaderSerializer implements BitcoinSerializer<BlockHeader> {

    private static BlockHeaderSerializer instance;

    private BlockHeaderSerializer() {}

    public static BlockHeaderSerializer getInstance() {
        if (instance == null) {
            synchronized (BlockHeaderSerializer.class) {
                instance = new BlockHeaderSerializer();
            }
        }
        return instance;
    }

    @Override
    public BlockHeader deserialize(ByteArrayReader byteReader) {
        long currentReaderPosition = byteReader.getBytesReadCount();

        long version = byteReader.readUint32();

        // IMPORTANT NOTE: This code does NOT work for a GENESIS Block. A Genesisi Block does NOT have a PARENT, so
        // there is NO "PrevBlockHash" info to read. The problem is that there is nothing in the BLOCK HEADER MSG that
        // can tell us whether this a Genesis Block or not.

        // This is usually OK because the Genesis Block is usually hardcoded in the Code, so there is no need to
        // Deserialize it.

        // NOTE: The "Number of TXs" field is NOT part of the Deserialization

        Sha256Wrapper prevBlockHash = BitcoinSerializerUtils.deserializeHash(byteReader);
        Sha256Wrapper merkleRoot = BitcoinSerializerUtils.deserializeHash(byteReader);
        long creationTime = byteReader.readUint32();
        long difficultyTarget = byteReader.readUint32();
        long nonce = byteReader.readUint32();

        // We calculate the size in bytes of this object...
        long finalReaderPosition = byteReader.getBytesReadCount();
        long sizeInBytes = finalReaderPosition - currentReaderPosition;

         // We return the Header
        BlockHeader result = BlockHeaderBean.builder()
                .sizeInBytes(sizeInBytes)
                .version(version)
                .prevBlockHash(prevBlockHash)
                .merkleRoot(merkleRoot)
                .time(creationTime)
                .difficultyTarget(difficultyTarget)
                .nonce(nonce)
                .build();

        return result;
    }

    @Override
    public void serialize(BlockHeader object, ByteArrayWriter byteWriter) {

        // NOTE: The "Number of TXs" field is NOT part of the Deserialization

        byteWriter.writeUint32LE(object.getVersion());

        // In case of a Genesis Block, there is no parent block:
        if (object.getPrevBlockHash() != null)
                BitcoinSerializerUtils.serializeHash(object.getPrevBlockHash(), byteWriter);
        else    BitcoinSerializerUtils.serializeHash(Sha256Wrapper.ZERO_HASH, byteWriter);

        BitcoinSerializerUtils.serializeHash(object.getMerkleRoot(), byteWriter);
        byteWriter.writeUint32LE(object.getTime());
        byteWriter.writeUint32LE(object.getDifficultyTarget());
        byteWriter.writeUint32LE(object.getNonce());
    }
}
