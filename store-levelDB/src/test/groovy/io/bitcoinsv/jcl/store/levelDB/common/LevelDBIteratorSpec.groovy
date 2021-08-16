/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.store.levelDB.common

import io.bitcoinsv.jcl.store.blockStore.BlockStore
import io.bitcoinsv.jcl.store.blockStore.metadata.Metadata
import io.bitcoinsv.jcl.store.common.IteratorSpecBase
import io.bitcoinsv.jcl.store.levelDB.blockStore.BlockStoreLevelDB
import io.bitcoinsv.jcl.store.levelDB.StoreFactory
import java.util.function.Function


/**
 * Testing iterator for the FBDIterator, which is a very basic class and plays a big role in the FoundationDB
 * implementation of the JCL-Store
 */
class LevelDBIteratorSpec extends IteratorSpecBase {

    @Override
    BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents, Class<? extends Metadata> blockMetadataClass) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents)
    }

    @Override
    Iterator<String> createIteratorForTxs(BlockStore db, String preffix, String suffix) {

        // We define a Function that returns the relative Key, that is the last Key to the right, after trimming all the
        // "directories" from the left:
        Function<Map.Entry<byte[], byte[]>, String> itemBuilder = { key ->
            String preffixToRemove = new String(((BlockStoreLevelDB) db).fullKeyForTxs())
            String keyStr = new String(key.getKey())
            return keyStr.substring(preffixToRemove.length() + 1)
        }

        BlockStoreLevelDB blockStoreLevelDB = (BlockStoreLevelDB) db;

        LevelDBIterator.LevelDBIteratorBuilder<String> itBuilder = LevelDBIterator.<String>builder()
            .database(blockStoreLevelDB.levelDBStore)
            .startingWithPreffix(blockStoreLevelDB.fullKey(blockStoreLevelDB.fullKeyForTxs(), preffix))
            .endingWithSuffix(blockStoreLevelDB.fullKey(suffix))
            .buildItemBy(itemBuilder)

        return itBuilder.build()
    }
}
