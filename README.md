# dcre-platform-persistence

> Part of the DCRE fleet. For the fleet map, the rulings and the diagrams that specify every stage, start at the [DCRE design register](https://github.com/sean-huni/dcre-design-register); the complete list of live repositories is its [Repositories](https://github.com/sean-huni/dcre-design-register/blob/dev/README.md#repositories) table.

Shared persistence platform library for the DCRE fleet: `BaseEntity` plus the Spring Data JDBC
`JdbcConfig` every Spring Boot stage service imports for CockroachDB persistence.

## What it does

A library, not a service: it has no runtime of its own and is baked into every stage-service image at
build time. Public API, package `za.co.fnb.dcre.platform.persistence`:

- `BaseEntity` (abstract): a client-assigned `UUID` primary key (`@Id`, `UUID.randomUUID()`, so v4),
  an optimistic-locking `@Version Long`, and `created_at` / `updated_at` audit timestamps
  (`Instant`, `@CreatedDate` / `@LastModifiedDate`). `assignIdIfMissing()` is a no-op once an id
  exists.
- `JdbcConfig` (`@Configuration`): `@EnableJdbcAuditing` plus a `BeforeConvertCallback<BaseEntity>`
  that assigns the UUID before the first INSERT, because Spring Data JDBC includes the id in INSERTs
  and bypasses database defaults.

## Consumers

Counted from each fleet repo's `build.gradle` on `origin/dev` (local clones, not fetched), plus
AGT (checked 2026-09-28): every Spring Boot service declares `za.co.fnb.dcre:platform-persistence:0.1.0`.

| Consumer | Version |
|---|---|
| Collections: `cde` `cir` `cix` `cpx` `crg` `crr` `crw` `csx` `ctv` | `0.1.0` |
| Payments: `pai` `pir` `pix` `ppx` `prg` `prr` `prw` `psx` `ptv` | `0.1.0` |
| Mandates: `mas` `mir` `mit` `mix` `mpx` `mrg` `mrr` `mrv` `mrw` `msx` | `0.1.0` |
| Shared: `hcs`, `rpt` | `0.1.0` |
| AGT (Quarkus), the other platform libraries | not consumers |

## Architecture and principles

- SOLID, single responsibility per class: `BaseEntity` owns aggregate identity and audit state;
  `JdbcConfig` owns wiring (auditing plus the id-assignment callback). Two small units with a clear
  interface: extend one, `@Import` the other.
- Layer-first packages: in consuming services entities live in `data/model` and extend
  `BaseEntity`; repositories live in `data/repo`. This library sits beneath that layer and imports
  only Spring Data and Spring context annotations.
- 12FactorApp Alignment - https://12factor.net/: dependencies explicitly declared
  (`spring-boot-starter-data-jdbc` is `compileOnly`, the consuming service owns the runtime); the
  library holds zero configuration; one artifact version fleet-wide keeps dev/prod parity.
- Idempotent restart semantics: `assignIdIfMissing()` is a no-op once an id exists (unit-tested), so
  re-converted aggregates keep their identity. Client-assigned UUID PKs pair with the fleet rule that
  CockroachDB `UPSERT` arbitrates on the primary key only, so idempotent writes use
  `INSERT ... ON CONFLICT (business key)`.

### Fleet persistence conventions this library assumes (design register R-04, R-21, R-34)

- Liquibase changelogs in calendar layout (`YYYY/MM`), per-service history tables; single writer per
  table via grants.
- v4 UUID PKs; wire and status codes persisted as text under CHECK constraints, never enum identity.
- Batch metadata: each service owns its Spring Batch tables under a per-service prefix (e.g.
  `CRR_BATCH_`) inside the database it writes to, created by a Liquibase-owned copy of the Spring Batch
  DDL with `EXIT_MESSAGE` widened to `TEXT` (design register A-39b). The persistent `JobRepository`
  over those tables is wired by `platform-batch`'s `BatchJdbcConfig`, which reads the prefix from
  `dcre.batch.table-prefix`; Boot 4.1 no longer binds `spring.batch.jdbc.*`.

## Prerequisites

- Java 25 (`.sdkmanrc`: `java=25-tem`; `build.gradle` sets `sourceCompatibility` /
  `targetCompatibility` 25)
- Gradle wrapper 9.5.1 (committed); no Docker and no database needed, tests are plain JUnit

## Build and publish

Coordinates: `za.co.fnb.dcre:platform-persistence:0.1.0`. Distribution is Maven Local only; no remote
repository is configured, so every consuming project builds on the same machine. A clean clone works
with no `.env`: the library reads no configuration at all.

```bash
git clone https://github.com/sean-huni/dcre-platform-persistence.git
cd dcre-platform-persistence
./gradlew test publishToMavenLocal
```

This writes `~/.m2/repository/za/co/fnb/dcre/platform-persistence/0.1.0/`: the binary jar, the
sources jar (`withSourcesJar()`), the POM and Gradle module metadata.

Consume from a service (`spring-boot-starter-data-jdbc` is `compileOnly` here, so the consumer must
provide Spring Data JDBC itself):

```groovy
repositories { mavenCentral(); mavenLocal() }
dependencies {
    implementation 'za.co.fnb.dcre:platform-persistence:0.1.0'
}
```

Wire it in: annotate the application class with
`@Import(za.co.fnb.dcre.platform.persistence.JdbcConfig.class)` and extend `BaseEntity` from each
`data/model` aggregate.

Releasing a change: bump `version` in `build.gradle` (SemVer; released versions are immutable), run
the tests, `./gradlew publishToMavenLocal`, then bump the dependency in every consuming service.
Fleet release tags (digits-only three-component SemVer, no `v` prefix) mark this repo uniformly with
the rest of the fleet and are independent of the artifact version.

## Configuration

None. The library defines no properties and reads no environment variables; datasource, Liquibase and
Batch-metadata configuration belong to the consuming service.

## Testing

```bash
./gradlew test
```

JUnit Jupiter (JUnit BOM 6.0.2), pure unit tests: no Docker, no database. 1 test class, 1 `@Test`
method (`BaseEntityTest`, counted from `src/test` at HEAD).

## Related repositories

The complete, current list of live DCRE repositories (stage services, orchestrator, platform libraries, infra and tooling) lives in one place: the [DCRE design register README](https://github.com/sean-huni/dcre-design-register/blob/dev/README.md#repositories). Deprecated and archived repositories are deliberately absent from it. This README does not copy that list, so it cannot drift.

- Design register: https://github.com/sean-huni/dcre-design-register (start at `docs/specs/DESIGN-REGISTER.md`; the diagrams in `docs/diagrams/` are the specification)
