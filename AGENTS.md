# AGENTS.md - Development Guide for Linux.org.ru

## Project Overview

This is a Java 21 + Scala 3 web application (WAR) for Linux.org.ru. It uses Maven for build,
Spring Framework, and includes both unit and integration tests.

## Build Commands

### Building the Project
```bash
mvn package -DskipTests    # Build WAR without running tests
mvn compile               # Compile main sources only
mvn test-compile          # Compile main and test sources
```

### Running Tests

**Run all unit tests only (no integration tests):**
```bash
mvn test
```

**Run a single test class:**
```bash
mvn test -Dtest=StringUtilTest
mvn test -Dtest=ru.org.linux.util.StringUtilTest
```

**Run a single test method:**
```bash
mvn test -Dtest=StringUtilTest#processTitle
```

**Run integration tests:**
```bash
mvn integration-test 
```

**Run a single integration test:**
```bash
mvn integration-test -Dit.test=TopicControllerIntegrationTest
```

**Run all tests (unit + integration):**
```bash
mvn verify
```

### Run application in development web server

Run in background shell:

```bash
mvn -DskipTests package jetty:run > server.log 2>&1 &
```

**Important:** The development web server must be restarted after any changes to the code.

Stop server with:

```bash
mvn jetty:stop
```

Server starts at http://127.0.0.1:8080/

### Other Commands
```bash
mvn clean                 # Clean target directory
mvn dependency:tree      # Show dependency tree
```

## Code Style Guidelines

### General

- All source files must include the Apache License header (see existing files)
- Maximum line length: 120 characters (enforced by Scalafmt)
- Package structure: `ru.org.linux.*`

### Java Conventions

- Use Spring annotations: `@Repository`, `@Service`, `@Controller`, etc.
- Use `@Nullable` and `@Nonnull` annotations from `javax.annotation`
- Use Java 17+ features (records, pattern matching) where appropriate
- Use `Optional` instead of null returns
- Use constructor injection over field injection
- Import order: java.*, javax.*, org.*, com.*, ru.*

### Scala Conventions

- Follow `.scalafmt.conf` configuration (version 3.10.7, Scala 3 dialect)
- Use strict logging: `com.typesafe.scalalogging.StrictLogging`
- Use Akka/Pekko for async operations

### Naming Conventions

- Java classes: `CamelCase` (e.g., `UserDao`, `TopicController`)
- Scala classes/objects: `CamelCase` (e.g., `CommentCreateService`)
- Methods/fields: `camelCase`
- Java constants: `UPPER_SNAKE_CASE`
- Scala constants: `UpperCamelCase`
- Test classes: `*Test`, `*IntegrationTest`, `*WebTest.scala`

### Error Handling

- Use custom exceptions (e.g., `MessageNotFoundException`, `UserNotFoundException`)
- Return `Optional<T>` for potentially missing values
- Use Spring's `@ExceptionHandler` for controller-level error handling
- Log errors with appropriate levels (error for exceptions, info/debug for flow)

### Testing

- Use JUnit 4/5 for Java unit tests
- Use MUnit for Scala tests
- Follow AAA pattern (Arrange/Act/Assert or Given/When/Then)
- Place test classes in same package under `src/test/java` or `src/test/scala`
- Integration tests require database (use testcontainers if needed)

### Database Access

- Use Spring's `JdbcTemplate` and `NamedParameterJdbcTemplate`
- Use `@Transactional` for database operations on Java and `.transactional()` on Scala 
- Repository pattern with `@Repository` annotation

### Dependencies

- Spring Framework 6.x
- Spring Security 6.x
- PostgreSQL JDBC driver
- OpenSearch 3 via opensearch-java.
- Pekko for async

## Project Structure

```
src/
├── main/
│   ├── java/ru/org/linux/          # Java sources
│   └── scala/ru/org/linux/         # Scala sources
├── test/
│   ├── java/ru/org/linux/          # Java test sources
│   ├── scala/ru/org/linux/         # Scala test sources
│   └── resources/                   # Test resources (spring configs, etc.)
```

## IDE Recommendations

- IntelliJ IDEA (has good Scala/Java support)
- For VS Code: Use Java extension with null analysis mode set to "automatic"

## Code Search Tools

### Finding Symbol Usage (ast-grep recommended)

For cross-language searches (Java↔Scala), use ast-grep instead of LSP:
```bash
# Find usage of a method
ast-grep find code --pattern "StringUtil.checkLoginName" --lang scala
# Find usage with YAML rule (more precise)
ast-grep scan --inline-rules '
id: find-checkLoginName
language: scala
rule:
  pattern: StringUtil.checkLoginName
'
When to use which:
- ast-grep: Find all usages of a symbol across Java/Scala boundary (recommended)
- LSP findReferences: Find references within same file/type system (may miss cross-language)
- grep: Quick text search fallback
