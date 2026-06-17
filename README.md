# CaramelStray — Backend CI/CD & Quality Assurance

## Project Context

This repository is a backend-only fork of the original [CaramelStray project](https://github.com/CaramelStray/CaramelStray-Api3-Semestre/tree/main/CaramelStray), developed as the practical assignment for the **Database Development Laboratory V** course.

The goal was not to add new business features, but to apply professional software quality practices to an existing codebase:

- Static code analysis with a strict SonarCloud Quality Gate
- Automated unit testing with JUnit 5 and Mockito
- End-to-end system testing with REST-Assured against a live PostgreSQL database
- A three-stage CI/CD pipeline in GitHub Actions

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.5.5 |
| Security | Spring Security + JWT (JJWT 0.11.5) |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL |
| Build | Maven (with Maven Surefire Plugin 3.x, JaCoCo 0.8.11) |
| Unit tests | JUnit 5 + Mockito |
| System tests | JUnit 5 + REST-Assured + JSON Schema Validator |
| Static analysis | SonarCloud |
| CI/CD | GitHub Actions |

---

## Architecture

The application follows a layered architecture:

```
HTTP Request
    │
    ▼
Controller        (@RestController, handles DTOs only, no entities exposed)
    │
    ▼
Service           (business logic, authorization, transactions)
    │
    ▼
Repository        (Spring Data JPA interfaces)
    │
    ▼
PostgreSQL        (via JDBC, connection pool managed by HikariCP)
```

**Security model:** Every request passes through `JwtAuthFilter`, which validates the Bearer token and populates the `SecurityContext`. Controllers and services use `@PreAuthorize` annotations for role-based access control. Three roles exist: `ROLE_ADMIN` (Diretoria, profile 1), `ROLE_GESTOR` (Supervisao, profile 2), and `ROLE_USER` (Colaborador, profile 3).

**DTOs:** All controllers receive and return Data Transfer Objects. Persistent entities are never serialized directly into HTTP responses — this prevents accidental exposure of internal fields and avoids infinite recursion from bidirectional JPA relationships.

---

## How to Run Locally

### Prerequisites

- Java 17
- Docker (for PostgreSQL)
- Maven 3.8+

### Environment variables

The application reads its secrets from environment variables. Set these in your shell before starting:

```bash
export DB_USERNAME=your_db_user
export DB_PASSWORD=your_db_password
export JWT_SECRET_KEY=your_base64_encoded_secret_key_at_least_256_bits
```

### Start the database

```bash
docker compose up -d --wait
```

### Run the application

```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

### Run unit tests only (no database required)

```bash
mvn verify
```

### Run end-to-end system tests (database must be running)

```bash
mvn test -Psystem-test
```

---

## Test Strategy

### Unit Tests

Unit tests verify individual layers in isolation using mocks. They live under `src/test/java` and are named without a `SystemTest` suffix.

**What is tested:**
- Service methods: business rules, authorization checks, exception throwing
- Controller endpoints: HTTP status codes, request validation, response body structure (using `@WebMvcTest` with a mocked service layer)

**How they run:**
`mvn verify` executes unit tests. The Maven Surefire Plugin is configured in `pom.xml` to exclude any file matching `**/*SystemTest.java`, so system tests never run accidentally during the standard build.

**Why `mvn verify` and not `mvn test`:**
The `verify` phase also executes JaCoCo's `report` goal, which generates the XML coverage report that SonarCloud reads. Using `mvn test` alone would run the tests but skip the coverage report generation.

**Coverage exclusions:**
The following packages are excluded from JaCoCo coverage measurement because they contain no testable logic (pure data holders, framework configurations, and infrastructure):
- `dto/**` — record classes with no behavior
- `model/**` — JPA entity classes
- `config/**` — Spring Security configuration
- `exception/**` — custom exception classes (no logic)
- `repository/**` — Spring Data interfaces (no implementation to test)
- `CaramelStrayApplication*` — application entry point

### System Tests (End-to-End)

System tests verify the complete HTTP request-response cycle, from the REST endpoint through the service and repository layers to the real PostgreSQL database. They live under `src/test/java/.../system/` and are named `*SystemTest.java`.

**What is tested:**

| Test class | Endpoints covered |
|---|---|
| `AuthSystemTest` | `POST /api/auth/login`, `POST /api/auth/register`, `GET /api/auth/me` |
| `AreaSystemTest` | `POST /api/area`, `GET /api/area` |
| `PerfilSystemTest` | `POST /api/perfil`, `GET /api/perfil` |
| `CompetenciaSystemTest` | Full CRUD on `/api/competencia` |
| `PerguntaSystemTest` | Full CRUD on `/api/perguntas` |
| `FuncionarioSystemTest` | Full CRUD on `/api/funcionario`, plus certificates, competencies, and work history sub-resources |
| `AvaliacaoSystemTest` | Complete evaluation lifecycle: create, list, view instances, answer, finalize, review |
| `DashboardSystemTest` | `GET /api/dashboard` with and without area filter |

Each test class covers three categories of scenarios:
- **Main flows:** the expected happy-path behavior (2xx responses)
- **Alternative flows:** valid but secondary paths (other roles, other filters)
- **Exception flows:** invalid inputs, missing authorization, nonexistent resources (4xx responses)

**How they run:**
`mvn test -Psystem-test` activates the `system-test` Maven profile, which overrides the default Surefire configuration to include only `**/*SystemTest.java` files. This means the profile run executes system tests exclusively, with no unit tests mixed in.

---

## Key Engineering Decisions

### Surefire tag exclusion bug

The initial attempt to separate unit tests from system tests used `<excludedGroups>system</excludedGroups>` inside the Surefire plugin configuration block. With Surefire 3.5.3 (shipped by Spring Boot 3.5.5) and the JUnit Platform provider, placing tag-based filtering in the plugin `<configuration>` block causes the provider to report zero tests discovered — the filter is applied before the test discovery phase completes.

The fix was to use file-pattern exclusion instead:

```xml
<excludes>
    <exclude>**/*SystemTest.java</exclude>
</excludes>
```

This approach works reliably because Surefire resolves file patterns before handing control to the JUnit Platform provider, so test discovery is never disrupted. Tag filtering with `-DexcludedGroups=system` on the command line still works correctly; only the plugin configuration block is affected by the bug.

### Database bootstrap without a chicken-and-egg problem

`BaseSystemTest` needs to create area and profile records in the database before registering test users, and it needs registered users before it can obtain JWT tokens. However, the `POST /api/area` and `POST /api/perfil` endpoints require an authenticated request — and no token exists yet.

The solution was to bypass the API entirely for the initial data setup by using `JdbcTemplate` with PostgreSQL's `RETURNING` clause:

```java
baseAreaId = jdbcTemplate.queryForObject(
    "INSERT INTO tb_cad_area (nome, descricao) VALUES (?, ?) RETURNING codigo",
    Integer.class, "Technology", "IT Department");
```

This inserts the area and profiles directly into the database and captures the generated primary keys in one statement. After that, the `POST /api/auth/register` endpoint is public, so user registration can proceed via HTTP without any token.

### Static shared state in BaseSystemTest

The base class holds `adminToken`, `gestorToken`, `colaboradorToken`, and the base entity IDs as `static` fields. This means they survive across all test class instances that extend `BaseSystemTest` within a single JVM run.

The `@BeforeAll` method is guarded by `if (adminToken == null)` so the database truncate and seed runs only once per test suite execution, regardless of how many subclasses inherit it. All subclasses are annotated with `@TestInstance(Lifecycle.PER_CLASS)` so that `@BeforeAll` methods can be non-static, which is required for Spring to inject `JdbcTemplate` via `@Autowired`.

The `@SuppressWarnings("java:S2696")` annotation on `createBaseTestData()` suppresses the SonarQube S2696 rule (non-static instance method modifying static fields). The suppression is legitimate: the Spring test context guarantees single-threaded `@BeforeAll` execution, so there is no race condition risk.

### TRUNCATE with RESTART IDENTITY CASCADE

The `resetDatabase()` method truncates all tables with `RESTART IDENTITY CASCADE`. This resets PostgreSQL sequences to 1, which means the first inserted profile gets `codigo = 1` (Diretoria → ADMIN), the second gets `codigo = 2` (Supervisao → GESTOR), and the third gets `codigo = 3` (Colaborador → USER). This matches the hardcoded role mapping in `CustomUserDetails`, which assigns roles based on the numeric profile ID.

---

## CI/CD Pipeline

The pipeline is defined in [.github/workflows/build.yml](.github/workflows/build.yml) and runs on every push to `main` and every pull request.

It is split into three independent jobs that fan out from a common gate:

```
         ┌──────────┐
         │unit-test │  (compiles + runs unit tests + uploads JaCoCo XML)
         └────┬─────┘
              │ needs
       ┌──────┴──────────┐
       ▼                 ▼
┌──────────────┐  ┌─────────────┐
│static-       │  │system-test  │
│analysis      │  │             │
│(sonar scan)  │  │(E2E vs DB)  │
└──────────────┘  └─────────────┘
```

### Job 1: unit-test

**Runs:** `mvn -B verify`

**Purpose:** Compiles the entire project and runs all unit tests. Measures line and branch coverage with JaCoCo and generates the XML report.

**Database:** Not required. No PostgreSQL container is started in this job.

**Secrets needed:** `JWT_SECRET_KEY` — the Spring security configuration reads the JWT signing key from an environment variable. Without it, the application context fails to start even during `@WebMvcTest` tests.

**Artifact produced:** Uploads `target/site/jacoco/jacoco.xml` as a GitHub Actions artifact named `jacoco-report`. This artifact is consumed by the static-analysis job.

### Job 2: static-analysis

**Runs after:** `unit-test`

**Runs:** `mvn -B verify -DskipTests -Djacoco.skip=true org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.projectKey=... -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml`

**Purpose:** Downloads the JaCoCo XML artifact from the unit-test job, compiles the code (so that `.class` files are present for bytecode analysis), then sends the full Sonar analysis to SonarCloud. Coverage data comes from the downloaded XML.

**Why `-DskipTests -Djacoco.skip=true`:** Tests were already executed in unit-test. Repeating them here would double the CI time. The JaCoCo agent is also skipped since no tests run and no `.exec` file would be generated anyway.

**Why `fetch-depth: 0`:** SonarCloud uses the full Git history for blame tracking, computing new-vs-changed code analysis, and pull request decoration. A shallow clone (the default with `actions/checkout@v4`) breaks this analysis.

**Secrets needed:** `SONAR_TOKEN` — required to authenticate with SonarCloud and submit the analysis result.

### Job 3: system-test

**Runs after:** `unit-test`

**Runs:** `mvn test -Psystem-test`

**Purpose:** Starts a real PostgreSQL instance using `docker compose up -d --wait`, then executes the full end-to-end test suite. The `system-test` Maven profile overrides the Surefire configuration to include only `*SystemTest.java` files.

**Database:** Required. PostgreSQL starts via `docker compose` and is stopped in an `if: always()` step to ensure cleanup even when tests fail.

**Secrets needed:** `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET_KEY`.

**Why does system-test also `needs: unit-test` instead of running in parallel with everything?**
The unit-test job acts as a compile-and-validate gate. If the code doesn't compile or unit tests fail, there is no point starting a database and running E2E tests. Waiting for unit-test ensures the system-test job only runs against code that already passed the fast check.

---

## SonarCloud Quality Gate

The project targets the following thresholds on SonarCloud:

| Metric | Required |
|---|---|
| Line coverage | > 75% |
| Branch coverage | = 100% |
| Bugs | 0 |
| Vulnerabilities | 0 |
| Code duplication | < 5% |
| Blocker issues | 0 |
| Critical issues | 0 |
| Major + Minor + Info issues | < 5 |

### Suppressed rules

Two rules are suppressed globally via `sonar.issue.ignore.multicriteria` in `pom.xml`:

**S120 — Package name does not match the required convention**

The project package is `br.com.AllTallent` with intentional uppercase letters (the company name). Renaming it to `br.com.alltallent` would require modifying every Java file in the project and every import statement — significant refactoring risk with no functional value. The uppercase is a deliberate choice that identifies the organization.

**S1598 — File path does not match the package declaration**

On Windows (where development happens), the file system is case-insensitive, so tools report paths as `br/com/alltallent`. On Linux (where the GitHub Actions runner executes), the paths are `br/com/AllTallent` with the correct casing. SonarCloud sees the Windows-style paths and flags a mismatch that does not exist at runtime. Suppressing this rule avoids ~100 false-positive critical issues caused entirely by the case-insensitive Windows file system.

### SonarLint vs SonarCloud behavior on string constants

SonarLint (the IDE plugin) performs **constant folding** when checking S1192 (duplicate string literals). If `FUNCIONARIO_URL = "/api/funcionario"` and you write `FUNCIONARIO_URL + "/" + id`, SonarLint expands the constant and flags `/api/funcionario/` as a duplicate.

SonarCloud (the actual gate) does **source-literal analysis only**. It sees `FUNCIONARIO_URL + "/"` as the string literal `"/"`, not `/api/funcionario/`. Constants defined as pure string literals (not composed from other constants) satisfy SonarCloud even when SonarLint still shows warnings in the IDE.

---

## Security Decisions

### Secrets are never stored in source control

The following values are loaded from environment variables at runtime:

- `DB_USERNAME` and `DB_PASSWORD` — read by `application.properties` as `${DB_USERNAME}` and `${DB_PASSWORD}`
- `JWT_SECRET_KEY` — read by `application.properties` as `${JWT_SECRET_KEY}`

In GitHub Actions, these are stored in repository secrets and injected at workflow runtime. Locally, they must be set as shell environment variables before starting the application.

### SONAR_TOKEN is never exposed

`SONAR_TOKEN` is referenced only in the `static-analysis` job's step-level `env:` block, not at the job level. This limits its scope to only the step that needs it and prevents it from appearing in job logs outside that step.

### application-test.properties is gitignored

The file `src/main/resources/application-test.properties` contains real database credentials and a real JWT secret for the local test environment. It is listed in `.gitignore` and must never be committed. In CI, the same configuration is supplied through GitHub Secrets injected as environment variables, so no properties file is needed.

---

## Sonar Remediation History

Before implementing tests, the codebase was cleaned up to remove high-priority Sonar issues. The key changes were:

**Hardcoded secrets removed:**
- `application.properties` now reads `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET_KEY` from environment variables instead of containing plaintext values.
- `docker-compose.yml` reads PostgreSQL credentials from environment variables.

**Entity exposure eliminated:**
- `AreaController`, `PerfilController`, and `CompetenciaController` now use DTOs instead of exposing JPA entities directly in HTTP responses.
- `AuthController` and `AvaliacaoController` no longer use wildcard response types.

**Duplicate literals extracted to constants:**
- Role strings (`ROLE_ADMIN`, `ROLE_GESTOR`, `ROLE_USER`) extracted in `CustomUserDetails`.
- Error message strings extracted to private static final constants in service classes.
- All system test URL strings and JSON field names extracted to per-class constants.

**Constructor injection enforced:**
- Field-level `@Autowired` replaced with constructor injection (using Lombok `@RequiredArgsConstructor`) across service and filter classes.

**Other fixes:**
- Integer overflow fixed in `JwtService` by using explicit UTC timezone and long arithmetic for token expiry.
- `DashboardController` uses `Logger` instead of `System.out.println`.
- `AuthService` uses specific exception types and `ZonedDateTime.now(ZoneOffset.UTC)` instead of `new Date()`.
