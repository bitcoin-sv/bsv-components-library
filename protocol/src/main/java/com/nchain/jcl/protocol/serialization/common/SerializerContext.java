package com.nchain.jcl.protocol.serialization.common;


import com.nchain.jcl.protocol.config.ProtocolConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-07-17
 *
 * This class contains information used by a Serializer in order to do its job. It stores
 * system/environemnt-level variables.
 *
 * This class is immutable and safe for Multithreading
 */
@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public final class SerializerContext {

    // Global Protocol Configuration
    private ProtocolConfig protocolconfig;

    // Indicates whether the current Serialization is happening inside a Version Message (since some
    // serialization logic is slightly different in that case)
    private boolean insideVersionMsg;

}
