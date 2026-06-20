package br.com.AllTallent.caramelstray.system;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DashboardSystemTest extends BaseSystemTest {

    private static final String DASHBOARD_URL = "/api/dashboard";

    // Main Flows

    @Test
    void getDashboardShouldReturn200ForAdmin() {
        asAdmin()
            .when().get(DASHBOARD_URL)
            .then()
            .statusCode(200)
            .body(matchesJsonSchemaInClasspath("schemas/dashboard-schema.json"))
            .body("totalColaboradores", notNullValue());
    }

    @Test
    void getDashboardShouldReturn200ForGestor() {
        asGestor()
            .when().get(DASHBOARD_URL)
            .then()
            .statusCode(200)
            .body(matchesJsonSchemaInClasspath("schemas/dashboard-schema.json"))
            .body("totalColaboradores", notNullValue());
    }

    // Alternative Flows

    @Test
    void getDashboardWithAreaFilterShouldReturn200ForAdmin() {
        asAdmin()
            .when().get(DASHBOARD_URL + "?codigoArea=" + baseAreaId)
            .then()
            .statusCode(200)
            .body(matchesJsonSchemaInClasspath("schemas/dashboard-schema.json"));
    }

    @Test
    void getDashboardShouldReturn200ForColaborador() {
        asColaborador()
            .when().get(DASHBOARD_URL)
            .then()
            .statusCode(200)
            .body("totalColaboradores", notNullValue());
    }

    // Exception Flows

    @Test
    void getDashboardShouldReturn403WithoutToken() {
        anonymous()
            .when().get(DASHBOARD_URL)
            .then()
            .statusCode(403);
    }
}
