package com.nchain.jcl.base.domain.api;

import com.nchain.jcl.base.tools.crypto.Sha256Wrapper;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * A Hashable Object is an Object that, apart from any information it might contain, it also contains a field which is
 * a HASH (256Hash), which value is calcualted based on the content of the object itself. So the value of this "hash"
 * field is NOT "setteable", instead is calculated based on the values of the rest of fields of this object.
 */
public interface BitcoinHashableObject extends BitcoinSerializableObject {
    /**
     * returns the Value of the Hash of this Object.
     * NOTE: In most implementations, the initial value might be either NULL or provided through the Builder.
     * If NULL, then it's value will be calculated the first time this method is called, and the value reused any
     * time afterwards.
     */
    Sha256Wrapper getHash();
}
