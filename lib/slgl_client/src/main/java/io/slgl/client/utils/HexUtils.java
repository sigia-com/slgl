package io.slgl.client.utils;

public class HexUtils {

    private static final char[] DIGITS =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private HexUtils() {
    }

    protected static String toHex(byte[] data) {
        StringBuilder result = new StringBuilder(data.length * 2);

        for (byte b : data) {
            result.append(DIGITS[(0xF0 & b) >>> 4]);
            result.append(DIGITS[0x0F & b]);
        }

        return result.toString();
    }
}
