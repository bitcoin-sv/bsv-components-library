package com.nchain.jcl.store.keyValue.blockChainStore;

import com.nchain.jcl.base.serialization.BitcoinSerializerUtils;
import com.nchain.jcl.base.tools.bytes.ByteArrayReader;
import com.nchain.jcl.base.tools.bytes.ByteArrayWriter;

import java.math.BigInteger;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A serializer class for the BlockChainInfo class.
 */
public class BlockChainInfoSerializer {

    // Singleton:
    private static BlockChainInfoSerializer instance;

    /** Private Constructor. Use "getInstance" instead */
    private BlockChainInfoSerializer() {}

    /** Returns the instance of this Serialzier (Singleton) */
    public static BlockChainInfoSerializer getInstance() {
       synchronized (BlockChainInfoSerializer.class) {
           if (instance == null) instance = new BlockChainInfoSerializer();
       }
        return instance;
    }

    /** Serialize the object into raw format (byte array), using the Bitcoin Codification */
    public byte[] serialize(BlockChainInfo object) {
        if (object == null) return null;
        ByteArrayWriter writer = new ByteArrayWriter();
        BitcoinSerializerUtils.serializeVarStr(object.getBlockHash(), writer);
        BitcoinSerializerUtils.serializeVarInt(object.getChainWork().intValue(), writer);
        BitcoinSerializerUtils.serializeVarInt(object.getHeight(), writer);
        BitcoinSerializerUtils.serializeVarInt(object.getTotalChainSize(), writer);
        return writer.reader().getFullContentAndClose();
    }

    /** Deserialize the raw data (byte array) into a Java Object, using the Bitcoin Codification */
    public BlockChainInfo deserialize(byte[] raw) {
        if (raw == null) return null;
        BlockChainInfo.BlockChainInfoBuilder resultBuilder = BlockChainInfo.builder();
        ByteArrayReader reader = new ByteArrayReader(raw);
        resultBuilder
                .blockHash(BitcoinSerializerUtils.deserializeVarStr(reader))
                .chainWork(BigInteger.valueOf(BitcoinSerializerUtils.deserializeVarInt(reader)))
                .height((int) BitcoinSerializerUtils.deserializeVarInt(reader))
                .totalChainSize(BitcoinSerializerUtils.deserializeVarInt(reader))
                .build();
        return resultBuilder.build();
    }
}
