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
class CompetenciaSystemTest extends BaseSystemTest {

    private static final String COMPETENCIA_URL = "/api/competencia";
    private static final String NONEXISTENT_ID  = "/999999";

    private Integer testCompetenciaId;

    @BeforeAll
    void createTestCompetencia() {
        testCompetenciaId = asAdmin()
            .body("{\"nome\":\"Java Programming\",\"categoria\":\"Technical\"}")
            .when().post(COMPETENCIA_URL)
            .then().statusCode(201)
            .extract().path("id");
    }

    // ─── Main Flows ───────────────────────────────────────────────────────────

    @Test
    @Order(1)
    void listAllCompetenciasShouldReturn200WithList() {
        asAdmin()
            .when().get(COMPETENCIA_URL)
            .then()
            .statusCode(200)
            .body(matchesJsonSchemaInClasspath("schemas/competencia-list-schema.json"))
            .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    @Order(2)
    void getCompetenciaByIdShouldReturn200WithCorrectData() {
        asAdmin()
            .when().get(COMPETENCIA_URL + "/" + testCompetenciaId)
            .then()
            .statusCode(200)
            .body(matchesJsonSchemaInClasspath("schemas/competencia-schema.json"))
            .body("id", equalTo(testCompetenciaId))
            .body("nome", equalTo("Java Programming"))
            .body("categoria", equalTo("Technical"));
    }

    @Test
    @Order(3)
    void createCompetenciaShouldReturn201WithCreatedData() {
        asAdmin()
            .body("{\"nome\":\"Agile Methodology\",\"categoria\":\"Process\"}")
            .when().post(COMPETENCIA_URL)
            .then()
            .statusCode(201)
            .body(matchesJsonSchemaInClasspath("schemas/competencia-schema.json"))
            .body("nome", equalTo("Agile Methodology"))
            .body("id", notNullValue());
    }

    @Test
    @Order(4)
    void updateCompetenciaShouldReturn200WithUpdatedData() {
        asAdmin()
            .body("{\"nome\":\"Java Programming Advanced\",\"categoria\":\"Technical\"}")
            .when().put(COMPETENCIA_URL + "/" + testCompetenciaId)
            .then()
            .statusCode(200)
            .body("nome", equalTo("Java Programming Advanced"))
            .body("id", equalTo(testCompetenciaId));
    }

    // ─── Alternative Flows ────────────────────────────────────────────────────

    @Test
    @Order(5)
    void createCompetenciaShouldReturn400ForDuplicateName() {
        asAdmin()
            .body("{\"nome\":\"Java Programming Advanced\",\"categoria\":\"Technical\"}")
            .when().post(COMPETENCIA_URL)
            .then()
            .statusCode(400);
    }

    // ─── Exception Flows ──────────────────────────────────────────────────────

    @Test
    @Order(6)
    void getCompetenciaByIdShouldReturn404WhenNotFound() {
        asAdmin()
            .when().get(COMPETENCIA_URL + NONEXISTENT_ID)
            .then()
            .statusCode(404);
    }

    @Test
    @Order(7)
    void updateCompetenciaShouldReturn404WhenNotFound() {
        asAdmin()
            .body("{\"nome\":\"Ghost\",\"categoria\":\"None\"}")
            .when().put(COMPETENCIA_URL + NONEXISTENT_ID)
            .then()
            .statusCode(404);
    }

    @Test
    @Order(8)
    void deleteCompetenciaShouldReturn204WhenFound() {
        Integer idToDelete = asAdmin()
            .body("{\"nome\":\"ToDeleteCompetencia\",\"categoria\":\"Temp\"}")
            .when().post(COMPETENCIA_URL)
            .then().statusCode(201)
            .extract().path("id");

        asAdmin()
            .when().delete(COMPETENCIA_URL + "/" + idToDelete)
            .then()
            .statusCode(204);
    }

    @Test
    @Order(9)
    void deleteCompetenciaShouldReturn404WhenNotFound() {
        asAdmin()
            .when().delete(COMPETENCIA_URL + NONEXISTENT_ID)
            .then()
            .statusCode(404);
    }
}
