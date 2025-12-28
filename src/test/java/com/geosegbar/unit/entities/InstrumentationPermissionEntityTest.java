package com.geosegbar.unit.entities;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.InstrumentationPermissionEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - InstrumentationPermissionEntity")
class InstrumentationPermissionEntityTest extends BaseUnitTest {

    private UserEntity user;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();
        user = TestDataBuilder.user().build();
    }

    @Test
    @DisplayName("Should create instrumentation permission with all fields")
    void shouldCreateInstrumentationPermissionWithAllFields() {
        // Given
        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setId(1L);
        permission.setUser(user);
        permission.setViewGraphs(true);
        permission.setEditGraphsLocal(true);
        permission.setEditGraphsDefault(true);
        permission.setViewRead(true);
        permission.setEditRead(true);
        permission.setViewSections(true);
        permission.setEditSections(true);
        permission.setViewInstruments(true);
        permission.setEditInstruments(true);

        // Then
        assertThat(permission).satisfies(p -> {
            assertThat(p.getId()).isEqualTo(1L);
            assertThat(p.getUser()).isEqualTo(user);
            assertThat(p.getViewGraphs()).isTrue();
            assertThat(p.getEditGraphsLocal()).isTrue();
            assertThat(p.getEditGraphsDefault()).isTrue();
            assertThat(p.getViewRead()).isTrue();
            assertThat(p.getEditRead()).isTrue();
            assertThat(p.getViewSections()).isTrue();
            assertThat(p.getEditSections()).isTrue();
            assertThat(p.getViewInstruments()).isTrue();
            assertThat(p.getEditInstruments()).isTrue();
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {
        // Given & When
        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity(
                1L,
                user,
                true,
                true,
                false,
                true,
                false,
                true,
                false,
                true,
                false
        );

        // Then
        assertThat(permission.getId()).isEqualTo(1L);
        assertThat(permission.getUser()).isEqualTo(user);
        assertThat(permission.getViewGraphs()).isTrue();
        assertThat(permission.getEditGraphsLocal()).isTrue();
        assertThat(permission.getEditGraphsDefault()).isFalse();
        assertThat(permission.getViewRead()).isTrue();
        assertThat(permission.getEditRead()).isFalse();
        assertThat(permission.getViewSections()).isTrue();
        assertThat(permission.getEditSections()).isFalse();
        assertThat(permission.getViewInstruments()).isTrue();
        assertThat(permission.getEditInstruments()).isFalse();
    }

    @Test
    @DisplayName("Should default all permissions to false")
    void shouldDefaultAllPermissionsToFalse() {
        // Given & When
        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();

        // Then
        assertThat(permission.getViewGraphs()).isFalse();
        assertThat(permission.getEditGraphsLocal()).isFalse();
        assertThat(permission.getEditGraphsDefault()).isFalse();
        assertThat(permission.getViewRead()).isFalse();
        assertThat(permission.getEditRead()).isFalse();
        assertThat(permission.getViewSections()).isFalse();
        assertThat(permission.getEditSections()).isFalse();
        assertThat(permission.getViewInstruments()).isFalse();
        assertThat(permission.getEditInstruments()).isFalse();
    }

    @Test
    @DisplayName("Should maintain OneToOne relationship with User")
    void shouldMaintainOneToOneRelationshipWithUser() {
        // Given
        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
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
        InstrumentationPermissionEntity permission1 = new InstrumentationPermissionEntity();
        permission1.setId(1L);
        permission1.setUser(user);

        InstrumentationPermissionEntity permission2 = new InstrumentationPermissionEntity();
        permission2.setId(2L);
        permission2.setUser(user);

        // Then - In database, this would violate unique constraint
        assertThat(permission1.getUser()).isEqualTo(permission2.getUser());
        assertThat(permission1.getId()).isNotEqualTo(permission2.getId());
    }

    @Test
    @DisplayName("Should grant graph view permission")
    void shouldGrantGraphViewPermission() {
        // Given
        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setViewGraphs(false);

        // When
        permission.setViewGraphs(true);

        // Then
        assertThat(permission.getViewGraphs()).isTrue();
    }

    @Test
    @DisplayName("Should grant graph edit local permission")
    void shouldGrantGraphEditLocalPermission() {
        // Given
        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setEditGraphsLocal(false);

        // When
        permission.setEditGraphsLocal(true);

        // Then
        assertThat(permission.getEditGraphsLocal()).isTrue();
    }

    @Test
    @DisplayName("Should grant graph edit default permission")
    void shouldGrantGraphEditDefaultPermission() {
        // Given
        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setEditGraphsDefault(false);

        // When
        permission.setEditGraphsDefault(true);

        // Then
        assertThat(permission.getEditGraphsDefault()).isTrue();
    }

    @Test
    @DisplayName("Should toggle permissions independently")
    void shouldTogglePermissionsIndependently() {
        // Given
        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();

        // When - Grant only viewGraphs
        permission.setViewGraphs(true);

        // Then
        assertThat(permission.getViewGraphs()).isTrue();
        assertThat(permission.getEditGraphsLocal()).isFalse();
        assertThat(permission.getEditGraphsDefault()).isFalse();
        assertThat(permission.getViewRead()).isFalse();
        assertThat(permission.getEditRead()).isFalse();
        assertThat(permission.getViewSections()).isFalse();
        assertThat(permission.getEditSections()).isFalse();
        assertThat(permission.getViewInstruments()).isFalse();
        assertThat(permission.getEditInstruments()).isFalse();
    }

    @Test
    @DisplayName("Should support graph permissions category")
    void shouldSupportGraphPermissionsCategory() {
        // Given
        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setViewGraphs(true);
        permission.setEditGraphsLocal(true);
        permission.setEditGraphsDefault(false);

        // Then - Graph-related permissions
        assertThat(permission.getViewGraphs()).isTrue();
        assertThat(permission.getEditGraphsLocal()).isTrue();
        assertThat(permission.getEditGraphsDefault()).isFalse();
    }

    @Test
    @DisplayName("Should support read permissions category")
    void shouldSupportReadPermissionsCategory() {
        // Given
        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setViewRead(true);
        permission.setEditRead(true);

        // Then - Read-related permissions
        assertThat(permission.getViewRead()).isTrue();
        assertThat(permission.getEditRead()).isTrue();
    }

    @Test
    @DisplayName("Should support sections permissions category")
    void shouldSupportSectionsPermissionsCategory() {
        // Given
        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setViewSections(true);
        permission.setEditSections(false);

        // Then - Sections-related permissions
        assertThat(permission.getViewSections()).isTrue();
        assertThat(permission.getEditSections()).isFalse();
    }

    @Test
    @DisplayName("Should support instruments permissions category")
    void shouldSupportInstrumentsPermissionsCategory() {
        // Given
        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setViewInstruments(true);
        permission.setEditInstruments(true);

        // Then - Instruments-related permissions
        assertThat(permission.getViewInstruments()).isTrue();
        assertThat(permission.getEditInstruments()).isTrue();
    }

    @Test
    @DisplayName("Should support read-only access across all categories")
    void shouldSupportReadOnlyAccessAcrossAllCategories() {
        // Given
        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setViewGraphs(true);
        permission.setViewRead(true);
        permission.setViewSections(true);
        permission.setViewInstruments(true);

        // Then - All view permissions granted, no edit permissions
        assertThat(permission.getViewGraphs()).isTrue();
        assertThat(permission.getEditGraphsLocal()).isFalse();
        assertThat(permission.getEditGraphsDefault()).isFalse();
        assertThat(permission.getViewRead()).isTrue();
        assertThat(permission.getEditRead()).isFalse();
        assertThat(permission.getViewSections()).isTrue();
        assertThat(permission.getEditSections()).isFalse();
        assertThat(permission.getViewInstruments()).isTrue();
        assertThat(permission.getEditInstruments()).isFalse();
    }

    @Test
    @DisplayName("Should support full access pattern")
    void shouldSupportFullAccessPattern() {
        // Given
        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setViewGraphs(true);
        permission.setEditGraphsLocal(true);
        permission.setEditGraphsDefault(true);
        permission.setViewRead(true);
        permission.setEditRead(true);
        permission.setViewSections(true);
        permission.setEditSections(true);
        permission.setViewInstruments(true);
        permission.setEditInstruments(true);

        // Then - All permissions granted
        assertThat(permission.getViewGraphs()).isTrue();
        assertThat(permission.getEditGraphsLocal()).isTrue();
        assertThat(permission.getEditGraphsDefault()).isTrue();
        assertThat(permission.getViewRead()).isTrue();
        assertThat(permission.getEditRead()).isTrue();
        assertThat(permission.getViewSections()).isTrue();
        assertThat(permission.getEditSections()).isTrue();
        assertThat(permission.getViewInstruments()).isTrue();
        assertThat(permission.getEditInstruments()).isTrue();
    }

    @Test
    @DisplayName("Should support no access pattern")
    void shouldSupportNoAccessPattern() {
        // Given
        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();

        // Then - All permissions false by default
        assertThat(permission.getViewGraphs()).isFalse();
        assertThat(permission.getEditGraphsLocal()).isFalse();
        assertThat(permission.getEditGraphsDefault()).isFalse();
        assertThat(permission.getViewRead()).isFalse();
        assertThat(permission.getEditRead()).isFalse();
        assertThat(permission.getViewSections()).isFalse();
        assertThat(permission.getEditSections()).isFalse();
        assertThat(permission.getViewInstruments()).isFalse();
        assertThat(permission.getEditInstruments()).isFalse();
    }

    @Test
    @DisplayName("Should differentiate local and default graph editing")
    void shouldDifferentiateLocalAndDefaultGraphEditing() {
        // Given
        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setEditGraphsLocal(true);
        permission.setEditGraphsDefault(false);

        // Then - Can edit local but not default graphs
        assertThat(permission.getEditGraphsLocal()).isTrue();
        assertThat(permission.getEditGraphsDefault()).isFalse();
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {
        // Given
        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setId(1L);
        permission.setViewGraphs(false);

        Long originalId = permission.getId();

        // When
        permission.setViewGraphs(true);
        permission.setEditGraphsLocal(true);

        // Then
        assertThat(permission.getId()).isEqualTo(originalId);
        assertThat(permission.getViewGraphs()).isTrue();
    }

    @Test
    @DisplayName("Should allow different users with different permission sets")
    void shouldAllowDifferentUsersWithDifferentPermissionSets() {
        // Given
        UserEntity user2 = TestDataBuilder.user().withName("User 2").build();

        InstrumentationPermissionEntity permission1 = new InstrumentationPermissionEntity();
        permission1.setUser(user);
        permission1.setViewGraphs(true);
        permission1.setEditGraphsLocal(false);

        InstrumentationPermissionEntity permission2 = new InstrumentationPermissionEntity();
        permission2.setUser(user2);
        permission2.setViewGraphs(true);
        permission2.setEditGraphsLocal(true);

        // Then
        assertThat(permission1.getUser()).isNotEqualTo(permission2.getUser());
        assertThat(permission1.getEditGraphsLocal()).isFalse();
        assertThat(permission2.getEditGraphsLocal()).isTrue();
    }

    @Test
    @DisplayName("Should handle permission escalation")
    void shouldHandlePermissionEscalation() {
        // Given - User starts with view only
        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setViewGraphs(true);

        // When - Escalate to edit
        permission.setEditGraphsLocal(true);

        // Then
        assertThat(permission.getViewGraphs()).isTrue();
        assertThat(permission.getEditGraphsLocal()).isTrue();
    }

    @Test
    @DisplayName("Should handle permission downgrade")
    void shouldHandlePermissionDowngrade() {
        // Given - User has full access
        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setViewGraphs(true);
        permission.setEditGraphsLocal(true);
        permission.setEditGraphsDefault(true);

        // When - Downgrade to view only
        permission.setEditGraphsLocal(false);
        permission.setEditGraphsDefault(false);

        // Then
        assertThat(permission.getViewGraphs()).isTrue();
        assertThat(permission.getEditGraphsLocal()).isFalse();
        assertThat(permission.getEditGraphsDefault()).isFalse();
    }

    @Test
    @DisplayName("Should support partial permissions per category")
    void shouldSupportPartialPermissionsPerCategory() {
        // Given
        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setViewGraphs(true);
        permission.setEditGraphsLocal(true);
        permission.setViewRead(true);
        permission.setViewInstruments(true);

        // Then - Mixed permissions across categories
        assertThat(permission.getViewGraphs()).isTrue();
        assertThat(permission.getEditGraphsLocal()).isTrue();
        assertThat(permission.getEditGraphsDefault()).isFalse();
        assertThat(permission.getViewRead()).isTrue();
        assertThat(permission.getEditRead()).isFalse();
        assertThat(permission.getViewSections()).isFalse();
        assertThat(permission.getEditSections()).isFalse();
        assertThat(permission.getViewInstruments()).isTrue();
        assertThat(permission.getEditInstruments()).isFalse();
    }
}
