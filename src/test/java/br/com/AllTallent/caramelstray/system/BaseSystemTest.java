package br.com.AllTallent.caramelstray.system;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("system")
public abstract class BaseSystemTest {

    private static final String PERFIL_INSERT_SQL =
        "INSERT INTO tb_cad_perfil (nome, descricao) VALUES (?, ?) RETURNING codigo";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @LocalServerPort
    protected int port;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected static String adminToken;
    protected static String gestorToken;
    protected static String colaboradorToken;
    protected static Integer baseAreaId;
    protected static Integer basePerfilAdminId;
    protected static Integer basePerfilGestorId;
    protected static Integer basePerfilColabId;
    protected static Integer adminId;
    protected static Integer gestorId;
    protected static Integer colaboradorId;

    @BeforeAll
    void configureRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        if (adminToken == null) {
            resetDatabase();
            createBaseTestData();
        }
    }

    private void resetDatabase() {
        jdbcTemplate.execute(
            "TRUNCATE TABLE " +
            "tb_cad_resposta_colaborador, " +
            "tb_cad_funcionario_avalicacao, " +
            "tb_cad_avaliacao_pergunta, " +
            "tb_cad_avaliacao, " +
            "tb_cad_pergunta_opcao, " +
            "tb_cad_pergunta, " +
            "tb_cad_funcionario_competencia, " +
            "tb_cad_funcionario_certificado, " +
            "tb_cad_funcionario_historico_experiencia, " +
            "tb_cad_funcionario, " +
            "tb_cad_competencia, " +
            "tb_cad_area, " +
            "tb_cad_perfil " +
            "RESTART IDENTITY CASCADE"
        );
    }

    // Area and Perfil endpoints require authentication, but no token exists at setup time.
    // Insert base data directly via JdbcTemplate so sequences reset to predictable IDs:
    // perfilId 1=Diretoria (ADMIN), 2=Supervisao (GESTOR), 3=Colaborador (USER) —
    // matching the hardcoded role mapping in CustomUserDetails.
    @SuppressWarnings("java:S2696")
    private void createBaseTestData() {
        baseAreaId = jdbcTemplate.queryForObject(
            "INSERT INTO tb_cad_area (nome, descricao) VALUES (?, ?) RETURNING codigo",
            Integer.class, "Technology", "IT Department");

        basePerfilAdminId  = jdbcTemplate.queryForObject(PERFIL_INSERT_SQL, Integer.class, "Diretoria",  "Admin profile");
        basePerfilGestorId = jdbcTemplate.queryForObject(PERFIL_INSERT_SQL, Integer.class, "Supervisao", "Manager profile");
        basePerfilColabId  = jdbcTemplate.queryForObject(PERFIL_INSERT_SQL, Integer.class, "Colaborador","Employee profile");

        register("Admin E2E", "admin@e2e.com", "Admin@2024", "E2E-ADM-001", "111.111.111-11", baseAreaId, basePerfilAdminId);
        adminToken = loginAndGetToken("admin@e2e.com", "Admin@2024");
        adminId    = getLoggedInUserId(adminToken);

        register("Gestor E2E", "gestor@e2e.com", "Gestor@2024", "E2E-GST-001", "222.222.222-22", baseAreaId, basePerfilGestorId);
        gestorToken = loginAndGetToken("gestor@e2e.com", "Gestor@2024");
        gestorId    = getLoggedInUserId(gestorToken);

        register("Colaborador E2E", "colab@e2e.com", "Colab@2024", "E2E-COL-001", "333.333.333-33", baseAreaId, basePerfilColabId);
        colaboradorToken = loginAndGetToken("colab@e2e.com", "Colab@2024");
        colaboradorId    = getLoggedInUserId(colaboradorToken);
    }

    protected void register(String nome, String email, String senha, String cracha, String cpf,
                            Integer areaId, Integer perfilId) {
        given().contentType(ContentType.JSON)
            .body(String.format(
                "{\"nomeCompleto\":\"%s\",\"email\":\"%s\",\"senha\":\"%s\"," +
                "\"idCracha\":\"%s\",\"cpf\":\"%s\",\"codigoArea\":%d,\"codigoPerfil\":%d}",
                nome, email, senha, cracha, cpf, areaId, perfilId))
            .when().post("/api/auth/register")
            .then().statusCode(201);
    }

    protected String loginAndGetToken(String email, String password) {
        return given().contentType(ContentType.JSON)
            .body(String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password))
            .when().post("/api/auth/login")
            .then().statusCode(200)
            .extract().path("token");
    }

    protected Integer getLoggedInUserId(String token) {
        return given()
            .header(AUTH_HEADER, BEARER_PREFIX + token)
            .when().get("/api/auth/me")
            .then().statusCode(200)
            .extract().path("codigo");
    }

    protected RequestSpecification asAdmin() {
        return given().contentType(ContentType.JSON)
            .header(AUTH_HEADER, BEARER_PREFIX + adminToken);
    }

    protected RequestSpecification asGestor() {
        return given().contentType(ContentType.JSON)
            .header(AUTH_HEADER, BEARER_PREFIX + gestorToken);
    }

    protected RequestSpecification asColaborador() {
        return given().contentType(ContentType.JSON)
            .header(AUTH_HEADER, BEARER_PREFIX + colaboradorToken);
    }

    protected RequestSpecification anonymous() {
        return given().contentType(ContentType.JSON);
    }
}
