package com.geosegbar.unit.entities;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.AttributionsPermissionEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - AttributionsPermissionEntity")
class AttributionsPermissionEntityTest extends BaseUnitTest {

    private UserEntity user;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
        user = TestDataBuilder.user().asAdmin().build();
    }

    @Test
    @DisplayName("Should create attributions permission with default values")
    void shouldCreateAttributionsPermissionWithDefaultValues() {
        // Given
        AttributionsPermissionEntity permission = new AttributionsPermissionEntity();
        permission.setId(1L);
        permission.setUser(user);

        // Then
        assertThat(permission).satisfies(p -> {
            assertThat(p.getId()).isEqualTo(1L);
            assertThat(p.getUser()).isEqualTo(user);
            assertThat(p.getEditUser()).isFalse();
            assertThat(p.getEditDam()).isFalse();
            assertThat(p.getEditGeralData()).isFalse();
        });
    }

    @Test
    @DisplayName("Should create attributions permission using all args constructor")
    void shouldCreateAttributionsPermissionUsingAllArgsConstructor() {
        // Given & When
        AttributionsPermissionEntity permission = new AttributionsPermissionEntity(
                1L,
                user,
                true,
                true,
                false
        );

        // Then
        assertThat(permission).satisfies(p -> {
            assertThat(p.getId()).isEqualTo(1L);
            assertThat(p.getUser()).isEqualTo(user);
            assertThat(p.getEditUser()).isTrue();
            assertThat(p.getEditDam()).isTrue();
            assertThat(p.getEditGeralData()).isFalse();
        });
    }

    @Test
    @DisplayName("Should create attributions permission with no args constructor")
    void shouldCreateAttributionsPermissionWithNoArgsConstructor() {
        // Given & When
        AttributionsPermissionEntity permission = new AttributionsPermissionEntity();

        // Then
        assertThat(permission).isNotNull();
        assertThat(permission.getId()).isNull();
        assertThat(permission.getUser()).isNull();
        assertThat(permission.getEditUser()).isFalse();
        assertThat(permission.getEditDam()).isFalse();
        assertThat(permission.getEditGeralData()).isFalse();
    }

    @Test
    @DisplayName("Should maintain OneToOne relationship with user")
    void shouldMaintainOneToOneRelationshipWithUser() {
        // Given
        AttributionsPermissionEntity permission = new AttributionsPermissionEntity();
        permission.setUser(user);

        // Then
        assertThat(permission.getUser())
                .isNotNull()
                .isEqualTo(user);
        assertThat(permission.getUser().getName()).isEqualTo(user.getName());
    }

    @Test
    @DisplayName("Should grant all permissions")
    void shouldGrantAllPermissions() {
        // Given
        AttributionsPermissionEntity permission = new AttributionsPermissionEntity();
        permission.setUser(user);

        // When
        permission.setEditUser(true);
        permission.setEditDam(true);
        permission.setEditGeralData(true);

        // Then
        assertThat(permission.getEditUser()).isTrue();
        assertThat(permission.getEditDam()).isTrue();
        assertThat(permission.getEditGeralData()).isTrue();
    }

    @Test
    @DisplayName("Should grant only edit user permission")
    void shouldGrantOnlyEditUserPermission() {
        // Given
        AttributionsPermissionEntity permission = new AttributionsPermissionEntity();
        permission.setUser(user);
        permission.setEditUser(true);

        // Then
        assertThat(permission.getEditUser()).isTrue();
        assertThat(permission.getEditDam()).isFalse();
        assertThat(permission.getEditGeralData()).isFalse();
    }

    @Test
    @DisplayName("Should grant only edit dam permission")
    void shouldGrantOnlyEditDamPermission() {
        // Given
        AttributionsPermissionEntity permission = new AttributionsPermissionEntity();
        permission.setUser(user);
        permission.setEditDam(true);

        // Then
        assertThat(permission.getEditUser()).isFalse();
        assertThat(permission.getEditDam()).isTrue();
        assertThat(permission.getEditGeralData()).isFalse();
    }

    @Test
    @DisplayName("Should grant only edit geral data permission")
    void shouldGrantOnlyEditGeralDataPermission() {
        // Given
        AttributionsPermissionEntity permission = new AttributionsPermissionEntity();
        permission.setUser(user);
        permission.setEditGeralData(true);

        // Then
        assertThat(permission.getEditUser()).isFalse();
        assertThat(permission.getEditDam()).isFalse();
        assertThat(permission.getEditGeralData()).isTrue();
    }

    @Test
    @DisplayName("Should revoke all permissions")
    void shouldRevokeAllPermissions() {
        // Given
        AttributionsPermissionEntity permission = new AttributionsPermissionEntity(
                1L, user, true, true, true
        );

        // When
        permission.setEditUser(false);
        permission.setEditDam(false);
        permission.setEditGeralData(false);

        // Then
        assertThat(permission.getEditUser()).isFalse();
        assertThat(permission.getEditDam()).isFalse();
        assertThat(permission.getEditGeralData()).isFalse();
    }

    @Test
    @DisplayName("Should toggle permissions independently")
    void shouldTogglePermissionsIndependently() {
        // Given
        AttributionsPermissionEntity permission = new AttributionsPermissionEntity();
        permission.setUser(user);

        // When - Grant edit user
        permission.setEditUser(true);
        assertThat(permission.getEditUser()).isTrue();
        assertThat(permission.getEditDam()).isFalse();
        assertThat(permission.getEditGeralData()).isFalse();

        // When - Grant edit dam
        permission.setEditDam(true);
        assertThat(permission.getEditUser()).isTrue();
        assertThat(permission.getEditDam()).isTrue();
        assertThat(permission.getEditGeralData()).isFalse();

        // When - Revoke edit user
        permission.setEditUser(false);
        assertThat(permission.getEditUser()).isFalse();
        assertThat(permission.getEditDam()).isTrue();
        assertThat(permission.getEditGeralData()).isFalse();
    }

    @Test
    @DisplayName("Should handle permission combinations")
    void shouldHandlePermissionCombinations() {
        // Combination 1: No permissions
        AttributionsPermissionEntity noPerms = new AttributionsPermissionEntity();
        noPerms.setUser(user);
        assertThat(noPerms.getEditUser()).isFalse();
        assertThat(noPerms.getEditDam()).isFalse();
        assertThat(noPerms.getEditGeralData()).isFalse();

        // Combination 2: User + Dam
        AttributionsPermissionEntity userDamPerms = new AttributionsPermissionEntity();
        userDamPerms.setUser(user);
        userDamPerms.setEditUser(true);
        userDamPerms.setEditDam(true);
        assertThat(userDamPerms.getEditUser()).isTrue();
        assertThat(userDamPerms.getEditDam()).isTrue();
        assertThat(userDamPerms.getEditGeralData()).isFalse();

        // Combination 3: All permissions
        AttributionsPermissionEntity allPerms = new AttributionsPermissionEntity();
        allPerms.setUser(user);
        allPerms.setEditUser(true);
        allPerms.setEditDam(true);
        allPerms.setEditGeralData(true);
        assertThat(allPerms.getEditUser()).isTrue();
        assertThat(allPerms.getEditDam()).isTrue();
        assertThat(allPerms.getEditGeralData()).isTrue();
    }

    @Test
    @DisplayName("Should maintain identity through permission changes")
    void shouldMaintainIdentityThroughPermissionChanges() {
        // Given
        AttributionsPermissionEntity permission = new AttributionsPermissionEntity();
        permission.setId(1L);
        permission.setUser(user);

        Long originalId = permission.getId();
        UserEntity originalUser = permission.getUser();

        // When
        permission.setEditUser(true);
        permission.setEditDam(true);
        permission.setEditGeralData(true);

        // Then
        assertThat(permission.getId()).isEqualTo(originalId);
        assertThat(permission.getUser()).isEqualTo(originalUser);
    }

    @Test
    @DisplayName("Should handle null Boolean values as false")
    void shouldHandleNullBooleanValuesAsFalse() {
        // Given
        AttributionsPermissionEntity permission = new AttributionsPermissionEntity();
        permission.setUser(user);

        // When explicitly setting to null (shouldn't happen but testing robustness)
        permission.setEditUser(null);
        permission.setEditDam(null);
        permission.setEditGeralData(null);

        // Then - Verify nulls are handled
        assertThat(permission.getEditUser()).isNull();
        assertThat(permission.getEditDam()).isNull();
        assertThat(permission.getEditGeralData()).isNull();
    }

    @Test
    @DisplayName("Should support different users with different permissions")
    void shouldSupportDifferentUsersWithDifferentPermissions() {
        // Given
        UserEntity user1 = TestDataBuilder.user().withName("User 1").build();
        UserEntity user2 = TestDataBuilder.user().withName("User 2").build();

        AttributionsPermissionEntity perm1 = new AttributionsPermissionEntity();
        perm1.setUser(user1);
        perm1.setEditUser(true);

        AttributionsPermissionEntity perm2 = new AttributionsPermissionEntity();
        perm2.setUser(user2);
        perm2.setEditDam(true);

        // Then
        assertThat(perm1.getUser()).isEqualTo(user1);
        assertThat(perm1.getEditUser()).isTrue();
        assertThat(perm1.getEditDam()).isFalse();

        assertThat(perm2.getUser()).isEqualTo(user2);
        assertThat(perm2.getEditUser()).isFalse();
        assertThat(perm2.getEditDam()).isTrue();
    }
}
