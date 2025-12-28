package com.geosegbar.unit.entities;

import com.geosegbar.common.enums.StatusEnum;
import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.StatusEntity;
import com.geosegbar.entities.UserEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class StatusEntityTest extends BaseUnitTest {

    @Test
    @DisplayName("Should create status with all required fields")
    void shouldCreateStatusWithAllRequiredFields() {

        StatusEntity status = new StatusEntity();
        status.setId(1L);
        status.setStatus(StatusEnum.ACTIVE);

        assertThat(status).satisfies(s -> {
            assertThat(s.getId()).isEqualTo(1L);
            assertThat(s.getStatus()).isEqualTo(StatusEnum.ACTIVE);
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        StatusEntity status = new StatusEntity(1L, StatusEnum.ACTIVE, null, null, null);

        assertThat(status.getId()).isEqualTo(1L);
        assertThat(status.getStatus()).isEqualTo(StatusEnum.ACTIVE);
    }

    @Test
    @DisplayName("Should support ACTIVE status enum")
    void shouldSupportActiveStatusEnum() {

        StatusEntity status = new StatusEntity();
        status.setStatus(StatusEnum.ACTIVE);

        assertThat(status.getStatus()).isEqualTo(StatusEnum.ACTIVE);
    }

    @Test
    @DisplayName("Should support DISABLED status enum")
    void shouldSupportDisabledStatusEnum() {

        StatusEntity status = new StatusEntity();
        status.setStatus(StatusEnum.DISABLED);

        assertThat(status.getStatus()).isEqualTo(StatusEnum.DISABLED);
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of users")
    void shouldMaintainOneToManyCollectionOfUsers() {

        StatusEntity status = new StatusEntity();
        status.setStatus(StatusEnum.ACTIVE);

        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setStatus(status);

        status.getUsers().add(user);

        assertThat(status.getUsers())
                .isNotNull()
                .hasSize(1)
                .contains(user);
    }

    @Test
    @DisplayName("Should initialize empty users collection by default")
    void shouldInitializeEmptyUsersCollectionByDefault() {

        StatusEntity status = new StatusEntity();

        assertThat(status.getUsers()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of clients")
    void shouldMaintainOneToManyCollectionOfClients() {

        StatusEntity status = new StatusEntity();
        status.setStatus(StatusEnum.ACTIVE);

        ClientEntity client = new ClientEntity();
        client.setId(1L);
        client.setStatus(status);

        status.getClients().add(client);

        assertThat(status.getClients())
                .isNotNull()
                .hasSize(1)
                .contains(client);
    }

    @Test
    @DisplayName("Should initialize empty clients collection by default")
    void shouldInitializeEmptyClientsCollectionByDefault() {

        StatusEntity status = new StatusEntity();

        assertThat(status.getClients()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of dams")
    void shouldMaintainOneToManyCollectionOfDams() {

        StatusEntity status = new StatusEntity();
        status.setStatus(StatusEnum.ACTIVE);

        DamEntity dam = new DamEntity();
        dam.setId(1L);
        dam.setStatus(status);

        status.getDams().add(dam);

        assertThat(status.getDams())
                .isNotNull()
                .hasSize(1)
                .contains(dam);
    }

    @Test
    @DisplayName("Should initialize empty dams collection by default")
    void shouldInitializeEmptyDamsCollectionByDefault() {

        StatusEntity status = new StatusEntity();

        assertThat(status.getDams()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support multiple users per status")
    void shouldSupportMultipleUsersPerStatus() {

        StatusEntity status = new StatusEntity();
        status.setStatus(StatusEnum.ACTIVE);

        UserEntity user1 = new UserEntity();
        user1.setId(1L);

        UserEntity user2 = new UserEntity();
        user2.setId(2L);

        UserEntity user3 = new UserEntity();
        user3.setId(3L);

        status.getUsers().add(user1);
        status.getUsers().add(user2);
        status.getUsers().add(user3);

        assertThat(status.getUsers()).hasSize(3);
    }

    @Test
    @DisplayName("Should support multiple clients per status")
    void shouldSupportMultipleClientsPerStatus() {

        StatusEntity status = new StatusEntity();
        status.setStatus(StatusEnum.DISABLED);

        ClientEntity client1 = new ClientEntity();
        client1.setId(1L);

        ClientEntity client2 = new ClientEntity();
        client2.setId(2L);

        status.getClients().add(client1);
        status.getClients().add(client2);

        assertThat(status.getClients()).hasSize(2);
    }

    @Test
    @DisplayName("Should support multiple dams per status")
    void shouldSupportMultipleDamsPerStatus() {

        StatusEntity status = new StatusEntity();
        status.setStatus(StatusEnum.ACTIVE);

        DamEntity dam1 = new DamEntity();
        dam1.setId(1L);

        DamEntity dam2 = new DamEntity();
        dam2.setId(2L);

        status.getDams().add(dam1);
        status.getDams().add(dam2);

        assertThat(status.getDams()).hasSize(2);
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        StatusEntity status = new StatusEntity();
        status.setId(1L);
        status.setStatus(StatusEnum.ACTIVE);

        Long originalId = status.getId();

        status.setStatus(StatusEnum.DISABLED);

        assertThat(status.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support different status values")
    void shouldSupportDifferentStatusValues() {

        StatusEntity activeStatus = new StatusEntity();
        activeStatus.setStatus(StatusEnum.ACTIVE);

        StatusEntity disabledStatus = new StatusEntity();
        disabledStatus.setStatus(StatusEnum.DISABLED);

        assertThat(activeStatus.getStatus()).isNotEqualTo(disabledStatus.getStatus());
    }
}
