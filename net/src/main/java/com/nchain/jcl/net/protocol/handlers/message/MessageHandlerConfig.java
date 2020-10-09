package com.nchain.jcl.net.protocol.handlers.message;

import com.nchain.jcl.base.tools.handlers.HandlerConfig;
import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig;
import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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
