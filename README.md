# Spring MVC + JPA Service Template

A **service template**, not a product. It gives you a production-shaped Kotlin + Spring Boot web service with the tedious parts already configured — database, migrations, authentication, messaging, observability, tests, CI, and containerization — so you can start a new app, prototype, or validate an idea in minutes instead of days.

Copy the repo, rename the project, delete the features you don't need (see [Removing optional features](#removing-optional-features)), and start building your actual domain. The reference `permission` feature shows the intended structure and patterns for new features.

## Tech stack

| Concern | Technology |
|---|---|
| Language / runtime | Kotlin (JVM), Java 24 toolchain, virtual threads |
| Framework | Spring Boot 4.1.1 (Spring MVC, Spring Data JPA) |
| Database | PostgreSQL 16 + Liquibase 5 migrations (`ddl-auto=validate`) |
| Auth | Keycloak (OAuth2/OIDC resource server + client-credentials client) |
| Messaging | Apache Kafka (KRaft in compose) |
| Resilience | Resilience4j circuit breaker for external calls |
| Observability | Actuator + Prometheus, OpenTelemetry (traces/metrics/logs), springdoc/Swagger |
| Testing | JUnit 5, Testcontainers 2 (Kafka + Postgres), MockK/springmockk, WireMock |
| Build | Gradle wrapper 9.3.1 (`./gradlew`), GraalVM/Temurin JDK 24 |
| Delivery | GitHub Actions (build/test + multi-arch Docker image) |

## Features

**Core (intended for every service):**
- REST CRUD pattern with pagination, sorting, validation, and a global exception handler
- JPA entities with UUID business id, audit fields, and optimistic locking (`@Version`) — three update strategies demonstrated (save-and-flush, EntityManager, audited JPQL)
- Liquibase-managed schema that runs automatically at startup
- Integration-test harness: full Spring context against real Postgres + Kafka containers, with security disabled in the `integration` profile
- Health/metrics endpoints and OpenAPI docs out of the box

**Optional (remove what you don't need):**
- Keycloak authentication & role-based authorization
- Kafka producer/consumer + Keycloak-events consumer + in-app events forwarded to Kafka
- Example secured external service call with circuit breaker and fallback (`sample/` package)
- OpenTelemetry export to an OTLP collector
- The `permission` reference feature itself (replace it with your own domain)

## Project structure

```
.
├── build.gradle                 # versions, dependencies, Liquibase Gradle activity
├── docker-compose.yml           # Kafka + Keycloak (+ its Postgres) + app Postgres
├── docker/postgres/             # minimal compose with only Postgres (fastest start)
├── Dockerfile                   # runs build/libs/*.jar with the `docker` profile
├── http/                        # JetBrains HTTP client requests + env template
│   ├── auth.http                #   obtain Keycloak tokens
│   └── user-app.http            #   exercise the REST API
├── src/main/kotlin/com/softeno/template/
│   ├── SoftenoMvcJpaApp.kt      # Spring Boot entry point
│   ├── app/
│   │   ├── common/              # BaseEntity (audit + @Version), error handling, app events
│   │   ├── config/              # security (JWT, auditing), OpenTelemetry
│   │   ├── kafka/               # sample producer/consumer, Keycloak event consumer
│   │   └── permission/          # reference feature: api/ db/ mapper/ service/ + entity
│   └── sample/http/external/    # example external WebClient call + circuit breaker
├── src/main/resources/
│   ├── application*.properties  # default + local/docker/integration profiles
│   └── db/                      # Liquibase changelog + changesets
├── src/test/kotlin/…/app/       # BaseIntegrationTest + tests (Testcontainers)
└── .github/workflows/           # CI build/test + Docker image publishing
```

## Quick start

Prerequisites: JDK 24 (required by the Gradle toolchain; no auto-download resolver is configured, so install it, e.g. via SDKMAN or Temurin) and Docker.

```bash
git clone <this-repo> && cd spring-mvc-jpa-template

# start supporting services (Kafka, Keycloak, Postgres)
mkdir -p postgres/volumes/postgres kafka+keycloak/volumes/{kafka-broker,postgres,keycloak}
docker compose up -d

# run the app (local profile skips OTLP export; port 8082)
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

> **Port note:** the default config expects Postgres on `localhost:5434`, while compose publishes it on `5432`. Either set `SPRING_DATASOURCE_URL` and `SPRING_LIQUIBASE_URL` (e.g. `jdbc:postgresql://localhost:5432/application`) or change one side to match.

Then:
- API: `http://localhost:8082/permissions` (requires a `ROLE_ADMIN` JWT)
- Swagger UI: `http://localhost:8082/swagger-ui.html` · Actuator: `/actuator`
- Keycloak admin console: `http://localhost:8090` (`admin`/`admin`), realm `app`
- For manual API testing, copy `http/http-client.env.template.json` to `http-client.env.json`, fill in the values, and run `http/auth.http` first to obtain tokens

Run the test suite (Docker required — Testcontainers pull Kafka/Postgres images):

```bash
./gradlew test
```

## Removing optional features

Each feature is deliberately isolated to a package, a dependency block, and a few properties. To remove one:

| Feature | Delete code | Remove from `build.gradle` | Clean up config |
|---|---|---|---|
| Kafka | `app/kafka/`, `app/common/events/` **and** the `publishEvent(AppEvent(...))` call in `PermissionController` | `spring-boot-starter-kafka`, `spring-kafka-test`, `testcontainers-kafka` | `spring.kafka.*`, `com.softeno.kafka.*`; Kafka services in compose; `KafkaContainer` in `BaseIntegrationTest` |
| Keycloak auth | main `app/config/security/SecurityConfig.kt` (keep the test one or simplify it); `sample/http/external/` depends on it, so remove both together | the three `oauth2-*`/`security` starters + `spring-security-test` | `spring.security.oauth2.*`; keep `AuditorConfig.kt` (it enables JPA auditing and `PermissionService` injects its bean) but replace the JWT-`sub` lookup with a constant (`"system"`) |
| External call sample | `sample/`, `ExternalControllerTest`, test `WebClientConfig` | `spring-cloud-starter-circuitbreaker-resilience4j`, `spring-boot-starter-webclient(-test)` | `com.softeno.external.*`, `resilience4j.*` |
| OpenTelemetry | `app/config/opentelemetry/` | `spring-boot-starter-opentelemetry(-test)`, logback appender | `management.opentelemetry.*`, `management.tracing.*`, `management.otlp.metrics.export.*` (or just use the `local` profile to disable export) |
| Swagger docs | — | `springdoc-openapi-*` | `springdoc.*`, swagger paths in `SecurityConfig` |
| Permission sample feature | `app/permission/`, `PermissionFixture`, permission tests | — | drop `db/changesets/changelog-001.yml` and create your own first changeset |

After removing anything, delete its references from `src/test/resources/application.properties` too, then run `./gradlew build` to catch leftovers.

## CI/CD

- `ci.yml` — `./gradlew build` on JDK 24 for every branch and PR
- `build-and-upload.yml` — on `main`/`release/*`: build, upload the jar, publish a multi-arch (amd64/arm64) image to Docker Hub, and submit a dependency graph

## License

MIT — see [LICENSE.md](LICENSE.md).
