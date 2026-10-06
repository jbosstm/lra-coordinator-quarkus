package org.jboss.narayana.rts;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * Verifies the opt-in inbound JWT protection of the LRA coordinator endpoints.
 *
 * <p>With {@code lra.auth.policy=authenticated} (set by {@link JwtTestResource}), requests to
 * {@code /lra-coordinator} must carry a valid Bearer token. This exercises the MicroProfile JWT
 * contract: missing credentials and tokens that fail signature/issuer verification are rejected
 * with HTTP 401, while a correctly signed token with the expected issuer is accepted.
 *
 * <p>{@code restrictToAnnotatedClass = true} keeps enforcement scoped to this test so the
 * unauthenticated {@code OpenTelemetryEndpointTest} is unaffected.
 */
@QuarkusTest
@QuarkusTestResource(value = JwtTestResource.class, restrictToAnnotatedClass = true)
class JwtSecurityTest {

    private static final String COORDINATOR_PATH = "/lra-coordinator";

    @Test
    void requestWithoutTokenIsRejected() {
        given()
            .when().get(COORDINATOR_PATH)
            .then()
            .statusCode(401);
    }

    @Test
    void requestWithValidTokenIsAccepted() {
        String token = JwtTestKeys.signedToken(JwtTestResource.privateKey(), JwtTestResource.ISSUER);

        // getAllLRAs returns 200 with the (empty) list once the caller is authenticated.
        given()
            .header("Authorization", "Bearer " + token)
            .when().get(COORDINATOR_PATH)
            .then()
            .statusCode(200);
    }

    @Test
    void requestWithWrongIssuerIsRejected() {
        String token = JwtTestKeys.signedToken(JwtTestResource.privateKey(), "https://attacker/issuer");

        given()
            .header("Authorization", "Bearer " + token)
            .when().get(COORDINATOR_PATH)
            .then()
            .statusCode(401);
    }

    @Test
    void requestWithTokenSignedByUnknownKeyIsRejected() {
        // A token signed by a key the coordinator does not trust must fail signature verification.
        String token = JwtTestKeys.signedToken(JwtTestKeys.generateRsa().getPrivate(), JwtTestResource.ISSUER);

        given()
            .header("Authorization", "Bearer " + token)
            .when().get(COORDINATOR_PATH)
            .then()
            .statusCode(401);
    }
}
