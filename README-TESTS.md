# Test Suite — Technical Reference

This document describes the complete test suite: what is tested, how it is structured, which engineering decisions were made, and how to run each test type.

---

## Overview

The project has two distinct test types that run in separate CI stages:

| Type | Scope | Infrastructure | Command |
|---|---|---|---|
| **Unit tests** | Individual class in isolation | No database, no network | `mvn verify` |
| **System tests (E2E)** | Full HTTP request → service → database → HTTP response | Real PostgreSQL | `mvn test -Psystem-test` |

Unit tests never start a database. System tests never use mocks.

---

## Unit Tests

### Purpose

Unit tests verify the behavior of individual classes in isolation. Dependencies (repositories, services, the password encoder) are replaced by Mockito mocks. Each test exercises a specific method or branch and asserts the return value or the exceptions thrown.

### Test classes

#### Controller tests (`@WebMvcTest`)

`@WebMvcTest` loads only the MVC layer — the controller being tested and the Spring Security filter chain. The service layer is replaced by a `@MockitoBean`. Tests send HTTP requests via `MockMvc` and assert the response status and JSON body.

| Test class | Controller covered | Key scenarios |
|---|---|---|
| `AuthControllerTest` | `AuthController` | Login 200 with token, login 401 with wrong credentials, register 201, `/me` 200 with authenticated user, `/me` 403 without token |
| `AreaControllerTest` | `AreaController` | Create 201, list 200, unauthorized 403 |
| `PerfilControllerTest` | `PerfilController` | Create 201, list 200, unauthorized 403 |
| `CompetenciaControllerTest` | `CompetenciaController` | Full CRUD: create 201, get by id 200, list 200, update 200, delete 204, not found 404 |
| `PerguntaControllerTest` | `PerguntaController` | Create 201, list 200, get by id 200, delete 204, 404 on missing resource, 403 for collaborator |
| `AvaliacaoControllerTest` | `AvaliacaoController` | Create 201, list 200, get by id 200, instance operations, pending evaluations, answer saving, finalize, review |
| `FuncionarioControllerTest` | `FuncionarioController` | List 200, get by id 200, update 200, delete 204, competencies 200, experiences CRUD, certificates CRUD, 403 for unauthorized operations |
| `DashboardControllerTest` | `DashboardController` | Dashboard 200 with and without area filter, 403 without token |

#### Service tests (`@ExtendWith(MockitoExtension.class)`)

Service tests load no Spring context. Repositories are injected as `@Mock` fields. Tests call service methods directly and assert behavior using JUnit 5 assertions and Mockito verifications.

| Test class | Service covered | Key scenarios |
|---|---|---|
| `AuthServiceTest` | `AuthService` | User registration, duplicate email rejection, role assignment by profile ID |
| `FuncionarioServiceTest` | `FuncionarioService` | List with and without text filter, find by id (found / not found), create with all field combinations, update, delete, add/remove certificates, add/update experiences, associate competencies with all `canAssociate` permission branches (self-edit, gestor same area, gestor different area, admin, pure ROLE_USER) |
| `DashboardServiceTest` | `DashboardService` | Dashboard data with and without area filter, aggregation queries |
| `PerguntaServiceTest` | `PerguntaService` | Create, list, find by id, delete, not found handling |
| `AvaliacaoServiceTest` | `AvaliacaoService` | Create evaluation, list, get by id, get instances, pending evaluations, answer saving, finalize, review, permission checks |

### How to run

```bash
# Run unit tests only (no database needed)
mvn verify
```

JaCoCo generates the XML coverage report at `target/site/jacoco/jacoco.xml` after all tests pass.

### JaCoCo coverage exclusions

The following packages are excluded from coverage measurement because they contain no testable logic:

| Package | Reason |
|---|---|
| `dto/**` | Record classes — pure data carriers with no behavior |
| `model/**` | JPA entity classes — field mapping and getters/setters only |
| `config/**` | Spring Security filter chain and JWT configuration — wired by the framework |
| `exception/**` | Custom exception classes that extend `RuntimeException` with no logic |
| `repository/**` | Spring Data JPA interfaces — Hibernate provides the implementation |
| `CaramelStrayApplication*` | Application entry point (`public static void main`) |

Excluding these classes lets the coverage percentage reflect the actual business and controller logic being tested, without being diluted by the large volume of framework-managed or data-only classes.

### Conventions

- All class names, method names, attribute names, and comments are in **English**
- Test method names follow the `{subject}Should{Expected}When{Condition}` pattern
- `@ExtendWith(MockitoExtension.class)` enables strict stubbing mode — any stubbed call that is never invoked causes the test to fail with `UnnecessaryStubbingException`

---

## System Tests (E2E)

### Purpose

System tests verify the complete request-response cycle from the HTTP boundary through the controller, service, and repository layers to a real PostgreSQL database — and back. No mocks are used. They run against the same Spring Boot application context and the same database schema that production uses.

### What is validated in each test

Every test for a successful response (2xx) validates all three of:
1. **Response code** — the exact HTTP status expected
2. **JSON Schema** — the response body structure matches a schema file in `src/test/resources/schemas/`
3. **Controlled expected values** — at least one field in the response is asserted to equal a known value set during test setup

Tests for error responses (4xx) validate the response code only — there is no schema for error bodies.

### Test classes and coverage

#### `AuthSystemTest` — `/api/auth`

| Test | Flow | Validates |
|---|---|---|
| `loginShouldReturn200WithTokenAndUserInfo` | Main | Status 200, login-response schema, non-null token, userId, nomeCompleto |
| `getMeShouldReturn200WithAuthenticatedUserProfile` | Main | Status 200, funcionario schema, codigo, nomeCompleto, email |
| `registerShouldReturn201ForValidNewUser` | Main | Status 201 |
| `getMeShouldReturn200WithGestorProfile` | Alternative | Status 200, gestor's codigo and nomeCompleto |
| `registerShouldReturn400ForDuplicateEmail` | Exception | Status 400 |
| `loginShouldReturn401ForWrongPassword` | Exception | Status 401 |
| `loginShouldReturn401ForUnknownEmail` | Exception | Status 401 |
| `getMeShouldReturn403WithoutToken` | Exception | Status 403 |

#### `AreaSystemTest` — `/api/area`

| Test | Flow | Validates |
|---|---|---|
| `createAreaShouldReturn201WithCreatedArea` | Main | Status 201, area schema, nome, descricao, non-null codigo |
| `getAllAreasShouldReturn200WithNonEmptyList` | Main | Status 200, area-list schema, non-empty list, non-null fields |
| `createAreaWithoutDescricaoShouldReturn201` | Alternative | Status 201, nome |
| `getAllAreasShouldReturnBaseAreaCreatedInSetup` | Alternative | Status 200, "Technology" in nome list |
| `createAreaWithEmptyBodyShouldNotReturn500` | Exception | Status is 201 or 400 (API has no mandatory field validation on Area) |

#### `PerfilSystemTest` — `/api/perfil`

| Test | Flow | Validates |
|---|---|---|
| `createPerfilShouldReturn201WithCreatedPerfil` | Main | Status 201, perfil schema, nome, non-null codigo |
| `getAllPerfisShouldReturn200WithNonEmptyList` | Main | Status 200, non-empty list |
| `createPerfilWithoutDescricaoShouldReturn201` | Alternative | Status 201, nome |
| `getAllPerfisShouldContainBaseProfiles` | Alternative | Status 200, "Diretoria", "Supervisao", "Colaborador" present |
| `createPerfilWithNullNameShouldNotReturn500` | Exception | Status is 201 or 400 |

#### `CompetenciaSystemTest` — `/api/competencia`

| Test | Flow | Validates |
|---|---|---|
| `listAllCompetenciasShouldReturn200WithList` | Main | Status 200, competencia-list schema, non-empty |
| `getCompetenciaByIdShouldReturn200WithCorrectData` | Main | Status 200, competencia schema, id, nome, categoria |
| `createCompetenciaShouldReturn201WithCreatedData` | Main | Status 201, competencia schema, nome, non-null id |
| `updateCompetenciaShouldReturn200WithUpdatedData` | Main | Status 200, updated nome, same id |
| `createCompetenciaShouldReturn400ForDuplicateName` | Alternative | Status 400 |
| `getCompetenciaByIdShouldReturn404WhenNotFound` | Exception | Status 404 |
| `updateCompetenciaShouldReturn404WhenNotFound` | Exception | Status 404 |
| `deleteCompetenciaShouldReturn204WhenFound` | Main | Status 204 |
| `deleteCompetenciaShouldReturn404WhenNotFound` | Exception | Status 404 |

#### `PerguntaSystemTest` — `/api/perguntas`

| Test | Flow | Validates |
|---|---|---|
| `createPerguntaShouldReturn201ForAdmin` | Main | Status 201, pergunta schema, pergunta text, competenciaCodigo, non-null codigo |
| `createPerguntaShouldReturn201ForGestor` | Alternative | Status 201, pergunta text |
| `listAllPerguntasShouldReturn200WithList` | Main | Status 200, pergunta-list schema, non-empty, non-null codigo |
| `getPerguntaByIdShouldReturn200WithCorrectData` | Main | Status 200, pergunta schema, codigo, pergunta text, competenciaCodigo |
| `listPerguntasShouldReturn200WithExpectedQuestion` | Alternative | Status 200, specific question text in list |
| `getPerguntaByIdShouldReturn404WhenNotFound` | Exception | Status 404 |
| `createPerguntaShouldReturn403ForColaborador` | Exception | Status 403 |
| `createPerguntaShouldReturn400WhenCompetenciaNotFound` | Exception | Status 400 |
| `deletePerguntaShouldReturn204WhenFound` | Main | Status 204 |
| `deletePerguntaShouldReturn404WhenNotFound` | Exception | Status 404 |

#### `FuncionarioSystemTest` — `/api/funcionario`

| Test | Flow | Validates |
|---|---|---|
| `listAllShouldReturn200WithEmployeeListForAdmin` | Main | Status 200, non-empty list, non-null codigo and nomeCompleto |
| `getByIdShouldReturn200WithCorrectEmployeeForAdmin` | Main | Status 200, funcionario schema, codigo, nomeCompleto, email |
| `getByIdShouldReturn200WhenEmployeeAccessesOwnProfile` | Alternative | Status 200, own codigo |
| `getProfileByIdShouldReturn200ForAdmin` | Main | Status 200, non-null codigo |
| `updateShouldReturn200WhenEmployeeUpdatesSelf` | Main | Status 200, funcionario schema, updated nomeCompleto |
| `addCertificateShouldReturn201` | Main | Status 201, nome, non-null codigo |
| `removeCertificateShouldReturn204` | Main | Status 204 |
| `getCompetenciasShouldReturn200` | Main | Status 200, non-null competencias list |
| `updateCompetenciasShouldReturn204` | Main | Status 204 |
| `getExperienciasShouldReturn200` | Main | Status 200, codigoFuncionario |
| `addExperienciaShouldReturn201` | Main | Status 201, cargo, non-null codigo |
| `updateExperienciaShouldReturn200` | Main | Status 200, updated cargo |
| `listAllWithTextFilterShouldReturn200` | Alternative | Status 200, non-empty list |
| `getByIdShouldReturn200WhenGestorAccessesAnyProfile` | Alternative | Status 200, colaborador's codigo |
| `deleteEmployeeShouldReturn204ForAdmin` | Main | Status 204 |
| `listAllShouldReturn403WithoutToken` | Exception | Status 403 |
| `createEmployeeShouldReturn403ForColaborador` | Exception | Status 403 |
| `updateShouldReturn403WhenColaboradorAttemptsToUpdateOtherEmployee` | Exception | Status 403 |
| `getCompetenciasShouldReturn403WhenColaboradorAccessesOtherProfile` | Exception | Status 403 |

#### `AvaliacaoSystemTest` — `/api/avaliacoes`

| Test | Flow | Validates |
|---|---|---|
| `listAvaliacoesShouldReturn200WithList` | Main | Status 200, avaliacao-list schema, non-empty, non-null titulo |
| `getAvaliacaoByIdShouldReturn200WithCorrectData` | Main | Status 200, titulo, codigo |
| `getInstanciasByAvaliacaoShouldReturn200WithList` | Main | Status 200, size 1, funcionarioCodigo |
| `getAvaliacaoPendentesShouldReturn200ForColaborador` | Main | Status 200, non-empty |
| `getAvaliacaoParaResponderShouldReturn200` | Main | Status 200, non-null perguntas |
| `saveAnswerShouldReturn200` | Main | Status 200 |
| `getAnswersByInstanciaShouldReturn200` | Main | Status 200, non-empty |
| `getRevisaoShouldReturn200ForAdmin` | Main | Status 200 |
| `finalizeAvaliacaoShouldReturn204ForColaborador` | Main | Status 204 |
| `saveRevisaoShouldReturn200ForAdmin` | Main | Status 200, resultadoStatus |
| `createAvaliacaoShouldReturn201ForGestor` | Alternative | Status 201, avaliacao schema, titulo, non-null codigo |
| `createAvaliacaoShouldReturn403ForColaborador` | Exception | Status 403 |
| `getAvaliacaoByIdShouldReturn404WhenNotFound` | Exception | Status 404 |
| `saveAnswerShouldReturn400ForInvalidInstanciaId` | Exception | Status 400 |
| `getPendentesShouldReturn403WhenAccessingAnotherEmployeePendentes` | Exception | Status 403 |

#### `DashboardSystemTest` — `/api/dashboard`

| Test | Flow | Validates |
|---|---|---|
| `getDashboardShouldReturn200ForAdmin` | Main | Status 200, dashboard schema, non-null totalColaboradores |
| `getDashboardShouldReturn200ForGestor` | Alternative | Status 200, dashboard schema, non-null totalColaboradores |
| `getDashboardWithAreaFilterShouldReturn200ForAdmin` | Alternative | Status 200, dashboard schema |
| `getDashboardShouldReturn200ForColaborador` | Alternative | Status 200, non-null totalColaboradores |
| `getDashboardShouldReturn403WithoutToken` | Exception | Status 403 |

#### `CaramelStrayApplicationSystemTest`

| Test | Validates |
|---|---|
| `contextLoads` | Spring context wires `JdbcTemplate`, confirming database connectivity |

### JSON Schema files

All schema files live in `src/test/resources/schemas/`:

| File | Used by |
|---|---|
| `login-response-schema.json` | `AuthSystemTest.loginShouldReturn200WithTokenAndUserInfo` |
| `funcionario-schema.json` | `AuthSystemTest.getMeShouldReturn200*`, `FuncionarioSystemTest.getByIdShouldReturn200*`, `FuncionarioSystemTest.updateShouldReturn200*` |
| `area-schema.json` | `AreaSystemTest.createAreaShouldReturn201*` |
| `area-list-schema.json` | `AreaSystemTest.getAllAreasShouldReturn200*` |
| `perfil-schema.json` | `PerfilSystemTest.createPerfilShouldReturn201*` |
| `competencia-schema.json` | `CompetenciaSystemTest.getCompetenciaByIdShouldReturn200*`, `createCompetenciaShouldReturn201*` |
| `competencia-list-schema.json` | `CompetenciaSystemTest.listAllCompetenciasShouldReturn200*` |
| `pergunta-schema.json` | `PerguntaSystemTest.createPerguntaShouldReturn201ForAdmin`, `getPerguntaByIdShouldReturn200*` |
| `pergunta-list-schema.json` | `PerguntaSystemTest.listAllPerguntasShouldReturn200*` |
| `avaliacao-schema.json` | `AvaliacaoSystemTest.createAvaliacaoShouldReturn201ForGestor` |
| `avaliacao-list-schema.json` | `AvaliacaoSystemTest.listAvaliacoesShouldReturn200*` |
| `avaliacao-instance-schema.json` | Avaliacao instance responses |
| `dashboard-schema.json` | `DashboardSystemTest.getDashboardShouldReturn200ForAdmin`, `getDashboardWithAreaFilter*`, `getDashboardShouldReturn200ForGestor` |

### How to run

```bash
# Start PostgreSQL first
docker compose up -d --wait

# Run system tests only
mvn test -Psystem-test

# Stop PostgreSQL when done
docker compose down
```

---

## Engineering Decisions

### BaseSystemTest — shared infrastructure

All system test classes extend `BaseSystemTest`, which holds the Spring context, the `JdbcTemplate`, and the shared authentication state.

```
BaseSystemTest  (abstract, @SpringBootTest, @ActiveProfiles("test"), @Tag("system"))
├── AuthSystemTest
├── AreaSystemTest
├── PerfilSystemTest
├── CompetenciaSystemTest
├── PerguntaSystemTest
├── FuncionarioSystemTest
├── AvaliacaoSystemTest
├── DashboardSystemTest
└── CaramelStrayApplicationSystemTest
```

`@SpringBootTest(webEnvironment = RANDOM_PORT)` starts a complete Spring Boot application context on a random port. `@ActiveProfiles("test")` loads `application-test.properties` (database URL, credentials, JWT secret for the test environment). `@Tag("system")` marks all subclasses as system tests without requiring each class to repeat the annotation.

### Static shared state

`adminToken`, `gestorToken`, `colaboradorToken`, `adminId`, `gestorId`, `colaboradorId`, and the base entity IDs are `static` fields. This makes them visible across all subclasses in the same JVM run.

The `@BeforeAll` method in `BaseSystemTest` is guarded by `if (adminToken == null)` — the database truncate and seed runs only once per test suite execution, regardless of how many subclass instances are created. This avoids re-creating the base data (and re-registering the same users) before each test class.

All subclasses are annotated with `@TestInstance(Lifecycle.PER_CLASS)` so that `@BeforeAll` methods can be non-static, which is required for Spring to inject `@Autowired JdbcTemplate` into the base class instance.

The `@SuppressWarnings("java:S2696")` annotation on `createBaseTestData()` suppresses SonarQube rule S2696 (instance method modifying static field). The suppression is legitimate: `@BeforeAll` with `PER_CLASS` lifecycle guarantees single-threaded execution before any test method runs.

### Database bootstrap — the chicken-and-egg problem

`BaseSystemTest` must insert area and profile records before registering test users, and it must register users before obtaining JWT tokens. However, `POST /api/area` and `POST /api/perfil` require an authenticated Bearer token — which does not exist yet.

The solution bypasses the API entirely for this initial data by using `JdbcTemplate` with PostgreSQL's `RETURNING` clause:

```java
baseAreaId = jdbcTemplate.queryForObject(
    "INSERT INTO tb_cad_area (nome, descricao) VALUES (?, ?) RETURNING codigo",
    Integer.class, "Technology", "IT Department");
```

`RETURNING codigo` captures the auto-generated primary key in one round-trip. After area and profiles exist, `POST /api/auth/register` is public (no token required), so the three test users are created via HTTP.

### TRUNCATE with RESTART IDENTITY CASCADE

`resetDatabase()` truncates all tables with `RESTART IDENTITY CASCADE`. The `RESTART IDENTITY` resets PostgreSQL sequences to 1. This makes the first inserted profile get `codigo = 1` (Diretoria → `ROLE_ADMIN`), the second get `codigo = 2` (Supervisao → `ROLE_GESTOR`), and the third get `codigo = 3` (Colaborador → `ROLE_USER`).

This matters because `CustomUserDetails` assigns Spring Security roles based on the numeric profile ID. By controlling the insertion order and resetting the sequence, the test setup guarantees that the role mapping is always deterministic, regardless of what was in the database before the test run.

`CASCADE` ensures that all foreign-key-dependent child records are removed when parent tables are truncated, avoiding constraint violations.

### Test ordering in stateful test classes

`FuncionarioSystemTest`, `CompetenciaSystemTest`, `PerguntaSystemTest`, and `AvaliacaoSystemTest` use `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)`. Some tests in these classes depend on state produced by earlier tests — for example, `removeCertificateShouldReturn204` deletes the certificate whose ID was captured in `addCertificateShouldReturn201`. Ordering ensures that tests run in the expected sequence and that dependent IDs are available.

### Surefire tag exclusion bug

`@Tag("system")` on `BaseSystemTest` marks all subclasses as system tests. The original plan was to use `<excludedGroups>system</excludedGroups>` in the Surefire plugin configuration to exclude them from `mvn verify`. With Surefire 3.5.3 (shipped by Spring Boot 3.5.5) and the JUnit Platform provider, tag-based filtering in the plugin `<configuration>` block applies before test discovery completes, causing the provider to report **zero tests** — including zero unit tests.

The fix was file-pattern exclusion:

```xml
<excludes>
    <exclude>**/*SystemTest.java</exclude>
</excludes>
```

Surefire resolves file patterns before handing control to the JUnit Platform provider, so test discovery is not disrupted. `@Tag("system")` is retained on `BaseSystemTest` as documentation, but the actual exclusion mechanism is the file-pattern exclude.

### JPA cascade save does not populate the in-memory child entity's ID

`FuncionarioService.adicionarCertificado()` and `adicionarExperiencia()` originally saved child entities by adding them to the parent's collection and calling `funcionarioRepository.save(parent)`. When JPA performs a cascade `merge` on a parent entity that has a new (transient) child, it creates a **new managed copy** of the child with the generated ID — but the original local variable still has `getId() == null`. The DTO built from the original variable returned a null `codigo`.

The fix saves the child via its own repository, which calls `persist` directly:

```java
FuncionarioCertificado certificadoSalvo = certificadoRepository.save(novoCertificado);
return new CertificadoDTO(certificadoSalvo);
```

`persist` on a new entity with `GenerationType.IDENTITY` triggers an immediate `INSERT` and sets the generated ID on the entity object in place. The returned `certificadoSalvo` always has a non-null `codigo`.

### Auth endpoint returns 401 (not 403) on invalid credentials

Spring Security's `ExceptionTranslationFilter` wraps the `DispatcherServlet` in the filter chain. When `AuthenticationManager.authenticate()` throws `BadCredentialsException` (a subtype of `AuthenticationException`) without being caught in the controller, the filter intercepts it and uses the configured authentication entry point — which by default returns 403 (Forbidden) instead of 401 (Unauthorized).

The fix catches `AuthenticationException` inside the controller method and returns `new ResponseEntity<>(HttpStatus.UNAUTHORIZED)` directly. Because the response is written from within the controller, it never propagates to the security filter:

```java
@PostMapping("/login")
public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO request) {
    try {
        Authentication authentication = authenticationManager.authenticate(...);
        ...
        return ResponseEntity.ok(new LoginResponseDTO(...));
    } catch (AuthenticationException e) {
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
}
```

Using `new ResponseEntity<>(HttpStatus.UNAUTHORIZED)` instead of `ResponseEntity<?>` avoids SonarQube rule S1452 (generic wildcard return type). Java infers `<LoginResponseDTO>` from the declared method return type.
