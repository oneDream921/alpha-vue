# Task 1 report: backend and infrastructure foundation

## Scope delivered

- Created the root project guidance and editor/ignore configuration.
- Created the `alpha-server` Spring Boot 4.0.0 Maven module with Java 21.
- Added Spring Boot Web, Validation, Test, MyBatis-Plus, Flyway, MySQL, Redis, Sa-Token, BCrypt, Lombok, and OpenAPI dependencies.
- Added the `AlphaVueApplication` bootstrap class and a `@SpringBootTest` context-load test that uses H2 and verifies a Flyway migration.
- Added the default `dev`, production, and test Spring profiles. Database and Redis connection values are required from environment variables outside the isolated test profile.
- Added Docker Compose definitions for MySQL 8.4, Redis 7, and the pinned `minio/minio:RELEASE.2025-04-22T22-12-26Z` image, each with a named volume and health check. `deploy/.env.example` documents all required local variables with non-secret placeholders.

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

## Review remediation and verification evidence

1. Updated the backend to Spring Boot 4.0.0 and resolved the Boot 4-specific artifacts from Maven Central:

   - `com.baomidou:mybatis-plus-spring-boot4-starter:3.5.13`
   - `cn.dev33:sa-token-spring-boot4-starter:1.45.0`
   - `org.springframework.boot:spring-boot-starter-flyway:4.0.0`
   - `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3`

   `mvn dependency:tree` completed successfully and showed the three Boot 4 compatibility artifacts above.

2. Removed MySQL, Redis, and MinIO credential/connection fallbacks from Spring profiles and Compose. `deploy/.env.example` lists each required local variable with replace-before-use placeholder values; no real secret was added.

3. Replaced the persistence auto-configuration exclusions with a `test` profile backed by H2. The regression test exercises a real `JdbcTemplate` datasource and verifies that Flyway applies `V1__create_migration_probe.sql`. Redis repositories are disabled for this profile, so no Redis server is required.

4. TDD red phase: before adding H2, `mvn test -DskipTests=false` failed with `Cannot load driver class: org.h2.Driver`. After changing to Boot 4, the test initially exposed that Flyway migration did not run; root-cause analysis showed Spring Boot 4 moves Flyway auto-configuration into `spring-boot-starter-flyway`. Adding that starter produced Flyway's logged V1 migration.

5. Focused regression verification:

   ```bash
   /Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn -Dtest=AlphaVueApplicationTests test -DskipTests=false
   ```

   Result: `BUILD SUCCESS`; 1 test run, 0 failures, 0 errors, 0 skipped. Logs show Hikari connecting to `jdbc:h2:mem:alpha_vue` and Flyway successfully applying V1.

6. Full verification:

   ```bash
   /Users/mac/Documents/my-develop-tool/maven/apache-maven-3.9.11/bin/mvn test
   ```

   Result: `BUILD SUCCESS`; 1 test run, 0 failures, 0 errors, 0 skipped. The same run logged successful H2 datasource initialization and Flyway V1 migration.

7. Static validation found no MySQL, Redis, or MinIO credential/connection fallback expressions. `git diff --check` completed with no whitespace errors. The pinned MinIO image tag was verified with a Docker Registry v2 manifest request returning HTTP 200.

8. `docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config` remains unexecuted because this environment has no `docker` executable. Run it on a Docker Compose v2 host before starting the local services.
