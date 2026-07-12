# dcre-platform-persistence

Persistence conventions for DCRE services (R-04/R-21/R-34):
- Liquibase XML changelogs, calendar layout (YYYY/MM), single writer per table via grants.
- v4 UUID PKs; wire codes persisted as text under CHECK constraints, never enum identity.
- Batch metadata: per-service DATABASE (e.g. crr_meta) on the shared cluster,
  `spring.batch.jdbc.initialize-schema=always` for M2 (SYNTHETIC-CONTRACT seam);
  the Liquibase-owned copy with widened EXIT_MESSAGE lands with A-39b once the
  Batch 6 DDL surface is verified.
- CRDB gotchas: no make_interval named args (use INTERVAL literals); 40001
  serialization retries expected under contention.

## Publishing (how this module is made available for reuse)

This module is published as a Maven artifact via the Gradle `maven-publish` plugin
(see `build.gradle`) so other DCRE services can import it as a normal dependency.

Coordinates:

```
za.co.fnb.dcre:dcre-platform-persistence:0.1.0
```

### How it was published

1. `build.gradle` applies `java-library` + `maven-publish`, sets
   `group = 'za.co.fnb.dcre'` and `version = '0.1.0'`, and declares a single
   `MavenPublication` from `components.java`. `withSourcesJar()` publishes a
   sources jar alongside the binary jar.
2. Publish to the local Maven repository (`~/.m2/repository`):

   ```bash
   ./gradlew publishToMavenLocal
   ```

3. This produces, under `~/.m2/repository/za/co/fnb/dcre/dcre-platform-persistence/0.1.0/`:
   - `dcre-platform-persistence-0.1.0.jar` (classes)
   - `dcre-platform-persistence-0.1.0-sources.jar`
   - `dcre-platform-persistence-0.1.0.pom` (Maven metadata)
   - `dcre-platform-persistence-0.1.0.module` (Gradle module metadata)

There is currently no remote repository configured; distribution is Maven Local only.
Every consuming project is built on the same machine, so `publishToMavenLocal` is the
whole release step. When a shared artifact repository (e.g. Nexus/Artifactory) becomes
available, add it under `publishing.repositories` and publish with `./gradlew publish`.

### How to consume it from another project

1. Make sure the version you need exists locally (clone this repo at the matching
   commit and run `./gradlew publishToMavenLocal` if it does not).
2. In the consuming project's `build.gradle`, include `mavenLocal()` in the
   repositories and add the dependency:

   ```groovy
   repositories {
       mavenCentral()
       mavenLocal()
   }

   dependencies {
       implementation 'za.co.fnb.dcre:dcre-platform-persistence:0.1.0'
   }
   ```

3. Note: `spring-boot-starter-data-jdbc` is declared `compileOnly` here, so the
   consuming service must provide Spring Data JDBC itself (any Spring Boot 4 service
   with the `spring-boot-starter-data-jdbc` starter already does).

### Releasing a new version

1. Bump `version` in `build.gradle` (SemVer; released versions are immutable, so any
   change after a release means a new version, never a re-publish of the same one).
2. Run the tests: `./gradlew test`.
3. Publish: `./gradlew publishToMavenLocal`.
4. Commit with the JIRA ticket in the title, then bump the dependency version in the
   consuming projects.

The sibling platform modules (`dcre-platform-model`, `dcre-platform-batch`,
`dcre-platform-files`) follow the same publish/consume flow under the same
`za.co.fnb.dcre` group.
