package com.geosegbar.unit.services;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.StatusEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.fixtures.TestDataBuilder;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.client.service.ClientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Teste unitário para ClientService.
 *
 * Este é um EXEMPLO de teste unitário usando: - BaseUnitTest: Classe base com
 * Mockito configurado - @Mock: Para criar mocks de dependências - @InjectMocks:
 * Para injetar mocks no service - TestDataBuilder: Para criar dados de teste
 * facilmente - AssertJ: Para assertions fluentes e expressivas
 *
 * Características deste teste: - Rápido (não carrega contexto Spring) - Isolado
 * (usa mocks para dependências) - Focado em lógica de negócio
 */
@DisplayName("ClientService - Testes Unitários")
class ClientServiceTest extends BaseUnitTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientService clientService;

    @Test
    @DisplayName("Deve retornar client quando ID existe")
    void shouldReturnClientWhenIdExists() {
        // Given
        Long clientId = 1L;
        ClientEntity expectedClient = TestDataBuilder.client()
                .withId(clientId)
                .withName("Cliente Teste")
                .withEmail("cliente@test.com")
                .build();

        when(clientRepository.findById(clientId))
                .thenReturn(Optional.of(expectedClient));

        // When
        ClientEntity actualClient = clientService.findById(clientId);

        // Then
        assertThat(actualClient)
                .isNotNull()
                .satisfies(client -> {
                    assertThat(client.getId()).isEqualTo(clientId);
                    assertThat(client.getName()).isEqualTo("Cliente Teste");
                    assertThat(client.getEmail()).isEqualTo("cliente@test.com");
                });

        verify(clientRepository, times(1)).findById(clientId);
    }

    @Test
    @DisplayName("Deve lançar NotFoundException quando ID não existe")
    void shouldThrowNotFoundExceptionWhenIdNotExists() {
        // Given
        Long nonExistentId = 999L;
        when(clientRepository.findById(nonExistentId))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> clientService.findById(nonExistentId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Cliente não encontrado");

        verify(clientRepository, times(1)).findById(nonExistentId);
    }

    @Test
    @DisplayName("Deve verificar se cliente existe por nome")
    void shouldCheckIfClientExistsByName() {
        // Given
        String clientName = "Cliente Existente";
        when(clientRepository.existsByName(clientName))
                .thenReturn(true);

        // When
        boolean exists = clientService.existsByName(clientName);

        // Then
        assertThat(exists).isTrue();
        verify(clientRepository, times(1)).existsByName(clientName);
    }

    @Test
    @DisplayName("Deve verificar se cliente existe por email")
    void shouldCheckIfClientExistsByEmail() {
        // Given
        String email = "cliente@test.com";
        when(clientRepository.existsByEmail(email))
                .thenReturn(false);

        // When
        boolean exists = clientService.existsByEmail(email);

        // Then
        assertThat(exists).isFalse();
        verify(clientRepository, times(1)).existsByEmail(email);
    }

    @Test
    @DisplayName("Deve verificar se status do cliente está ativo")
    void shouldCheckIfClientStatusIsActive() {
        // Given
        ClientEntity client = TestDataBuilder.client()
                .withStatus(TestDataBuilder.activeStatus())
                .build();

        when(clientRepository.findById(anyLong()))
                .thenReturn(Optional.of(client));

        // When
        ClientEntity foundClient = clientService.findById(1L);

        // Then
        assertThat(foundClient.getStatus())
                .isNotNull()
                .extracting(StatusEntity::getId)
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("Deve utilizar TestDataBuilder para múltiplos clientes")
    void shouldUseTestDataBuilderForMultipleClients() {
        // Given
        ClientEntity client1 = TestDataBuilder.client()
                .withName("Cliente 1")
                .withCity("São Paulo")
                .build();

        ClientEntity client2 = TestDataBuilder.client()
                .withName("Cliente 2")
                .withCity("Rio de Janeiro")
                .build();

        // Then
        assertThat(client1.getId()).isNotEqualTo(client2.getId());
        assertThat(client1.getCity()).isEqualTo("São Paulo");
        assertThat(client2.getCity()).isEqualTo("Rio de Janeiro");
    }
}
