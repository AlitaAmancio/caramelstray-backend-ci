package br.com.AllTallent.caramelstray.system;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthSystemTest extends BaseSystemTest {

    private static final String LOGIN_URL         = "/api/auth/login";
    private static final String ME_URL            = "/api/auth/me";
    private static final String REGISTER_URL      = "/api/auth/register";
    private static final String ADMIN_EMAIL       = "admin@e2e.com";
    private static final String FIELD_NOME_COMPLETO = "nomeCompleto";
    private static final String FIELD_CODIGO      = "codigo";

    // Main Flows

    @Test
    void loginShouldReturn200WithTokenAndUserInfo() {
        anonymous()
            .body("{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"Admin@2024\"}")
            .when().post(LOGIN_URL)
            .then()
            .statusCode(200)
            .body(matchesJsonSchemaInClasspath("schemas/login-response-schema.json"))
            .body("token", not(emptyOrNullString()))
            .body("userId", equalTo(adminId))
            .body(FIELD_NOME_COMPLETO, equalTo("Admin E2E"));
    }

    @Test
    void registerShouldReturn201ForValidNewUser() {
        anonymous()
            .body(String.format(
                "{\"nomeCompleto\":\"New User E2E\",\"email\":\"newuser@e2e.com\"," +
                "\"senha\":\"NewUser@2024\",\"idCracha\":\"E2E-NEW-001\"," +
                "\"cpf\":\"444.444.444-44\",\"codigoArea\":%d,\"codigoPerfil\":%d}",
                baseAreaId, basePerfilColabId))
            .when().post(REGISTER_URL)
            .then()
            .statusCode(201);
    }

    @Test
    void getMeShouldReturn200WithAuthenticatedUserProfile() {
        asAdmin()
            .when().get(ME_URL)
            .then()
            .statusCode(200)
            .body(matchesJsonSchemaInClasspath("schemas/funcionario-schema.json"))
            .body(FIELD_CODIGO, equalTo(adminId))
            .body(FIELD_NOME_COMPLETO, equalTo("Admin E2E"))
            .body("email", equalTo(ADMIN_EMAIL));
    }

    // Alternative Flows

    @Test
    void getMeShouldReturn200WithGestorProfile() {
        asGestor()
            .when().get(ME_URL)
            .then()
            .statusCode(200)
            .body(FIELD_CODIGO, equalTo(gestorId))
            .body(FIELD_NOME_COMPLETO, equalTo("Gestor E2E"));
    }

    @Test
    void registerShouldReturn400ForDuplicateEmail() {
        anonymous()
            .body(String.format(
                "{\"nomeCompleto\":\"Dup\",\"email\":\"" + ADMIN_EMAIL + "\",\"senha\":\"Admin@2024\"," +
                "\"idCracha\":\"DUP-001\",\"cpf\":\"999.999.999-99\"," +
                "\"codigoArea\":%d,\"codigoPerfil\":%d}", baseAreaId, basePerfilColabId))
            .when().post(REGISTER_URL)
            .then()
            .statusCode(400);
    }

    // Exception Flows

    @Test
    void loginShouldReturn401ForWrongPassword() {
        anonymous()
            .body("{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"WrongPass\"}")
            .when().post(LOGIN_URL)
            .then()
            .statusCode(401);
    }

    @Test
    void loginShouldReturn401ForUnknownEmail() {
        anonymous()
            .body("{\"email\":\"nobody@e2e.com\",\"password\":\"pass\"}")
            .when().post(LOGIN_URL)
            .then()
            .statusCode(401);
    }

    @Test
    void getMeShouldReturn403WithoutToken() {
        anonymous()
            .when().get(ME_URL)
            .then()
            .statusCode(403);
    }
}
