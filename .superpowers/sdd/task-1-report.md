# Task 1 report: backend and infrastructure foundation

## Scope delivered

- Created the root project guidance and editor/ignore configuration.
- Created the `alpha-server` Spring Boot 3.5.3 Maven module with Java 21.
- Added Spring Boot Web, Validation, Test, MyBatis-Plus, Flyway, MySQL, Redis, Sa-Token, BCrypt, Lombok, and OpenAPI dependencies.
- Added the `AlphaVueApplication` bootstrap class and a `@SpringBootTest` context-load test.
- Added default development, development, and production Spring profiles. Service endpoints and credentials are environment-configured; development supplies local-safe defaults while production requires database and Redis connection variables.
- Added Docker Compose definitions for MySQL 8.4, Redis 7, and MinIO, each with a named volume and health check. `deploy/.env.example` documents local variables.

## TDD and verification evidence

1. Before scaffolding, ran:

   ```bash
   /Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -f alpha-server/pom.xml test -DskipTests=false
   ```

   It failed as intended because `alpha-server/pom.xml` did not exist.

2. Confirmed Maven 3.9.11 with Temurin Java 21.0.11 is available at the requested path.

3. After implementation, ran:

   ```bash
   /Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn test
   ```

   Result: `BUILD SUCCESS`; 1 test run, 0 failures, 0 errors, 0 skipped.

4. Ran `git diff --check`; result: exit code 0 (no whitespace errors).

5. Attempted the required Compose validation:

   ```bash
   docker compose -f deploy/docker-compose.yml config
   ```

   It could not run because this environment has no `docker` executable (`zsh: command not found: docker`). Docker was not installed because it is a host-level container runtime rather than a project dependency.

## Concern / follow-up

Run the Compose validation command on a machine with Docker Compose v2 installed before starting local services. The Maven test deliberately excludes JDBC, Flyway, and MyBatis-Plus auto-configuration so the foundation context test does not require a locally running MySQL instance; integration coverage against the Compose services belongs to a later task.
