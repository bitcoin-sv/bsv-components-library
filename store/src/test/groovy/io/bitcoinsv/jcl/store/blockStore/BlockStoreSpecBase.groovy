package io.bitcoinsv.jcl.store.blockStore


import io.bitcoinsv.jcl.store.blockStore.metadata.Metadata
import spock.lang.Specification

/**
 * Base class for all the Testing classes in the JCL-Store module.
 *
 * In the JCL-Store module, only the interfaces (BlockStore, BlockChainStore) are defined, but the creation of
 * instances of those interfaces are implementation-specific, so that part is not included here.
 *
 * Since the interfaces are defined in this module, it also makes sense to define the tests that those interfaces
 * should meet here, so tests are only defined once and future implementation projects will only have to specify
 * how to create instances of those interfaces.
 *
 * That's the point of this class. It defines a "createInstance" method that will have to be implemented by
 * implementation-specific test classes. The extending class will only have to define that method, the tests themselves
 * will be defined in this JCL-Sotre Module.
 */
abstract class BlockStoreSpecBase extends Specification {

    /** Returns a concrete implementation of the BlockStore interface. This is implementation-specific */
    BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents) {
        return getInstance(netId, triggerBlockEvents, triggerTxEvents, null);
    }

    /** Returns a concrete implementation of the BlockStore interface. This is implementation-specific */
    abstract BlockStore getInstance(String netId, boolean triggerBlockEvents, boolean triggerTxEvents, Class<? extends Metadata> blockMetadataClass);
}
