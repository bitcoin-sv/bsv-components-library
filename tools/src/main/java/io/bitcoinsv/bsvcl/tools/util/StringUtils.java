package io.bitcoinsv.bsvcl.tools.util;

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

    /**
     * It returns the String given with a Fixed Length
     */
    public static String fixedLength(String str, int length) {
        if (str == null) {
            return org.apache.commons.lang3.StringUtils.leftPad(" ", length, " ");
        } else {
            String result = org.apache.commons.lang3.StringUtils.abbreviate(str, length);
            if (result.length() < length) {
                result = org.apache.commons.lang3.StringUtils.rightPad(result, length, " ");
            }
            return result;
        }
    }

    public static String fixedLength(Integer value, int length) {
        return (value != null) ? fixedLength(value.toString(), length) : fixedLength((String) null, length);
    }
}
