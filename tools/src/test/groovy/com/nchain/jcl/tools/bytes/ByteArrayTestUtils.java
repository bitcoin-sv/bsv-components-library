package com.nchain.jcl.tools.bytes;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2020-03-11 12:07
 *
 * An utility class for testing Byte Array Classes.
 */
public class ByteArrayTestUtils {

    /**
     * Does some intensive memory work, so the Garbage Collection will most probably be triggered after
     * calling this method
     */
    public static void forceGC() {
        // We do some intensive memory work...
        try {
            for (int i = 0; i < 100_000; i++) {
                int[] a = new int[100000];
            }
            Thread.sleep(10);
        } catch (InterruptedException e) {}

        // We also try to force the GC (not sure will be triggered though)
        System.gc();
        System.runFinalization();
    }
}
