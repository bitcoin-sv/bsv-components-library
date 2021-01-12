package com.nchain.jcl.store.levelDB.common

import com.nchain.jcl.store.blockStore.BlockStore
import com.nchain.jcl.store.common.IteratorSpecBase
import com.nchain.jcl.store.levelDB.blockStore.BlockStoreLevelDB
import com.nchain.jcl.store.levelDB.StoreFactory
import java.util.function.Function


/**
 * Testing iterator for the FBDIterator, which is a very basic class and plays a big role in the FoundationDB
 * implementation of the JCL-Store
 */
class LevelDBIteratorSpec extends IteratorSpecBase {

    @Override
    BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents) {
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
