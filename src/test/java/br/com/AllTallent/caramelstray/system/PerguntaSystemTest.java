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
class PerguntaSystemTest extends BaseSystemTest {

    private static final String PERGUNTAS_URL    = "/api/perguntas";
    private static final String NONEXISTENT_ID   = "/999999";
    private static final String CONFLICT_QUESTION = "How do you handle conflict?";
    private static final String FIELD_PERGUNTA   = "pergunta";
    private static final String FIELD_CODIGO     = "codigo";

    private Integer competenciaId;
    private Long perguntaId;

    @BeforeAll
    void createTestDependencies() {
        competenciaId = asAdmin()
            .body("{\"nome\":\"Leadership Skills\",\"categoria\":\"Soft Skills\"}")
            .when().post("/api/competencia")
            .then().statusCode(201)
            .extract().path("id");
    }

    // ─── Main Flows ───────────────────────────────────────────────────────────

    @Test
    @Order(1)
    void createPerguntaShouldReturn201ForAdmin() {
        perguntaId = asAdmin()
            .body(String.format(
                "{\"pergunta\":\"%s\",\"competenciaCodigo\":%d}", CONFLICT_QUESTION, competenciaId))
            .when().post(PERGUNTAS_URL)
            .then()
            .statusCode(201)
            .body(matchesJsonSchemaInClasspath("schemas/pergunta-schema.json"))
            .body(FIELD_PERGUNTA, equalTo(CONFLICT_QUESTION))
            .body("competenciaCodigo", equalTo(competenciaId))
            .body(FIELD_CODIGO, notNullValue())
            .extract().<Integer>path(FIELD_CODIGO).longValue();
    }

    @Test
    @Order(2)
    void createPerguntaShouldReturn201ForGestor() {
        asGestor()
            .body(String.format(
                "{\"pergunta\":\"Describe your leadership style.\",\"competenciaCodigo\":%d}", competenciaId))
            .when().post(PERGUNTAS_URL)
            .then()
            .statusCode(201)
            .body(FIELD_PERGUNTA, equalTo("Describe your leadership style."));
    }

    @Test
    @Order(3)
    void listAllPerguntasShouldReturn200WithList() {
        asAdmin()
            .when().get(PERGUNTAS_URL)
            .then()
            .statusCode(200)
            .body(matchesJsonSchemaInClasspath("schemas/pergunta-list-schema.json"))
            .body("size()", greaterThanOrEqualTo(1))
            .body("[0]." + FIELD_CODIGO, notNullValue());
    }

    @Test
    @Order(4)
    void getPerguntaByIdShouldReturn200WithCorrectData() {
        asAdmin()
            .when().get(PERGUNTAS_URL + "/" + perguntaId)
            .then()
            .statusCode(200)
            .body(matchesJsonSchemaInClasspath("schemas/pergunta-schema.json"))
            .body(FIELD_CODIGO, equalTo(perguntaId.intValue()))
            .body(FIELD_PERGUNTA, equalTo(CONFLICT_QUESTION))
            .body("competenciaCodigo", equalTo(competenciaId));
    }

    // ─── Alternative Flows ────────────────────────────────────────────────────

    @Test
    @Order(5)
    void listPerguntasShouldReturn200WithExpectedQuestion() {
        asGestor()
            .when().get(PERGUNTAS_URL)
            .then()
            .statusCode(200)
            .body(FIELD_PERGUNTA, hasItem(CONFLICT_QUESTION));
    }

    // ─── Exception Flows ──────────────────────────────────────────────────────

    @Test
    @Order(6)
    void getPerguntaByIdShouldReturn404WhenNotFound() {
        asAdmin()
            .when().get(PERGUNTAS_URL + NONEXISTENT_ID)
            .then()
            .statusCode(404);
    }

    @Test
    @Order(7)
    void createPerguntaShouldReturn403ForColaborador() {
        asColaborador()
            .body(String.format(
                "{\"pergunta\":\"Unauthorized question.\",\"competenciaCodigo\":%d}", competenciaId))
            .when().post(PERGUNTAS_URL)
            .then()
            .statusCode(403);
    }

    @Test
    @Order(8)
    void createPerguntaShouldReturn400WhenCompetenciaNotFound() {
        asAdmin()
            .body("{\"pergunta\":\"Question with invalid competencia.\",\"competenciaCodigo\":999999}")
            .when().post(PERGUNTAS_URL)
            .then()
            .statusCode(400);
    }

    @Test
    @Order(9)
    void deletePerguntaShouldReturn204WhenFound() {
        Long idToDelete = asAdmin()
            .body(String.format(
                "{\"pergunta\":\"ToDeletePergunta\",\"competenciaCodigo\":%d}", competenciaId))
            .when().post(PERGUNTAS_URL)
            .then().statusCode(201)
            .extract().<Integer>path(FIELD_CODIGO).longValue();

        asAdmin()
            .when().delete(PERGUNTAS_URL + "/" + idToDelete)
            .then()
            .statusCode(204);
    }

    @Test
    @Order(10)
    void deletePerguntaShouldReturn404WhenNotFound() {
        asAdmin()
            .when().delete(PERGUNTAS_URL + NONEXISTENT_ID)
            .then()
            .statusCode(404);
    }
}
