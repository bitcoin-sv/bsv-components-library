package com.nchain.jcl.base.serialization;

import com.nchain.jcl.base.domain.api.extended.ChainInfo;
import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;
import static com.google.common.base.Preconditions.checkState;

import java.math.BigInteger;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2020-09-16
 */
public class ChainInfoSerializer implements BitcoinSerializer<ChainInfo> {
    private static ChainInfoSerializer instance;

    private ChainInfoSerializer() {}

    public static ChainInfoSerializer getInstance() {
        if (instance == null) {
            synchronized (ChainInfoSerializer.class) {
                instance = new ChainInfoSerializer();
            }
        }
        return instance;
    }

    @Override
    public ChainInfo deserialize(ByteArrayReader byteReader) {
        BigInteger chainWork;
        long chainWorkLength = BitcoinSerializerUtils.deserializeVarInt(byteReader);
        if (chainWorkLength > 0) {
            byte[] chainWorkBytes = byteReader.read((int)chainWorkLength);
            chainWork = new BigInteger(chainWorkBytes);
        } else chainWork = BigInteger.ZERO;

        int height = (int)byteReader.readUint32();
        long totalChainTxs = byteReader.readInt64LE();

        ChainInfo result = ChainInfo.builder()
                .height(height)
                .chainWork(chainWork)
                .totalChainTxs(totalChainTxs)
                .build();
        return result;
    }

    @Override
    public void serialize(ChainInfo object, ByteArrayWriter byteWriter) {
        byte[] chainWorkBytes = object.getChainWork().toByteArray();
        checkState(chainWorkBytes.length < 128, "Ran out of space to store chain work!");
        BitcoinSerializerUtils.serializeVarInt(chainWorkBytes.length, byteWriter);
        if (chainWorkBytes.length > 0) byteWriter.write(chainWorkBytes);
        byteWriter.writeUint32LE(object.getHeight());
        byteWriter.writeUint64LE(object.getTotalChainTxs());
    }

}
