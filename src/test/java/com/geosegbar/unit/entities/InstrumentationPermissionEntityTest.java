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

        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();

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

        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setUser(user);

        assertThat(permission.getUser())
                .isNotNull()
                .isEqualTo(user);
    }

    @Test
    @DisplayName("Should enforce unique user constraint concept")
    void shouldEnforceUniqueUserConstraintConcept() {

        InstrumentationPermissionEntity permission1 = new InstrumentationPermissionEntity();
        permission1.setId(1L);
        permission1.setUser(user);

        InstrumentationPermissionEntity permission2 = new InstrumentationPermissionEntity();
        permission2.setId(2L);
        permission2.setUser(user);

        assertThat(permission1.getUser()).isEqualTo(permission2.getUser());
        assertThat(permission1.getId()).isNotEqualTo(permission2.getId());
    }

    @Test
    @DisplayName("Should grant graph view permission")
    void shouldGrantGraphViewPermission() {

        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setViewGraphs(false);

        permission.setViewGraphs(true);

        assertThat(permission.getViewGraphs()).isTrue();
    }

    @Test
    @DisplayName("Should grant graph edit local permission")
    void shouldGrantGraphEditLocalPermission() {

        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setEditGraphsLocal(false);

        permission.setEditGraphsLocal(true);

        assertThat(permission.getEditGraphsLocal()).isTrue();
    }

    @Test
    @DisplayName("Should grant graph edit default permission")
    void shouldGrantGraphEditDefaultPermission() {

        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setEditGraphsDefault(false);

        permission.setEditGraphsDefault(true);

        assertThat(permission.getEditGraphsDefault()).isTrue();
    }

    @Test
    @DisplayName("Should toggle permissions independently")
    void shouldTogglePermissionsIndependently() {

        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();

        permission.setViewGraphs(true);

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

        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setViewGraphs(true);
        permission.setEditGraphsLocal(true);
        permission.setEditGraphsDefault(false);

        assertThat(permission.getViewGraphs()).isTrue();
        assertThat(permission.getEditGraphsLocal()).isTrue();
        assertThat(permission.getEditGraphsDefault()).isFalse();
    }

    @Test
    @DisplayName("Should support read permissions category")
    void shouldSupportReadPermissionsCategory() {

        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setViewRead(true);
        permission.setEditRead(true);

        assertThat(permission.getViewRead()).isTrue();
        assertThat(permission.getEditRead()).isTrue();
    }

    @Test
    @DisplayName("Should support sections permissions category")
    void shouldSupportSectionsPermissionsCategory() {

        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setViewSections(true);
        permission.setEditSections(false);

        assertThat(permission.getViewSections()).isTrue();
        assertThat(permission.getEditSections()).isFalse();
    }

    @Test
    @DisplayName("Should support instruments permissions category")
    void shouldSupportInstrumentsPermissionsCategory() {

        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setViewInstruments(true);
        permission.setEditInstruments(true);

        assertThat(permission.getViewInstruments()).isTrue();
        assertThat(permission.getEditInstruments()).isTrue();
    }

    @Test
    @DisplayName("Should support read-only access across all categories")
    void shouldSupportReadOnlyAccessAcrossAllCategories() {

        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setViewGraphs(true);
        permission.setViewRead(true);
        permission.setViewSections(true);
        permission.setViewInstruments(true);

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

        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();

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

        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setEditGraphsLocal(true);
        permission.setEditGraphsDefault(false);

        assertThat(permission.getEditGraphsLocal()).isTrue();
        assertThat(permission.getEditGraphsDefault()).isFalse();
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setId(1L);
        permission.setViewGraphs(false);

        Long originalId = permission.getId();

        permission.setViewGraphs(true);
        permission.setEditGraphsLocal(true);

        assertThat(permission.getId()).isEqualTo(originalId);
        assertThat(permission.getViewGraphs()).isTrue();
    }

    @Test
    @DisplayName("Should allow different users with different permission sets")
    void shouldAllowDifferentUsersWithDifferentPermissionSets() {

        UserEntity user2 = TestDataBuilder.user().withName("User 2").build();

        InstrumentationPermissionEntity permission1 = new InstrumentationPermissionEntity();
        permission1.setUser(user);
        permission1.setViewGraphs(true);
        permission1.setEditGraphsLocal(false);

        InstrumentationPermissionEntity permission2 = new InstrumentationPermissionEntity();
        permission2.setUser(user2);
        permission2.setViewGraphs(true);
        permission2.setEditGraphsLocal(true);

        assertThat(permission1.getUser()).isNotEqualTo(permission2.getUser());
        assertThat(permission1.getEditGraphsLocal()).isFalse();
        assertThat(permission2.getEditGraphsLocal()).isTrue();
    }

    @Test
    @DisplayName("Should handle permission escalation")
    void shouldHandlePermissionEscalation() {

        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setViewGraphs(true);

        permission.setEditGraphsLocal(true);

        assertThat(permission.getViewGraphs()).isTrue();
        assertThat(permission.getEditGraphsLocal()).isTrue();
    }

    @Test
    @DisplayName("Should handle permission downgrade")
    void shouldHandlePermissionDowngrade() {

        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setViewGraphs(true);
        permission.setEditGraphsLocal(true);
        permission.setEditGraphsDefault(true);

        permission.setEditGraphsLocal(false);
        permission.setEditGraphsDefault(false);

        assertThat(permission.getViewGraphs()).isTrue();
        assertThat(permission.getEditGraphsLocal()).isFalse();
        assertThat(permission.getEditGraphsDefault()).isFalse();
    }

    @Test
    @DisplayName("Should support partial permissions per category")
    void shouldSupportPartialPermissionsPerCategory() {

        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setViewGraphs(true);
        permission.setEditGraphsLocal(true);
        permission.setViewRead(true);
        permission.setViewInstruments(true);

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
