package com.nchain.jcl.store.levelDB.blockStore;

import com.nchain.jcl.base.tools.config.RuntimeConfig;
import com.nchain.jcl.store.keyValue.blockStore.BlockStoreKeyValueConfig;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.nio.file.Path;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Configuration class for the BlockStoreLevelDB Imlementation
 */

@Getter
public class BlockStoreLevelDBConfig implements BlockStoreKeyValueConfig {

    // Transaction BATCH Default Size
    private static final int TRANSACTION_BATCH_SIZE = 5000;

    /** Maximun number of Items that can be processed in a single DB Tramnsaction */
    private final int transactionBatchSize;

    /** Working Folder: The DB info will be created inside this folder */
    private final Path workingFolder;

    /** Runtime Config */
    private final RuntimeConfig runtimeConfig;

    /**
     * The network Id to use as a Base Directory. We use a String here to keep dependencies simple,
     * but in real scenarios this value will be obtained from a ProtocolConfiguration form the JCL-Net
     * module
     */
    private String networkId;

    @Builder(toBuilder = true)
    public BlockStoreLevelDBConfig(Path workingFolder,
                                   RuntimeConfig runtimeConfig,
                                   Integer transactionBatchSize,
                                   @NonNull String networkId) {
        this.runtimeConfig = runtimeConfig;
        this.workingFolder = (workingFolder != null)? workingFolder : runtimeConfig.getFileUtils().getRootPath();
        this.transactionBatchSize = (transactionBatchSize != null) ? transactionBatchSize : TRANSACTION_BATCH_SIZE;
        this.networkId = networkId;
    }
}
