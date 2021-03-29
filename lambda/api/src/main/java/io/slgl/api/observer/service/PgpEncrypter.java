package io.slgl.api.observer.service;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.KeyringConfigCallbacks;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.InMemoryKeyring;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfig;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfigs;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.util.io.Streams;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class PgpEncrypter {

    static  {
        BouncyGPG.registerProvider();
    }

    public byte[] encrypt(byte[] data, String publicPgpKey) {
        try (ByteArrayOutputStream encryptedDataStream = new ByteArrayOutputStream()) {
            InMemoryKeyring keyring = KeyringConfigs.forGpgExportedKeys(KeyringConfigCallbacks.withPassword(""));
            keyring.addPublicKey(publicPgpKey.getBytes(StandardCharsets.US_ASCII));
            try (
                ByteArrayInputStream in = new ByteArrayInputStream(data);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(encryptedDataStream);
                OutputStream outputStream = BouncyGPG
                    .encryptToStream()
                    .withConfig(keyring)
                    .withStrongAlgorithms()
                    .toRecipient(getRecipientId(keyring))
                    .andDoNotSign()
                    .binaryOutput()
                    .andWriteTo(bufferedOutputStream);
            ) {
                Streams.pipeAll(in, outputStream);
            }
            return encryptedDataStream.toByteArray();
        } catch (IOException | PGPException | NoSuchAlgorithmException | SignatureException | NoSuchProviderException e) {
            throw new IllegalStateException("Data encryption failed", e);
        }
    }

    private String getRecipientId(KeyringConfig keyringConfig) throws IOException, PGPException {
        return toStream(keyringConfig.getPublicKeyRings().getKeyRings())
            .findFirst()
            .map(PGPPublicKeyRing::getPublicKeys)
            .flatMap(keys -> toStream(keys).findFirst())
            .map(PGPPublicKey::getUserIDs)
            .flatMap(userIds -> toStream(userIds).findFirst())
            .orElseThrow(() -> new IllegalArgumentException("Incorrect PGP Public key user id can't be extracted"));
    }

    private <T> Stream<T> toStream(Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }
}
