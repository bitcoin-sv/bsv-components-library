/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.keyValue.common;


import java.nio.charset.Charset;
import java.util.stream.Collectors;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Serializer for a List of HASHes, using the BITCOIN Serialization.
 */

/*
    NOTE:
    The initial version of this Serializer used the BitcoinJ Serialization to write/read the hashes. But since the support
    for forks was added to JCL-Store, this Serializer is more heavily used and those operations are quite slow. So in
    this new version we are using the regular conversion between byte[]/Strings provided by Java. In order to avoid
    incompatibilities with other languages, we are specifying the Charset to use (UTF-8), but at this moment we cannot
    rule out definitely problems that might show up in the future if another client developed in a different
    language tries to access the Db.

    Something to bear in mind
 */
public class HashesListSerializer {

    //private final int HASH_LENGTH = 32; // in bytes...
    private final Charset CHARSET = Charset.forName("UTF-8");
    private final int HASH_LENGTH = "000000000002d2024b6a09a467ddbaf83f3024f11b8bde2394cc37b52fb2e560".getBytes(CHARSET).length; // in bytes...

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

//  INITIAL IMPLEMENTATION USING BITCOIN CODIFICATION:
//    public byte[] serialize(HashesList hashesList) {
//        if (hashesList == null || hashesList.getHashes().isEmpty()) return new byte[0];
//        byte[] result = new byte[hashesList.getHashes().size() * HASH_LENGTH];
//        for (int i = 0; i < hashesList.getHashes().size(); i++) {
//            byte[] hashBytes = Utils.HEX.decode(hashesList.getHashes().get(i));
//            System.arraycopy(hashBytes,0, result, i * HASH_LENGTH, HASH_LENGTH);
//        }
//        return result;
//    }

    /** Serialize the list of Hashes into raw format (byte array), using the Bitcoin Codification */
    public byte[] serialize(HashesList hashesList) {
        if (hashesList == null || hashesList.getHashes().isEmpty()) return new byte[0];

        String allHashesTogether = hashesList.getHashes().stream().collect(Collectors.joining());
        byte[] result = allHashesTogether.getBytes(CHARSET);

        return result;
    }

//  INITIAL IMPLEMENTATION USING BITCOIN CODIFICATION:
//    public HashesList deserialize(byte[] raw) {
//        HashesList result = new HashesList.HashesListBuilder().build();
//        if (raw != null && raw.length > 0) {
//            int numHashes = raw.length / HASH_LENGTH;
//            for (int i = 0; i < numHashes; i++) {
//                byte[] hashBytes = new byte[HASH_LENGTH];
//                System.arraycopy(raw, i * HASH_LENGTH, hashBytes, 0, HASH_LENGTH);
//                String hash = Utils.HEX.encode(hashBytes);
//                result.addHash(hash);
//            }
//        }
//        return result;
//    }

    /** Deserialize the raw data (byte array) into a Java Object, using the Bitcoin Codification */
    public HashesList deserialize(byte[] raw) {
        HashesList result = new HashesList.HashesListBuilder().build();

        if (raw != null && raw.length > 0) {
            int numHashes = raw.length / HASH_LENGTH;
            for (int i = 0; i < numHashes; i++) {
                String hash = new String(raw, i * HASH_LENGTH, HASH_LENGTH, CHARSET);
                result.addHash(hash);
            }
        }
        return result;
    }
}
