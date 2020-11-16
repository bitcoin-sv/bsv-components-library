package com.nchain.jcl.store.levelDB.blockStore;

import com.google.common.primitives.Longs;
import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.domain.api.base.Tx;
import com.nchain.jcl.base.serialization.BlockHeaderSerializer;
import com.nchain.jcl.base.serialization.TxSerializer;
import com.nchain.jcl.store.levelDB.common.HashesList;

import java.io.*;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * This classes contains usefule methods that are commomnly used by the LevelDB Engine:
 *  - to generate KEYS (String format) out of other values
 *  - to extract "useful" information (like Tx or Block Hash from a Key)
 *  - to serialize values in order to insert them as keys/values
 *  - to deserialize values in order to extract them from keys/values
 *
 *  This class assumes that the Keys and values are stored following this structure:
 *
 *  - key: b:[block_hash]
 *  - value: a BlockHeader.
 *    example: "b:00a12918c6674caf3c96ce977372eb0bf41cbca68768c7b6" = (Block Header Serialized)
 *
 *  - key: b_p[block_hash]:[property]
 *  - value: a single Block property
 *    example: "tx:187b49bcae37859e34e1c695d842473f1f65db9ef23e068:numTxs" = (total number of Txs in the Block)
 *
 *  - key: tx:[tx_hash]
 *  - value: a Transaction.
 *    example: "tx:187b49bcae37859e34e1c695d842473f1f65db9ef2b2968" = (Tx Serialized)
 *
 *  - key: tx_p[tx_hash]:[property]
 *  - value: a single Tx property
 *    example: "tx:187b49bcae37859e34e1c695d842473f1f65db9ef23e068:block" = (block Hash this Tx belongs to)
 *
 *  - key: btx:[block_hash]:[tx_hash] (example: "btx:00a12918c6674caf3c...:187b49bcae37859e34e1c695d842473...")
 *  - value_ Just a 1-bte array. The value is not important here, only the Key. If this key exists, then the Block
 *    identified by block_hash contains the Tx identified by tx_hash.
 *
 */

public class BlockStoreKeyValueUtils {

    // Keys used to store Block info:
    protected static final String PREFFIX_KEY_BLOCK       = "b:";           // A Whole Block
    protected static final String PREFFIX_KEY_BLOCK_PROP  = "b_p:";         // An individual property (suffix)
    protected static final String SUFFIX_KEY_BLOCK_NUMTXS = ":numTxs";      // The number of Txs in a Block

    // Keys used to store Tx info:
    protected static final String PREFFIX_KEY_TX          = "tx:";          // A whole Tx
    protected static final String PREFFIX_KEY_TX_PROP     = "tx_p:";        // An individual property (suffiex)
    protected static final String SUFFIX_KEY_TX_BLOCK     = ":block";       // The block Hash this Tx belongs to
    protected static final String SUFFIX_KEY_TX_NEEDED    = ":txsNeeded";   // List of TXs THIS Tx depends on

    // Keys used to store relationship between entities:
    protected static final String PREFFIX_KEY_BLOCKTXS    = "btx:";         // A combination Block-Tx hashes in the Key


    // We keep an internal reference here of the Serializers we'll need:
    private static final BlockHeaderSerializer headerSer = BlockHeaderSerializer.getInstance();
    private static final TxSerializer txSer = TxSerializer.getInstance();


    // Functions to generate Keys in String format:

    public static String getKeyForBlock(String blockHash) { return PREFFIX_KEY_BLOCK + blockHash; }
    public static String getKeyForBlockNumTxs(String blockHash) { return PREFFIX_KEY_BLOCK_PROP + blockHash + SUFFIX_KEY_BLOCK_NUMTXS; }
    public static String getKeyForBlockTx(String blockHash, String txHash) { return PREFFIX_KEY_BLOCKTXS + blockHash + ":" + txHash; }
    public static String getKeyForTx(String txHash) { return PREFFIX_KEY_TX + txHash;}
    public static String getKeyForTxBlock(String txHash) { return PREFFIX_KEY_TX_PROP + txHash + SUFFIX_KEY_TX_BLOCK; }
    public static String getKeyForTxNeededTxs(String txHash) { return PREFFIX_KEY_TX_PROP + txHash + SUFFIX_KEY_TX_NEEDED; }


    // Functions to extract some Hashes from the Keys:

    public static String getBlockHashFromKey(String key) {
        if (key.startsWith(PREFFIX_KEY_BLOCK)) return key.substring(key.indexOf(":") + 1);
        if (key.startsWith(PREFFIX_KEY_BLOCKTXS)) return key.substring(key.indexOf(":") + 1, key.lastIndexOf(":"));
        if (key.startsWith(PREFFIX_KEY_BLOCK_PROP)) return key.substring(key.indexOf(":") + 1, key.lastIndexOf(":"));
        return null;
    }

    public static String getTxHashFromKey(String key) {
        if (key.startsWith(PREFFIX_KEY_TX)) return key.substring(key.indexOf(":") + 1);
        if (key.startsWith(PREFFIX_KEY_BLOCKTXS)) return key.substring(key.lastIndexOf(":") + 1);
        if (key.startsWith(PREFFIX_KEY_TX_PROP)) return key.substring(key.indexOf(":") + 1);
        return null;
    }

    // Functions to convert keys/values to byte arrays:
    // for those Beans in JCL-Base, we are using the Serializers from that package instead of using a default
    // Java-Serializer, since they are expected to be more efficient.

    protected static byte[] objectToBytes(Object obj) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oStream = new ObjectOutputStream(bos)) {
            oStream.writeObject(obj);
            return bos.toByteArray();
        } catch (IOException ioe) { throw new RuntimeException(ioe);}
    }

    public static byte[] bytes(HashesList chainTips) { return objectToBytes(chainTips);}
    public static byte[] bytes(BlockHeader header) { return headerSer.serialize(header); }
    public static byte[] bytes(Tx tx) { return txSer.serialize(tx);}
    public static byte[] bytes(String value) { return value.getBytes(); }
    public static byte[] bytes(Long value) { return Longs.toByteArray(value); }

    // Functions to convert byte[] to values:

    protected static Object bytesToObject(byte[] value) {
        if (value == null || value.length == 0) return null;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(value);
             ObjectInputStream ois = new ObjectInputStream(bis)){
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) { throw new RuntimeException(e);}
    }


    public static boolean     isBytesOk(byte[] bytes) { return (bytes != null && bytes.length > 0);}
    public static BlockHeader bytesToBlockHeader(byte[] bytes) { return (isBytesOk(bytes)) ? headerSer.deserialize(bytes) : null;}
    public static HashesList  bytesToHashesList(byte[] bytes) { return (HashesList) bytesToObject(bytes);}
    public static Tx          bytesToTx(byte[] bytes) { return (isBytesOk(bytes)) ? txSer.deserialize(bytes) : null; }


}
