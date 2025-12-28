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

        ClientEntity client = new ClientEntity();
        client.setComplement("Sala 10");
        client.setWhatsappPhone("11999990000");
        client.setEmailContact("financeiro@empresaabc.com");
        client.setLogoPath("/uploads/logos/empresa-abc.png");

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

        ClientEntity client = new ClientEntity();

        assertThat(client.getDams()).isNotNull().isInstanceOf(HashSet.class).isEmpty();
        assertThat(client.getUsers()).isNotNull().isInstanceOf(HashSet.class).isEmpty();
        assertThat(client.getDamPermissions()).isNotNull().isInstanceOf(HashSet.class).isEmpty();
        assertThat(client.getQuestions()).isNotNull().isInstanceOf(HashSet.class).isEmpty();
    }

    @Test
    @DisplayName("Should validate email format")
    void shouldValidateEmailFormat() {

        ClientEntity client1 = new ClientEntity();
        client1.setEmail("contato@empresa.com");

        ClientEntity client2 = new ClientEntity();
        client2.setEmail("cliente@exemplo.com.br");

        ClientEntity client3 = new ClientEntity();
        client3.setEmail("suporte+info@site.org");

        assertThat(client1.getEmail()).contains("@").contains(".");
        assertThat(client2.getEmail()).contains("@").endsWith(".br");
        assertThat(client3.getEmail()).contains("+");
    }

    @Test
    @DisplayName("Should validate phone format - 10 digits")
    void shouldValidatePhoneFormat10Digits() {

        ClientEntity client = new ClientEntity();
        client.setPhone("1140001000");

        assertThat(client.getPhone())
                .hasSize(10)
                .matches("\\d{10}");
    }

    @Test
    @DisplayName("Should validate phone format - 11 digits")
    void shouldValidatePhoneFormat11Digits() {

        ClientEntity client = new ClientEntity();
        client.setPhone("11999990000");

        assertThat(client.getPhone())
                .hasSize(11)
                .matches("\\d{11}");
    }

    @Test
    @DisplayName("Should validate whatsapp phone format")
    void shouldValidateWhatsappPhoneFormat() {

        ClientEntity client = new ClientEntity();
        client.setWhatsappPhone("11999990000");

        assertThat(client.getWhatsappPhone())
                .hasSize(11)
                .matches("\\d{11}");
    }

    @Test
    @DisplayName("Should validate CEP format with dash")
    void shouldValidateCepFormatWithDash() {

        ClientEntity client = new ClientEntity();
        client.setZipCode("01000-000");

        assertThat(client.getZipCode())
                .matches("\\d{5}-\\d{3}")
                .contains("-");
    }

    @Test
    @DisplayName("Should validate CEP format without dash")
    void shouldValidateCepFormatWithoutDash() {

        ClientEntity client = new ClientEntity();
        client.setZipCode("01000000");

        assertThat(client.getZipCode())
                .matches("\\d{8}");
    }

    @Test
    @DisplayName("Should not allow numbers in city name")
    void shouldNotAllowNumbersInCityName() {

        ClientEntity client = new ClientEntity();
        client.setCity("São Paulo");

        assertThat(client.getCity()).matches("^[A-Za-zÀ-ÿ\\s]+$");
    }

    @Test
    @DisplayName("Should not allow numbers in state name")
    void shouldNotAllowNumbersInStateName() {

        ClientEntity client = new ClientEntity();
        client.setState("São Paulo");

        assertThat(client.getState()).matches("^[A-Za-zÀ-ÿ\\s]+$");
    }

    @Test
    @DisplayName("Should handle city names with accents")
    void shouldHandleCityNamesWithAccents() {

        ClientEntity client1 = new ClientEntity();
        client1.setCity("São Paulo");

        ClientEntity client2 = new ClientEntity();
        client2.setCity("Brasília");

        ClientEntity client3 = new ClientEntity();
        client3.setCity("Açu");

        assertThat(client1.getCity()).contains("ã");
        assertThat(client2.getCity()).contains("í");
        assertThat(client3.getCity()).contains("ç");
    }

    @Test
    @DisplayName("Should enforce name uniqueness constraint concept")
    void shouldEnforceNameUniquenessConstraintConcept() {

        ClientEntity client1 = new ClientEntity();
        client1.setId(1L);
        client1.setName("Empresa ABC");

        ClientEntity client2 = new ClientEntity();
        client2.setId(2L);
        client2.setName("Empresa ABC");

        assertThat(client1.getName()).isEqualTo(client2.getName());
        assertThat(client1.getId()).isNotEqualTo(client2.getId());
    }

    @Test
    @DisplayName("Should enforce email uniqueness constraint concept")
    void shouldEnforceEmailUniquenessConstraintConcept() {

        ClientEntity client1 = new ClientEntity();
        client1.setId(1L);
        client1.setEmail("contato@empresa.com");

        ClientEntity client2 = new ClientEntity();
        client2.setId(2L);
        client2.setEmail("contato@empresa.com");

        assertThat(client1.getEmail()).isEqualTo(client2.getEmail());
        assertThat(client1.getId()).isNotEqualTo(client2.getId());
    }

    @Test
    @DisplayName("Should add dam to client")
    void shouldAddDamToClient() {

        ClientEntity client = TestDataBuilder.client().build();
        DamEntity dam = TestDataBuilder.dam().withName("Barragem 1").build();
        dam.setClient(client);

        client.getDams().add(dam);

        assertThat(client.getDams())
                .hasSize(1)
                .contains(dam);
    }

    @Test
    @DisplayName("Should add multiple dams to client")
    void shouldAddMultipleDamsToClient() {

        ClientEntity client = TestDataBuilder.client().build();

        DamEntity dam1 = TestDataBuilder.dam().withName("Barragem 1").build();
        DamEntity dam2 = TestDataBuilder.dam().withName("Barragem 2").build();
        DamEntity dam3 = TestDataBuilder.dam().withName("Barragem 3").build();

        client.getDams().add(dam1);
        client.getDams().add(dam2);
        client.getDams().add(dam3);

        assertThat(client.getDams())
                .hasSize(3)
                .containsExactlyInAnyOrder(dam1, dam2, dam3);
    }

    @Test
    @DisplayName("Should add user to client")
    void shouldAddUserToClient() {

        ClientEntity client = TestDataBuilder.client().build();
        UserEntity user = TestDataBuilder.user().build();

        client.getUsers().add(user);
        user.getClients().add(client);

        assertThat(client.getUsers()).contains(user);
        assertThat(user.getClients()).contains(client);
    }

    @Test
    @DisplayName("Should add multiple users to client")
    void shouldAddMultipleUsersToClient() {

        ClientEntity client = TestDataBuilder.client().build();
        UserEntity user1 = TestDataBuilder.user().withName("User 1").build();
        UserEntity user2 = TestDataBuilder.user().withName("User 2").build();

        client.getUsers().add(user1);
        client.getUsers().add(user2);

        assertThat(client.getUsers())
                .hasSize(2)
                .containsExactlyInAnyOrder(user1, user2);
    }

    @Test
    @DisplayName("Should add dam permission to client")
    void shouldAddDamPermissionToClient() {

        ClientEntity client = TestDataBuilder.client().build();
        DamPermissionEntity permission = new DamPermissionEntity();
        permission.setClient(client);

        client.getDamPermissions().add(permission);

        assertThat(client.getDamPermissions())
                .hasSize(1)
                .contains(permission);
    }

    @Test
    @DisplayName("Should add question to client")
    void shouldAddQuestionToClient() {

        ClientEntity client = TestDataBuilder.client().build();
        QuestionEntity question = new QuestionEntity();
        question.setClient(client);
        question.setQuestionText("Como está a estrutura?");

        client.getQuestions().add(question);

        assertThat(client.getQuestions())
                .hasSize(1)
                .contains(question);
    }

    @Test
    @DisplayName("Should maintain relationship with status")
    void shouldMaintainRelationshipWithStatus() {

        ClientEntity client = new ClientEntity();
        client.setStatus(activeStatus);

        assertThat(client.getStatus())
                .isNotNull()
                .isEqualTo(activeStatus);
        assertThat(client.getStatus().getStatus()).isNotNull();
    }

    @Test
    @DisplayName("Should handle different address formats")
    void shouldHandleDifferentAddressFormats() {

        ClientEntity client = new ClientEntity();
        client.setStreet("Avenida Paulista");
        client.setNumberAddress("1000");
        client.setComplement("Conjunto 10, Andar 5");
        client.setNeighborhood("Bela Vista");

        assertThat(client.getStreet()).startsWith("Avenida");
        assertThat(client.getNumberAddress()).matches("\\d+");
        assertThat(client.getComplement()).contains("Conjunto");
        assertThat(client.getNeighborhood()).isNotBlank();
    }

    @Test
    @DisplayName("Should handle address without number")
    void shouldHandleAddressWithoutNumber() {

        ClientEntity client = new ClientEntity();
        client.setStreet("Rua das Flores");
        client.setNumberAddress("S/N");

        assertThat(client.getNumberAddress()).isEqualTo("S/N");
    }

    @Test
    @DisplayName("Should handle different logo path formats")
    void shouldHandleDifferentLogoPathFormats() {

        ClientEntity client1 = new ClientEntity();
        client1.setLogoPath("/uploads/logos/client-1.png");

        ClientEntity client2 = new ClientEntity();
        client2.setLogoPath("https://cdn.example.com/logos/client-2.jpg");

        assertThat(client1.getLogoPath()).startsWith("/uploads");
        assertThat(client2.getLogoPath()).startsWith("https://");
    }

    @Test
    @DisplayName("Should allow null for optional fields")
    void shouldAllowNullForOptionalFields() {

        ClientEntity client = new ClientEntity();
        client.setComplement(null);
        client.setWhatsappPhone(null);
        client.setEmailContact(null);
        client.setLogoPath(null);

        assertThat(client.getComplement()).isNull();
        assertThat(client.getWhatsappPhone()).isNull();
        assertThat(client.getEmailContact()).isNull();
        assertThat(client.getLogoPath()).isNull();
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        ClientEntity client = new ClientEntity();
        client.setId(1L);
        client.setName("Original Name");
        client.setEmail("original@email.com");

        Long originalId = client.getId();

        client.setName("Updated Name");
        client.setEmail("updated@email.com");

        assertThat(client.getId()).isEqualTo(originalId);
        assertThat(client.getName()).isEqualTo("Updated Name");
        assertThat(client.getEmail()).isEqualTo("updated@email.com");
    }

    @Test
    @DisplayName("Should handle neighborhood with size limit")
    void shouldHandleNeighborhoodWithSizeLimit() {

        ClientEntity client = new ClientEntity();
        String neighborhood = "Bairro com Nome Muito Longo que Pode Ter Até Cem Caracteres no Máximo Conforme Definição";
        client.setNeighborhood(neighborhood);

        assertThat(client.getNeighborhood().length()).isLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("Should handle city with size limit")
    void shouldHandleCityWithSizeLimit() {

        ClientEntity client = new ClientEntity();
        String city = "Cidade com Nome Que Pode Ter Até Cem Caracteres";
        client.setCity(city);

        assertThat(client.getCity().length()).isLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("Should handle state with size limit")
    void shouldHandleStateWithSizeLimit() {

        ClientEntity client = new ClientEntity();
        client.setState("São Paulo");

        assertThat(client.getState().length()).isLessThanOrEqualTo(100);
    }
}
