package org.jboss.narayana.rts;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Map;

/**
 * Generates an RSA key pair programmatically at test startup (no PEM committed to the repo)
 * and wires it into the coordinator as the inbound JWT verification key, enabling JWT
 * enforcement for the annotated test only.
 *
 * <p>The public key is injected via {@code mp.jwt.verify.publickey} (base64-encoded X.509
 * DER); the matching private key is exposed to the test so it can sign tokens.
 */
public class JwtTestResource implements QuarkusTestResourceLifecycleManager {

    static final String ISSUER = "https://lra-coordinator-quarkus/test-issuer";

    private static volatile KeyPair keyPair;

    static PrivateKey privateKey() {
        return keyPair.getPrivate();
    }

    @Override
    public Map<String, String> start() {
        keyPair = JwtTestKeys.generateRsa();

        return Map.of(
                "lra.auth.policy", "authenticated",
                "mp.jwt.verify.publickey", JwtTestKeys.base64Der(keyPair.getPublic()),
                "mp.jwt.verify.issuer", ISSUER);
    }

    @Override
    public void stop() {
        keyPair = null;
    }
}
