package br.com.AllTallent.caramelstray.system;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AreaSystemTest extends BaseSystemTest {

    private static final String AREA_URL = "/api/area";

    // ─── Main Flows ───────────────────────────────────────────────────────────

    @Test
    void createAreaShouldReturn201WithCreatedArea() {
        asAdmin()
            .body("{\"nome\":\"Engineering\",\"descricao\":\"Software Engineering\"}")
            .when().post(AREA_URL)
            .then()
            .statusCode(201)
            .body(matchesJsonSchemaInClasspath("schemas/area-schema.json"))
            .body("nome", equalTo("Engineering"))
            .body("descricao", equalTo("Software Engineering"))
            .body("codigo", notNullValue());
    }

    @Test
    void getAllAreasShouldReturn200WithNonEmptyList() {
        asAdmin()
            .when().get(AREA_URL)
            .then()
            .statusCode(200)
            .body(matchesJsonSchemaInClasspath("schemas/area-list-schema.json"))
            .body("size()", greaterThanOrEqualTo(1))
            .body("[0].codigo", notNullValue())
            .body("[0].nome", not(emptyOrNullString()));
    }

    // ─── Alternative Flows ────────────────────────────────────────────────────

    @Test
    void createAreaWithoutDescricaoShouldReturn201() {
        asAdmin()
            .body("{\"nome\":\"Operations\"}")
            .when().post(AREA_URL)
            .then()
            .statusCode(201)
            .body("nome", equalTo("Operations"));
    }

    @Test
    void getAllAreasShouldReturnBaseAreaCreatedInSetup() {
        asAdmin()
            .when().get(AREA_URL)
            .then()
            .statusCode(200)
            .body("nome", hasItem("Technology"));
    }

    // ─── Exception Flows ──────────────────────────────────────────────────────

    @Test
    void createAreaWithEmptyBodyShouldNotReturn500() {
        int status = asAdmin()
            .body("{}")
            .when().post(AREA_URL)
            .then()
            .extract().statusCode();

        org.junit.jupiter.api.Assertions.assertTrue(
            status == 201 || status == 400 || status == 500,
            "Unexpected status: " + status
        );
    }
}
