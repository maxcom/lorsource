# AGENTS.md - Development Guide for Linux.org.ru

## Project Overview

Java 25 + Scala 3.10 web application (WAR) for Linux.org.ru. Mixed-language codebase: most code is Scala
(`src/main/scala`), some Java (`src/main/java`), JSP views (`src/main/webapp/WEB-INF/jsp`). All tests are Scala.

Stack: Maven (≥3.9.13, enforcer-checked; `./mvnw` wrapper available), Spring Framework 7.x + Spring Security 7.x,
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

Scala compiles **before** Java (`scala-compile-first` execution at `process-resources`) with joint compilation —
cross-language references work in both directions.

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
`-Dtest` overrides Surefire's includes/excludes, so this also runs a single `*IntegrationTest` class directly,
skipping the Jetty/Liquibase lifecycle — but PostgreSQL must still be up.

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

Test stdout/stderr is redirected to files (`redirectTestOutputToFile`), not the console — look in
`target/surefire-reports/<Class>-output.txt` (unit) and `target/failsafe-reports/<Class>-output.txt` (integration)
when debugging.

**JS tests:** `src/test/js/*.test.js` use the built-in `node:test` runner (no npm dependencies) and cover the markup
editor logic in `src/main/webapp/js/markup-editor.js`, its loading order and the pages' script wiring.
`exec-maven-plugin` runs them in `process-test-classes`, right
before Surefire, so `mvn test` includes them; they honour `-DskipTests` but not `-Dmaven.test.skip=true`. Output goes
to the console and a JUnit report is written to `target/js-tests.junit.xml`. Node itself is downloaded on the first
build by `frontend-maven-plugin` into `node/` (gitignored) — neither CI nor the devcontainer needs Node installed.
The first build needs network access to nodejs.org; later builds reuse `node/`, which survives `mvn clean`.
On Linux the Node binary (25 and newer) is linked against `libatomic.so.1`, which the `maven` image lacks: the CI
workflow and the devcontainer Dockerfile install `libatomic1` for it, and so must any other Linux build host.

To run the JS tests without Maven: `node --test src/test/js/`.

### Database & Environment Prerequisites

- Integration tests and the dev server need PostgreSQL with **hostname `postgres` resolvable** — the JDBC URL
  `jdbc:postgresql://postgres:5432/lor` (user `linuxweb`/`linuxweb`) is hard-coded in `src/test/resources/database.xml`.
  Devcontainer and CI provide this host; on bare metal add a `postgres` alias (e.g. in `/etc/hosts` → 127.0.0.1).
- Database `lor` must exist with extensions `hstore` and `fuzzystrmatch`, loaded from `sql/demo.db`, with users
  `maxcom`, `linuxweb`, `jamwiki` (created by `.devcontainer/init-db.sh`; CI in `.github/workflows/it.yml` mirrors it).
- Liquibase migrations: add a changeset XML to `sql/updates/` and reference it from `sql/main.xml`; applied by
  `mvn liquibase:update` (auto-runs before integration tests). Production uses profile `-P production` with
  `sql/production-liquibase.config`.
- Runtime config comes from the committed `WEB-INF/config.properties.dist` (always loaded). A local
  `src/main/webapp/WEB-INF/config.properties` (**gitignored**) is optional and only overrides values of `.dist`
  (`applicationContext.xml` loads both and ignores missing files). The devcontainer init creates a minimal one holding
  just the OpenSearch URL of the compose network.
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

Server: http://127.0.0.1:8080/ — **must be restarted after a change in Scala or Java code.** Stop with `mvn jetty:stop`
(stopPort 9999). `jetty:run-war` takes the lifecycle through `package` first (unit tests included), so it rebuilds the
webapp before starting.

JS and CSS URLs carry a version stamp: `?<lor:assetVersion/>` (`WEB-INF/tags/assetVersion.tag`) prints the modification
time of the newest of the files listed in `AssetVersion.Files`; `urlrewrite.xml` serves assets with a query string using
`Cache-Control: max-age=315360000` (10 years). Nothing is substituted into JSPs at build time, so no `clean` is needed.
After an edit, with the server running:

- JS or SCSS: `mvn -DskipTests compile`, then reload the page. The server serves the static files straight from
  `target/lor-1.0-SNAPSHOT` and the page links to them under the new stamp — no `package`, no restart.
- JSP: `mvn -DskipTests package`, then reload the page. Jasper notices the changed file within a few seconds
  (`modificationTestInterval`); no restart.
- Scala or Java: `mvn -DskipTests package`, then restart.

A new file whose URL carries the stamp must be added to `AssetVersion.Files` — the stamp follows only the listed files.

All users in the test database have password `passwd`:
* maxcom: administrator, full permissions
* svu: moderator
* edo: user (score >= 50)

### JavaScript & CSS Build (Maven-only — no npm)

- CSS: `src/main/webapp/sass/*.scss` → `dart-sass-maven-plugin` compiles (COMPRESSED, no BOM), `yuicompressor-maven-plugin`
  minifies and aggregates per-theme `combined.css` (tango, waltz, black, white2, zomg_ponies, qrerror).
- JS: `closure-compiler-maven-plugin` merges `src/main/webapp/js/lor/*.js` into `lor.js` and minifies individual files
  (`markup-editor.js`, `add-form.js`, `realtime.js`, ...); WebJar JS libraries are unpacked and concatenated into
  `plugins.js` by `maven-antrun-plugin` at `compile` phase. A new file is shipped only if it is listed in the plugin's
  includes.
- The WAR excludes raw source CSS/JS (`warSourceExcludes`) — only processed copies ship. The pre-built
  `highlight.min.js` and `qrerror/good-penguin.png` are copied by `maven-antrun-plugin` at `process-resources`, so they
  are in place in a build without tests (`-DskipTests`) as well.
- The directories the dev server writes to (`target/uploads/{gallery,images,gallery/preview,photos}` for `upload.path`)
  are created by `maven-antrun-plugin` at `process-resources`; the application never creates them itself.
- Markup editors (`.markup-tabs` in the topic, comment, profile and group forms) are split in two:
  - `markup-editor.js` (global `MarkupEditor`) is the logic: commands for Markdown and LORCODE, finding markup that is
    already there, editing the textarea. It must not touch the DOM at load time — that is what keeps it loadable in Node
    for the JS tests.
  - `add-form.js` builds the UI on top of it (preview tabs `initPreviewTabs`, toolbar `initMarkupToolbar`); styles live
    in `sass/_form.scss`.
  - Enter in a list item continues the list, and a second Enter on the empty item leaves it — a LORCODE list gets its
    closing `[/list]`. Inside code blocks Enter is left to the browser. The logic is `editor.newline()` in
    `markup-editor.js`, the key handlers live in `initMarkupToolbar`. Shift+Enter stays a plain line break, and a
    `beforeinput` handler covers mobile keyboards that do not report Enter in `keydown`.
  Pages load both files with a single call — `$script(['/js/markup-editor.js?<lor:assetVersion/>',
  '/js/add-form.js?<lor:assetVersion/>'], 'markup-editor')` — which fetches them in parallel; they execute in
  either order. `$script.ready('markup-editor')` fires once both have executed, and only code running inside it may
  touch `MarkupEditor`: `initPreviewTabs` waits for it itself, so it is safe to call from anywhere (including a
  DOM-ready callback, which does *not* wait for the second file). A page that needs code after loading uses
  `$script.ready('markup-editor', function () {...})`. `markup-editor-loading.test.js` checks that every page
  registers the name — without it `initPreviewTabs` would never run.
- The reply field of the inline comment form (`#commentForm` inside `.comment-form-inline` on the topic page) is sized by
  CSS alone (`sass/_form.scss`): 5 lines by default (`min-height`), it grows with the text (`field-sizing: content`) up
  to the window (`max-height`) and then scrolls; in browsers without `field-sizing` it stays 5 lines. The corner handle
  is kept on purpose: a drag sets an inline width/height, and the field stops following the text until the page is
  reloaded. YUI Compressor strips the spaces around `+` inside `calc()` (`calc(5lh + 1em)` becomes an invalid
  `calc(5lh+1em)`), so 5 lines is a plain `em` value over an explicit `line-height` — don't put `a + b` into `calc()`.
- Node.js is downloaded by the build solely to run the JS tests (see Running Tests); there is no `package.json`.
- The editor logic is covered by the JS tests; layout and DOM behaviour have no automated tests — verify those in a
  browser against the dev server.
- Committed pre-built artifacts: `js/highlight.min.js` (rebuild via `./build-hljs <path-to-highlight.js-checkout>`),
  `js/script.min.js`.

### Ops Scripts (production)

- `./install_www` — full production deploy, run on the prod host (uses `sudo`): `./mvnw -T 1C -P production clean
  package`, installs `deploy/tomcat-opt.service` + `deploy/nginx.conf`, production Liquibase, rsync to
  `/var/lib/tomcat/webapps/ROOT`.
- `deploy/` — production configs: Tomcat `server.xml`, systemd unit, nginx.

## Code Style

- All source files carry the Apache License header (see existing files; `Copyright 1998-2026 Linux.org.ru`)
- Max line length 120 (Scalafmt, `.scalafmt.conf`); package structure `ru.org.linux.*`
- Scala compiles with **`-Werror -Wunused:all`** — unused imports/locals/params fail the build

### Scala (3.10)

- Follow `.scalafmt.conf` (scala3 dialect, new syntax: `rewrite.scala3.convertToNewSyntax`, optional braces removed)
- Strict logging: `com.typesafe.scalalogging.StrictLogging`
- `if then` / `if then else` without parens; indentation-based `match`; `end` markers where clarity benefits
- Prefer `given`/`using`, extension methods, enums over sealed trait ADTs
- Constants: `UpperCamelCase`

### Java

- Nullness annotations: `@Nullable`/`@NonNull` from JSpecify (`org.jspecify:jspecify`, `org.jspecify.annotations`);
  JSR-305 `javax.annotation` and `org.springframework.lang` are not supported by Spring 7 — do **not** re-add
  `jsr305`/`javax.annotation-api`
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
- JS (`src/test/js`): `node:test` + `node:assert/strict`, no dependencies. Editor tests go through the public API
  (`MarkupEditor`) with a fake textarea (`fake-textarea.js`); there is no DOM in Node, so the tests of `add-form.js`
  run it inside a function with stubbed `window`/`$script`/`$` and read the JSPs as text. Helpers must not be named
  `*.test.js`, the Maven run picks up only that pattern

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
│   ├── scala/ru/org/linux/         # Scala tests (MUnit); no src/test/java
│   ├── js/                         # JS tests (node:test): markup-editor.js and its loading
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
