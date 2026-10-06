package org.jboss.narayana.rts;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Minimal REST client that drives a full LRA lifecycle (start -> join -> close) against the
 * running coordinator, used by the outbound-propagation tests. A {@code null} bearer means the
 * request is sent unauthenticated.
 */
final class LraCoordinatorClient {

    private LraCoordinatorClient() {
    }

    private static RequestSpecification request(String bearer) {
        RequestSpecification spec = given().accept(ContentType.TEXT);
        return bearer == null ? spec : spec.header("Authorization", "Bearer " + bearer);
    }

    /** Starts an LRA and returns its id URL (the {@code Location} header of the 201 response). */
    static String start(String bearer) {
        return request(bearer)
                .queryParam("ClientID", "jwt-integration-test")
                .when().post("/lra-coordinator/start")
                .then().statusCode(201)
                .extract().header("Location");
    }

    /** Enlists a participant via the non-deprecated {@code Link} header (complete + compensate rels). */
    static void join(String lraUrl, String completeUrl, String compensateUrl, String bearer) {
        String link = "<" + completeUrl + ">; rel=\"complete\", <" + compensateUrl + ">; rel=\"compensate\"";
        request(bearer)
                .header("Link", link)
                .when().put(lraUrl)
                .then().statusCode(200);
    }

    /**
     * Closes the LRA, which drives the complete callback on every participant. Requires HTTP 200:
     * at the default API version the coordinator completes synchronously (in the caller's request
     * scope), which is what the token-propagation assertions depend on. A 202 would mean asynchronous
     * completion on a scope-less thread and must fail the test rather than pass ambiguously.
     */
    static void close(String lraUrl, String bearer) {
        request(bearer)
                .when().put(lraUrl + "/close")
                .then().statusCode(200);
    }

    /** Waits for a named callback to be recorded, failing with a clear message if it never arrives. */
    static void awaitCallback(String callback) {
        for (int i = 0; i < 100; i++) {
            if (MockParticipantResource.RECEIVED_AUTH.containsKey(callback)) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        fail("Participant '" + callback + "' callback was not received within 10s");
    }
}
