package com.nchain.jcl.net.protocol.streams.deserializer;

import com.nchain.jcl.net.protocol.messages.BlockHeaderMsg;
import com.nchain.jcl.net.protocol.messages.HeadersMsg;
import com.nchain.jcl.net.protocol.messages.TxMsg;
import com.nchain.jcl.net.protocol.messages.VersionAckMsg;
import lombok.Builder;
import lombok.Value;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Configuration class for the Deserializer.
 */
@Builder(toBuilder = true)
@Value
public class DeserializerConfig {

    /** Maximum Size of the Cache (in Bytes) */
    @Builder.Default
    private final Long maxCacheSizeInBytes = 10_000_000L; // 10 MB

    /** Only messages Smaller than this Value will be cached: */
    @Builder.Default
    private final Long maxMsgSizeInBytes = 100_000L; // 10KB

    /** If TRUE; statistics of the Cache are generating in real-time */
    @Builder.Default
    private boolean generateStats = false;

    // Default List of Messages to Cache...
    private static final String[] DEFAULT_MSGS_TO_CACHE = {
            HeadersMsg.MESSAGE_TYPE.toUpperCase(),
            TxMsg.MESSAGE_TYPE.toUpperCase(),
            BlockHeaderMsg.MESSAGE_TYPE.toUpperCase()
    };

    /** If the Message is NOT part of this List, then it won't be cached */
    @Builder.Default
    private final Set<String> messagesToCache = new HashSet<>(Arrays.asList(DEFAULT_MSGS_TO_CACHE));

}
