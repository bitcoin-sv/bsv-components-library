package io.bitcoinsv.jcl.store.keyValue.blockChainStore;


import io.bitcoinsv.jcl.tools.bytes.ByteArrayReader;
import io.bitcoinsv.jcl.tools.bytes.ByteArrayWriter;
import io.bitcoinsv.jcl.tools.serialization.BitcoinSerializerUtils;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A serializer class for the ChainBranchInfo class.
 */
public class ChainPathInfoSerializer {

    // Singleton:
    private static ChainPathInfoSerializer instance;

    /** Private Constructor. Use "getInstance" instead */
    private ChainPathInfoSerializer() {}

    /** Returns the instance of this Serialzier (Singleton) */
    public static ChainPathInfoSerializer getInstance() {
       synchronized (ChainPathInfoSerializer.class) {
           if (instance == null) instance = new ChainPathInfoSerializer();
       }
        return instance;
    }

    /** Serialize the object into raw format (byte array), using the Bitcoin Codification */
    public byte[] serialize(ChainPathInfo object) {
        if (object == null) return null;
        ByteArrayWriter writer = new ByteArrayWriter();
        writer.writeUint32LE(object.getId());
        writer.writeUint32LE(object.getParent_id());
        BitcoinSerializerUtils.serializeVarStr(object.getBlockHash(), writer);

        return writer.reader().getFullContentAndClose();
    }

    /** Deserialize the raw data (byte array) into a Java Object, using the Bitcoin Codification */
    public ChainPathInfo deserialize(byte[] raw) {
        if (raw == null) return null;
        ChainPathInfo.ChainPathInfoBuilder resultBuilder = ChainPathInfo.builder();
        ByteArrayReader reader = new ByteArrayReader(raw);
        resultBuilder
                .id((int)reader.readUint32())
                .parent_id((int)reader.readUint32())
                .blockHash(BitcoinSerializerUtils.deserializeVarStr(reader))
                .build();
        return resultBuilder.build();
    }
}
