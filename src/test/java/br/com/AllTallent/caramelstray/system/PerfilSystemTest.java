package br.com.AllTallent.caramelstray.system;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PerfilSystemTest extends BaseSystemTest {

    private static final String PERFIL_URL = "/api/perfil";

    // Main Flows

    @Test
    void createPerfilShouldReturn201WithCreatedPerfil() {
        asAdmin()
            .body("{\"nome\":\"Analyst\",\"descricao\":\"Business analyst profile\"}")
            .when().post(PERFIL_URL)
            .then()
            .statusCode(201)
            .body(matchesJsonSchemaInClasspath("schemas/perfil-schema.json"))
            .body("nome", equalTo("Analyst"))
            .body("codigo", notNullValue());
    }

    @Test
    void getAllPerfisShouldReturn200WithNonEmptyList() {
        asAdmin()
            .when().get(PERFIL_URL)
            .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1))
            .body("[0].codigo", notNullValue())
            .body("[0].nome", not(emptyOrNullString()));
    }

    // Alternative Flows

    @Test
    void createPerfilWithoutDescricaoShouldReturn201() {
        asAdmin()
            .body("{\"nome\":\"Intern\"}")
            .when().post(PERFIL_URL)
            .then()
            .statusCode(201)
            .body("nome", equalTo("Intern"));
    }

    @Test
    void getAllPerfisShouldContainBaseProfiles() {
        asAdmin()
            .when().get(PERFIL_URL)
            .then()
            .statusCode(200)
            .body("nome", hasItems("Diretoria", "Supervisao", "Colaborador"));
    }

    // Exception Flows

    @Test
    void createPerfilWithNullNameShouldNotReturn500() {
        int status = asAdmin()
            .body("{}")
            .when().post(PERFIL_URL)
            .then()
            .extract().statusCode();

        org.junit.jupiter.api.Assertions.assertTrue(
            status == 201 || status == 400,
            "Unexpected status: " + status
        );
    }
}
