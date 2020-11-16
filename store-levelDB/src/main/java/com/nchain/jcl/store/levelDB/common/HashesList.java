package com.nchain.jcl.store.levelDB.common;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An object that sores a List of Hashes. It might used for storing Block Hashes, Tx Hashes, etc.
 */
@Builder(toBuilder = true)
@Value
public class HashesList implements Serializable {
    @Builder.Default
    private List<String> hashes = new ArrayList<>();
}
