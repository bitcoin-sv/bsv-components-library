/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
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

    // The ChainWork is stored in a 12-byte array, same as in BitcoinJ (what if it takes more?).
    // The value is stored in a BigInteger, which then extracts it as a byte array, which might be shorter.
    // In order to save time, we store a predefined set of smaller Arrays that can be used to pad

    private static final int CHAIN_WORK_BYTES = 12;
    private static final byte[][] EMPTY_ARRAYS = new byte[CHAIN_WORK_BYTES][];

    static {
        for (int i = 0; i < CHAIN_WORK_BYTES; i++) EMPTY_ARRAYS[i] = new byte[i];
    }

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
        if (chainWorkBytes.length < CHAIN_WORK_BYTES) {
            // Pad to the right size
            writer.write(EMPTY_ARRAYS[CHAIN_WORK_BYTES - chainWorkBytes.length]);
        }
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
        resultBuilder
                .blockHash(BitcoinSerializerUtils.deserializeVarStr(reader))
                .chainWork(new BigInteger(1, reader.read(CHAIN_WORK_BYTES)))
                .height((int) BitcoinSerializerUtils.deserializeVarInt(reader))
                .totalChainSize(BitcoinSerializerUtils.deserializeVarInt(reader))
                .chainPathId((int)reader.readUint32())
                .build();
        return resultBuilder.build();
    }
}
