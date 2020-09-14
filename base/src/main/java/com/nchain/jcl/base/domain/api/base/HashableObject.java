package com.nchain.jcl.base.domain.api.base;

import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Hashable Object is an Object that, apart from any information it might contain, it also contains a field which is
 * a HASH (256Hash), which value is calcualted based on the content of the boject itself. So the value of this "hash"
 * field is NOT "setteable", instead is calculated based on the values of the rest of fields of this object.
 */
public interface HashableObject {
    /**
     * returns the Value of the Hash of this Object.
     * NOTE: In most implementations, the value of the Hash is NULL at first, but it will be calculated the first time
     * this method is called, and the value reused any time afterwards.
     */
    Sha256Wrapper getHash();
}
