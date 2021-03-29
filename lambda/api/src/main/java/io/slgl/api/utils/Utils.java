package io.slgl.api.utils;

import com.arakelian.jq.*;
import com.sun.jna.Platform;
import org.apache.commons.lang3.RandomStringUtils;
import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.bouncycastle.util.encoders.Hex;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import static com.arakelian.jq.JqRequest.Indent.SPACE;

public class Utils {
    /**
     * This method returns equivalent of:
     * jq -S . file.with.original.json | rhash --sha3-512 /dev/stdin
     */
    public static String getSha3OnJqSCompliantJson(String json) {
        String normalizedJson = normalizeJson(json);

        SHA3.DigestSHA3 digestSHA3 = new SHA3.Digest512();
        byte[] digest = digestSHA3.digest(normalizedJson.getBytes());
        return Hex.toHexString(digest).toLowerCase();
    }

    private static String normalizeJson(String json) {
        if (Platform.isWindows()) {
            return json; // TODO java-jq doesn't work on Windows
        }

        JqLibrary library = ImmutableJqLibrary.of();
        JqRequest request = ImmutableJqRequest.builder().lib(library).input(json).sortKeys(true).indent(SPACE).build();
        JqResponse response = request.execute();

        // add new line to make it match what linux generates
        return response.getOutput() + "\n";
    }

    public static String getSha3OfBytes(byte[] bytes) {
        SHA3.DigestSHA3 digestSHA3 = new SHA3.Digest512();
        byte[] digest = digestSHA3.digest(bytes);
        return Hex.toHexString(digest).toLowerCase();
    }

    public static String generateSha3Salt() {
        return RandomStringUtils.randomAlphanumeric(128);
    }

    public static String generateId() {
        return UUID.randomUUID().toString().toLowerCase();
    }

    public static String concatenateAsUrlPartsWithSlash(String... parts) {
        if (parts == null) {
            return null;
        }
        return Arrays
                .stream(parts)
                .filter(Objects::nonNull)
                .map(pathPart -> pathPart.replaceAll("^/+", "").replaceAll("/+$", ""))
                .reduce((a, b) -> a + "/" + b)
                .orElse("");
    }

    private static String randomId() {
        return RandomStringUtils.randomAlphanumeric(25);
    }
}
