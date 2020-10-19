package com.nchain.jcl.blockStore;

import lombok.Builder;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
@Builder
public class PaginatedRequest {
    private int numPage;
    private int numItemsPage;
}
