# Building and testing

Use Java 21 and the checked-in Maven wrapper, which pins Maven 3.9.16 and validates
its distribution SHA-256. On Windows, use `mvnw.cmd` in place of `./mvnw`.

```sh
./mvnw --batch-mode --no-transfer-progress clean verify
./mvnw --batch-mode --no-transfer-progress -Pdependency-upgrades clean verify
pre-commit run --all-files
```

`verify` runs JUnit Jupiter, jqwik properties, MockBukkit tests, and packaging
checks. It also generates `target/site/jacoco/index.html` and
requires at least 75% line coverage and 60% branch coverage. These are regression
floors, not a claim of exhaustive coverage. Surefire and Failsafe XML reports are
under `target/surefire-reports` and `target/failsafe-reports`.

H2 is pinned to 2.2.224 in both dependency sets and ignored by Dependabot.
The `dependency-upgrades` profile remains available for compatibility comparisons.
Keep it synchronized with future dependency decisions.

## What the suite exercises

- Database pooling, prepared parameters (including generated strings), failure
  handling, persistence, shutdown, and reconnects against real embedded H2 and SQLite.
- GUI pagination and boundaries, opening inventories, marking cloned items,
  click routing, drag protection, leaked-item cleanup, drops, and pickups.
- Item metadata, lore, tags, glow, player inventory overflow, messages, lifecycle,
  chat input/cancellation/replacement/timeout, configuration binding/default saves,
  concurrent UTF-8 logging, sounds, and update notifications.
- Webhook JSON and update HTTP behavior against loopback HTTP servers only.
- The final shaded JAR contains relocated runtime dependencies, valid JDBC service
  registration, and no test framework or provided database drivers.

MockBukkit 3.133.2 is pinned to the existing Paper 1.21 API and Java 21 target.
Its unimplemented Component inventory factory is bridged in the test fixture;
other inventory/player behavior uses MockBukkit. Scheduler callbacks are captured
with Mockito and invoked explicitly for deterministic Folia scheduling contracts.
MockBukkit aborts are converted into failures in the shared Bukkit fixture, so
unsupported methods cannot silently skip those tests. These tests do not launch
a Minecraft server or establish real Folia thread/region correctness.

## Real MariaDB and MySQL integration tests

Start disposable MariaDB and MySQL databases with an `aliien_test` schema and a
user that can create tables there. The integration test deletes its own
`aliien_entries` table's rows, so never point it at a production database.

```sh
export DB_HOST=127.0.0.1
export DB_USER=aliien
export DB_PASSWORD=integration-password
export MARIADB_PORT=3307
export MYSQL_PORT=3308
./mvnw --batch-mode --no-transfer-progress -Pdatabase-integration clean verify
./mvnw --batch-mode --no-transfer-progress -Pdatabase-integration,dependency-upgrades clean verify
```

Missing settings and unavailable services fail the integration profile; they do
not skip it. It exercises both public connection helpers, bound values, commits,
rollbacks, pooled connections, and shutdown. The services run without TLS, also
checking compatibility with the existing helpers' explicit `useSSL=false`.
It does not cover TLS certificate validation, failover, or cloud authentication.

GitHub Actions provisions MariaDB 11.4 and MySQL 8.4 and runs both dependency sets.
It uploads test and coverage reports even on failure. Pre-commit runs formatting and hygiene checks only; Maven tests run in the
GitHub Actions verification jobs on pull requests and pushes to `main`, or locally
when you invoke the wrapper explicitly. To install the lightweight hook locally,
run `pre-commit install`.
