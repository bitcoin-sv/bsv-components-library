package com.nchain.jcl.tools.bitcoin;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-08-19 13:06
 *
 * An Utility Class for managing NONCE Values
 * TODO: Make sure this class is really necessary
 */
public class NonceUtils {

    /** Return a new NONCE Value */
    public static long newOnce() {
        return (long) (Math.random() * Long.MAX_VALUE);
    }
}
