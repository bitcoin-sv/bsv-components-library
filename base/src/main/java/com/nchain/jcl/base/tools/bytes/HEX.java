/**
 * @author a.vilches@nchain.com
 * Copyright (c) 2018-2019 Bitcoin Association
 * Distributed under the Open BSV software license, see the accompanying file LICENSE.
 * @date 2019-06-24 23:58
 */
package com.nchain.jcl.base.tools.bytes;

public class HEX {

    /*
    Safe with leading zeros (unlike BigInteger) and with negative byte values (unlike Byte.parseByte)
    Doesn't convert the String into a char[], or create StringBuilder and String objects for every single byte.
     */
    public static byte[] decode(String hexString) {
        hexString = hexString.toLowerCase();
        if (hexString.startsWith("0x")) {
            hexString = hexString.substring(2);
        }
        final int len = hexString.length();
        final byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    private final static char[] hexArray = "0123456789abcdef".toCharArray();

    /*
    Fastest implementation. It doesn't use Integer.toHexString() because it doesn't pad with zeroes and
    a widening primitive conversion is performed to the byte argument, which involves sign extension (-1 == "ffffffff")
     */
    public static String encodeLE(long val) {
        return encode(ByteTools.uint64ToByteArrayLE(val));
    }

    public static String encode(final byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        final char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            final int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
