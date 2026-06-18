# CI/CD Pipeline — Technical Reference

This document describes the complete GitHub Actions pipeline, every decision behind its structure, the SonarCloud quality gate, and the suppressed rules.

---

## Overview

The pipeline is defined in [`.github/workflows/build.yml`](.github/workflows/build.yml) and triggers on every push to `main` and on every pull request targeting `main`.

It is organized as three jobs in a fan-out topology:

```
         ┌──────────────────┐
         │   unit-test      │
         │  mvn -B verify   │
         │  uploads JaCoCo  │
         └────────┬─────────┘
                  │  needs: unit-test
         ┌────────┴───────────────┐
         ▼                       ▼
┌─────────────────┐    ┌──────────────────┐
│ static-analysis │    │   system-test    │
│  sonar scan     │    │  E2E vs real DB  │
│  (no tests run) │    │  docker compose  │
└─────────────────┘    └──────────────────┘
```

`unit-test` is the shared gate: it compiles the code, runs all unit tests, and generates the JaCoCo coverage report. Only if it passes do `static-analysis` and `system-test` start — in parallel, saving wall-clock time.

---

## Job 1 — unit-test

### What it does

```bash
mvn -B verify
```

This compiles the project, runs all unit tests (file pattern `**/*SystemTest.java` is excluded by the Surefire plugin), and triggers JaCoCo's `report` goal to generate the XML coverage report.

### Why `verify` and not `test`

The `verify` Maven phase runs after `test` and includes the `post-integration-test` and `verify` lifecycle phases. JaCoCo's `report` goal is bound to the `verify` phase. Running `mvn test` alone would execute the tests but would skip the JaCoCo report, leaving no coverage data for SonarCloud to read.

### Database

**Not required.** No PostgreSQL container is started in this job. Unit tests use mocked dependencies (`@WebMvcTest` with `@MockitoBean` for controllers, `@ExtendWith(MockitoExtension.class)` for services).

The JWT secret key is still required because the Spring Security auto-configuration reads it at context startup even when a test does not touch the security layer.

### Secrets

| Secret | Usage |
|---|---|
| `JWT_SECRET_KEY` | Required to build the Spring `SecurityFilterChain` at context startup in `@WebMvcTest` tests |

### Artifact produced

`target/site/jacoco/jacoco.xml` is uploaded as a GitHub Actions artifact named `jacoco-report`. The static-analysis job downloads this artifact in the next stage.

---

## Job 2 — static-analysis

### What it does

```bash
mvn -B verify -DskipTests -Djacoco.skip=true \
  org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
  -Dsonar.projectKey=AlitaAmancio_caramelstray-backend-ci \
  -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
```

### Dependency

`needs: unit-test` — runs only after `unit-test` succeeds. It downloads the `jacoco-report` artifact before running the Sonar scanner.

### Why `-DskipTests`

Tests were already executed in `unit-test`. Running them again doubles CI time and produces no new information. The coverage data comes from the downloaded artifact, not from a fresh test run.

### Why `-Djacoco.skip=true`

Without tests running, the JaCoCo agent would not produce a `.exec` file, and the `report` goal would fail or generate an empty report. Skipping JaCoCo entirely avoids the warning and prevents it from interfering with the manually specified XML path.

### Why `fetch-depth: 0`

```yaml
- uses: actions/checkout@v4
  with:
    fetch-depth: 0
```

By default `actions/checkout@v4` performs a shallow clone (depth 1). SonarCloud uses the full Git history for:
- **Blame analysis** — identifying which commit introduced each issue (used for New Code period tracking)
- **Pull request decoration** — comparing the PR branch against the target
- **Issue detection on changed lines only** — SonarCloud distinguishes "new code" from "existing code" based on git blame

A shallow clone breaks all of these. `fetch-depth: 0` ensures SonarCloud has the complete history.

### Secrets

| Secret | Usage |
|---|---|
| `SONAR_TOKEN` | Authenticates with SonarCloud to submit the analysis result |

`SONAR_TOKEN` is scoped at the **step level** inside `Run SonarQube analysis`, not at the job level. This limits the secret's visibility to only the step that needs it, reducing the window in which it could appear in logs or environment dumps.

---

## Job 3 — system-test

### What it does

```bash
docker compose up -d --wait --wait-timeout 60
mvn test -Psystem-test
docker compose down  # always runs, even on failure
```

### Dependency

`needs: unit-test` — waits for the compile-and-unit-test gate before starting a database and running E2E tests. If the code doesn't compile or unit tests fail, there is no value in spending the time to spin up PostgreSQL and run system tests.

`system-test` does **not** depend on `static-analysis`, so the two jobs run concurrently once `unit-test` passes.

### The system-test Maven profile

The `system-test` Maven profile in `pom.xml` overrides the default Surefire configuration:

```xml
<profile>
    <id>system-test</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration combine.self="override">
                    <includes>
                        <include>**/*SystemTest.java</include>
                    </includes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</profile>
```

`combine.self="override"` replaces the default `<excludes>` block entirely, so `*SystemTest.java` files are included instead of excluded. This means `mvn test -Psystem-test` runs **only** system tests, with no unit tests mixed in.

### Docker Compose

`--wait` blocks until all healthchecks in `docker-compose.yml` pass. `--wait-timeout 60` limits the wait to 60 seconds before failing the step. The `docker compose down` step uses `if: always()` so the database container is always stopped, even when tests fail and the job exits early.

### Secrets

| Secret | Usage |
|---|---|
| `DB_USERNAME` | PostgreSQL username, read by `application.properties` via `${DB_USERNAME}` |
| `DB_PASSWORD` | PostgreSQL password, read by `application.properties` via `${DB_PASSWORD}` |
| `JWT_SECRET_KEY` | JWT signing key, required for the full Spring context to start |

---

## SonarCloud Configuration

SonarCloud properties are defined in `pom.xml` under `<properties>`:

```xml
<sonar.organization>alitaamancio</sonar.organization>
<sonar.host.url>https://sonarcloud.io</sonar.host.url>
<sonar.projectKey>AlitaAmancio_caramelstray-backend-ci</sonar.projectKey>
<sonar.coverage.jacoco.xmlReportPaths>
    ${project.build.directory}/site/jacoco/jacoco.xml
</sonar.coverage.jacoco.xmlReportPaths>
```

### Coverage exclusions

The following packages are excluded from both JaCoCo measurement and SonarCloud coverage analysis because they contain no testable logic:

```xml
<sonar.coverage.exclusions>
    **/dto/**,
    **/model/**,
    **/config/**,
    **/exception/**,
    **/repository/**,
    **/CaramelStrayApplication*
</sonar.coverage.exclusions>
```

| Package | Reason excluded |
|---|---|
| `dto/**` | Record classes with no behavior — pure data carriers |
| `model/**` | JPA entity classes — field mapping, no logic |
| `config/**` | Spring Security and JWT configuration — framework-wired, not unit-testable in isolation |
| `exception/**` | Custom exception classes — extend RuntimeException, no logic |
| `repository/**` | Spring Data JPA interfaces — no implementation to test |
| `CaramelStrayApplication*` | Application entry point (`main` method only) |

---

## Quality Gate

The project must pass all of the following thresholds on SonarCloud:

| Metric | Required |
|---|---|
| Code line coverage | > 75% |
| Code branch coverage | = 100% |
| Bugs | 0 |
| Vulnerabilities | 0 |
| Code duplication | < 5% |
| Issues — Blocker | 0 |
| Issues — Critical | 0 |
| Issues — Major + Minor + Info | < 5 |

---

## Suppressed Rules

### S120 — Package name does not follow convention

**Rule:** SonarQube requires package names to match the regex `^[a-z]+(\.[a-z][a-z0-9]*)*$` (all lowercase).

**Why suppressed:** The project package is `br.com.AllTallent` with intentional uppercase letters. `AllTallent` is the company name. Renaming it to `br.com.alltallent` would require modifying the package declaration and every import statement across the entire codebase — significant refactoring with zero functional or security benefit, and high risk of introducing regressions near the submission deadline.

**Scope:** Suppressed globally across all files via `sonar.issue.ignore.multicriteria`.

```xml
<sonar.issue.ignore.multicriteria.e1.ruleKey>java:S120</sonar.issue.ignore.multicriteria.e1.ruleKey>
<sonar.issue.ignore.multicriteria.e1.resourceKey>**/*</sonar.issue.ignore.multicriteria.e1.resourceKey>
```

---

### S1598 — File path does not match the package declaration

**Rule:** SonarQube checks that the file path matches the package name (e.g., `br/com/AllTallent/service/FooService.java` must live at path `br/com/AllTallent/service/FooService.java`).

**Why suppressed:** Development happens on Windows, where the file system is case-insensitive. Git stores the paths as `br/com/AllTallent/`, but Windows Explorer and most Windows tools report them as `br/com/alltallent/` (lowercase). SonarCloud receives the Windows-reported paths and flags a mismatch with the `br.com.AllTallent` package declaration — a false positive that does not exist on a case-sensitive Linux file system (where the CI runner and production deployment operate). Without suppression, this rule would generate approximately 100 Critical issues from a non-existent problem.

**Scope:** Suppressed globally across all files.

```xml
<sonar.issue.ignore.multicriteria.e2.ruleKey>java:S1598</sonar.issue.ignore.multicriteria.e2.ruleKey>
<sonar.issue.ignore.multicriteria.e2.resourceKey>**/*</sonar.issue.ignore.multicriteria.e2.resourceKey>
```

---

### S2696 — Instance method modifying a static field (in-source suppression)

**Rule:** SonarQube flags instance methods that write to `static` fields because it can indicate a thread-safety problem.

**Why suppressed:** `BaseSystemTest.createBaseTestData()` is a non-static `@BeforeAll` method (required so that Spring can inject `@Autowired JdbcTemplate`) that writes to `static` fields (`adminToken`, `adminId`, etc.). The static fields allow the one-time test setup to be visible across all subclasses in the same JVM run. The method is called exactly once, inside a `@BeforeAll` which JUnit 5 guarantees runs in a single thread before any test method. There is no race condition.

The suppression is scoped to that specific method using `@SuppressWarnings("java:S2696")` — not a global rule exclusion.

---

### S1192 — Duplicate string literals (SonarLint vs SonarCloud behavior)

This rule is **not suppressed** — it is resolved by extracting string literals into `private static final` constants. However, the way SonarLint (the IDE plugin) and SonarCloud analyze constants differs:

**SonarLint** performs **constant folding**: when it sees `FUNCIONARIO_URL + "/" + id`, it expands the constant to `"/api/funcionario/" + id` and re-evaluates whether the string `/api/funcionario/` appears three or more times. This can cause the IDE to show S1192 warnings on usages of a constant even though the constant itself satisfies the rule.

**SonarCloud** (the actual gate) performs **source-literal analysis only**: it sees the literal `"/"` in `FUNCIONARIO_URL + "/"`, not `/api/funcionario/`. Constants defined as pure string literals (not composed from other constants) satisfy SonarCloud even when SonarLint still highlights them in the IDE.

**Practical conclusion:** If the IDE shows S1192 warnings on a line that uses a constant (not on the constant definition line itself), those warnings are SonarLint false positives and will not block the SonarCloud quality gate.

---

## Security Decisions

### Secrets never stored in source control

Three categories of sensitive values are externalized:

| Value | Mechanism |
|---|---|
| `DB_USERNAME`, `DB_PASSWORD` | `application.properties` reads `${DB_USERNAME}` and `${DB_PASSWORD}` from environment |
| `JWT_SECRET_KEY` | `application.properties` reads `${JWT_SECRET_KEY}` from environment |
| `SONAR_TOKEN` | Referenced only in a step-level `env:` block in the CI workflow |

### application-test.properties is gitignored

`src/main/resources/application-test.properties` contains concrete database credentials and a concrete JWT secret for the local developer test environment. This file is listed in `.gitignore` and must never be committed. In CI, the Spring `test` profile reads the same configuration from environment variables injected via GitHub Secrets — no properties file is needed in CI.

### SONAR_TOKEN scope

`SONAR_TOKEN` is declared at the step level inside `Run SonarQube analysis`, not at the job level:

```yaml
- name: Run SonarQube analysis
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
  run: mvn ... sonar ...
```

This limits the token's availability to the single shell command that needs it. If it were declared at the job level (`jobs.static-analysis.env`), it would be visible to every step in the job, including potential third-party actions.

---

## Engineering Decisions

### Surefire 3.5.3 + JUnit Platform tag exclusion bug

The original approach to separating unit tests from system tests used `<excludedGroups>system</excludedGroups>` inside the Surefire plugin configuration block. With Surefire 3.5.3 (shipped by Spring Boot 3.5.5) and the JUnit Platform provider, tag-based filtering placed directly in the plugin `<configuration>` block causes the provider to apply the filter before test discovery completes, resulting in **zero tests discovered**.

The fix was to use file-pattern exclusion:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <excludes>
            <exclude>**/*SystemTest.java</exclude>
        </excludes>
    </configuration>
</plugin>
```

File patterns are resolved by Surefire before handing control to the JUnit Platform provider, so test discovery is not disrupted. Tag filtering with `-DexcludedGroups=system` on the command line still works correctly — the bug only affects the plugin configuration block.

### Why `mvn verify` instead of `mvn test` in unit-test

JaCoCo's `report` goal is bound to the `verify` lifecycle phase. Using `mvn test` executes the tests but skips the coverage report, leaving no `jacoco.xml` for SonarCloud. `mvn verify` runs through the full default lifecycle including `verify`, which triggers the report goal and produces `target/site/jacoco/jacoco.xml`.
