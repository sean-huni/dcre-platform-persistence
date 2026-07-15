# dcre-platform-persistence

Shared persistence platform library for the DCRE Collections fleet: `BaseEntity` plus the Spring Data JDBC `JdbcConfig` every Spring Boot stage service imports for CockroachDB persistence.

## What it does

Provides the persistence base shared by all DCRE stage services. `BaseEntity` gives every aggregate a client-assigned v4 UUID primary key, an optimistic-locking `@Version` and `created_at`/`updated_at` audit timestamps (`Instant`). `JdbcConfig` wires `@EnableJdbcAuditing` plus a `BeforeConvertCallback<BaseEntity>` that assigns the UUID before the first INSERT, because Spring Data JDBC includes the id in INSERTs and bypasses DB defaults. Published as `za.co.fnb.dcre:platform-persistence:0.1.0` to Maven Local and consumed as a normal Gradle dependency.

## Architecture and principles

- SOLID, single responsibility per class: `BaseEntity` owns aggregate identity and audit state; `JdbcConfig` owns wiring (auditing plus the id-assignment callback). Two small units with a clear interface: extend one, `@Import` the other.
- Layer-first packages: in consuming services entities live in `data/model` and extend `BaseEntity`; repositories live in `data/repo`. This library sits beneath that layer and imports only `org.springframework.data.*`.
- 12FactorApp Alignment - https://12factor.net/: dependencies explicitly declared (`spring-boot-starter-data-jdbc` is `compileOnly`, the consuming service owns the runtime); the library holds zero configuration, config stays strictly in the consuming service's environment; one artifact version fleet-wide keeps dev/prod parity.
- Idempotent restart semantics: `assignIdIfMissing()` is a no-op once an id exists (unit-tested), so re-converted aggregates keep their identity. Client-assigned UUID PKs pair with the fleet rule that CockroachDB `UPSERT` arbitrates on the primary key only, so idempotent writes use `INSERT ... ON CONFLICT (business key)`.

### Fleet persistence conventions carried by this repo (design register R-04, R-21, R-34)

- Liquibase pure-XML typed changelogs in calendar layout (`YYYY/MM`); single writer per table via grants.
- v4 UUID PKs; wire/status codes persisted as text under CHECK constraints, never enum identity.
- Batch metadata: every stage service shares one CockroachDB database (`dcre_collections`) and isolates its Spring Batch 6 tables with a per-service prefix (e.g. `CRR_BATCH_`), single-writer via grants (R-04). `spring.batch.jdbc.initialize-schema` stays `never`: the tables come from a Liquibase-owned copy of the Spring Batch DDL with `EXIT_MESSAGE` widened to `TEXT` (design register A-39b, resolved; see `crr`'s `002-batch-metadata.xml` for the reference implementation).

## Prerequisites

- JDK for the Gradle toolchain: Java 25 (`build.gradle` pins `JavaLanguageVersion.of(25)`).
- Gradle wrapper included (Gradle 9.5.1); no Docker and no database needed, tests are plain JUnit.

## Quickstart

A clean clone works with no `.env`: the library reads no configuration at all.

```bash
git clone https://github.com/sean-huni/dcre-platform-persistence.git
cd dcre-platform-persistence
./gradlew test publishToMavenLocal
```

Consume from a service (`spring-boot-starter-data-jdbc` is `compileOnly` here, so the consumer must provide Spring Data JDBC itself):

```groovy
repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation 'za.co.fnb.dcre:platform-persistence:0.1.0'
}
```

Wire it in: annotate the application class with `@Import(za.co.fnb.dcre.platform.persistence.JdbcConfig.class)` and extend `BaseEntity` from each `data/model` aggregate.

## Configuration

None. The library defines no properties and reads no environment variables; datasource, Liquibase and Batch-metadata configuration belong to the consuming service (12FactorApp Alignment - https://12factor.net/).

## Testing

```bash
./gradlew test
```

JUnit Jupiter (JUnit BOM 6.0.2), pure unit tests: no Docker, no database.

## Fleet build step (in place of cluster deployment)

This library is never deployed to the kind cluster itself; it is baked into every stage-service image at build time. In the dev-environment quickstart it is step 2 (build platform libs before any service image):

```bash
./gradlew publishToMavenLocal
```

This produces, under `~/.m2/repository/za/co/fnb/dcre/platform-persistence/0.1.0/`, the binary jar, sources jar (`withSourcesJar()`), POM and Gradle module metadata. Distribution is Maven Local only (no remote repository configured); every consuming project builds on the same machine.

Releasing a change: bump `version` in `build.gradle` (SemVer; released versions are immutable, any change means a new version), run the tests, `./gradlew publishToMavenLocal`, then bump the dependency version in consuming services. Fleet releases are digits-only 3-component SemVer git tags (no `v` prefix), uniform across the fleet; current release tag: 2.1.1.

## Related repositories

- https://github.com/sean-huni/dcre-agt (orchestrator, mints stage-service k8s Jobs)
- https://github.com/sean-huni/dcre-crr
- https://github.com/sean-huni/dcre-ctv
- https://github.com/sean-huni/dcre-cde
- https://github.com/sean-huni/dcre-cir
- https://github.com/sean-huni/dcre-crw
- https://github.com/sean-huni/dcre-ixr
- https://github.com/sean-huni/dcre-sxr
- https://github.com/sean-huni/dcre-pxr
- https://github.com/sean-huni/dcre-prg
- https://github.com/sean-huni/dcre-ais
- https://github.com/sean-huni/dcre-hcs
- https://github.com/sean-huni/dcre-platform-model
- https://github.com/sean-huni/dcre-platform-files
- https://github.com/sean-huni/dcre-platform-batch
- https://github.com/sean-huni/dcre-infra
- https://github.com/sean-huni/dcre-fixture-toolkit
- https://github.com/sean-huni/dcre-design-register
- https://github.com/sean-huni/dcre-rpt
