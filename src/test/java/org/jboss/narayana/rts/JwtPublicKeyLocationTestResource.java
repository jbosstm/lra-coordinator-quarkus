package org.jboss.narayana.rts;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Map;

/**
 * Enables inbound JWT enforcement where the verification key is loaded from
 * {@code mp.jwt.verify.publickey.location} (a PEM file written at startup), exercising the
 * key-loading-from-location path rather than the inline {@code mp.jwt.verify.publickey} property.
 * No PEM is committed: the key pair is generated programmatically and the file is temporary.
 *
 * <p>Quarkus' dev/test in-memory JWT key generation (which would set an inline public key that
 * shadows this location) is suppressed at build time via the {@code smallrye.jwt.sign.key.location}
 * Surefire system property; see the surefire configuration in {@code pom.xml}.
 */
public class JwtPublicKeyLocationTestResource implements QuarkusTestResourceLifecycleManager {

    static final String ISSUER = "https://lra-coordinator-quarkus/location-issuer";

    private static volatile KeyPair keyPair;
    private static Path publicPemFile;

    static PrivateKey privateKey() {
        return keyPair.getPrivate();
    }

    @Override
    public Map<String, String> start() {
        keyPair = JwtTestKeys.generateRsa();
        try {
            publicPemFile = Files.createTempFile("lra-jwt-publickey", ".pem");
            Files.writeString(publicPemFile, JwtTestKeys.toPem(keyPair.getPublic()));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write test public key PEM", e);
        }

        return Map.of(
                "lra.auth.policy", "authenticated",
                "mp.jwt.verify.publickey.location", publicPemFile.toUri().toString(),
                "mp.jwt.verify.issuer", ISSUER);
    }

    @Override
    public void stop() {
        try {
            if (publicPemFile != null) {
                Files.deleteIfExists(publicPemFile);
            }
        } catch (IOException ignored) {
            // temp file cleanup is best-effort
        }
        keyPair = null;
    }
}
