# ApiHelidonParent

Maven parent project that provides [Helidon 4](https://helidon.io/) integration for the Progbits **ApiObject** model. These libraries wire Progbits API types into Helidon web servers and HTTP clients, with built-in support for observability, health checks, and API documentation.

**Version:** 1.2.0  
**Java:** 21  
**Helidon:** 4.3.2

## Modules

| Module | Artifact | Description |
|--------|----------|-------------|
| [ApiModelsHelidonMediaJson](ApiModelsHelidonMediaJson/) | `com.progbits.api.helidon.media:ApiModelsHelidonMediaJson` | JSON serialization/deserialization for `ApiObject` via Helidon media support |
| [ApiModelsHelidonMediaYaml](ApiModelsHelidonMediaYaml/) | `com.progbits.api.helidon.media:ApiModelsHelidonMediaYaml` | YAML serialization/deserialization for `ApiObject` |
| [ApiModelsHelidonCommon](ApiModelsHelidonCommon/) | `com.progbits.api.helidon.filters:ApiModelsHelidonCommon` | Server bootstrap, routing, filters, handlers, and HTTP client utilities |

`ApiModelsHelidonCommon` depends on both media modules and on other Progbits libraries (`ApiTransforms_jre21`, `ConfigProvider_jre21`).

## Features

### Web server

- **`WebServerProcessor`** — Creates a preconfigured `WebServerConfig.Builder` with optional `ApiObject` media handling (JSON + YAML), GZip encoding, and configuration from `application.yaml` or Progbits `ConfigProvider`.
- **`ApiRouterProcessor`** — Fluent router setup that can register:
  - **Health checks** at `{contextPath}/healthcheck` (plain-text or HTML detail views)
  - **Prometheus metrics** at `{contextPath}/metrics`
  - **RapiDoc** at `{contextPath}/api` (serves bundled OpenAPI YAML)
  - **X-Flow-Id** request tracing (header propagation and MDC integration)
  - **SLF4J access logging**
  - Centralized error handling for `HttpException` and `ApiException`

### HTTP client

- **`WebClientUtil`** — Builds Helidon `WebClient` instances with `ApiObject` media support, optional Prometheus metrics (`webclient_totals`, `webclient_status`, `webclient_duration_seconds`), and gzip/deflate compression.
- Convenience methods for making HTTP calls with headers, query params, path params, form data, and automatic `X-Flow-Id` propagation from MDC.

### Request helpers

- **`ApiHelidonUtils`** — Extract path variables, query parameters (including typed and list values), and headers from Helidon requests; send `ApiObject` responses with appropriate status codes.

### Filters

| Filter | Purpose |
|--------|---------|
| `XFlowIdFilter` | Ensures every request has an `X-Flow-Id` header; stores the value in SLF4J MDC |
| `HelidonSlf4jAccessLogFilter` | Structured access logging via SLF4J |
| `HelidonPrometheusFilter` | Collects HTTP server metrics for Prometheus |

## Requirements

- JDK 21+
- Maven 3.x
- Access to the Progbits Maven repository (`https://archiva.progbits.com/coffer/repository/internal/`) for Progbits dependencies

## Build

From the project root:

```bash
mvn clean install
```

Run tests for the common module:

```bash
mvn test -pl ApiModelsHelidonCommon
```

## Usage

### Start a web server

Add `application.yaml` to the classpath with a `server` section, then:

```java
WebServerProcessor.returnWebServer(true, true)
    .routing(routing -> {
        ApiRouterProcessor.builder(routing, "/api")
            .healthCheck("My Service", "Production")
            .registerHealthCheck(HealthcheckHandler.LEVEL_DEFAULT, myHealthCheck)
            .xflowId("mysvc")
            .prometheus("mysvc")
            .process(log);
        // register your routes on `routing`
    })
    .build()
    .start();
```

### Make an HTTP call

```java
WebClient client = WebClientUtil.getClient("https://api.example.com", true, true);

ApiObject props = new ApiObject();
props.createObject("params").setString("limit", "10");

ApiObject response = WebClientUtil.makeHttpCall(
    client, "/items", "GET", null, "message", props, null);
```

### Health checks

Implement the `HealthCheck` interface and register checks by priority level (`DEFAULT`, `HIGH`, `MEDIUM`, `LOW`):

```java
ApiRouterProcessor.builder(routing, "/api")
    .healthCheck("My Service", "v1.2.0")
    .registerHealthCheck(HealthcheckHandler.LEVEL_DEFAULT, () -> {
        ApiObject result = new ApiObject();
        result.setString(HealthcheckHandler.FIELD_PROGRAM, "Database");
        result.setString(HealthcheckHandler.FIELD_PRIORITY, "DEFAULT");
        result.setBoolean(HealthcheckHandler.FIELD_HEALTHCHECK, db.isConnected());
        result.setString(HealthcheckHandler.FIELD_STATUS, db.isConnected() ? "OK" : "DOWN");
        return result;
    })
    .process(log);
```

Endpoints:

| URL | Response |
|-----|----------|
| `/healthcheck` | `Ok` or `Fail` (503 on failure) |
| `/healthcheck?warn` | Checks `DEFAULT` and `HIGH` priority levels |
| `/healthcheck?details` | HTML status page |

## Project structure

```
ApiHelidonParent/
├── pom.xml                          # Parent POM
├── ApiModelsHelidonMediaJson/       # JSON media support
├── ApiModelsHelidonMediaYaml/       # YAML media support
└── ApiModelsHelidonCommon/          # Server, client, filters, handlers
    └── src/main/resources/
        ├── healthcheck.html         # Health check detail page template
        └── rapidoc.html             # RapiDoc UI template
```

## License

Proprietary — Progbits internal library.
