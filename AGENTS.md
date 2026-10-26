# AGENTS.md

Guidance for coding agents working in this repository.

## Project overview

Template/demo service from Softeno showing production patterns for a Kotlin + Spring Boot web service: Spring MVC, Spring Data JPA on PostgreSQL, Keycloak OAuth2 resource server, Kafka consumers/producers, Liquibase migrations, Testcontainers integration tests, circuit-breaker external calls, and observability (actuator/Prometheus, OpenTelemetry, springdoc). It is a starting point to copy and adapt, not a product.

## Stack and versions (from `build.gradle`)

- Kotlin 2.4.20 (JVM, kapt enabled, `-Xjsr305=strict`), Java toolchain 24 (virtual threads enabled)
- Spring Boot 4.1.1, Spring Cloud 2025.1.3 (resilience4j circuit breaker)
- Gradle wrapper 9.3.1 — always use `./gradlew`, never a system Gradle
- PostgreSQL 16.x, Liquibase 5.0.4, Testcontainers 2.0.5, MockK 1.14 + springmockk, WireMock 3.x, springdoc-openapi 3.x
- No lint/format tooling is configured (no ktlint/detekt/spotless).

## Key paths

- `src/main/kotlin/com/softeno/template/app/` — application code, organized **by feature**:
  - `permission/` — reference CRUD feature (`api/`, `db/`, `mapper/`, `service/` subpackages, entity at root)
  - `kafka/` — sample producer/consumer + Keycloak events consumer
  - `common/` — `BaseEntity`, global exception handling, app events, error types
  - `config/` — security (`SecurityConfig`, `AuditorConfig`), OpenTelemetry
- `src/main/kotlin/com/softeno/template/sample/` — illustrative external HTTP client (WebClient + circuit breaker)
- `src/main/resources/` — `application*.properties`, `db/db.changelog.yml` + `db/changesets/`, `logback-spring.xml`
- `src/test/kotlin/.../app/IntegrationTest.kt` — all integration tests live here (base class + test classes)
- `README.md` — user-facing template guide (purpose, features, structure, quick start, feature-removal table)
- `http/` — JetBrains HTTP client requests (`auth.http`, `user-app.http`)
- `docker-compose.yml` — local Kafka, Keycloak (+ its Postgres on 5433), and app Postgres on 5432

## Conventions

- Feature-per-package layout; within a feature: `api` (controllers), `db` (repositories), `mapper` (DTO + mapping extension functions), `service`.
- Entities extend `app/common/db/BaseEntity`: UUID business id, sequence `id`, audit fields (`createdBy/Date`, `modifiedBy/Date`), `@Version` optimistic locking. New entities must also be created as Liquibase changesets — `ddl-auto=validate`.
- DTOs are plain data classes with `toDto()` extension mappers (no MapStruct/ModelMapper).
- Constructor injection; `@ConfigurationProperties` data classes scanned from `com.softeno`; logging via `LogFactory.getLog(javaClass)`.
- JPA auditing resolves the auditor from the JWT `sub` claim (`AuditorConfig`); falls back to `"system"`.
- Controllers return `ResponseEntity` with Spring Data `Page` (paged via `getPageRequest` helper); exceptions map through `GlobalExceptionHandler`.
- App events: publish `AppEvent` from controllers/services; `AppEventHandler` forwards them to Kafka (outside the `integration` profile).

## Commands

| Task | Command |
|---|---|
| Full build + tests (what CI runs) | `./gradlew build` |
| Fast compile check only | `./gradlew compileKotlin compileTestKotlin` (verified) |
| Run app locally (port 8082) | `./gradlew bootRun` — needs Postgres/Kafka/Keycloak up, or use the `local` profile to skip OTLP export |
| Run tests only | `./gradlew test` — **requires Docker** (Testcontainers spins up Kafka + Postgres); not executed in this repo session |
| Executable jar | `./gradlew bootJar` → `build/libs/*-SNAPSHOT.jar` (Dockerfile copies `build/libs/*.jar`; delete the `*plain.jar` first, as CI does) |
| Apply Liquibase changelogs manually | `./gradlew update` / `status` / `updateSql` (targets `jdbc:postgresql://localhost:5432/application`, admin/admin per `build.gradle`) |
| Generate changelog from DB diff | `./gradlew diffChangelog` (both url and referenceUrl point at the same Postgres; `liquibase-hibernate6` is on the classpath for Hibernate references if reconfigured) |
| Build container image | `docker build .` after `./gradlew bootJar` (image runs profile `docker`, EXPOSE 8082) |
| Start supporting services | `docker compose up -d` (bind-mount volumes live under `./postgres/volumes` and `./kafka+keycloak/volumes`; create them if compose fails) |

## Configuration profiles

- **default** (`application.properties`): app on `:8082`; Postgres at `localhost:5434`; Kafka `localhost:9094`; Keycloak realm `app` at `localhost:8090`; OTLP endpoint `localhost:4318`. Swagger UI at `/swagger-ui.html`, actuator at `/actuator`.
- **local**: same but disables OTLP metrics/log/tracing export (use when no collector runs).
- **docker**: used by the Dockerfile; only toggles bean-definition overriding.
- **integration**: used by tests; security is permit-all (test `SecurityConfig`), datasource/Kafka are Testcontainers with dynamic properties.
- `http/http-client.env.json` is gitignored — copy `http/http-client.env.template.json` and fill it to use the `http/*.http` requests.

## Security notes

- JWT resource server: Keycloak `realm_access.roles` become Spring authorities; `/permissions/**`, `/external/**`, `/error/**` require `ROLE_ADMIN`; actuator and swagger paths are public; CSRF disabled, CORS wide-open (template defaults).
- `application.properties` contains a committed dev-only Keycloak client secret. Treat it as a template placeholder: never add real credentials here; prefer env/`SPRING_APPLICATION_JSON` overrides for real deployments.

## Testing

- All tests are `@SpringBootTest` integration tests extending `BaseIntegrationTest` (Testcontainers Kafka `apache/kafka-native`, Postgres `16-alpine`, Liquibase runs against the container DB). Docker must be running.
- Add new tests as subclasses of `BaseIntegrationTest`; reuse `PermissionFixture` companion-object builders and the `*Ability` interface pattern for WireMock stubs (WireMock binds port 4500, matching `com.softeno.external.url` in test properties).
- Use `@MockkBean`/springmockk for bean-level mocking (see `PermissionTestMockk`); `WebTestClient` for HTTP assertions.

## Gotchas

- Default datasource URL is `localhost:5434`, but the root compose maps Postgres to `5432` — either set `SPRING_DATASOURCE_URL`/`SPRING_LIQUIBASE_URL` or change the mapping. The Liquibase Gradle block uses `5432`.
- Liquibase runs automatically at startup (`spring.liquibase.enabled=true`); schema changes go in `src/main/resources/db/changesets/`, included from `db/db.changelog.yml`.
- Tests define their own datasource via dynamic properties; `src/test/resources/application.properties` must stay consistent with container expectations (e.g. external URL port 4500).
- Build uses deprecated Gradle features (warns toward Gradle 10); a macOS-ARM-only Netty DNS resolver dependency is added conditionally.
- CI: `ci.yml` runs `./gradlew build` on JDK 24 for all branches/PRs; `build-and-upload.yml` additionally publishes a multi-arch image to Docker Hub on `main`/`release/*`.

## Maintaining the docs

- **`README.md` must always be up to date.** After finishing any development task, review the changes against the README and update it whenever anything it describes changed: project purpose, features (including which are optional/removable), project structure, quick-start steps, commands, config/profiles, or the feature-removal table. Do not skip this even when the README "obviously" still reads well — stale docs are bugs.
- **`AGENTS.md` (this file) must also be kept up to date.** After finishing any development task, review the changes against this file and update it when anything it documents changed: stack versions, module layout, commands, profiles, service dependencies, security behavior, gotchas, or test strategy. Keep it concise; verify commands against `build.gradle`/CI before adding them. `.gitmodules` is empty — there are no submodules.
