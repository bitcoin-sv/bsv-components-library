package io.bitcoinsv.jcl.net.protocol.handlers.block.strategies;

import static io.bitcoinsv.jcl.net.protocol.handlers.block.BlockDownloaderHandlerConfig.*;
import io.bitcoinsv.jcl.tools.events.EventBus;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Base class for the Download Strategies. The basic behaviour of a Download Strategy depends mostly on
 * its "bestMAtchCriteria", which is the POLICY to choose the best Peer for each Block. Other Strategies
 * might have additional Policies.
 */
public abstract class BaseStrategy {
    // Criteria used to fins the BEST MATCH POSSIBLE for each Block (Best Peer possible):
    protected BestMatchCriteria bestMatchCriteria;

    // Event Bus shared with rest of JCL
    protected EventBus eventBus;

    public BaseStrategy(BestMatchCriteria bestMatchCriteria, EventBus eventBus) {
        this.bestMatchCriteria = bestMatchCriteria;
        this.eventBus = eventBus;
    }
}
