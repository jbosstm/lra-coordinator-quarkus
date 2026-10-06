package org.jboss.narayana.rts;

import io.smallrye.jwt.build.Jwt;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;

/**
 * Shared helpers for the JWT security tests: RSA key generation (programmatic, so no PEM is
 * committed to the repo), public-key encodings accepted by MicroProfile JWT, and token signing.
 */
final class JwtTestKeys {

    private JwtTestKeys() {
    }

    static KeyPair generateRsa() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Could not generate RSA key pair for JWT test", e);
        }
    }

    /** Base64-encoded X.509 DER, suitable for the inline {@code mp.jwt.verify.publickey} property. */
    static String base64Der(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /** PEM-encoded public key, suitable for writing to a file referenced by
     *  {@code mp.jwt.verify.publickey.location}. */
    static String toPem(PublicKey publicKey) {
        String der = Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(publicKey.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + der + "\n-----END PUBLIC KEY-----\n";
    }

    /** Signs a minimal, valid MicroProfile JWT with the given key and issuer. */
    static String signedToken(PrivateKey privateKey, String issuer) {
        return Jwt.issuer(issuer)
                .upn("lra-client")
                .groups(Set.of("user"))
                .expiresIn(Duration.ofHours(1))
                .sign(privateKey);
    }
}
