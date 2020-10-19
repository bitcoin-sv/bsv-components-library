package com.nchain.jcl.blockStore;

import lombok.Builder;

import java.util.List;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
@Builder
public class PaginatedResult<T> {
    private List<T> results;
}
