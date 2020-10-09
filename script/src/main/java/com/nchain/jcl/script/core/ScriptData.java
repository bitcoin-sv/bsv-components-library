package com.nchain.jcl.script.core;

import lombok.Builder;
import lombok.Value;

import java.util.Arrays;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
@Value
public class ScriptData {
    private byte[] data;

    @Builder(toBuilder = true)
    public ScriptData(byte[] data) {
        this.data = (data == null) ? null : Arrays.copyOf(data, data.length);
    }

    public int length()  { return (data != null) ? data.length : 0;}
    public byte[] data() { return (data == null) ? null : Arrays.copyOf(data, data.length); }
}
