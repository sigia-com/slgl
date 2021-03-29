package io.slgl.api.it.utils;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.KeyringConfigCallbacks;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.InMemoryKeyring;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfigs;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.util.io.Streams;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchProviderException;

public class PgpDecrypter {

    static  {
        BouncyGPG.registerProvider();
    }

    public byte[] decrypt(byte[] encryptedData, String pgpPrivateKey, String passphrase) {
        try (ByteArrayOutputStream decryptedStream = new ByteArrayOutputStream()){
            InMemoryKeyring keyring = KeyringConfigs.forGpgExportedKeys(KeyringConfigCallbacks.withPassword(passphrase));
            keyring.addSecretKey(pgpPrivateKey.getBytes(StandardCharsets.US_ASCII));
            try (
                ByteArrayInputStream in = new ByteArrayInputStream(encryptedData);
                BufferedOutputStream buffDecryptedStream = new BufferedOutputStream(decryptedStream);
                InputStream plaintextStream = BouncyGPG
                    .decryptAndVerifyStream()
                    .withConfig(keyring)
                    .andIgnoreSignatures()
                    .fromEncryptedInputStream(in);
            ) {
                Streams.pipeAll(plaintextStream, decryptedStream);
            }
            return decryptedStream.toByteArray();
        } catch (IOException | PGPException | NoSuchProviderException e) {
            throw new IllegalStateException("Data decryption failed", e);
        }
    }
}
