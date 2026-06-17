package br.com.AllTallent.caramelstray.system;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FuncionarioSystemTest extends BaseSystemTest {

    private static final String FUNCIONARIO_URL  = "/api/funcionario";
    private static final String PATH_COMPETENCIAS = "/competencias";
    private static final String PATH_EXPERIENCIAS = "/experiencias";
    private static final String PATH_CERTIFICADOS = "/certificados";
    private static final String FIELD_CODIGO      = "codigo";

    private Integer competenciaId;
    private Integer certificadoId;
    private Integer experienciaId;

    @BeforeAll
    void createTestDependencies() {
        competenciaId = asAdmin()
            .body("{\"nome\":\"Communication\",\"categoria\":\"Soft Skills\"}")
            .when().post("/api/competencia")
            .then().statusCode(201)
            .extract().path("id");
    }

    // ─── Main Flows ───────────────────────────────────────────────────────────

    @Test
    @Order(1)
    void listAllShouldReturn200WithEmployeeListForAdmin() {
        asAdmin()
            .when().get(FUNCIONARIO_URL)
            .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1))
            .body("[0]." + FIELD_CODIGO, notNullValue())
            .body("[0].nomeCompleto", not(emptyOrNullString()));
    }

    @Test
    @Order(2)
    void getByIdShouldReturn200WithCorrectEmployeeForAdmin() {
        asAdmin()
            .when().get(FUNCIONARIO_URL + "/" + adminId)
            .then()
            .statusCode(200)
            .body(matchesJsonSchemaInClasspath("schemas/funcionario-schema.json"))
            .body(FIELD_CODIGO, equalTo(adminId))
            .body("nomeCompleto", equalTo("Admin E2E"))
            .body("email", equalTo("admin@e2e.com"));
    }

    @Test
    @Order(3)
    void getByIdShouldReturn200WhenEmployeeAccessesOwnProfile() {
        asColaborador()
            .when().get(FUNCIONARIO_URL + "/" + colaboradorId)
            .then()
            .statusCode(200)
            .body(FIELD_CODIGO, equalTo(colaboradorId));
    }

    @Test
    @Order(4)
    void getProfileByIdShouldReturn200ForAdmin() {
        asAdmin()
            .when().get(FUNCIONARIO_URL + "/" + colaboradorId + "/perfil")
            .then()
            .statusCode(200)
            .body(FIELD_CODIGO, notNullValue());
    }

    @Test
    @Order(5)
    void updateShouldReturn200WhenEmployeeUpdatesSelf() {
        asColaborador()
            .body("{\"nomeCompleto\":\"Colaborador E2E Updated\",\"email\":\"colab@e2e.com\"," +
                  "\"cpf\":\"333.333.333-33\",\"areaId\":" + baseAreaId + ",\"perfilId\":" + basePerfilColabId + "}")
            .when().put(FUNCIONARIO_URL + "/" + colaboradorId)
            .then()
            .statusCode(200)
            .body(matchesJsonSchemaInClasspath("schemas/funcionario-schema.json"))
            .body("nomeCompleto", equalTo("Colaborador E2E Updated"));
    }

    @Test
    @Order(6)
    void addCertificateShouldReturn201() {
        certificadoId = asAdmin()
            .body("{\"nome\":\"AWS Certified Developer\"}")
            .when().post(FUNCIONARIO_URL + "/" + adminId + PATH_CERTIFICADOS)
            .then()
            .statusCode(201)
            .body("nome", equalTo("AWS Certified Developer"))
            .body(FIELD_CODIGO, notNullValue())
            .extract().path(FIELD_CODIGO);
    }

    @Test
    @Order(7)
    void removeCertificateShouldReturn204() {
        asAdmin()
            .when().delete(FUNCIONARIO_URL + PATH_CERTIFICADOS + "/" + certificadoId)
            .then()
            .statusCode(204);
    }

    @Test
    @Order(8)
    void getCompetenciasShouldReturn200() {
        asAdmin()
            .when().get(FUNCIONARIO_URL + "/" + adminId + PATH_COMPETENCIAS)
            .then()
            .statusCode(200)
            .body("funcionarioCodigo", equalTo(adminId));
    }

    @Test
    @Order(9)
    void updateCompetenciasShouldReturn204() {
        asAdmin()
            .body("{\"codigosCompetencia\":[" + competenciaId + "]}")
            .when().put(FUNCIONARIO_URL + "/" + adminId + PATH_COMPETENCIAS)
            .then()
            .statusCode(204);
    }

    @Test
    @Order(10)
    void getExperienciasShouldReturn200() {
        asAdmin()
            .when().get(FUNCIONARIO_URL + "/" + adminId + PATH_EXPERIENCIAS)
            .then()
            .statusCode(200)
            .body("funcionarioCodigo", equalTo(adminId));
    }

    @Test
    @Order(11)
    void addExperienciaShouldReturn201() {
        experienciaId = asAdmin()
            .body("{\"cargo\":\"Software Engineer\",\"empresa\":\"TechCorp\"," +
                  "\"dataInicio\":\"2020-01-01\",\"descricao\":\"Backend development\"}")
            .when().post(FUNCIONARIO_URL + "/" + adminId + PATH_EXPERIENCIAS)
            .then()
            .statusCode(201)
            .body("cargo", equalTo("Software Engineer"))
            .body(FIELD_CODIGO, notNullValue())
            .extract().path(FIELD_CODIGO);
    }

    @Test
    @Order(12)
    void updateExperienciaShouldReturn200() {
        asAdmin()
            .body("{\"cargo\":\"Senior Software Engineer\",\"empresa\":\"TechCorp\"," +
                  "\"dataInicio\":\"2020-01-01\",\"descricao\":\"Senior backend development\"}")
            .when().put(FUNCIONARIO_URL + PATH_EXPERIENCIAS + "/" + experienciaId)
            .then()
            .statusCode(200)
            .body("cargo", equalTo("Senior Software Engineer"));
    }

    @Test
    @Order(13)
    void listAllWithTextFilterShouldReturn200() {
        asAdmin()
            .when().get(FUNCIONARIO_URL + "?texto=Admin")
            .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1));
    }

    // ─── Alternative Flows ────────────────────────────────────────────────────

    @Test
    @Order(14)
    void getByIdShouldReturn200WhenGestorAccessesAnyProfile() {
        asGestor()
            .when().get(FUNCIONARIO_URL + "/" + colaboradorId)
            .then()
            .statusCode(200)
            .body(FIELD_CODIGO, equalTo(colaboradorId));
    }

    @Test
    @Order(15)
    void deleteEmployeeShouldReturn204ForAdmin() {
        register("ToDelete E2E", "todelete@e2e.com", "Delete@2024",
                 "E2E-DEL-001", "666.666.666-66", baseAreaId, basePerfilColabId);
        Integer deleteId = getLoggedInUserId(loginAndGetToken("todelete@e2e.com", "Delete@2024"));

        asAdmin()
            .when().delete(FUNCIONARIO_URL + "/" + deleteId)
            .then()
            .statusCode(204);
    }

    // ─── Exception Flows ──────────────────────────────────────────────────────

    @Test
    @Order(16)
    void listAllShouldReturn403WithoutToken() {
        anonymous()
            .when().get(FUNCIONARIO_URL)
            .then()
            .statusCode(403);
    }

    @Test
    @Order(17)
    void createEmployeeShouldReturn403ForColaborador() {
        asColaborador()
            .body("{\"nomeCompleto\":\"Unauthorized\",\"email\":\"unauth@e2e.com\"}")
            .when().post(FUNCIONARIO_URL)
            .then()
            .statusCode(403);
    }

    @Test
    @Order(18)
    void updateShouldReturn403WhenColaboradorAttemptsToUpdateOtherEmployee() {
        asColaborador()
            .body("{\"nomeCompleto\":\"Hack Attempt\",\"email\":\"colab@e2e.com\"," +
                  "\"cpf\":\"333.333.333-33\",\"areaId\":" + baseAreaId + ",\"perfilId\":" + basePerfilColabId + "}")
            .when().put(FUNCIONARIO_URL + "/" + adminId)
            .then()
            .statusCode(403);
    }

    @Test
    @Order(19)
    void getCompetenciasShouldReturn403WhenColaboradorAccessesOtherProfile() {
        asColaborador()
            .when().get(FUNCIONARIO_URL + "/" + adminId + PATH_COMPETENCIAS)
            .then()
            .statusCode(403);
    }
}
