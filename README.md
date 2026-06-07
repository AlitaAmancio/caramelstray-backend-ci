# CaramelStray - Backend & DevOps Implementation

## 📌 Project Context & Disclaimer
This repository is a decoupled, backend-only fork derived from the original [CaramelStray Project](https://github.com/CaramelStray/CaramelStray-Api3-Semestre/tree/main/CaramelStray). 

This project was developed as part of the semester assignment for the **Database Development Laboratory V** (Laboratório de Desenv. de BD V) course. The primary objective of this repository is not to develop new business features, but to evolve the existing architecture by implementing:
* **Static Code Analysis:** Achieving strict quality gates using SonarCloud.
* **Automated Testing:** Implementing Unit Tests (with JUnit/Mockito) and End-to-End System Tests (with REST-Assured).
* **CI/CD Pipeline:** Automating the entire validation process using Git integration.

## GitHub Actions Workflow

The repository uses [.github/workflows/build.yml](.github/workflows/build.yml) as its main CI workflow. Even though the file is named `build.yml`, the workflow itself does more than just compile the application. It builds the project, runs tests, starts PostgreSQL for integration support, waits for the database to become healthy, prints English log messages for each major stage, and sends the result to SonarQube/SonarCloud.

### When it runs

This workflow is triggered in two situations:

* when code is pushed to `main`
* when a pull request is opened, synchronized, or reopened

### What each step does

#### 1. Checkout the repository

The workflow uses `actions/checkout@v4` with `fetch-depth: 0`.

This clones the full Git history instead of only the latest commit. That is important because Sonar analysis can use commit history to improve blame tracking and code analysis.

The workflow also prints a confirmation message after the checkout completes.

#### 2. Set up JDK 17

The workflow installs Java 17 using `actions/setup-java@v4` with the Zulu distribution.

This matches the project configuration in `pom.xml`, which also targets Java 17.

The next log line prints the installed Java version so the runner output clearly shows which JDK is active.

#### 3. Start PostgreSQL for the tests

The workflow runs `docker compose up -d --wait --wait-timeout 60` before the Maven build.

That starts the PostgreSQL container defined in `docker-compose.yml` and waits until the container reports a healthy state, so the application has a database available while tests and verification run.

The healthcheck is defined in `docker-compose.yml` using `pg_isready`, which checks whether PostgreSQL is ready to accept connections.

This stage also logs when PostgreSQL starts, when it becomes ready, and the container status after startup.

#### 4. Cache SonarQube packages

The workflow caches `~/.sonar/cache`.

This reduces repeated downloads of Sonar scanner dependencies and speeds up later pipeline runs.

#### 5. Cache Maven packages

The workflow caches `~/.m2` using the `pom.xml` hash as part of the cache key.

This keeps Maven dependencies available between runs and avoids downloading the same artifacts every time.

#### 6. Build, test, and analyze

The main build command is:

`mvn -B verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.projectKey=AlitaAmancio_caramelstray-backend-ci`

This command does three things in sequence:

* `verify` compiles the code and runs the test phase
* the Sonar Maven plugin sends the analysis to SonarQube/SonarCloud
* `sonar.projectKey` identifies the project in Sonar

The step also requires `SONAR_TOKEN` to be configured in GitHub Secrets.

Before and after the Maven execution, the workflow prints English messages so the build log is easier to follow.

#### 7. Stop PostgreSQL

The workflow ends by running `docker compose down` inside an `if: always()` block.

This guarantees that the PostgreSQL container is stopped even if the build or analysis fails.

The shutdown step also prints explicit start and completion messages.

### Sanity check

This workflow is coherent with the rest of the repository:

* `pom.xml` targets Java 17 and uses Spring Boot 3.5.5
* `application.properties` connects to `jdbc:postgresql://localhost:5432/alltallent`
* `docker-compose.yml` exposes PostgreSQL on port `5432` and now reads its credentials from environment variables

### Required configuration

The application no longer stores its database credentials or JWT secret in source-controlled configuration. Instead, the following values must be provided at runtime:

* `DB_USERNAME`
* `DB_PASSWORD`
* `JWT_SECRET_KEY`

In GitHub Actions, these values are read from repository secrets. Locally, they must be set in the shell or through a non-committed `.env` file before starting the app or running the database container.

## Sonar Remediation Strategy

Before starting the test implementation phase, the repository should first clear the highest-priority Sonar findings. That reduces the chance of tests breaking immediately after structural fixes and avoids rework.

### Priority 1: Remove hardcoded secrets and credentials

The most important issues are the values stored directly in configuration:

* [src/main/resources/application.properties](src/main/resources/application.properties#L4-L12) now reads the database username, database password, and JWT secret key from environment variables instead of storing them in plain text.
* [docker-compose.yml](docker-compose.yml#L1-L14) now reads the PostgreSQL username and password from environment variables as well.

These values should stay externalized in environment variables or CI secrets, because they are the strongest security concern and usually sit at the top of Sonar's priority list.

### Priority 2: Reduce sensitive error exposure

The authentication and registration paths should avoid exposing internal exception messages to clients.

* [src/main/java/br/com/AllTallent/config/JwtAuthFilter.java](src/main/java/br/com/AllTallent/config/JwtAuthFilter.java#L69-L80) should return generic responses while logging the detailed error server-side.
* [src/main/java/br/com/AllTallent/controller/AuthController.java](src/main/java/br/com/AllTallent/controller/AuthController.java#L50-L76) should not return `e.getMessage()` from the registration endpoint.

This keeps implementation details out of the API response and reduces the chance of leaking internal behavior.

### Priority 3: Remove duplicated role literals

The `CustomUserDetails` class had repeated role string literals such as `ROLE_USER`, `ROLE_GESTOR`, and `ROLE_ADMIN`.

That duplication was cleaned up by extracting the role names into constants in [src/main/java/br/com/AllTallent/config/CustomUserDetails.java](src/main/java/br/com/AllTallent/config/CustomUserDetails.java).

This is a maintainability fix rather than a security fix, but it helps reduce Sonar code smell noise before test work starts.