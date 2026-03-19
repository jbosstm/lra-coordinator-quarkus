package org.jboss.narayana.rts;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class OpenTelemetryEndpointTest {

    @Test
    void metricsEndpointReturnsPrometheusData() {
        given()
            .when().get("/q/metrics")
            .then()
            .statusCode(200)
            .contentType(containsString("openmetrics-text"))
            .body(containsString("jvm_memory"));
    }

    @Test
    void metricsEndpointContainsHttpServerMetrics() {
        given()
            .when().get("/q/metrics")
            .then()
            .statusCode(200)
            .body(containsString("http_server"));
    }
}
