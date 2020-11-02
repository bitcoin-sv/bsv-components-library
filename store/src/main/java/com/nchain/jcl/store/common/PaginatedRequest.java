package com.nchain.jcl.store.common;

import lombok.Builder;
import lombok.Value;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
@Builder(toBuilder = true)
@Value
public class PaginatedRequest {
    /** Number of Items returned in each Page */
    private long pageSize;
    /** Page number (zero-based) */
    private long numPage;

}
