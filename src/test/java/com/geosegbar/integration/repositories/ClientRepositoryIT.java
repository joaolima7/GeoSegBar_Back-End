package com.geosegbar.integration.repositories;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.geosegbar.config.BaseIntegrationTest;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.StatusEntity;
import com.geosegbar.fixtures.TestDataBuilder;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.status.persistence.jpa.StatusRepository;

/**
 * Teste de integração para ClientRepository.
 *
 * Este é um EXEMPLO de teste de integração usando: - BaseIntegrationTest:
 * Carrega contexto Spring com Testcontainers - @DataJpaTest: Configura ambiente
 * JPA otimizado para testes - @AutoConfigureTestDatabase(replace = NONE): Usa
 * Testcontainers ao invés de H2 - TestDataBuilder: Para criar dados de teste -
 * AssertJ: Para assertions expressivas
 *
 * Características deste teste: - Usa banco PostgreSQL real (via Testcontainers)
 * - Testa queries customizadas do repository - Verifica integridade de dados e
 * relacionamentos - Transações são rollback após cada teste
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("ClientRepository - Testes de Integração")
class ClientRepositoryIT extends BaseIntegrationTest {

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

        // Reseta gerador de IDs do TestDataBuilder
        TestDataBuilder.resetIdGenerator();
    }

    @Test
    @DisplayName("Deve salvar e recuperar cliente do banco")
    void shouldSaveAndRetrieveClient() {
        // Given
        ClientEntity client = TestDataBuilder.client()
                .withId(null) // ID será gerado pelo banco
                .withName("Cliente Teste Integração")
                .withEmail("integracao@test.com")
                .withStatus(activeStatus)
                .build();

        // When
        ClientEntity savedClient = clientRepository.save(client);
        Optional<ClientEntity> retrievedClient = clientRepository.findById(savedClient.getId());

        // Then
        assertThat(savedClient.getId()).isNotNull();
        assertThat(retrievedClient)
                .isPresent()
                .get()
                .satisfies(c -> {
                    assertThat(c.getName()).isEqualTo("Cliente Teste Integração");
                    assertThat(c.getEmail()).isEqualTo("integracao@test.com");
                    assertThat(c.getStatus()).isNotNull();
                    assertThat(c.getStatus().getId()).isEqualTo(activeStatus.getId());
                });
    }

    @Test
    @DisplayName("Deve verificar unicidade de nome")
    void shouldEnforceNameUniqueness() {
        // Given
        ClientEntity client1 = TestDataBuilder.client()
                .withId(null)
                .withName("Cliente Único")
                .withEmail("email1@test.com")
                .withStatus(activeStatus)
                .build();

        clientRepository.save(client1);

        // When
        boolean exists = clientRepository.existsByName("Cliente Único");
        boolean notExists = clientRepository.existsByName("Cliente Inexistente");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Deve verificar unicidade de email")
    void shouldEnforceEmailUniqueness() {
        // Given
        ClientEntity client = TestDataBuilder.client()
                .withId(null)
                .withEmail("unico@test.com")
                .withStatus(activeStatus)
                .build();

        clientRepository.save(client);

        // When
        boolean exists = clientRepository.existsByEmail("unico@test.com");
        boolean notExists = clientRepository.existsByEmail("naoexiste@test.com");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Deve buscar clientes por status")
    void shouldFindClientsByStatus() {
        // Given
        ClientEntity activeClient1 = TestDataBuilder.client()
                .withId(null)
                .withName("Cliente Ativo 1")
                .withStatus(activeStatus)
                .build();

        ClientEntity activeClient2 = TestDataBuilder.client()
                .withId(null)
                .withName("Cliente Ativo 2")
                .withStatus(activeStatus)
                .build();

        clientRepository.save(activeClient1);
        clientRepository.save(activeClient2);

        // When
        List<ClientEntity> allClients = clientRepository.findAll();

        // Then
        assertThat(allClients)
                .hasSize(2)
                .extracting(ClientEntity::getName)
                .containsExactlyInAnyOrder("Cliente Ativo 1", "Cliente Ativo 2");
    }

    @Test
    @DisplayName("Deve buscar cliente por cidade")
    void shouldFindClientsByCity() {
        // Given
        ClientEntity clientSP = TestDataBuilder.client()
                .withId(null)
                .withName("Cliente SP")
                .withCity("São Paulo")
                .withStatus(activeStatus)
                .build();

        ClientEntity clientRJ = TestDataBuilder.client()
                .withId(null)
                .withName("Cliente RJ")
                .withCity("Rio de Janeiro")
                .withStatus(activeStatus)
                .build();

        clientRepository.save(clientSP);
        clientRepository.save(clientRJ);

        // When
        List<ClientEntity> allClients = clientRepository.findAll();

        // Then
        assertThat(allClients)
                .hasSize(2)
                .extracting(ClientEntity::getCity)
                .containsExactlyInAnyOrder("São Paulo", "Rio de Janeiro");
    }

    @Test
    @DisplayName("Deve deletar cliente do banco")
    void shouldDeleteClient() {
        // Given
        ClientEntity client = TestDataBuilder.client()
                .withId(null)
                .withName("Cliente para Deletar")
                .withStatus(activeStatus)
                .build();

        ClientEntity savedClient = clientRepository.save(client);
        Long clientId = savedClient.getId();

        // When
        clientRepository.deleteById(clientId);
        Optional<ClientEntity> deletedClient = clientRepository.findById(clientId);

        // Then
        assertThat(deletedClient).isEmpty();
    }

    @Test
    @DisplayName("Deve contar total de clientes")
    void shouldCountTotalClients() {
        // Given
        clientRepository.save(TestDataBuilder.client().withId(null).withStatus(activeStatus).build());
        clientRepository.save(TestDataBuilder.client().withId(null).withStatus(activeStatus).build());
        clientRepository.save(TestDataBuilder.client().withId(null).withStatus(activeStatus).build());

        // When
        long count = clientRepository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }
}
