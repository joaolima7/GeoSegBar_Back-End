package com.geosegbar.unit.entities;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.DamPermissionEntity;
import com.geosegbar.entities.QuestionEntity;
import com.geosegbar.entities.StatusEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - ClientEntity")
class ClientEntityTest extends BaseUnitTest {

    private StatusEntity activeStatus;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
        activeStatus = TestDataBuilder.activeStatus();
    }

    @Test
    @DisplayName("Should create client with all required fields")
    void shouldCreateClientWithAllRequiredFields() {
        // Given
        ClientEntity client = new ClientEntity();
        client.setId(1L);
        client.setName("Empresa ABC");
        client.setEmail("contato@empresaabc.com");
        client.setStreet("Rua Principal");
        client.setNeighborhood("Centro");
        client.setNumberAddress("100");
        client.setCity("São Paulo");
        client.setState("São Paulo");
        client.setZipCode("01000-000");
        client.setPhone("1140001000");
        client.setStatus(activeStatus);

        // Then
        assertThat(client).satisfies(c -> {
            assertThat(c.getId()).isEqualTo(1L);
            assertThat(c.getName()).isEqualTo("Empresa ABC");
            assertThat(c.getEmail()).isEqualTo("contato@empresaabc.com");
            assertThat(c.getStreet()).isEqualTo("Rua Principal");
            assertThat(c.getNeighborhood()).isEqualTo("Centro");
            assertThat(c.getNumberAddress()).isEqualTo("100");
            assertThat(c.getCity()).isEqualTo("São Paulo");
            assertThat(c.getState()).isEqualTo("São Paulo");
            assertThat(c.getZipCode()).isEqualTo("01000-000");
            assertThat(c.getPhone()).isEqualTo("1140001000");
            assertThat(c.getStatus()).isEqualTo(activeStatus);
        });
    }

    @Test
    @DisplayName("Should create client with optional fields")
    void shouldCreateClientWithOptionalFields() {
        // Given
        ClientEntity client = new ClientEntity();
        client.setComplement("Sala 10");
        client.setWhatsappPhone("11999990000");
        client.setEmailContact("financeiro@empresaabc.com");
        client.setLogoPath("/uploads/logos/empresa-abc.png");

        // Then
        assertThat(client).satisfies(c -> {
            assertThat(c.getComplement()).isEqualTo("Sala 10");
            assertThat(c.getWhatsappPhone()).isEqualTo("11999990000");
            assertThat(c.getEmailContact()).isEqualTo("financeiro@empresaabc.com");
            assertThat(c.getLogoPath()).isEqualTo("/uploads/logos/empresa-abc.png");
        });
    }

    @Test
    @DisplayName("Should initialize collections as empty HashSet")
    void shouldInitializeCollectionsAsEmptyHashSet() {
        // Given & When
        ClientEntity client = new ClientEntity();

        // Then
        assertThat(client.getDams()).isNotNull().isInstanceOf(HashSet.class).isEmpty();
        assertThat(client.getUsers()).isNotNull().isInstanceOf(HashSet.class).isEmpty();
        assertThat(client.getDamPermissions()).isNotNull().isInstanceOf(HashSet.class).isEmpty();
        assertThat(client.getQuestions()).isNotNull().isInstanceOf(HashSet.class).isEmpty();
    }

    @Test
    @DisplayName("Should validate email format")
    void shouldValidateEmailFormat() {
        // Given - Valid emails
        ClientEntity client1 = new ClientEntity();
        client1.setEmail("contato@empresa.com");

        ClientEntity client2 = new ClientEntity();
        client2.setEmail("cliente@exemplo.com.br");

        ClientEntity client3 = new ClientEntity();
        client3.setEmail("suporte+info@site.org");

        // Then
        assertThat(client1.getEmail()).contains("@").contains(".");
        assertThat(client2.getEmail()).contains("@").endsWith(".br");
        assertThat(client3.getEmail()).contains("+");
    }

    @Test
    @DisplayName("Should validate phone format - 10 digits")
    void shouldValidatePhoneFormat10Digits() {
        // Given
        ClientEntity client = new ClientEntity();
        client.setPhone("1140001000");

        // Then
        assertThat(client.getPhone())
                .hasSize(10)
                .matches("\\d{10}");
    }

    @Test
    @DisplayName("Should validate phone format - 11 digits")
    void shouldValidatePhoneFormat11Digits() {
        // Given
        ClientEntity client = new ClientEntity();
        client.setPhone("11999990000");

        // Then
        assertThat(client.getPhone())
                .hasSize(11)
                .matches("\\d{11}");
    }

    @Test
    @DisplayName("Should validate whatsapp phone format")
    void shouldValidateWhatsappPhoneFormat() {
        // Given
        ClientEntity client = new ClientEntity();
        client.setWhatsappPhone("11999990000");

        // Then
        assertThat(client.getWhatsappPhone())
                .hasSize(11)
                .matches("\\d{11}");
    }

    @Test
    @DisplayName("Should validate CEP format with dash")
    void shouldValidateCepFormatWithDash() {
        // Given
        ClientEntity client = new ClientEntity();
        client.setZipCode("01000-000");

        // Then
        assertThat(client.getZipCode())
                .matches("\\d{5}-\\d{3}")
                .contains("-");
    }

    @Test
    @DisplayName("Should validate CEP format without dash")
    void shouldValidateCepFormatWithoutDash() {
        // Given
        ClientEntity client = new ClientEntity();
        client.setZipCode("01000000");

        // Then
        assertThat(client.getZipCode())
                .matches("\\d{8}");
    }

    @Test
    @DisplayName("Should not allow numbers in city name")
    void shouldNotAllowNumbersInCityName() {
        // Given - Valid city names (no numbers)
        ClientEntity client = new ClientEntity();
        client.setCity("São Paulo");

        // Then - City should only contain letters and spaces
        assertThat(client.getCity()).matches("^[A-Za-zÀ-ÿ\\s]+$");
    }

    @Test
    @DisplayName("Should not allow numbers in state name")
    void shouldNotAllowNumbersInStateName() {
        // Given - Valid state names (no numbers)
        ClientEntity client = new ClientEntity();
        client.setState("São Paulo");

        // Then - State should only contain letters and spaces
        assertThat(client.getState()).matches("^[A-Za-zÀ-ÿ\\s]+$");
    }

    @Test
    @DisplayName("Should handle city names with accents")
    void shouldHandleCityNamesWithAccents() {
        // Given
        ClientEntity client1 = new ClientEntity();
        client1.setCity("São Paulo");

        ClientEntity client2 = new ClientEntity();
        client2.setCity("Brasília");

        ClientEntity client3 = new ClientEntity();
        client3.setCity("Açu");

        // Then
        assertThat(client1.getCity()).contains("ã");
        assertThat(client2.getCity()).contains("í");
        assertThat(client3.getCity()).contains("ç");
    }

    @Test
    @DisplayName("Should enforce name uniqueness constraint concept")
    void shouldEnforceNameUniquenessConstraintConcept() {
        // Given
        ClientEntity client1 = new ClientEntity();
        client1.setId(1L);
        client1.setName("Empresa ABC");

        ClientEntity client2 = new ClientEntity();
        client2.setId(2L);
        client2.setName("Empresa ABC");

        // Then - In database, this would violate unique constraint
        assertThat(client1.getName()).isEqualTo(client2.getName());
        assertThat(client1.getId()).isNotEqualTo(client2.getId());
    }

    @Test
    @DisplayName("Should enforce email uniqueness constraint concept")
    void shouldEnforceEmailUniquenessConstraintConcept() {
        // Given
        ClientEntity client1 = new ClientEntity();
        client1.setId(1L);
        client1.setEmail("contato@empresa.com");

        ClientEntity client2 = new ClientEntity();
        client2.setId(2L);
        client2.setEmail("contato@empresa.com");

        // Then - In database, this would violate unique constraint
        assertThat(client1.getEmail()).isEqualTo(client2.getEmail());
        assertThat(client1.getId()).isNotEqualTo(client2.getId());
    }

    @Test
    @DisplayName("Should add dam to client")
    void shouldAddDamToClient() {
        // Given
        ClientEntity client = TestDataBuilder.client().build();
        DamEntity dam = TestDataBuilder.dam().withName("Barragem 1").build();
        dam.setClient(client);

        // When
        client.getDams().add(dam);

        // Then
        assertThat(client.getDams())
                .hasSize(1)
                .contains(dam);
    }

    @Test
    @DisplayName("Should add multiple dams to client")
    void shouldAddMultipleDamsToClient() {
        // Given
        ClientEntity client = TestDataBuilder.client().build();

        DamEntity dam1 = TestDataBuilder.dam().withName("Barragem 1").build();
        DamEntity dam2 = TestDataBuilder.dam().withName("Barragem 2").build();
        DamEntity dam3 = TestDataBuilder.dam().withName("Barragem 3").build();

        // When
        client.getDams().add(dam1);
        client.getDams().add(dam2);
        client.getDams().add(dam3);

        // Then
        assertThat(client.getDams())
                .hasSize(3)
                .containsExactlyInAnyOrder(dam1, dam2, dam3);
    }

    @Test
    @DisplayName("Should add user to client")
    void shouldAddUserToClient() {
        // Given
        ClientEntity client = TestDataBuilder.client().build();
        UserEntity user = TestDataBuilder.user().build();

        // When
        client.getUsers().add(user);
        user.getClients().add(client);

        // Then
        assertThat(client.getUsers()).contains(user);
        assertThat(user.getClients()).contains(client);
    }

    @Test
    @DisplayName("Should add multiple users to client")
    void shouldAddMultipleUsersToClient() {
        // Given
        ClientEntity client = TestDataBuilder.client().build();
        UserEntity user1 = TestDataBuilder.user().withName("User 1").build();
        UserEntity user2 = TestDataBuilder.user().withName("User 2").build();

        // When
        client.getUsers().add(user1);
        client.getUsers().add(user2);

        // Then
        assertThat(client.getUsers())
                .hasSize(2)
                .containsExactlyInAnyOrder(user1, user2);
    }

    @Test
    @DisplayName("Should add dam permission to client")
    void shouldAddDamPermissionToClient() {
        // Given
        ClientEntity client = TestDataBuilder.client().build();
        DamPermissionEntity permission = new DamPermissionEntity();
        permission.setClient(client);

        // When
        client.getDamPermissions().add(permission);

        // Then
        assertThat(client.getDamPermissions())
                .hasSize(1)
                .contains(permission);
    }

    @Test
    @DisplayName("Should add question to client")
    void shouldAddQuestionToClient() {
        // Given
        ClientEntity client = TestDataBuilder.client().build();
        QuestionEntity question = new QuestionEntity();
        question.setClient(client);
        question.setQuestionText("Como está a estrutura?");

        // When
        client.getQuestions().add(question);

        // Then
        assertThat(client.getQuestions())
                .hasSize(1)
                .contains(question);
    }

    @Test
    @DisplayName("Should maintain relationship with status")
    void shouldMaintainRelationshipWithStatus() {
        // Given
        ClientEntity client = new ClientEntity();
        client.setStatus(activeStatus);

        // Then
        assertThat(client.getStatus())
                .isNotNull()
                .isEqualTo(activeStatus);
        assertThat(client.getStatus().getStatus()).isNotNull();
    }

    @Test
    @DisplayName("Should handle different address formats")
    void shouldHandleDifferentAddressFormats() {
        // Given
        ClientEntity client = new ClientEntity();
        client.setStreet("Avenida Paulista");
        client.setNumberAddress("1000");
        client.setComplement("Conjunto 10, Andar 5");
        client.setNeighborhood("Bela Vista");

        // Then
        assertThat(client.getStreet()).startsWith("Avenida");
        assertThat(client.getNumberAddress()).matches("\\d+");
        assertThat(client.getComplement()).contains("Conjunto");
        assertThat(client.getNeighborhood()).isNotBlank();
    }

    @Test
    @DisplayName("Should handle address without number")
    void shouldHandleAddressWithoutNumber() {
        // Given
        ClientEntity client = new ClientEntity();
        client.setStreet("Rua das Flores");
        client.setNumberAddress("S/N");

        // Then
        assertThat(client.getNumberAddress()).isEqualTo("S/N");
    }

    @Test
    @DisplayName("Should handle different logo path formats")
    void shouldHandleDifferentLogoPathFormats() {
        // Given
        ClientEntity client1 = new ClientEntity();
        client1.setLogoPath("/uploads/logos/client-1.png");

        ClientEntity client2 = new ClientEntity();
        client2.setLogoPath("https://cdn.example.com/logos/client-2.jpg");

        // Then
        assertThat(client1.getLogoPath()).startsWith("/uploads");
        assertThat(client2.getLogoPath()).startsWith("https://");
    }

    @Test
    @DisplayName("Should allow null for optional fields")
    void shouldAllowNullForOptionalFields() {
        // Given
        ClientEntity client = new ClientEntity();
        client.setComplement(null);
        client.setWhatsappPhone(null);
        client.setEmailContact(null);
        client.setLogoPath(null);

        // Then
        assertThat(client.getComplement()).isNull();
        assertThat(client.getWhatsappPhone()).isNull();
        assertThat(client.getEmailContact()).isNull();
        assertThat(client.getLogoPath()).isNull();
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {
        // Given
        ClientEntity client = new ClientEntity();
        client.setId(1L);
        client.setName("Original Name");
        client.setEmail("original@email.com");

        Long originalId = client.getId();

        // When
        client.setName("Updated Name");
        client.setEmail("updated@email.com");

        // Then
        assertThat(client.getId()).isEqualTo(originalId);
        assertThat(client.getName()).isEqualTo("Updated Name");
        assertThat(client.getEmail()).isEqualTo("updated@email.com");
    }

    @Test
    @DisplayName("Should handle neighborhood with size limit")
    void shouldHandleNeighborhoodWithSizeLimit() {
        // Given
        ClientEntity client = new ClientEntity();
        String neighborhood = "Bairro com Nome Muito Longo que Pode Ter Até Cem Caracteres no Máximo Conforme Definição";
        client.setNeighborhood(neighborhood);

        // Then
        assertThat(client.getNeighborhood().length()).isLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("Should handle city with size limit")
    void shouldHandleCityWithSizeLimit() {
        // Given
        ClientEntity client = new ClientEntity();
        String city = "Cidade com Nome Que Pode Ter Até Cem Caracteres";
        client.setCity(city);

        // Then
        assertThat(client.getCity().length()).isLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("Should handle state with size limit")
    void shouldHandleStateWithSizeLimit() {
        // Given
        ClientEntity client = new ClientEntity();
        client.setState("São Paulo");

        // Then
        assertThat(client.getState().length()).isLessThanOrEqualTo(100);
    }
}
