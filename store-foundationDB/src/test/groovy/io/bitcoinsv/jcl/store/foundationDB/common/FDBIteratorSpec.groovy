package io.bitcoinsv.jcl.store.foundationDB.common


import io.bitcoinsv.jcl.store.foundationDB.FDBTestUtils
import io.bitcoinsv.jcl.store.foundationDB.blockStore.BlockStoreFDB
import io.bitcoinsv.jcl.store.foundationDB.StoreFactory
import io.bitcoinsv.jcl.store.blockStore.BlockStore
import io.bitcoinsv.jcl.store.blockStore.metadata.Metadata
import io.bitcoinsv.jcl.store.common.IteratorSpecBase
import spock.lang.Ignore

import java.util.function.Function

/**
 * Testing iterator for the FBDIterator, which is a very basic class and plays a big role in the FoundationDB
 * implementation of the JCL-Store
 */
// Test Ignored. If you want to run this Test, set up a local FDB or configure FDBTestUtils.useDocker to use the
// Docker image provided instead (not fully tested at the moment)
@Ignore
class FDBIteratorSpec extends IteratorSpecBase {

    // Start & Stop FoundationDB in Docker Container (check DockerTestUtils for details)...
    def setupSpec()     { FDBTestUtils.checkFDBBefore()}
    def cleanupSpec()   { FDBTestUtils.checkFDBAfter()}

    @Override
    BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents, Class<? extends Metadata> blockMetadataClass, Class<? extends Metadata> txMetadataClass) {
        return StoreFactory.getInstance(netId, triggerBlockEvents, triggerTxEvents, blockMetadataClass, txMetadataClass)
    }

    @Override
    Iterator<byte[]> createIteratorForTxs(BlockStore db, String preffix, String suffix) {

        // We define a Function that returns the relative Key, that is the last Key to the right, after trimming all the
        // "directories" from the left:

        Function<Map.Entry<byte[], byte[]>, String> itemBuilder = { key ->


            // Each Key returned by this Iterator represents a Tx Key, which is made of:
            // [txDirKey] + [TX_KEY]
            // [TX_KEY] example: tx:1716126a699c8a76fb6ae591661a22622ea0909ff57eb143fe2f479694b75792

            // We need to return ONLY the [TX_KEY], so we remove the rest:

            int numBytesToRemove = ((BlockStoreFDB) db).fullKeyForTxs().length
            int byteTxKeyPos = numBytesToRemove
            int txKeyLength = key.key.length - numBytesToRemove;

            byte[] result = new byte[key.key.length - numBytesToRemove]
            System.arraycopy(key.key, byteTxKeyPos, result, 0, txKeyLength)
            return result;
        }

        BlockStoreFDB blockStoreFDB = (BlockStoreFDB) db;
        byte[] keyPreffix = blockStoreFDB.fullKey(blockStoreFDB.fullKeyForTxs(), preffix)
        byte[] keySuffix = (suffix != null) ? suffix.getBytes() : null;

        FDBSafeIterator.FDBSafeIteratorBuilder<String> itBuilder = FDBSafeIterator.<String>safeBuilder()
            .database(blockStoreFDB.db)
            .incompleteTxsDir(blockStoreFDB.netDir)
            .startingWithPreffix(keyPreffix)
            .endingWithSuffix(keySuffix)
            .buildItemBy(itemBuilder)
        return itBuilder.build()
    }

}
