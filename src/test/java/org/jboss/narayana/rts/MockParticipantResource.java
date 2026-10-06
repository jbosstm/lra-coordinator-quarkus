package org.jboss.narayana.rts;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.lra.annotation.ParticipantStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A minimal LRA participant used by the outbound-propagation tests. It records the
 * {@code Authorization} header the coordinator sends on each termination callback, so a test can
 * assert whether (and which) Bearer token was propagated. It lives outside {@code /lra-coordinator}
 * so it is never subject to the coordinator's inbound auth policy.
 */
@Path("/mock-participant")
public class MockParticipantResource {

    static final String COMPLETE = "complete";
    static final String COMPENSATE = "compensate";

    /** Callback name -> the raw Authorization header received ("" when none). */
    static final Map<String, String> RECEIVED_AUTH = new ConcurrentHashMap<>();

    static void reset() {
        RECEIVED_AUTH.clear();
    }

    @PUT
    @Path("/complete")
    @Produces(MediaType.TEXT_PLAIN)
    public Response complete(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        RECEIVED_AUTH.put(COMPLETE, authorization == null ? "" : authorization);
        return Response.ok(ParticipantStatus.Completed.name()).build();
    }

    @PUT
    @Path("/compensate")
    @Produces(MediaType.TEXT_PLAIN)
    public Response compensate(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        RECEIVED_AUTH.put(COMPENSATE, authorization == null ? "" : authorization);
        return Response.ok(ParticipantStatus.Compensated.name()).build();
    }
}
