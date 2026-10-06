package org.jboss.narayana.rts;

import io.narayana.lra.coordinator.security.JwtTokenContext;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end tests of outbound JWT token propagation on coordinator -> participant callbacks,
 * driven by registering {@code JwtTokenCallbackRequestFilter} via {@code lra.http-client.providers}.
 *
 * <p>Both of {@code JwtTokenContext}'s resolution sources are covered:
 * <ol>
 *   <li>the caller's inbound token (via CDI {@code JsonWebToken}) on an authenticated close, and</li>
 *   <li>the pre-provisioned recovery service token when no inbound token is resolvable — exercised
 *       by invoking {@code JwtTokenContext.newClient()} from a plain thread with no CDI request
 *       scope, as the recovery thread does.</li>
 * </ol>
 * They share one app configuration because the context's provider list and service-token provider
 * are initialized once per JVM.
 */
@QuarkusTest
@QuarkusTestResource(value = JwtTokenPropagationTestResource.class, restrictToAnnotatedClass = true)
class JwtTokenPropagationTest {

    @TestHTTPResource("/mock-participant")
    URI participant;

    @BeforeEach
    void resetParticipant() {
        MockParticipantResource.reset();
    }

    @Test
    void inboundTokenIsForwardedOnCompleteCallback() {
        String token = JwtTestKeys.signedToken(
                JwtTokenPropagationTestResource.privateKey(), JwtTokenPropagationTestResource.ISSUER);

        String lra = LraCoordinatorClient.start(token);
        LraCoordinatorClient.join(lra, participant + "/complete", participant + "/compensate", token);
        LraCoordinatorClient.close(lra, token);

        LraCoordinatorClient.awaitCallback(MockParticipantResource.COMPLETE);

        assertEquals("Bearer " + token,
                MockParticipantResource.RECEIVED_AUTH.get(MockParticipantResource.COMPLETE),
                "coordinator should propagate the caller's Bearer token on the complete callback");
    }

    @Test
    void serviceTokenIsForwardedFromRecoveryThread() throws Exception {
        // The recovery thread drives callbacks outside any CDI request scope. Reproduce that by
        // using the coordinator's own JwtTokenContext.newClient() on a plain thread: with no inbound
        // token resolvable, it must attach the pre-provisioned service token to the outbound call.
        AtomicReference<Object> outcome = new AtomicReference<>();
        Thread recoveryThread = new Thread(() -> {
            try (Client client = JwtTokenContext.newClient()) {
                int status = client.target(participant + "/complete").request().put(Entity.text("")).getStatus();
                outcome.set(status);
            } catch (Throwable t) {
                outcome.set(t);
            }
        });
        recoveryThread.start();
        recoveryThread.join();

        assertEquals(200, outcome.get(), "recovery-thread callback should succeed; got: " + outcome.get());
        assertEquals("Bearer " + JwtTokenPropagationTestResource.serviceToken(),
                MockParticipantResource.RECEIVED_AUTH.get(MockParticipantResource.COMPLETE),
                "coordinator should attach the service token on callbacks made without a CDI request scope");
    }
}
