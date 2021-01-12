package com.nchain.jcl.store.foundationDB.common

import com.nchain.jcl.store.blockStore.BlockStore
import com.nchain.jcl.store.common.IteratorSpecBase
import com.nchain.jcl.store.foundationDB.DockerTestUtils
import com.nchain.jcl.store.foundationDB.blockStore.BlockStoreFDB
import com.nchain.jcl.store.foundationDB.StoreFactory
import org.junit.ClassRule
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared

import java.util.function.Function

/**
 * Testing iterator for the FBDIterator, which is a very basic class and plays a big role in the FoundationDB
 * implementation of the JCL-Store
 */

class FDBIteratorSpec extends IteratorSpecBase {

    // Start & Stop FoundationDB in Docker Container (check DockerTestUtils for details)...
    def setupSpec()     { DockerTestUtils.startDocker()}
    def cleanupSpec()   { DockerTestUtils.stopDocker()}

    @Override
    BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents)
    }

    @Override
    Iterator<byte[]> createIteratorForTxs(BlockStore db, String preffix, String suffix) {

        // We define a Function that returns the relative Key, that is the last Key to the right, after trimming all the
        // "directories" from the left:

        Function<Map.Entry<byte[], byte[]>, String> itemBuilder = { key ->


            // Each Key returned by this Iterator his a TUPLE, which means that its made of the following components:
            // [tuple beginning] [TXS DIR Key] [ tuple middle] [TX KEY] [tuple end]

            // We need to return ONLY the [TX KEY part, so we remove the rest:
            // [tuple beginning] : 1 byte
            // [tuple middle] : 2 bytes
            // [tuple end] : 1 byte

            int numBytesToRemove = 1 + ((BlockStoreFDB) db).fullKeyForTxs().length + 2 + 1
            int byteTxKeyPos = 1 + ((BlockStoreFDB) db).fullKeyForTxs().length + 2
            int txKeyLength = key.key.length - numBytesToRemove;

            byte[] result = new byte[key.key.length - numBytesToRemove]
            System.arraycopy(key.key, byteTxKeyPos, result, 0, txKeyLength)
            return result;
        }

        BlockStoreFDB blockStoreFDB = (BlockStoreFDB) db;
        byte[] keyPreffix = blockStoreFDB.keyStartingWith(blockStoreFDB.fullKey(blockStoreFDB.fullKeyForTxs(), preffix))
        byte[] keySuffix = (suffix != null) ? blockStoreFDB.keyEndingWith(suffix.getBytes()) : null;

        FDBSafeIterator.FDBSafeIteratorBuilder<String> itBuilder = FDBSafeIterator.<String>safeBuilder()
            .database(blockStoreFDB.db)
            .startingWithPreffix(keyPreffix)
            .endingWithSuffix(keySuffix)
            .buildItemBy(itemBuilder)
        return itBuilder.build()
    }

}
