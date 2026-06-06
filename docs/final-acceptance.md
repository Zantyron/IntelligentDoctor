# Day 5 Final Acceptance

## Scope

This record captures the Day 5 delivery state for IntelligentDoctor: runnable build,
automated checks, smoke demo, release documents, and external dependency readiness.

## Verified

| Area | Evidence |
| --- | --- |
| Build and tests | `scripts/verify-release.ps1` completed successfully with Maven test/build. Result: 13 tests run, 0 failures, 0 errors, 1 skipped live verification. |
| Patient diagnosis flow | `scripts/smoke-test.ps1` verified diagnosis SSE output in test profile. |
| Patient registration flow | `scripts/smoke-test.ps1` verified registration SSE draft creation and order confirmation. Example order: `IDR202606062145451119DD255`. |
| Offline demo startup | `scripts/run-demo.ps1` and the packaged jar can run with `spring.profiles.active=test` on a clean local port. |
| Release artifacts | README, API docs, architecture docs, deployment docs, demo script, env template, sample data, startup scripts, smoke test, and release checklist are present. |
| Secret hygiene | `.env` is ignored and `.env.example` contains placeholders only. |

## External Dependency Status

| Dependency | Status | Notes |
| --- | --- | --- |
| MySQL | Template ready | `docker-compose.yml`, `docker/mysql/init/01-schema.sql`, and `mysql-schema-v1.sql` are included. |
| MongoDB | Template ready | `docker-compose.yml` and `docker/mongodb/init/01-indexes.js` are included. |
| Redis | Template ready | `scripts/redis-hot-slot-loadtest.ps1` validates Redis Lua pre-deduction when Redis is reachable. |
| Kafka | Template ready | `docker-compose.yml` and provider toggles are included. |
| OpenAI | Template ready | Configure `OPENAI_API_KEY` and `APP_AI_PROVIDER=openai`. |
| Pinecone | Template ready | Configure `PINECONE_API_KEY`, `PINECONE_INDEX`, and `APP_VECTOR_PROVIDER=pinecone`. |

## Known Environment Gap

The Redis hotspot load test requires either `redis-cli` on `PATH` or a running Docker
daemon with the `intelligent-doctor-redis` container available. The current workstation
does not expose either of those Redis execution paths, so the script is committed and
documented but the actual Redis pressure result must be captured after Redis/Docker is
available.

Run it with:

```powershell
.\scripts\start-deps.ps1
.\scripts\redis-hot-slot-loadtest.ps1 -Stock 100 -Concurrency 500
```

The expected acceptance signal is JSON output with `noOversell: true` and
`successCount` less than or equal to the configured stock.
