package io.slgl.api.it.utils;

import com.google.common.base.Charsets;
import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.bouncycastle.util.encoders.Hex;

public class HashUtils {

    private static final SHA3.Digest512 SHA3_512 = new SHA3.Digest512();

    private HashUtils() {
    }

    public static String sha3_512(String input) {
        return sha3_512(input.getBytes(Charsets.UTF_8));
    }

    public static String sha3_512(byte[] input) {
        byte[] digest = SHA3_512.digest(input);
        return Hex.toHexString(digest).toLowerCase();
    }
}
