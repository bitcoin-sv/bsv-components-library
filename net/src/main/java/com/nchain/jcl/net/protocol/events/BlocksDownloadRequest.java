package com.nchain.jcl.net.protocol.events;


import com.nchain.jcl.tools.events.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-27 10:19
 *
 * A Request for Downloading a Block
 */
@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class BlocksDownloadRequest extends Event {
    private List<String> blockHashes;
}
