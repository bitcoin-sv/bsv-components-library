package com.nchain.jcl.store.keyValue.blockChainStore;

import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 * @date 2021-01-19
 */
@Builder(toBuilder = true)
@Value
public class ChainPathInfo {
    private int id;
    private int parent_id;
    private String blockHash;
}
