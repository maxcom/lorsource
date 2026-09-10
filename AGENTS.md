# AGENTS.md - Development Guide for Linux.org.ru

## Project Overview

Java 25 + Scala 3.9 web application (WAR) for Linux.org.ru. Mixed-language codebase: most code is Scala
(`src/main/scala`), some Java (`src/main/java`), JSP views (`src/main/webapp/WEB-INF/jsp`). All tests are Scala.

Stack: Maven (≥3.9.13, enforcer-checked; `./mvnw` wrapper available), Spring Framework 6.x + Spring Security 6.x,
ScalikeJDBC 4.x, PostgreSQL 16, OpenSearch 3.x (client `opensearch-java`), embedded ActiveMQ, Apache Pekko for async,
Log4j2.

**Important:** Spring beans are wired via XML in `src/main/webapp/WEB-INF/` (`applicationContext.xml`,
`springapp-servlet.xml`, `springapp-security.xml`) plus Scala config classes (`SpringSecurityConfiguration.scala`,
`PekkoConfiguration.scala`, ...). Do not assume annotation-only wiring.

## Build Commands

```bash
mvn package -DskipTests    # Build WAR (exploded, in target/lor-1.0-SNAPSHOT/) without tests
mvn compile               # Compile main sources only
mvn test-compile          # Compile main and test sources
```

### Running Tests

Test selection by class name suffix (all tests are MUnit `FunSuite` classes in `src/test/scala`):
- `*Test` → unit tests, run by Surefire (`mvn test`; parallel classes, 2 threads/core)
- `*IntegrationTest`, `*WebTest` → integration tests, run by Failsafe (`mvn integration-test` / `mvn verify`)

**Run all unit tests:**
```bash
mvn test
```

**Run a single test class / method:**
```bash
mvn test -Dtest=TopicControllerIntegrationTest
mvn test -Dtest=ru.org.linux.topic.TopicControllerIntegrationTest
mvn test -Dtest=StringUtilTest#processTitle
```

**Run integration tests / a single one:**
```bash
mvn integration-test
mvn integration-test -Dit.test=TopicControllerIntegrationTest
```

**Run everything:** `mvn verify`

`mvn verify` lifecycle (all automatic, per pom.xml):
1. Liquibase `update` (phase `pre-integration-test`)
2. Jetty starts the built webapp on port 8080 (`pre-integration-test`)
3. Failsafe runs `*IntegrationTest` (Spring context, DB) and `*WebTest` (HTTP against the live Jetty)
4. Jetty stops (`post-integration-test`)

### Database & Environment Prerequisites

- Integration tests and the dev server need PostgreSQL with **hostname `postgres` resolvable** — the JDBC URL
  `jdbc:postgresql://postgres:5432/lor` (user `linuxweb`/`linuxweb`) is hard-coded in `src/test/resources/database.xml`.
  Devcontainer and CI provide this host; on bare metal add a `postgres` alias (e.g. in `/etc/hosts` → 127.0.0.1).
- Database `lor` must exist with extensions `hstore` and `fuzzystrmatch`, loaded from `sql/demo.db`, with users
  `maxcom`, `linuxweb`, `jamwiki` (created by `.devcontainer/init-db.sh`; CI in `.github/workflows/it.yml` mirrors it).
- Liquibase migrations: add a changeset XML to `sql/updates/` and reference it from `sql/main.xml`; applied by
  `mvn liquibase:update` (auto-runs before integration tests). Production uses profile `-P production` with
  `sql/production-liquibase.config`.
- `src/main/webapp/WEB-INF/config.properties` is **gitignored** (required at runtime). The devcontainer init creates a
  minimal one; full template: `config.properties.dist` (DB URL, uploads path, Elasticsearch URL, etc.).
- CI (`.github/workflows/it.yml`): PostgreSQL 16 service container + Temurin JDK 25, runs `mvn verify`.

### Devcontainer (Docker Compose: Maven + PostgreSQL 16 + OpenSearch 3.6)

```bash
devcontainer up                # also runs init-db.sh: creates users, rebuilds DB 'lor' from sql/demo.db,
                               # runs liquibase:update, creates WEB-INF/config.properties
devcontainer exec bash         # shell inside the container
devcontainer exec mvn verify   # run tests
devcontainer up --workspace-folder . --remove-existing-container   # rebuild from scratch (DB is reset)
```

### Development Web Server

```bash
mvn -DskipTests package jetty:run-war > server.log 2>&1 &
```

Server: http://127.0.0.1:8080/ — **must be restarted after any code change.** Stop with `mvn jetty:stop` (stopPort 9999).

All users in the test database have password `passwd`:
* maxcom: administrator, full permissions
* svu: moderator
* edo: user (score >= 50)

### JavaScript & CSS Build (Maven-only — no Node.js/npm)

- CSS: `src/main/webapp/sass/*.scss` → `dart-sass-maven-plugin` compiles (COMPRESSED, no BOM), `yuicompressor-maven-plugin`
  minifies and aggregates per-theme `combined.css` (tango, waltz, black, white2, zomg_ponies, qrerror).
- JS: `closure-compiler-maven-plugin` merges `src/main/webapp/js/lor/*.js` into `lor.js` and minifies individual files
  (`add-form.js`, `realtime.js`, ...); WebJar JS libraries are unpacked and concatenated into `plugins.js` by
  `maven-antrun-plugin` at `compile` phase.
- The WAR excludes raw source CSS/JS (`warSourceExcludes`) — only processed copies ship.
- Committed pre-built artifacts: `js/highlight.min.js` (rebuild via `./build-hljs <path-to-highlight.js-checkout>`),
  `js/script.min.js`.

### Ops Scripts (production)

- `./install_www` — full production deploy: `mvn -P production clean package`, production Liquibase, rsync to Tomcat.
- `deploy/server.xml` — production Tomcat config.

## Code Style

- All source files carry the Apache License header (see existing files; `Copyright 1998-2026 Linux.org.ru`)
- Max line length 120 (Scalafmt, `.scalafmt.conf`); package structure `ru.org.linux.*`
- Scala compiles with **`-Werror -Wunused:all`** — unused imports/locals/params fail the build

### Scala (3.9)

- Follow `.scalafmt.conf` (scala3 dialect, new syntax: `rewrite.scala3.convertToNewSyntax`, optional braces removed)
- Strict logging: `com.typesafe.scalalogging.StrictLogging`
- `if then` / `if then else` without parens; indentation-based `match`; `end` markers where clarity benefits
- Prefer `given`/`using`, extension methods, enums over sealed trait ADTs
- Constants: `UpperCamelCase`

### Java

- `@Nullable`/`@Nonnull` from `javax.annotation` (provided by `com.google.code.findbugs:jsr305`; do **not** re-add
  `javax.annotation-api`)
- `Optional` instead of null returns; constructor injection over field injection
- Spring annotations (`@Repository`, `@Service`, `@Controller`); import order: java.*, javax.*, org.*, com.*, ru.*

### Testing

- MUnit (`munit.FunSuite`) only — JUnit is gone. Suffixes are load-bearing: `*Test`, `*IntegrationTest`, `*WebTest`
  (see test selection above); same package under `src/test/scala`
- Spring tests: mix in `SpringTestSupport` (`ru.org.linux.test`); for per-test rolled-back transactions mix in
  `TransactionalTestSupport` (analog of `@Transactional` + rollback). If a test overrides `beforeEach`/`afterEach`,
  it **must** call `super` — otherwise the test silently runs without a transaction
- Transactional test bodies must be synchronous (transaction is bound to the thread via ThreadLocal)
- Integration tests require the database; OpenSearch tests use Testcontainers (Docker required)

### Database Access

- ScalikeJDBC; connections/transactions managed by Spring
- `SpringDB.run` — SQL with autocommit; `SpringDB.localTx` — inside a transaction
- Repository pattern with `@Repository`

## Project Structure

```
src/
├── main/
│   ├── java/ru/org/linux/          # Java sources (smaller part)
│   ├── scala/ru/org/linux/         # Scala sources (most business logic)
│   └── webapp/                     # webapp root
│       ├── WEB-INF/                # JSP templates, Spring XML configs, web.xml, config.properties[.dist]
│       ├── js/                     # JS sources; js/lor/ → merged into lor.js
│       ├── sass/                   # SASS sources (compiled to per-theme CSS)
│       ├── help/                   # Help pages (markdown)
│       ├── black|tango|waltz|white2|zomg_ponies/   # theme assets
│       └── qrerror/                # standalone 502 page for CDN (self-contained)
├── test/
│   ├── scala/ru/org/linux/         # ALL tests (MUnit); no src/test/java
│   └── resources/                  # test Spring configs (common.xml, database.xml), log4j2-test.xml
sql/                                # liquibase (main.xml, updates/) + demo.db
.devcontainer/                      # Docker Compose dev environment + init-db.sh
```

## Git & Commit Rules

* **Wait for Approval:** Do not commit or push changes without explicit user confirmation.
* Update the copyright year in all modified files to 2026.

## To LLM

Update this file if the changes you have done are worth updating here. The intent of this file is to give you a rough
idea of the project, from where you can explore further, if needed.
