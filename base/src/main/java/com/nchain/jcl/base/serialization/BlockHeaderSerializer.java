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

        long version = byteReader.readUint32();
        Sha256Wrapper prevBlockHash = BitcoinSerializerUtils.deserializeHash(byteReader);
        Sha256Wrapper merkleRoot = BitcoinSerializerUtils.deserializeHash(byteReader);
        long creationTime = byteReader.readUint32();
        long difficultyTarget = byteReader.readUint32();
        long nonce = byteReader.readUint32();

         // We return the Header
        BlockHeader result = BlockHeaderBean.builder()
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
        byteWriter.writeUint32LE(object.getVersion());
        byteWriter.write(object.getPrevBlockHash().getReversedBytes());
        byteWriter.write(object.getMerkleRoot().getReversedBytes());
        byteWriter.writeUint32LE(object.getTime());
        byteWriter.writeUint32LE(object.getDifficultyTarget());
        byteWriter.writeUint32LE(object.getNonce());
    }
}
