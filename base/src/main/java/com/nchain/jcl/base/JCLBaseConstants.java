package com.nchain.jcl.base;

import java.math.BigInteger;

/**
 * @author Steve Shadders
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
 *
 * Placeholder for constants that are commonly referenced.
 */
public class JCLBaseConstants {

    /** The number that is one greater than the largest representable SHA-256 hash.*/
    public static BigInteger LARGEST_HASH = BigInteger.ONE.shiftLeft(256);
}
