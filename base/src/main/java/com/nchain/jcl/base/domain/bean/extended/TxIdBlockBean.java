package com.nchain.jcl.base.domain.bean.extended;

import com.nchain.jcl.base.domain.api.base.BlockHeader;
import com.nchain.jcl.base.domain.api.extended.TxIdBlock;
import com.nchain.jcl.base.domain.bean.base.AbstractBlockBean;
import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 */
@Value
public class TxIdBlockBean extends AbstractBlockBean implements TxIdBlock {
    private List<Sha256Wrapper> txids;

    @Builder(toBuilder = true)
    public TxIdBlockBean(BlockHeader header, List<Sha256Wrapper> txids) {
        this.header = header;
        this.txids = txids;
    }
}
