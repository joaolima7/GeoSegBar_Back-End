package com.geosegbar.unit.entities;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.DocumentationPermissionEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - DocumentationPermissionEntity")
class DocumentationPermissionEntityTest extends BaseUnitTest {

    private UserEntity user;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
        user = TestDataBuilder.user().build();
    }

    @Test
    @DisplayName("Should create documentation permission with all fields")
    void shouldCreateDocumentationPermissionWithAllFields() {
        // Given
        DocumentationPermissionEntity permission = new DocumentationPermissionEntity();
        permission.setId(1L);
        permission.setUser(user);
        permission.setViewPSB(true);
        permission.setEditPSB(true);
        permission.setSharePSB(true);

        // Then
        assertThat(permission).satisfies(p -> {
            assertThat(p.getId()).isEqualTo(1L);
            assertThat(p.getUser()).isEqualTo(user);
            assertThat(p.getViewPSB()).isTrue();
            assertThat(p.getEditPSB()).isTrue();
            assertThat(p.getSharePSB()).isTrue();
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {
        // Given & When
        DocumentationPermissionEntity permission = new DocumentationPermissionEntity(
                1L,
                user,
                true,
                true,
                false
        );

        // Then
        assertThat(permission.getId()).isEqualTo(1L);
        assertThat(permission.getUser()).isEqualTo(user);
        assertThat(permission.getViewPSB()).isTrue();
        assertThat(permission.getEditPSB()).isTrue();
        assertThat(permission.getSharePSB()).isFalse();
    }

    @Test
    @DisplayName("Should default all permissions to false")
    void shouldDefaultAllPermissionsToFalse() {
        // Given & When
        DocumentationPermissionEntity permission = new DocumentationPermissionEntity();

        // Then
        assertThat(permission.getViewPSB()).isFalse();
        assertThat(permission.getEditPSB()).isFalse();
        assertThat(permission.getSharePSB()).isFalse();
    }

    @Test
    @DisplayName("Should maintain OneToOne relationship with User")
    void shouldMaintainOneToOneRelationshipWithUser() {
        // Given
        DocumentationPermissionEntity permission = new DocumentationPermissionEntity();
        permission.setUser(user);

        // Then
        assertThat(permission.getUser())
                .isNotNull()
                .isEqualTo(user);
    }

    @Test
    @DisplayName("Should enforce unique user constraint concept")
    void shouldEnforceUniqueUserConstraintConcept() {
        // Given
        DocumentationPermissionEntity permission1 = new DocumentationPermissionEntity();
        permission1.setId(1L);
        permission1.setUser(user);

        DocumentationPermissionEntity permission2 = new DocumentationPermissionEntity();
        permission2.setId(2L);
        permission2.setUser(user);

        // Then - In database, this would violate unique constraint
        assertThat(permission1.getUser()).isEqualTo(permission2.getUser());
        assertThat(permission1.getId()).isNotEqualTo(permission2.getId());
    }

    @Test
    @DisplayName("Should grant view permission")
    void shouldGrantViewPermission() {
        // Given
        DocumentationPermissionEntity permission = new DocumentationPermissionEntity();
        permission.setViewPSB(false);

        // When
        permission.setViewPSB(true);

        // Then
        assertThat(permission.getViewPSB()).isTrue();
    }

    @Test
    @DisplayName("Should grant edit permission")
    void shouldGrantEditPermission() {
        // Given
        DocumentationPermissionEntity permission = new DocumentationPermissionEntity();
        permission.setEditPSB(false);

        // When
        permission.setEditPSB(true);

        // Then
        assertThat(permission.getEditPSB()).isTrue();
    }

    @Test
    @DisplayName("Should grant share permission")
    void shouldGrantSharePermission() {
        // Given
        DocumentationPermissionEntity permission = new DocumentationPermissionEntity();
        permission.setSharePSB(false);

        // When
        permission.setSharePSB(true);

        // Then
        assertThat(permission.getSharePSB()).isTrue();
    }

    @Test
    @DisplayName("Should revoke view permission")
    void shouldRevokeViewPermission() {
        // Given
        DocumentationPermissionEntity permission = new DocumentationPermissionEntity();
        permission.setViewPSB(true);

        // When
        permission.setViewPSB(false);

        // Then
        assertThat(permission.getViewPSB()).isFalse();
    }

    @Test
    @DisplayName("Should toggle permissions independently")
    void shouldTogglePermissionsIndependently() {
        // Given
        DocumentationPermissionEntity permission = new DocumentationPermissionEntity();

        // When - Grant only view
        permission.setViewPSB(true);

        // Then
        assertThat(permission.getViewPSB()).isTrue();
        assertThat(permission.getEditPSB()).isFalse();
        assertThat(permission.getSharePSB()).isFalse();
    }

    @Test
    @DisplayName("Should support read-only access pattern")
    void shouldSupportReadOnlyAccessPattern() {
        // Given
        DocumentationPermissionEntity permission = new DocumentationPermissionEntity();
        permission.setUser(user);
        permission.setViewPSB(true);
        permission.setEditPSB(false);
        permission.setSharePSB(false);

        // Then
        assertThat(permission.getViewPSB()).isTrue();
        assertThat(permission.getEditPSB()).isFalse();
        assertThat(permission.getSharePSB()).isFalse();
    }

    @Test
    @DisplayName("Should support full access pattern")
    void shouldSupportFullAccessPattern() {
        // Given
        DocumentationPermissionEntity permission = new DocumentationPermissionEntity();
        permission.setUser(user);
        permission.setViewPSB(true);
        permission.setEditPSB(true);
        permission.setSharePSB(true);

        // Then
        assertThat(permission.getViewPSB()).isTrue();
        assertThat(permission.getEditPSB()).isTrue();
        assertThat(permission.getSharePSB()).isTrue();
    }

    @Test
    @DisplayName("Should support no access pattern")
    void shouldSupportNoAccessPattern() {
        // Given
        DocumentationPermissionEntity permission = new DocumentationPermissionEntity();
        permission.setUser(user);

        // Then - All permissions false by default
        assertThat(permission.getViewPSB()).isFalse();
        assertThat(permission.getEditPSB()).isFalse();
        assertThat(permission.getSharePSB()).isFalse();
    }

    @Test
    @DisplayName("Should support edit without share permission")
    void shouldSupportEditWithoutSharePermission() {
        // Given
        DocumentationPermissionEntity permission = new DocumentationPermissionEntity();
        permission.setViewPSB(true);
        permission.setEditPSB(true);
        permission.setSharePSB(false);

        // Then
        assertThat(permission.getViewPSB()).isTrue();
        assertThat(permission.getEditPSB()).isTrue();
        assertThat(permission.getSharePSB()).isFalse();
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {
        // Given
        DocumentationPermissionEntity permission = new DocumentationPermissionEntity();
        permission.setId(1L);
        permission.setViewPSB(false);

        Long originalId = permission.getId();

        // When
        permission.setViewPSB(true);
        permission.setEditPSB(true);

        // Then
        assertThat(permission.getId()).isEqualTo(originalId);
        assertThat(permission.getViewPSB()).isTrue();
    }

    @Test
    @DisplayName("Should allow different users with different permissions")
    void shouldAllowDifferentUsersWithDifferentPermissions() {
        // Given
        UserEntity user2 = TestDataBuilder.user().withName("User 2").build();

        DocumentationPermissionEntity permission1 = new DocumentationPermissionEntity();
        permission1.setUser(user);
        permission1.setViewPSB(true);
        permission1.setEditPSB(false);

        DocumentationPermissionEntity permission2 = new DocumentationPermissionEntity();
        permission2.setUser(user2);
        permission2.setViewPSB(true);
        permission2.setEditPSB(true);

        // Then
        assertThat(permission1.getUser()).isNotEqualTo(permission2.getUser());
        assertThat(permission1.getEditPSB()).isFalse();
        assertThat(permission2.getEditPSB()).isTrue();
    }

    @Test
    @DisplayName("Should handle permission escalation")
    void shouldHandlePermissionEscalation() {
        // Given - User starts with view only
        DocumentationPermissionEntity permission = new DocumentationPermissionEntity();
        permission.setViewPSB(true);

        // When - Escalate to edit
        permission.setEditPSB(true);

        // Then
        assertThat(permission.getViewPSB()).isTrue();
        assertThat(permission.getEditPSB()).isTrue();
    }

    @Test
    @DisplayName("Should handle permission downgrade")
    void shouldHandlePermissionDowngrade() {
        // Given - User has full access
        DocumentationPermissionEntity permission = new DocumentationPermissionEntity();
        permission.setViewPSB(true);
        permission.setEditPSB(true);
        permission.setSharePSB(true);

        // When - Downgrade to view only
        permission.setEditPSB(false);
        permission.setSharePSB(false);

        // Then
        assertThat(permission.getViewPSB()).isTrue();
        assertThat(permission.getEditPSB()).isFalse();
        assertThat(permission.getSharePSB()).isFalse();
    }
}
