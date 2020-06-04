package com.nchain.jcl.tools.util;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-08-29 07:56
 *
 * String-related utility methods.
 */
public class StringUtils {

    /**
     * Remote the NULLS within a String
     */
    public static String removeNulls(String str) {
        if (str == null) return null;
        String result = str.replaceAll( "[^\\x00-\\x7F]", "" );
        return result;
    }
}
