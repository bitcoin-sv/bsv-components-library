package com.nchain.jcl.store.keyValue.common;


import com.nchain.jcl.tools.bytes.ByteArrayReader;
import com.nchain.jcl.tools.bytes.ByteArrayWriter;
import com.nchain.jcl.tools.serialization.BitcoinSerializerUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer for a List of HASHes, using the BITCOIN Serialization.
 */
public class HashesListSerializer {

    // Singleton :
    private static HashesListSerializer instance;

    /** Private Constructor. Use "getInstance" instead */
    private HashesListSerializer() {}

    /** Returns the instance of this Serialzier (Singleton) */
    public static HashesListSerializer getInstance() {
        if (instance == null) {
            synchronized (HashesListSerializer.class) {
                instance = new HashesListSerializer();
            }
        }
        return instance;
    }

    /** Serialize the list of Hashes into raw format (byte array), using the Bitcoin Codification */
    public byte[] serialize(HashesList hashesList) {
        ByteArrayWriter writer = new ByteArrayWriter();
        // first we write the size of the list
        BitcoinSerializerUtils.serializeVarInt(hashesList.getHashes().size(), writer);
        // Now we serialize the Hashes, one by one:
        hashesList.getHashes().forEach(h -> BitcoinSerializerUtils.serializeVarStr(h, writer));
        return writer.reader().getFullContentAndClose();
    }

    /** Deserialize the raw data (byte array) into a Java Object, using the Bitcoin Codification */
    public HashesList deserialize(byte[] raw) {
        ByteArrayReader reader = new ByteArrayReader(raw);
        HashesList.HashesListBuilder resultBuilder = HashesList.builder();
        long numHashes = BitcoinSerializerUtils.deserializeVarInt(reader);
        if (numHashes > 0) {
            List<String> hashes = new ArrayList<>();
            for (int i = 0; i < numHashes; i++) {
                String hash = BitcoinSerializerUtils.deserializeVarStr(reader);
                hashes.add(hash);
            }
            resultBuilder.hashes(hashes);
        }
        return resultBuilder.build();
    }

}
