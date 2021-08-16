/*
 * Distributed under the Open BSV software license, see the accompanying file LICENSE
 * Copyright (c) 2020 Bitcoin Association
 */
package io.bitcoinsv.jcl.net.protocol.events.control;

import io.bitcoinsv.jcl.net.network.events.P2PRequest;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 14/06/2021
 *
 * This requests starts the Block Download Handler, if it was in PAUSED State.
 * NOTE: The blocks Download process starts automatically when it receives a BlocksDownloadRequest, this
 * event is not needed, but it can be used to get back to RUNNING Mode if the Download process has been
 * set to PAUSED using the BlocksDownloadPauseRequest
 *
 * @see BlocksDownloadPauseRequest
 *
 */
public class BlocksDownloadStartRequest extends P2PRequest {

    /** Constructor */
    public BlocksDownloadStartRequest() {}
}
