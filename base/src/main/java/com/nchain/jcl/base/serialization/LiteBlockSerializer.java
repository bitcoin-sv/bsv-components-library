package com.nchain.jcl.base.serialization;

import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.domain.api.extended.BlockMeta;
import com.nchain.jcl.base.domain.api.extended.ChainInfo;
import com.nchain.jcl.base.domain.api.extended.LiteBlock;
import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2020-09-16
 */
public class LiteBlockSerializer implements BitcoinSerializer<LiteBlock> {

    private static LiteBlockSerializer instance;

    private LiteBlockSerializer() {}

    public static LiteBlockSerializer getInstance() {
        if (instance == null) {
            synchronized (LiteBlockSerializer.class) {
                instance = new LiteBlockSerializer();
            }
        }
        return instance;
    }

    @Override
    public LiteBlock deserialize(ByteArrayReader byteReader) {
        long currentReaderPosition = byteReader.getBytesReadCount();

        BlockHeader blockMeader = BlockHeaderSerializer.getInstance().deserialize(byteReader);
        BlockMeta blockMeta = BlockMetaSerializer.getInstance().deserialize(byteReader);
        ChainInfo chainInfo = ChainInfoSerializer.getInstance().deserialize(byteReader);

        long finalReaderPosition = byteReader.getBytesReadCount();
        long objectSize = finalReaderPosition - currentReaderPosition;

        return LiteBlock.builder()
                .sizeInBytes(objectSize)
                .header(blockMeader)
                .blockMeta(blockMeta)
                .chainInfo(chainInfo)
                .build();
    }

    @Override
    public void serialize(LiteBlock object, ByteArrayWriter byteWriter) {
        BlockHeaderSerializer.getInstance().serialize(object.getHeader(), byteWriter);
        BlockMetaSerializer.getInstance().serialize(object.getBlockMeta(), byteWriter);
        ChainInfoSerializer.getInstance().serialize(object.getChainInfo(), byteWriter);
    }

}
