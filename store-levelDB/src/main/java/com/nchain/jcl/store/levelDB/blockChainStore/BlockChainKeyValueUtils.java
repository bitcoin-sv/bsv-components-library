package com.nchain.jcl.store.levelDB.blockChainStore;

import com.nchain.jcl.store.levelDB.blockStore.BlockStoreKeyValueUtils;

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
 *  This class leverages on the Structure of Keys and Values already impolemented by the
 *  BlockChainKeyValueUtils class, and it adds the following:
 *
 *  - key: b_next:[block_hash] (example: "b_next:00a12918c6674caf3c96ce977372eb0bf41cbca687683251f199cdc1d988c7b6")
 *  - value: An instance of {@link com.nchain.jcl.store.levelDB.common.HashesList}, that contains a List of Block
 *           Hashes in String (HEX) format.
 *           These are the Blocks that have been built on top of the one specified by the Key.
 *           Different scenarios:
 *           - No value: The block specified in the Kay has not Blocks built on top of it
 *           - A 1-item list: The Block specified in the key has ONE BLock built on top of it. This is the normal
 *             scenario.
 *           - A n-item list: The Block specified in the Kay has more than one Block built on top of it. This is a
 *             FORK scenario.
 *
 *  - key: b_chain:[block_hash] (example: "b_chain:00a12918c6674caf3c96ce977372eb0bf41cbca687683251f199cdc1d988c7b6")
 *  - value: A blockChainInfo instance, containing the info of the Chain this Blocks belongs to
 *
 *
 *  - key: chain_tips: (we only have ONE Key for the whole DB)
 *  - value: An instance of {@link com.nchain.jcl.store.levelDB.common.HashesList}, that contains a List of Block
 *           Hashes in String (HEX) format.
 *           This the list of the different TIPS of the multiple Chains we might have. In a normal scenario we'll only have
 *           one Tip, but in case of a FORK, we might have more than one.
 */
public class BlockChainKeyValueUtils extends BlockStoreKeyValueUtils {

    // KEys used to store block info:
    protected static final String SUFFIX_KEY_BLOCK_NEXT  = ":next";         // Block built on top of this one
    protected static final String PREFFIX_KEY_BLOCK_CHAIN = "b_chain:";     // Chan info for this block
    protected static final String PREFFIX_KEY_CHAINS_TIPS = "chain_tips:";  // List of all the Tip Chains

    // Functions to generate Keys in String format:

    public static String getKeyForBlockNext(String blockHash)  { return PREFFIX_KEY_BLOCK_PROP + blockHash + SUFFIX_KEY_BLOCK_NEXT; }
    public static String getKeyForBlockChain(String blockHash) { return PREFFIX_KEY_BLOCK_CHAIN + blockHash; }

    // Functions to convert keys/values to byte arrays:
    // Here we are gonna use the standard Java Serialization:
    public static byte[] bytes(BlockChainInfo blockChainInfo) { return objectToBytes(blockChainInfo);}

    // Functions to convert byte[] to values:
    public static BlockChainInfo bytesToChainInfo(byte[] bytes) { return (BlockChainInfo) bytesToObject(bytes);}
}
