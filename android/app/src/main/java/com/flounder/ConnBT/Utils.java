package com.flounder.ConnBT;

public class Utils {

    public static byte[] combineByteArray(byte[] arr1, byte[] arr2) {
        byte[] ret = new byte[arr1.length + arr2.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] =  i < arr1.length ? arr1[i] : arr2[i - arr1.length];
        }
        return ret;
    }

    public static String byteArrayToHex(byte[] arr) {
        StringBuilder builder = new StringBuilder(arr.length * 2);
        for (byte b : arr) {
            builder.append(String.format("%02X", b & 0xFF));
        }
        return builder.toString();
    }
}
