package io.bitcoinsv.jcl.store.keyValue.blockChainStore;



import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;
import io.bitcoinsv.jcl.tools.serialization.BitcoinSerializerUtils;
import io.bitcoinsv.bitcoinjsv.core.Utils;

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

        // Chain Work Bytes:
        byte[] chainWorkBytes = object.getChainWork() == null ? Utils.EMPTY_BYTE_ARRAY : object.getChainWork().toByteArray();
        writer.writeUint16LE(chainWorkBytes.length);
        writer.write(chainWorkBytes);

        BitcoinSerializerUtils.serializeVarInt(object.getHeight(), writer);
        BitcoinSerializerUtils.serializeVarInt(object.getTotalChainSize(), writer);
        writer.writeUint32LE(object.getChainPathId());

        return writer.reader().getFullContentAndClose();
    }

    /** Deserialize the raw data (byte array) into a Java Object, using the Bitcoin Codification */
    public BlockChainInfo deserialize(byte[] raw) {
        if (raw == null) return null;
        BlockChainInfo.BlockChainInfoBuilder resultBuilder = BlockChainInfo.builder();
        ByteArrayReader reader = new ByteArrayReader(raw);

        var blockHash = BitcoinSerializerUtils.deserializeVarStr(reader);

        // Read number of chainwork bytes
        var length = reader.readUint16();
        var chainWork = new BigInteger(1, reader.read(length));

        var height = (int) BitcoinSerializerUtils.deserializeVarInt(reader);
        var chainSize = BitcoinSerializerUtils.deserializeVarInt(reader);
        var pathId = (int)reader.readUint32();

        return resultBuilder
                .blockHash(blockHash)
                .chainWork(chainWork)
                .height(height)
                .totalChainSize(chainSize)
                .chainPathId(pathId)
                .build();
    }
}
