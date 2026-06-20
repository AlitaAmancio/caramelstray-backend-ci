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
class AvaliacaoSystemTest extends BaseSystemTest {

    private static final String AVALIACOES_URL     = "/api/avaliacoes";
    private static final String INSTANCIAS_PATH    = "/instancias";
    private static final String INSTANCIA_OPS      = "/api/avaliacoes/instancias/";
    private static final String PENDENTES_PATH      = "/pendentes/";
    private static final String RESPOSTAS_PATH      = "/respostas";
    private static final String REVISAO_PATH        = "/revisao/";
    private static final String PAYLOAD_TAIL       =
        "\"codigosFuncionarios\":[%d],\"codigosPerguntas\":[%d]}";
    private static final String FIELD_CODIGO       = "codigo";
    private static final String FIELD_TITULO       = "titulo";
    private static final String SIZE_EXPR          = "size()";

    private Integer avaliacaoId;
    private Long instanciaId;
    private Long perguntaId;

    // sets up a full evaluation chain: competencia, pergunta, avaliacao, and fetches the generated instance id
    @BeforeAll
    void createTestDependencies() {
        Integer competenciaId = asAdmin()
            .body("{\"nome\":\"Problem Solving\",\"categoria\":\"Analytical\"}")
            .when().post("/api/competencia")
            .then().statusCode(201)
            .extract().path("id");

        perguntaId = asAdmin()
            .body(String.format(
                "{\"pergunta\":\"Rate your problem solving skills.\",\"competenciaCodigo\":%d}",
                competenciaId))
            .when().post("/api/perguntas")
            .then().statusCode(201)
            .extract().<Integer>path(FIELD_CODIGO).longValue();

        avaliacaoId = asAdmin()
            .body(String.format(
                "{\"titulo\":\"Q4 2024 Evaluation\",\"dataPrazo\":\"2026-12-31\"," + PAYLOAD_TAIL,
                colaboradorId, perguntaId))
            .when().post(AVALIACOES_URL)
            .then().statusCode(201)
            .extract().path(FIELD_CODIGO);

        instanciaId = asAdmin()
            .when().get(AVALIACOES_URL + "/" + avaliacaoId + INSTANCIAS_PATH)
            .then().statusCode(200)
            .extract().<Integer>path("[0]." + FIELD_CODIGO).longValue();
    }

    // Main Flows

    @Test
    @Order(1)
    void listAvaliacoesShouldReturn200WithList() {
        asAdmin()
            .when().get(AVALIACOES_URL)
            .then()
            .statusCode(200)
            .body(matchesJsonSchemaInClasspath("schemas/avaliacao-list-schema.json"))
            .body(SIZE_EXPR, greaterThanOrEqualTo(1))
            .body("[0]." + FIELD_TITULO, not(emptyOrNullString()));
    }

    @Test
    @Order(2)
    void getAvaliacaoByIdShouldReturn200WithCorrectData() {
        asAdmin()
            .when().get(AVALIACOES_URL + "/" + avaliacaoId)
            .then()
            .statusCode(200)
            .body(FIELD_TITULO, equalTo("Q4 2024 Evaluation"))
            .body(FIELD_CODIGO, equalTo(avaliacaoId));
    }

    @Test
    @Order(3)
    void getInstanciasByAvaliacaoShouldReturn200WithList() {
        asAdmin()
            .when().get(AVALIACOES_URL + "/" + avaliacaoId + INSTANCIAS_PATH)
            .then()
            .statusCode(200)
            .body(SIZE_EXPR, equalTo(1))
            .body("[0].funcionarioCodigo", equalTo(colaboradorId));
    }

    @Test
    @Order(4)
    void getAvaliacaoPendentesShouldReturn200ForColaborador() {
        asColaborador()
            .when().get(AVALIACOES_URL + PENDENTES_PATH + colaboradorId)
            .then()
            .statusCode(200)
            .body(SIZE_EXPR, greaterThanOrEqualTo(1));
    }

    @Test
    @Order(5)
    void getAvaliacaoParaResponderShouldReturn200() {
        asColaborador()
            .when().get(INSTANCIA_OPS + instanciaId + "/responder")
            .then()
            .statusCode(200)
            .body("perguntas", notNullValue());
    }

    @Test
    @Order(6)
    void saveAnswerShouldReturn200() {
        asColaborador()
            .body(String.format(
                "{\"funcionarioAvaliacaoCodigo\":%d,\"perguntaCodigo\":%d," +
                "\"respostaTexto\":\"I rate myself 4 out of 5\",\"opcaoSelecionadaCodigo\":null}",
                instanciaId, perguntaId))
            .when().post(AVALIACOES_URL + RESPOSTAS_PATH)
            .then()
            .statusCode(200);
    }

    @Test
    @Order(7)
    void getAnswersByInstanciaShouldReturn200() {
        asAdmin()
            .when().get(INSTANCIA_OPS + instanciaId + RESPOSTAS_PATH)
            .then()
            .statusCode(200)
            .body(SIZE_EXPR, greaterThanOrEqualTo(1));
    }

    @Test
    @Order(8)
    void getRevisaoShouldReturn200ForAdmin() {
        asAdmin()
            .when().get(AVALIACOES_URL + REVISAO_PATH + instanciaId)
            .then()
            .statusCode(200);
    }

    @Test
    @Order(9)
    void finalizeAvaliacaoShouldReturn204ForColaborador() {
        asColaborador()
            .when().put(INSTANCIA_OPS + instanciaId + "/finalizar")
            .then()
            .statusCode(204);
    }

    @Test
    @Order(10)
    void saveRevisaoShouldReturn200ForAdmin() {
        asAdmin()
            .body("{\"comentarioSupervisao\":\"Good performance overall.\"," +
                  "\"comentarioParaColaborador\":\"Keep up the good work!\"," +
                  "\"resultadoStatus\":\"APROVADO\"}")
            .when().put(INSTANCIA_OPS + instanciaId + "/revisar")
            .then()
            .statusCode(200)
            .body("resultadoStatus", equalTo("APROVADO"));
    }

    // Alternative Flows

    @Test
    @Order(11)
    void createAvaliacaoShouldReturn201ForGestor() {
        Integer secondAvaliacaoId = asGestor()
            .body(String.format(
                "{\"titulo\":\"Gestor Evaluation\",\"dataPrazo\":\"2026-12-31\"," + PAYLOAD_TAIL,
                colaboradorId, perguntaId))
            .when().post(AVALIACOES_URL)
            .then()
            .statusCode(201)
            .body(matchesJsonSchemaInClasspath("schemas/avaliacao-schema.json"))
            .body(FIELD_TITULO, equalTo("Gestor Evaluation"))
            .extract().path(FIELD_CODIGO);

        org.junit.jupiter.api.Assertions.assertNotNull(secondAvaliacaoId);
    }

    // Exception Flows

    @Test
    @Order(12)
    void createAvaliacaoShouldReturn403ForColaborador() {
        asColaborador()
            .body(String.format(
                "{\"titulo\":\"Unauthorized\",\"dataPrazo\":\"2026-12-31\"," + PAYLOAD_TAIL,
                colaboradorId, perguntaId))
            .when().post(AVALIACOES_URL)
            .then()
            .statusCode(403);
    }

    @Test
    @Order(13)
    void getAvaliacaoByIdShouldReturn404WhenNotFound() {
        asAdmin()
            .when().get(AVALIACOES_URL + "/999999")
            .then()
            .statusCode(404);
    }

    @Test
    @Order(14)
    void saveAnswerShouldReturn400ForInvalidInstanciaId() {
        asColaborador()
            .body(String.format(
                "{\"funcionarioAvaliacaoCodigo\":999999,\"perguntaCodigo\":%d," +
                "\"respostaTexto\":\"Invalid\",\"opcaoSelecionadaCodigo\":null}",
                perguntaId))
            .when().post(AVALIACOES_URL + RESPOSTAS_PATH)
            .then()
            .statusCode(400);
    }

    @Test
    @Order(15)
    void getPendentesShouldReturn403WhenAccessingAnotherEmployeePendentes() {
        asColaborador()
            .when().get(AVALIACOES_URL + PENDENTES_PATH + adminId)
            .then()
            .statusCode(403);
    }
}
