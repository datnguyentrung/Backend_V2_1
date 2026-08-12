# Leaderboard Operations

## Normal Flow

Leaderboard data is a Redis read model. PostgreSQL remains the source of truth.

1. A business service validates and commits an attendance, fitness record, or student change.
2. The service publishes an application event inside the database transaction.
3. A synchronous `AFTER_COMMIT` listener recalculates the affected student and scope from PostgreSQL.
4. The updater takes a PostgreSQL advisory lock for the scope and writes an absolute score to Redis atomically.

GET endpoints read only Redis. They never aggregate from PostgreSQL and never rebuild on a miss.

- An initialized empty scope returns `200` with no rankings.
- An unknown scope returns `200` with no rankings and emits `leaderboard.read{result=uninitialized}`.
- Redis connection failure or an incomplete initialized projection returns `503`.

## Recovery Command

Recovery is disabled by default and has no HTTP endpoint. Run the application artifact with the web server disabled.

Required properties:

- `--spring.main.web-application-type=none`
- `--leaderboard.rebuild.enabled=true`
- `--leaderboard.rebuild.type=quarter|fitness|all`
- `--leaderboard.rebuild.year=<year>`
- `--leaderboard.rebuild.quarter=<1..4>`
- `--leaderboard.rebuild.skill-level=BASIC|ADVANCED` for a single fitness level only

The artifact name follows the current Maven artifact and version: `target/AI_Receptionist_Web-1.0.0.jar`.

Rebuild quarter training-score leaderboard:

```powershell
java -jar target/AI_Receptionist_Web-1.0.0.jar `
  --spring.main.web-application-type=none `
  --leaderboard.rebuild.enabled=true `
  --leaderboard.rebuild.type=quarter `
  --leaderboard.rebuild.year=2026 `
  --leaderboard.rebuild.quarter=3
```

Rebuild all fitness leaderboards for a quarter:

```powershell
java -jar target/AI_Receptionist_Web-1.0.0.jar `
  --spring.main.web-application-type=none `
  --leaderboard.rebuild.enabled=true `
  --leaderboard.rebuild.type=fitness `
  --leaderboard.rebuild.year=2026 `
  --leaderboard.rebuild.quarter=3
```

Rebuild one fitness leaderboard for a single skill level:

```powershell
java -jar target/AI_Receptionist_Web-1.0.0.jar `
  --spring.main.web-application-type=none `
  --leaderboard.rebuild.enabled=true `
  --leaderboard.rebuild.type=fitness `
  --leaderboard.rebuild.year=2026 `
  --leaderboard.rebuild.quarter=3 `
  --leaderboard.rebuild.skill-level=BASIC
```

Rebuild both quarter training-score and all fitness leaderboards for a quarter:

```powershell
java -jar target/AI_Receptionist_Web-1.0.0.jar `
  --spring.main.web-application-type=none `
  --leaderboard.rebuild.enabled=true `
  --leaderboard.rebuild.type=all `
  --leaderboard.rebuild.year=2026 `
  --leaderboard.rebuild.quarter=3
```

Supported types are `quarter`, `fitness`, and `all`. For fitness, omitting
`--leaderboard.rebuild.skill-level` rebuilds every configured `SkillLevel`.

### When to Rebuild

Run recovery only as an operator action:

- after the first deployment of the Redis-only leaderboard read model;
- after Redis is flushed, migrated, restored incompletely, or loses leaderboard keys;
- when monitoring reports `leaderboard.read{result=uninitialized}` for a scope that should already contain data;
- when Redis projection integrity fails and GET returns `503`;
- before enabling reads for historical scopes that have never been backfilled.

Do not run recovery as a scheduler, startup task, GET fallback, or replacement for normal business events.
Do not enable `leaderboard.rebuild.enabled=true` in the normal web application process.

### Rebuild Business Flow

The recovery command uses PostgreSQL as the source of truth and Redis as a replaceable read model:

1. Validate the requested `type`, `year`, `quarter`, and optional `skill-level`.
2. Acquire the same scope advisory lock used by normal leaderboard updates.
3. Read active students from PostgreSQL in pages of 500.
4. For `quarter`, calculate the existing quarter training-score summary for each active student.
5. For `fitness`, load the matching `FitnessRecord` data for the requested quarter and skill level, then select the best record using the current `FitnessLeaderboardScorer`.
6. Write rank, detail, and member data to generation-specific temporary Redis keys.
7. Validate temporary key counts.
8. Atomically swap the completed generation into the active Redis keys.
9. Log completion and close the non-web application process.

The command pages active students in batches of 500, writes generation-specific temporary keys,
validates rank/detail/member counts, and atomically swaps the complete projection. Repeating the
command against unchanged database state produces the same result. Fitness rebuild resets
`rankBefore`; subsequent mutations repopulate it. Temporary generation keys expire after 24 hours
so an interrupted process does not leave permanent rebuild data.

### Verification After Rebuild

Check the command logs for:

- `LEADERBOARD_REBUILD_STARTED`
- `LEADERBOARD_REBUILD_COMPLETED`
- `LEADERBOARD_RECOVERY_COMMAND_COMPLETED`

Then verify the relevant GET endpoint:

```powershell
curl "http://localhost:8080/api/v1/leaderboards/quarter?year=2026&quarter=3"
```

```powershell
curl "http://localhost:8080/api/v1/leaderboards/quarter/fitness?year=2026&quarter=3&skillLevel=BASIC"
```

Compare at least the entry count, top-N students, representative student scores, year, quarter,
and fitness skill level against PostgreSQL calculations or a trusted report.

## Deployment

1. Stop any external caller of the removed `/api/v1/leaderboards/sync-batch` endpoint.
2. Deploy the application without enabling the recovery runner.
3. Run recovery for every quarter and fitness scope served by the application, including the current quarter.
4. Compare entry counts, top-N results, and representative student scores against database calculations.
5. Confirm the removed endpoint returns `404` or `405`, GET does not query PostgreSQL, and update/error metrics are healthy.
6. Bootstrap each new quarter explicitly. Do not schedule a database scan and do not rebuild at startup.

## Monitoring

Alert on these metric/result combinations and corresponding structured logs:

- `leaderboard.update{result=failure}`
- `leaderboard.update.retry.exhausted`
- `leaderboard.read{result=uninitialized}` outside a planned new scope
- `leaderboard.read{result=unavailable}` and `leaderboard.redis.operation{result=connection_failure}`
- `leaderboard.redis.integrity` for count, row, or numeric metadata corruption
- `leaderboard.rebuild{result=failure}`
- HTTP `503` from leaderboard GET endpoints

Also verify production Redis persistence and eviction policy. Active leaderboard keys have no TTL;
fitness rank history expires after 30 days. Redis persistence reduces recovery frequency but is not a correctness source.

## Failure and Rollback

Redis updates are retried three times after the database commit. If all retries fail, the business API
still remains successful because PostgreSQL has committed; monitoring must trigger operator recovery.
The application deliberately does not introduce an outbox. A process crash between commit and listener
execution is therefore a known recoverable window.

Rollback is code-only and does not revert database data. No destructive schema migration is required.
If the Redis projection is incompatible or incomplete after rollback, run the recovery command supported
by the deployed version.
