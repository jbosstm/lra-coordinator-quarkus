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

This endpoint provides:
- **HTTP metrics**: Request count, duration, and status codes
- **JVM metrics**: Memory usage, thread count, garbage collection statistics
- **Application metrics**: Custom LRA coordinator metrics

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
