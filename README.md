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
