package io.bitcoinsv.jcl.tools.unit.blobStore

import io.bitcoinsv.bitcoinjsv.core.Sha256Hash
import io.bitcoinsv.jcl.tools.blobStore.BlockStorePosix
import spock.lang.Specification
import java.time.Duration
import java.util.stream.Stream


/**
 * Test scenarios involving Polling(wait) of data
 */
class BlockStorePosixWaitTest extends Specification {

    // Reference to the internal DELAY Between Calls when we WAIT for a Block to be saved
    private final long WAIT_DELAY = BlockStorePosix.WAIT_DELAY.toMillis();

    /**
     * A Dummy class extending blockStorePosix that allows us to define the Tiemstamp when a Block starts to be
     * Available. It adds a "Start()" method that we use to establish a reference for a starting time.
     */
    class DummyWaitStore extends BlockStorePosix {
        private long millisecsToSaveBlock;  // Time Until block is available after Start
        long timestampStart;                // timestamp when we Start
        long timestampTxsRead;              // timestamp when Txs are read from Block
        DummyWaitStore(long millisecsToSaveBlock) {
            this.millisecsToSaveBlock = millisecsToSaveBlock;
        }
        void start() {
            this.timestampStart = System.currentTimeMillis();
        }
        @Override
        boolean containsBlock(Sha256Hash blockHash) {
            return ((System.currentTimeMillis() - timestampStart) > millisecsToSaveBlock);
        }
        @Override
        Stream<Sha256Hash> readBlockTxHashes(Sha256Hash blockHash) throws IllegalStateException {
            this.timestampTxsRead = System.currentTimeMillis();
            return Stream.empty(); // We don't care about the result
        }
    }

    private DummyWaitStore store;

    /**
     * Initialization
     */
    DummyWaitStore setupStore(long timeToSaveBlock) {
        this.store = new DummyWaitStore(timeToSaveBlock);
    }

    /**
     * We read Txs from a Block when the block is saved fromt he very beginning
     * It should return the Stream of Txs
     */
    def "test read txs From Block when Block is saved"() {
        given:
        long TIME_SAVE_BLOCK = 0; // Block is saved immediately
        long TIME_WE_WAIT = 100;  // We wait for 100 millis and then we give up
        when:
        DummyWaitStore store = setupStore(TIME_SAVE_BLOCK);
        store.start()
        Stream<Sha256Hash> result = store.readBlockTxHashesWithWait(null, Duration.ofMillis(TIME_WE_WAIT));
        then:
        result.count() == 0
    }

    /**
     * We read Txs from a Block when the block is saved some time AFTER we start.
     * It should return the list of Txs after some retries have been performed
     */
    def "test read txs From Block when Block is NOT saved and we wait until it is SAVED"() {
        given:
        long TIME_SAVE_BLOCK = 100; // Block is saved after 100 millis
        long TIME_WE_WAIT = 150;    // We wait for 150 millis and then we give up
        when:
        DummyWaitStore store = setupStore(TIME_SAVE_BLOCK);
        store.start()
        Stream<Sha256Hash> result = store.readBlockTxHashesWithWait(null, Duration.ofMillis(TIME_WE_WAIT));
        long timeToReadTxs = store.timestampTxsRead - store.timestampStart
        int expectedRetries = (int) Math.ceil(TIME_SAVE_BLOCK / WAIT_DELAY) + 1
        then:
        result.count() == 0
        timeToReadTxs >= TIME_SAVE_BLOCK
        timeToReadTxs <= (expectedRetries * WAIT_DELAY) + 2
    }

    /**
     * We read Txs from a Block when the block is saved after some time, but we dont wait that long, so
     * An Exception is raised.
     */
    def "test read txs From Block when Block is NOT saved and we wait BUt the timeout is triggered"() {
        given:
        long TIME_SAVE_BLOCK = 100; // Block is saved after 100 millis
        long TIME_WE_WAIT = 20;     // We wait for 20 millis and then we give up
        when:
        DummyWaitStore store = setupStore(TIME_SAVE_BLOCK);
        store.start()
        store.readBlockTxHashesWithWait(null, Duration.ofMillis(TIME_WE_WAIT));
        then:
        thrown(IllegalStateException)
    }


}
