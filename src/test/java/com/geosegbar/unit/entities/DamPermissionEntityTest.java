package com.geosegbar.unit.entities;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.DamPermissionEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - DamPermissionEntity")
class DamPermissionEntityTest extends BaseUnitTest {

    private UserEntity user;
    private DamEntity dam;
    private ClientEntity client;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
        user = TestDataBuilder.user().build();
        dam = TestDataBuilder.dam().build();
        client = TestDataBuilder.client().build();
    }

    @Test
    @DisplayName("Should create dam permission with all required fields")
    void shouldCreateDamPermissionWithAllRequiredFields() {

        DamPermissionEntity permission = new DamPermissionEntity();
        permission.setId(1L);
        permission.setUser(user);
        permission.setDam(dam);
        permission.setClient(client);
        permission.setHasAccess(true);

        assertThat(permission).satisfies(p -> {
            assertThat(p.getId()).isEqualTo(1L);
            assertThat(p.getUser()).isEqualTo(user);
            assertThat(p.getDam()).isEqualTo(dam);
            assertThat(p.getClient()).isEqualTo(client);
            assertThat(p.getHasAccess()).isTrue();
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        LocalDateTime now = LocalDateTime.now();
        UserEntity createdByUser = TestDataBuilder.user().withName("Admin").build();

        DamPermissionEntity permission = new DamPermissionEntity(
                1L,
                user,
                dam,
                client,
                true,
                now,
                now,
                createdByUser,
                createdByUser
        );

        assertThat(permission.getId()).isEqualTo(1L);
        assertThat(permission.getUser()).isEqualTo(user);
        assertThat(permission.getDam()).isEqualTo(dam);
        assertThat(permission.getClient()).isEqualTo(client);
        assertThat(permission.getHasAccess()).isTrue();
        assertThat(permission.getCreatedAt()).isEqualTo(now);
        assertThat(permission.getUpdatedAt()).isEqualTo(now);
        assertThat(permission.getCreatedBy()).isEqualTo(createdByUser);
        assertThat(permission.getUpdatedBy()).isEqualTo(createdByUser);
    }

    @Test
    @DisplayName("Should default hasAccess to false")
    void shouldDefaultHasAccessToFalse() {

        DamPermissionEntity permission = new DamPermissionEntity();

        assertThat(permission.getHasAccess()).isFalse();
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with User")
    void shouldMaintainManyToOneRelationshipWithUser() {

        DamPermissionEntity permission = new DamPermissionEntity();
        permission.setUser(user);

        assertThat(permission.getUser())
                .isNotNull()
                .isEqualTo(user);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with Dam")
    void shouldMaintainManyToOneRelationshipWithDam() {

        DamPermissionEntity permission = new DamPermissionEntity();
        permission.setDam(dam);

        assertThat(permission.getDam())
                .isNotNull()
                .isEqualTo(dam);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with Client")
    void shouldMaintainManyToOneRelationshipWithClient() {

        DamPermissionEntity permission = new DamPermissionEntity();
        permission.setClient(client);

        assertThat(permission.getClient())
                .isNotNull()
                .isEqualTo(client);
    }

    @Test
    @DisplayName("Should enforce unique constraint concept for user+dam+client")
    void shouldEnforceUniqueConstraintConceptForUserDamClient() {

        DamPermissionEntity permission1 = new DamPermissionEntity();
        permission1.setId(1L);
        permission1.setUser(user);
        permission1.setDam(dam);
        permission1.setClient(client);

        DamPermissionEntity permission2 = new DamPermissionEntity();
        permission2.setId(2L);
        permission2.setUser(user);
        permission2.setDam(dam);
        permission2.setClient(client);

        assertThat(permission1.getUser()).isEqualTo(permission2.getUser());
        assertThat(permission1.getDam()).isEqualTo(permission2.getDam());
        assertThat(permission1.getClient()).isEqualTo(permission2.getClient());
        assertThat(permission1.getId()).isNotEqualTo(permission2.getId());
    }

    @Test
    @DisplayName("Should allow different permissions for different users on same dam")
    void shouldAllowDifferentPermissionsForDifferentUsersOnSameDam() {

        UserEntity user2 = TestDataBuilder.user().withName("User 2").build();

        DamPermissionEntity permission1 = new DamPermissionEntity();
        permission1.setUser(user);
        permission1.setDam(dam);
        permission1.setClient(client);
        permission1.setHasAccess(true);

        DamPermissionEntity permission2 = new DamPermissionEntity();
        permission2.setUser(user2);
        permission2.setDam(dam);
        permission2.setClient(client);
        permission2.setHasAccess(false);

        assertThat(permission1.getDam()).isEqualTo(permission2.getDam());
        assertThat(permission1.getUser()).isNotEqualTo(permission2.getUser());
        assertThat(permission1.getHasAccess()).isTrue();
        assertThat(permission2.getHasAccess()).isFalse();
    }

    @Test
    @DisplayName("Should allow same user to have permissions on different dams")
    void shouldAllowSameUserToHavePermissionsOnDifferentDams() {

        DamEntity dam2 = TestDataBuilder.dam().withName("Barragem 2").build();

        DamPermissionEntity permission1 = new DamPermissionEntity();
        permission1.setUser(user);
        permission1.setDam(dam);
        permission1.setClient(client);

        DamPermissionEntity permission2 = new DamPermissionEntity();
        permission2.setUser(user);
        permission2.setDam(dam2);
        permission2.setClient(client);

        assertThat(permission1.getUser()).isEqualTo(permission2.getUser());
        assertThat(permission1.getDam()).isNotEqualTo(permission2.getDam());
    }

    @Test
    @DisplayName("Should handle audit fields - createdBy")
    void shouldHandleAuditFieldsCreatedBy() {

        UserEntity adminUser = TestDataBuilder.user().withName("Admin").build();
        DamPermissionEntity permission = new DamPermissionEntity();
        permission.setCreatedBy(adminUser);

        assertThat(permission.getCreatedBy())
                .isNotNull()
                .isEqualTo(adminUser);
    }

    @Test
    @DisplayName("Should handle audit fields - updatedBy")
    void shouldHandleAuditFieldsUpdatedBy() {

        UserEntity adminUser = TestDataBuilder.user().withName("Admin").build();
        DamPermissionEntity permission = new DamPermissionEntity();
        permission.setUpdatedBy(adminUser);

        assertThat(permission.getUpdatedBy())
                .isNotNull()
                .isEqualTo(adminUser);
    }

    @Test
    @DisplayName("Should handle audit timestamps - createdAt")
    void shouldHandleAuditTimestampsCreatedAt() {

        LocalDateTime now = LocalDateTime.now();
        DamPermissionEntity permission = new DamPermissionEntity();
        permission.setCreatedAt(now);

        assertThat(permission.getCreatedAt())
                .isNotNull()
                .isEqualTo(now);
    }

    @Test
    @DisplayName("Should handle audit timestamps - updatedAt")
    void shouldHandleAuditTimestampsUpdatedAt() {

        LocalDateTime now = LocalDateTime.now();
        DamPermissionEntity permission = new DamPermissionEntity();
        permission.setUpdatedAt(now);

        assertThat(permission.getUpdatedAt())
                .isNotNull()
                .isEqualTo(now);
    }

    @Test
    @DisplayName("Should track creation and update with different users")
    void shouldTrackCreationAndUpdateWithDifferentUsers() {

        UserEntity creator = TestDataBuilder.user().withName("Creator").build();
        UserEntity updater = TestDataBuilder.user().withName("Updater").build();

        DamPermissionEntity permission = new DamPermissionEntity();
        permission.setCreatedBy(creator);
        permission.setUpdatedBy(updater);

        assertThat(permission.getCreatedBy()).isEqualTo(creator);
        assertThat(permission.getUpdatedBy()).isEqualTo(updater);
        assertThat(permission.getCreatedBy()).isNotEqualTo(permission.getUpdatedBy());
    }

    @Test
    @DisplayName("Should toggle hasAccess flag")
    void shouldToggleHasAccessFlag() {

        DamPermissionEntity permission = new DamPermissionEntity();
        permission.setHasAccess(false);

        permission.setHasAccess(true);

        assertThat(permission.getHasAccess()).isTrue();

        permission.setHasAccess(false);

        assertThat(permission.getHasAccess()).isFalse();
    }

    @Test
    @DisplayName("Should grant access to user")
    void shouldGrantAccessToUser() {

        DamPermissionEntity permission = new DamPermissionEntity();
        permission.setUser(user);
        permission.setDam(dam);
        permission.setClient(client);
        permission.setHasAccess(false);

        permission.setHasAccess(true);

        assertThat(permission.getHasAccess()).isTrue();
    }

    @Test
    @DisplayName("Should revoke access from user")
    void shouldRevokeAccessFromUser() {

        DamPermissionEntity permission = new DamPermissionEntity();
        permission.setUser(user);
        permission.setDam(dam);
        permission.setClient(client);
        permission.setHasAccess(true);

        permission.setHasAccess(false);

        assertThat(permission.getHasAccess()).isFalse();
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        DamPermissionEntity permission = new DamPermissionEntity();
        permission.setId(1L);
        permission.setHasAccess(false);

        Long originalId = permission.getId();

        permission.setHasAccess(true);
        permission.setUpdatedAt(LocalDateTime.now());

        assertThat(permission.getId()).isEqualTo(originalId);
        assertThat(permission.getHasAccess()).isTrue();
    }

    @Test
    @DisplayName("Should handle temporal tracking of permission changes")
    void shouldHandleTemporalTrackingOfPermissionChanges() {

        LocalDateTime createdTime = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedTime = LocalDateTime.now();

        DamPermissionEntity permission = new DamPermissionEntity();
        permission.setCreatedAt(createdTime);
        permission.setUpdatedAt(updatedTime);

        assertThat(permission.getCreatedAt()).isBefore(permission.getUpdatedAt());
    }

    @Test
    @DisplayName("Should support multiple permissions per client")
    void shouldSupportMultiplePermissionsPerClient() {

        UserEntity user2 = TestDataBuilder.user().withName("User 2").build();
        DamEntity dam2 = TestDataBuilder.dam().withName("Barragem 2").build();

        DamPermissionEntity permission1 = new DamPermissionEntity();
        permission1.setUser(user);
        permission1.setDam(dam);
        permission1.setClient(client);

        DamPermissionEntity permission2 = new DamPermissionEntity();
        permission2.setUser(user2);
        permission2.setDam(dam2);
        permission2.setClient(client);

        assertThat(permission1.getClient()).isEqualTo(permission2.getClient());
        assertThat(permission1.getUser()).isNotEqualTo(permission2.getUser());
        assertThat(permission1.getDam()).isNotEqualTo(permission2.getDam());
    }

    @Test
    @DisplayName("Should allow null audit fields initially")
    void shouldAllowNullAuditFieldsInitially() {

        DamPermissionEntity permission = new DamPermissionEntity();

        assertThat(permission.getCreatedAt()).isNull();
        assertThat(permission.getUpdatedAt()).isNull();
        assertThat(permission.getCreatedBy()).isNull();
        assertThat(permission.getUpdatedBy()).isNull();
    }
}
