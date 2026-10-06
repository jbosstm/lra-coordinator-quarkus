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
 * Single configuration that enables both outbound-propagation concerns so that
 * {@code JwtTokenContext} — whose provider list and service-token provider are {@code static final}
 * and initialized once per JVM — is initialized with the complete configuration:
 * <ul>
 *   <li>the callback filter registered via {@code lra.http-client.providers}, and</li>
 *   <li>a pre-provisioned recovery service token via {@code lra.security.service-token.location}.</li>
 * </ul>
 * Inbound auth is left open ({@code permit}) so the service-token scenario can issue an
 * unauthenticated close; a provided token is still validated against the inline public key and
 * exposed via CDI. Key material is generated programmatically (no PEM committed).
 */
public class JwtTokenPropagationTestResource implements QuarkusTestResourceLifecycleManager {

    static final String ISSUER = "https://lra-coordinator-quarkus/propagation-issuer";

    private static volatile KeyPair keyPair;
    // The service token is a real RS256 MP-JWT (as the spec requires). Because smallrye-jwt enables
    // proactive authentication app-wide, any Bearer token on the callback is validated by the
    // container before reaching the resource, so an opaque string would be rejected with 401.
    private static volatile String serviceToken;
    private static Path tokenFile;

    static PrivateKey privateKey() {
        return keyPair.getPrivate();
    }

    static String serviceToken() {
        return serviceToken;
    }

    @Override
    public Map<String, String> start() {
        keyPair = JwtTestKeys.generateRsa();
        serviceToken = JwtTestKeys.signedToken(keyPair.getPrivate(), ISSUER);
        try {
            tokenFile = Files.createTempFile("lra-service-token", ".jwt");
            Files.writeString(tokenFile, serviceToken);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write test service token", e);
        }

        return Map.of(
                "mp.jwt.verify.publickey", JwtTestKeys.base64Der(keyPair.getPublic()),
                "mp.jwt.verify.issuer", ISSUER,
                "lra.http-client.providers",
                "io.narayana.lra.coordinator.security.JwtTokenCallbackRequestFilter",
                "lra.security.service-token.location", tokenFile.toAbsolutePath().toString());
    }

    @Override
    public void stop() {
        try {
            if (tokenFile != null) {
                Files.deleteIfExists(tokenFile);
            }
        } catch (IOException ignored) {
            // temp file cleanup is best-effort
        }
        keyPair = null;
    }
}
