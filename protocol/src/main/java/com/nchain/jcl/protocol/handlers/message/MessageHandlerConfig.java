package com.nchain.jcl.protocol.handlers.message;

import com.nchain.jcl.protocol.config.ProtocolBasicConfig;
import com.nchain.jcl.tools.handlers.HandlerConfig;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-07-17 10:33
 *
 * It soes the configuration variables needed by the Message Handler
 */
@Value
@Builder(toBuilder = true)
public class MessageHandlerConfig extends HandlerConfig {
    private ProtocolBasicConfig basicConfig;
    // If set, this object will be invoked BEFORE the DESERIALIZATION takes place
    private MessagePreSerializer preSerializer;

    public MessageHandlerConfig(ProtocolBasicConfig basicConfig) {
        this(basicConfig, null);
    }

    public MessageHandlerConfig(ProtocolBasicConfig basicConfig, MessagePreSerializer preSerializer) {
        this.basicConfig = basicConfig;
        this.preSerializer = preSerializer;
    }
}
