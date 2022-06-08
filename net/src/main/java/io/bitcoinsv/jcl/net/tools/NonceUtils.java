package io.bitcoinsv.jcl.net.tools;


/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * An Utility Class for managing NONCE Values
 */
public class NonceUtils {

    /** Return a new NONCE Value */
    public static long newOnce() {
        return (long) (Math.random() * Long.MAX_VALUE);
    }
}