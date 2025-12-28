package com.geosegbar.config;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Classe base para testes End-to-End (E2E).
 *
 * Esta classe fornece configurações comuns para testes E2E, incluindo
 * configuração do RestAssured e servidor web real.
 *
 * Características: - Carrega contexto Spring completo - Inicia servidor web em
 * porta aleatória - Configura RestAssured para chamadas HTTP - Herda containers
 * do BaseIntegrationTest - Profile 'test' ativado automaticamente
 *
 * Uso: public class UserE2ETest extends BaseE2ETest {
 *
 * @Test void shouldCreateUser() { given() .body(userDTO) .when()
 * .post("/api/users") .then() .statusCode(201); } }
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("e2e")
public abstract class BaseE2ETest extends BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    protected RequestSpecification requestSpec;

    @BeforeEach
    void setUpRestAssured() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Configuração padrão do RestAssured
        requestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();
    }

    /**
     * Retorna a URL base completa para testes.
     *
     * @return URL base (ex: http://localhost:8080)
     */
    protected String getBaseUrl() {
        return "http://localhost:" + port;
    }

    /**
     * Retorna a URL completa para um endpoint.
     *
     * @param endpoint caminho do endpoint (ex: /api/users)
     * @return URL completa (ex: http://localhost:8080/api/users)
     */
    protected String getUrl(String endpoint) {
        return getBaseUrl() + endpoint;
    }

    /**
     * Helper para autenticação JWT (pode ser customizado).
     *
     * @param token JWT token
     * @return RequestSpecification com header de autenticação
     */
    protected RequestSpecification withAuth(String token) {
        return new RequestSpecBuilder()
                .addRequestSpecification(requestSpec)
                .addHeader("Authorization", "Bearer " + token)
                .build();
    }

    /**
     * Helper para obter token de autenticação (implementar conforme
     * necessário).
     *
     * @param username nome de usuário
     * @param password senha
     * @return JWT token
     */
    protected String getAuthToken(String username, String password) {
        // TODO: Implementar conforme sistema de autenticação
        // Exemplo básico:
        // return given()
        //     .contentType(ContentType.JSON)
        //     .body(Map.of("username", username, "password", password))
        // .when()
        //     .post("/auth/login")
        // .then()
        //     .statusCode(200)
        //     .extract()
        //     .path("token");
        throw new UnsupportedOperationException("Implementar método de autenticação");
    }
}
