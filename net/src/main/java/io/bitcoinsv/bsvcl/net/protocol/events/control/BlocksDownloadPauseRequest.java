package io.bitcoinsv.bsvcl.net.protocol.events.control;

import com.google.common.base.Objects;
import io.bitcoinsv.bsvcl.net.network.events.P2PRequest;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 14/06/2021
 *
 * This requests pauses the Block Download Handler. While in PAUSED state, the Blocks that wer already being
 * downloaded before goinginto this state will finish their download, but no new blocks will be attempted until
 * we go back into RUNNING State. If the blocks currently being downloaded failed, they will NOT be re-attempted
 * either.
 *
 * @see BlocksDownloadStartRequest
 *
 */
public final class BlocksDownloadPauseRequest extends P2PRequest {

    /** Constructor */
    public BlocksDownloadPauseRequest() {}

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode());
    }
}
