package io.slgl.api.it.data;

import io.slgl.api.it.utils.PgpDecrypter;

import java.io.FileInputStream;
import java.io.IOException;

import static com.google.common.io.ByteStreams.toByteArray;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Objects.requireNonNull;

public class PgpTestKeys {

    public final static String PASSPHRASE = "Password1";

    public static String JON_DOE_PUBLIC_KEY;
    public static String JON_DOE_PRIVATE_KEY;

    public static String JACK_DANIELS_PUBLIC_KEY;
    public static String JACK_DANIELS_PRIVATE_KEY;

    static {
        try (
            FileInputStream jonPublicKeyStream = new FileInputStream(requireNonNull(PgpDecrypter.class.getClassLoader().getResource("pgp/jon.doe.asc")).getFile());
            FileInputStream jonPrivateKeyStream = new FileInputStream(requireNonNull(PgpDecrypter.class.getClassLoader().getResource("pgp/jon.doe.secret.pgp")).getFile());
            FileInputStream jackPublicKeyStream = new FileInputStream(requireNonNull(PgpDecrypter.class.getClassLoader().getResource("pgp/jack.daniels.asc")).getFile());
            FileInputStream jackPrivateKeyStream = new FileInputStream(requireNonNull(PgpDecrypter.class.getClassLoader().getResource("pgp/jack.daniels.secret.pgp")).getFile());
        ) {
            JON_DOE_PUBLIC_KEY = new String(toByteArray(jonPublicKeyStream), US_ASCII);
            JON_DOE_PRIVATE_KEY = new String(toByteArray(jonPrivateKeyStream), US_ASCII);
            JACK_DANIELS_PUBLIC_KEY = new String(toByteArray(jackPublicKeyStream), US_ASCII);
            JACK_DANIELS_PRIVATE_KEY = new String(toByteArray(jackPrivateKeyStream), US_ASCII);
        } catch (IOException e) {
            throw new IllegalArgumentException("PGP keys could't be loaded", e);
        }
    }

}
