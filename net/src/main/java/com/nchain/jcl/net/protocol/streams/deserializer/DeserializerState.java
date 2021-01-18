package com.nchain.jcl.net.protocol.streams.deserializer;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Runtime State of the Deserializer. It basically represents the state of the CACHE used when deserializing small
 * messages
 */
@Builder(toBuilder = true)
@Getter
@ToString
public class DeserializerState {
    @Builder.Default private long numLoads = 0L;
    @Builder.Default private long numHits = 0L;
    @Builder.Default private double hitRatio = 0.0;
}
