package com.nchain.jcl.store.common;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
@Builder
@Value
public class PaginatedResult<T> {
    private List<T> results;
}
