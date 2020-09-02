package com.nchain.jcl.net.protocol.serialization.common;


import com.nchain.jcl.net.protocol.config.ProtocolBasicConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-07-17
 *
 * This class contains information used by a Deserializer in order to do its job. It stores
 * system/environment-level variables.
 *
 * This class is immutable and safe for Multithreading
 */

// TODO: Document THIS...
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public final class DeserializerContext {

    //calculate the hashes when deserialzing block data
    private boolean calculateHashes;

    // Global P2P Configuration
    private ProtocolBasicConfig protocolBasicConfig;

    // Indicates whether the current Serialization is happening inside a Version Message (since some
    // serialization logic is slightly different in that case)
    private boolean insideVersionMsg;

    // If specified, this value indicates the maximum number of bytes that can be read during Deserialization.
    // If you try to read more bytes than this from the Source, the result might either be wrong or the execution
    // fails. If this value is NOT specified, that measn that its value is not needed to execute the
    // Deserialization, but you still need to be careful not to read more bytes than needed from the source.
    private Long maxBytesToRead;

}
