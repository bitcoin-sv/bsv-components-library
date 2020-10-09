package com.nchain.jcl.net.protocol.handlers.block;

import com.nchain.jcl.base.tools.handlers.Handler;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Operations provided by the BlockDownloader Handler.
 */
public interface BlockDownloaderHandler extends Handler {
    String HANDLER_ID = "BlockDownloader-Handler";

    @Override
    default String getId() { return HANDLER_ID; }

    /** Adds more Block Hashes to the list of Blocks to Download */
    void download(List<String> blockHashes);

}
