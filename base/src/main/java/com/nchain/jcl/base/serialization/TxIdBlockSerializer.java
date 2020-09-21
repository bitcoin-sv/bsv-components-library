package com.nchain.jcl.base.serialization;

import com.nchain.jcl.base.domain.api.base.BlockHeader;

import com.nchain.jcl.base.domain.api.extended.TxIdBlock;
import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
public class TxIdBlockSerializer implements BitcoinSerializer<TxIdBlock> {

    private static TxIdBlockSerializer instance;

    private TxIdBlockSerializer() {}

    public static TxIdBlockSerializer getInstance() {
        if (instance == null) {
            synchronized (TxIdBlockSerializer.class) {
                instance = new TxIdBlockSerializer();
            }
        }
        return instance;
    }

    @Override
    public TxIdBlock deserialize(ByteArrayReader byteReader) {
        long curentReaderPosition = byteReader.getBytesReadCount();

        BlockHeader header = (BlockHeader) BitcoinSerializerFactory.deserialize(BlockHeader.class, byteReader);
        long numTxs = BitcoinSerializerUtils.deserializeVarInt(byteReader);
        List<Sha256Wrapper> txs = new ArrayList<>();
        for (int i = 0; i < numTxs; i++)
            txs.add(BitcoinSerializerUtils.deserializeHash(byteReader));

        long finalReaderPosition = byteReader.getBytesReadCount();
        long objectSize = finalReaderPosition - curentReaderPosition;

         TxIdBlock result = TxIdBlock.builder()
                    .sizeInBytes(objectSize)
                    .header(header)
                    .txids(txs)
                    .build();
        return result;
    }

    @Override
    public void serialize(TxIdBlock object, ByteArrayWriter byteWriter) {
        BitcoinSerializer headerSerializer = BitcoinSerializerFactory.getSerializer(BlockHeader.class);
        headerSerializer.serialize(object.getHeader(), byteWriter);
        BitcoinSerializerUtils.serializeVarInt(object.getTxids().size(), byteWriter);
        for (int i = 0; i < object.getTxids().size(); i++)
            BitcoinSerializerUtils.serializeHash(object.getTxids().get(i), byteWriter);
    }
}
