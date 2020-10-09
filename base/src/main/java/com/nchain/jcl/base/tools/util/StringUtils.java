package com.nchain.jcl.base.tools.util;

/**
 * @author i.fernandez@nchain.com
 * Copyright (c) 2018-2020 nChain Ltd
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

    /**
     * Returns a String that can be used as a File name in any file system
     */
    public static String fileNamingFriendly(String str) {
        if (str == null) return null;
        String result = removeNulls(str);
        // We remove the Blanks
        result = str.replaceAll("\\s+","");
        // We remove the slashes
        result = result.replaceAll("\\\\", "");
        return result;
    }
}
