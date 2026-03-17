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
mvn integration-test -Pintegration-testing
```

**Run a single integration test:**
```bash
mvn integration-test -Pintegration-testing -Dit.test=TopicControllerIntegrationTest
```

**Run all tests (unit + integration):**
```bash
mvn verify
```

### Other Commands
```bash
mvn clean                 # Clean target directory
mvn dependency:tree      # Show dependency tree
mvn dependency:analyze   # Analyze dependencies
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
- Test classes: `*Test.java`, `*Spec.scala`, `*IntegrationTest.java`, `*WebTest.scala`

### Error Handling

- Use custom exceptions (e.g., `MessageNotFoundException`, `UserNotFoundException`)
- Return `Optional<T>` for potentially missing values
- Use Spring's `@ExceptionHandler` for controller-level error handling
- Log errors with appropriate levels (error for exceptions, info/debug for flow)

### Testing

- Use JUnit 4/5 for Java unit tests
- Use Specs2 for Scala tests
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
- OpenSearch 2 via opensearch-java.
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

## Docker Development

See `docker/README.md` for running the application in Docker containers:
- Development: `docker-compose -f docker-compose.dev.yml up web`
- Production: `docker-compose up web`
