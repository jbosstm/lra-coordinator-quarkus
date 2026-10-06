package org.jboss.narayana.rts;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * Verifies inbound JWT enforcement when the verification key is supplied via
 * {@code mp.jwt.verify.publickey.location} (a PEM file) rather than inline. Confirms the coordinator
 * loads the key from the configured location and validates tokens against it.
 */
@QuarkusTest
@QuarkusTestResource(value = JwtPublicKeyLocationTestResource.class, restrictToAnnotatedClass = true)
class JwtPublicKeyLocationTest {

    private static final String COORDINATOR_PATH = "/lra-coordinator";

    @Test
    void tokenVerifiedAgainstKeyFromLocationIsAccepted() {
        String token = JwtTestKeys.signedToken(
                JwtPublicKeyLocationTestResource.privateKey(), JwtPublicKeyLocationTestResource.ISSUER);

        given()
            .header("Authorization", "Bearer " + token)
            .when().get(COORDINATOR_PATH)
            .then()
            .statusCode(200);
    }

    @Test
    void requestWithoutTokenIsRejected() {
        given()
            .when().get(COORDINATOR_PATH)
            .then()
            .statusCode(401);
    }
}
