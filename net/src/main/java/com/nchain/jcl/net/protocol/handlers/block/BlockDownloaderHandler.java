package com.nchain.jcl.net.protocol.handlers.block;

import com.nchain.jcl.base.tools.handlers.Handler;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-27 10:13
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
