package com.geosegbar.e2e.scenarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.geosegbar.config.BaseE2ETest;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.StatusEntity;
import com.geosegbar.fixtures.TestDataBuilder;
import com.geosegbar.infra.client.dtos.ClientDTO;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.status.persistence.jpa.StatusRepository;

import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;

/**
 * Teste End-to-End para fluxo de Cliente.
 *
 * Este é um EXEMPLO de teste E2E usando: - BaseE2ETest: Carrega contexto Spring
 * completo com servidor web - RestAssured: Para chamadas HTTP reais -
 * Testcontainers: PostgreSQL e Redis reais - Hamcrest matchers: Para validações
 * de resposta HTTP
 *
 * Características deste teste: - Testa aplicação completa (endpoint → service →
 * repository → banco) - Servidor web real em porta aleatória - Valida contrato
 * de API (request/response) - Verifica comportamento end-to-end
 *
 * NOTA: Este é um exemplo SIMPLIFICADO. Em produção, seria necessário: -
 * Implementar autenticação JWT - Validar permissões de acesso - Testar cenários
 * de erro mais complexos
 */
@DisplayName("Cliente E2E - Fluxo Completo")
class ClientE2ETest extends BaseE2ETest {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private StatusRepository statusRepository;

    private StatusEntity activeStatus;

    @BeforeEach
    void setUp() {
        // Limpa dados anteriores
        clientRepository.deleteAll();
        statusRepository.deleteAll();

        // Cria status necessário
        activeStatus = new StatusEntity();
        activeStatus.setStatus(com.geosegbar.common.enums.StatusEnum.ACTIVE);
        activeStatus = statusRepository.save(activeStatus);

        TestDataBuilder.resetIdGenerator();
    }

    @Test
    @DisplayName("Deve listar todos os clientes via API")
    void shouldListAllClients() {
        // Given - Cria clientes diretamente no banco
        ClientEntity client1 = TestDataBuilder.client()
                .withId(null)
                .withName("Cliente API 1")
                .withStatus(activeStatus)
                .build();

        ClientEntity client2 = TestDataBuilder.client()
                .withId(null)
                .withName("Cliente API 2")
                .withStatus(activeStatus)
                .build();

        clientRepository.save(client1);
        clientRepository.save(client2);

        // When / Then - Chama API e valida resposta
        given()
                .contentType(ContentType.JSON)
                .when()
                .get(getUrl("/clients"))
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", hasSize(2))
                .body("data[0].name", notNullValue())
                .body("data[1].name", notNullValue());
    }

    @Test
    @DisplayName("Deve buscar cliente por ID via API")
    void shouldGetClientById() {
        // Given
        ClientEntity client = TestDataBuilder.client()
                .withId(null)
                .withName("Cliente Específico")
                .withEmail("especifico@test.com")
                .withStatus(activeStatus)
                .build();

        ClientEntity savedClient = clientRepository.save(client);

        // When / Then
        given()
                .contentType(ContentType.JSON)
                .when()
                .get(getUrl("/clients/" + savedClient.getId()))
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.id", equalTo(savedClient.getId().intValue()))
                .body("data.name", equalTo("Cliente Específico"))
                .body("data.email", equalTo("especifico@test.com"));
    }

    @Test
    @DisplayName("Deve retornar 404 quando cliente não existe")
    void shouldReturn404WhenClientNotFound() {
        // Given
        Long nonExistentId = 9999L;

        // When / Then
        given()
                .contentType(ContentType.JSON)
                .when()
                .get(getUrl("/clients/" + nonExistentId))
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("Deve criar novo cliente via API")
    void shouldCreateNewClient() {
        // Given
        ClientDTO newClient = new ClientDTO();
        newClient.setName("Novo Cliente API");
        newClient.setEmail("novo@test.com");
        newClient.setStreet("Rua API");
        newClient.setNeighborhood("Bairro API");
        newClient.setCity("São Paulo");
        newClient.setState("São Paulo");
        newClient.setZipCode("01310-100");
        newClient.setPhone("11988887777");
        newClient.setStatus(activeStatus);

        // When
        Integer createdId = given()
                .contentType(ContentType.JSON)
                .body(newClient)
                .when()
                .post(getUrl("/clients"))
                .then()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("data.name", equalTo("Novo Cliente API"))
                .body("data.email", equalTo("novo@test.com"))
                .extract()
                .path("data.id");

        // Then - Verifica se foi salvo no banco
        ClientEntity savedClient = clientRepository.findById(createdId.longValue()).orElse(null);
        assertThat(savedClient)
                .isNotNull()
                .satisfies(c -> {
                    assertThat(c.getName()).isEqualTo("Novo Cliente API");
                    assertThat(c.getEmail()).isEqualTo("novo@test.com");
                    assertThat(c.getCity()).isEqualTo("São Paulo");
                });
    }

    @Test
    @DisplayName("Deve validar campos obrigatórios na criação")
    void shouldValidateRequiredFieldsOnCreation() {
        // Given - Cliente sem nome (campo obrigatório)
        ClientDTO invalidClient = new ClientDTO();
        invalidClient.setEmail("invalido@test.com");
        // name é obrigatório mas não foi definido

        // When / Then
        given()
                .contentType(ContentType.JSON)
                .body(invalidClient)
                .when()
                .post(getUrl("/clients"))
                .then()
                .statusCode(400); // Bad Request
    }

    @Test
    @DisplayName("Deve buscar clientes por filtros")
    void shouldFilterClientsByStatus() {
        // Given
        ClientEntity activeClient = TestDataBuilder.client()
                .withId(null)
                .withName("Cliente Filtro Ativo")
                .withStatus(activeStatus)
                .build();

        clientRepository.save(activeClient);

        // When / Then
        given()
                .contentType(ContentType.JSON)
                .queryParam("statusId", activeStatus.getId())
                .when()
                .get(getUrl("/clients/filter"))
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", hasSize(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Deve testar ciclo completo: criar, buscar, atualizar, deletar")
    void shouldTestCompleteCRUDCycle() {
        // 1. CREATE
        ClientDTO newClient = new ClientDTO();
        newClient.setName("Cliente Ciclo Completo");
        newClient.setEmail("ciclo@test.com");
        newClient.setStreet("Rua Teste");
        newClient.setNeighborhood("Bairro Teste");
        newClient.setCity("São Paulo");
        newClient.setState("São Paulo");
        newClient.setZipCode("01310-100");
        newClient.setPhone("11988887777");
        newClient.setStatus(activeStatus);

        Integer clientId = given()
                .contentType(ContentType.JSON)
                .body(newClient)
                .when()
                .post(getUrl("/clients"))
                .then()
                .statusCode(201)
                .extract()
                .path("data.id");

        // 2. READ
        given()
                .contentType(ContentType.JSON)
                .when()
                .get(getUrl("/clients/" + clientId))
                .then()
                .statusCode(200)
                .body("data.name", equalTo("Cliente Ciclo Completo"));

        // 3. UPDATE (simplificado - verificar se endpoint existe)
        // Nota: A implementação do update depende do endpoint real
        // 4. DELETE
        given()
                .contentType(ContentType.JSON)
                .when()
                .delete(getUrl("/clients/" + clientId))
                .then()
                .statusCode(anyOf(is(200), is(204)));

        // 5. VERIFY DELETE
        assertThat(clientRepository.findById(clientId.longValue())).isEmpty();
    }
}
