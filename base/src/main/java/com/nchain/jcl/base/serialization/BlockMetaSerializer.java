package com.nchain.jcl.base.serialization;

import com.nchain.jcl.base.domain.api.extended.BlockMeta;
import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class BlockMetaSerializer implements BitcoinSerializer<BlockMeta> {
    private static BlockMetaSerializer instance;

    private BlockMetaSerializer() {}

    public static BlockMetaSerializer getInstance() {
        if (instance == null) {
            synchronized (BlockMetaSerializer.class) {
                instance = new BlockMetaSerializer();
            }
        }
        return instance;
    }

    @Override
    public BlockMeta deserialize(ByteArrayReader byteReader) {
        long currentReaderPosition = byteReader.getBytesReadCount();
        int txCount = (int) byteReader.readUint32();
        long blockSize = byteReader.readInt64LE();
        long finalReaderPosition = byteReader.getBytesReadCount();
        long objectSize = finalReaderPosition - currentReaderPosition;
        return BlockMeta.builder().sizeInBytes(objectSize).txCount(txCount).blockSize(blockSize).build();
    }

    @Override
    public void serialize(BlockMeta object, ByteArrayWriter byteWriter) {
        byteWriter.writeUint32LE(object.getTxCount());
        byteWriter.writeUint64LE(object.getBlockSize());
    }

}
