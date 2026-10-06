# lra-coordinator-quarkus Project

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## OpenTelemetry and Metrics

This application is instrumented with OpenTelemetry and exposes metrics in Prometheus format.

### Metrics Endpoint

Once the application is running, metrics are available at:
```
http://localhost:8080/q/metrics
```
For more information about the metrics provided by OpenTelemetry please visit https://github.com/quarkusio/quarkus/blob/main/docs/src/main/asciidoc/opentelemetry-metrics.adoc
To expose custom LRA coordinator metrics it is possible to create a REST resource as described in the just mentioned Quarkus doc.

### Example Usage

To view metrics in your browser or with curl:
```shell script
curl http://localhost:8080/q/metrics
```

### Integration with Prometheus

To scrape metrics with Prometheus, add the following job to your `prometheus.yml`:
```yaml
scrape_configs:
  - job_name: 'lra-coordinator'
    metrics_path: '/q/metrics'
    static_configs:
      - targets: ['localhost:8080']
```

### OpenTelemetry Configuration

The application is configured with:
- Service name: `lra-coordinator`
- Traces enabled: Yes
- Metrics enabled: Yes

Additional OpenTelemetry configuration can be modified in `src/main/resources/application.properties`.

### Grafana Dev Service (Optional)

For a fully integrated observability stack in dev mode (Grafana, Loki, Tempo, Prometheus), you can add the following dependency to your `pom.xml`:
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-observability-devservices-lgtm</artifactId>
    <scope>provided</scope>
</dependency>
```

This will automatically start a Grafana LGTM container when running in dev mode, giving you access to a pre-configured Grafana dashboard to visualize metrics, logs, and traces.

## JWT Security (optional)

Narayana LRA 2.x can secure the coordinator with JWT. Support is wired in but **opt-in**: by
default the coordinator runs open and unchanged. Three concerns can be enabled independently via
configuration (no code changes needed) — see the commented block in
`src/main/resources/application.properties`.

### Inbound authentication

Require a valid `Authorization: Bearer <jwt>` on the coordinator endpoints (`/lra-coordinator/*`),
validated by the container's MicroProfile JWT implementation:

```shell script
java -jar target/quarkus-app/quarkus-run.jar \
  -Dlra.auth.policy=authenticated \
  -Dmp.jwt.verify.publickey.location=https://keycloak.example/realms/lra/protocol/openid-connect/certs \
  -Dmp.jwt.verify.issuer=https://keycloak.example/realms/lra
```

`mp.jwt.verify.publickey.location` accepts a static PEM (file/classpath) or a JWKS/OIDC certs URL.
The default `lra.auth.policy=permit` leaves the coordinator open.

### Outbound token propagation

Forward the caller's token on coordinator → participant callbacks (compensate, complete, status,
forget, afterLRA) by registering the callback filter shipped in `lra-coordinator-jar`:

```
-Dlra.http-client.providers=io.narayana.lra.coordinator.security.JwtTokenCallbackRequestFilter
```

### Recovery service token

The recovery thread runs outside any request context, so it has no caller token to propagate.
Point it at a pre-provisioned token (file, classpath, or HTTP endpoint) that it attaches to retried
callbacks:

```
-Dlra.security.service-token.location=/var/run/secrets/lra/token
-Dlra.security.service-token.refresh-seconds=300
```

> **_NOTE:_** Avoid short-lived/expiring tokens for the service token — an LRA that outlives its
> token can never complete.

All settings have matching environment variables (e.g. `LRA_AUTH_POLICY`,
`MP_JWT_VERIFY_PUBLICKEY_LOCATION`). For the full reference and token/issuance details see the
[Narayana LRA security guide](https://github.com/jbosstm/lra/blob/2.0.0.Final/docs/src/main/asciidoc/security.adoc).

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/lra-coordinator-quarkus-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Provided Code

### RESTEasy Reactive

Easily start your Reactive RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
