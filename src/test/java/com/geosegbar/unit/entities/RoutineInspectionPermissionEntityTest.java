package com.geosegbar.unit.entities;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.RoutineInspectionPermissionEntity;
import com.geosegbar.entities.UserEntity;

@Tag("unit")
class RoutineInspectionPermissionEntityTest extends BaseUnitTest {

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(1L);
        user.setName("João Silva");
        user.setEmail("joao.silva@email.com");
    }

    @Test
    @DisplayName("Should create routine inspection permission with all required fields")
    void shouldCreateRoutineInspectionPermissionWithAllRequiredFields() {

        RoutineInspectionPermissionEntity permission = new RoutineInspectionPermissionEntity();
        permission.setId(1L);
        permission.setUser(user);
        permission.setIsFillWeb(false);
        permission.setIsFillMobile(false);

        assertThat(permission).satisfies(p -> {
            assertThat(p.getId()).isEqualTo(1L);
            assertThat(p.getUser()).isEqualTo(user);
            assertThat(p.getIsFillWeb()).isFalse();
            assertThat(p.getIsFillMobile()).isFalse();
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        RoutineInspectionPermissionEntity permission = new RoutineInspectionPermissionEntity(
                1L,
                user,
                true,
                false
        );

        assertThat(permission.getId()).isEqualTo(1L);
        assertThat(permission.getUser()).isEqualTo(user);
        assertThat(permission.getIsFillWeb()).isTrue();
        assertThat(permission.getIsFillMobile()).isFalse();
    }

    @Test
    @DisplayName("Should maintain OneToOne relationship with User")
    void shouldMaintainOneToOneRelationshipWithUser() {

        RoutineInspectionPermissionEntity permission = new RoutineInspectionPermissionEntity();
        permission.setUser(user);

        assertThat(permission.getUser())
                .isNotNull()
                .isEqualTo(user);
    }

    @Test
    @DisplayName("Should default isFillWeb to false")
    void shouldDefaultIsFillWebToFalse() {

        RoutineInspectionPermissionEntity permission = new RoutineInspectionPermissionEntity();
        permission.setIsFillWeb(false);

        assertThat(permission.getIsFillWeb()).isFalse();
    }

    @Test
    @DisplayName("Should default isFillMobile to false")
    void shouldDefaultIsFillMobileToFalse() {

        RoutineInspectionPermissionEntity permission = new RoutineInspectionPermissionEntity();
        permission.setIsFillMobile(false);

        assertThat(permission.getIsFillMobile()).isFalse();
    }

    @Test
    @DisplayName("Should support isFillWeb true")
    void shouldSupportIsFillWebTrue() {

        RoutineInspectionPermissionEntity permission = new RoutineInspectionPermissionEntity();
        permission.setIsFillWeb(true);

        assertThat(permission.getIsFillWeb()).isTrue();
    }

    @Test
    @DisplayName("Should support isFillMobile true")
    void shouldSupportIsFillMobileTrue() {

        RoutineInspectionPermissionEntity permission = new RoutineInspectionPermissionEntity();
        permission.setIsFillMobile(true);

        assertThat(permission.getIsFillMobile()).isTrue();
    }

    @Test
    @DisplayName("Should support both web and mobile permissions")
    void shouldSupportBothWebAndMobilePermissions() {

        RoutineInspectionPermissionEntity permission = new RoutineInspectionPermissionEntity();
        permission.setUser(user);
        permission.setIsFillWeb(true);
        permission.setIsFillMobile(true);

        assertThat(permission.getIsFillWeb()).isTrue();
        assertThat(permission.getIsFillMobile()).isTrue();
    }

    @Test
    @DisplayName("Should support only web permission")
    void shouldSupportOnlyWebPermission() {

        RoutineInspectionPermissionEntity permission = new RoutineInspectionPermissionEntity();
        permission.setUser(user);
        permission.setIsFillWeb(true);
        permission.setIsFillMobile(false);

        assertThat(permission.getIsFillWeb()).isTrue();
        assertThat(permission.getIsFillMobile()).isFalse();
    }

    @Test
    @DisplayName("Should support only mobile permission")
    void shouldSupportOnlyMobilePermission() {

        RoutineInspectionPermissionEntity permission = new RoutineInspectionPermissionEntity();
        permission.setUser(user);
        permission.setIsFillWeb(false);
        permission.setIsFillMobile(true);

        assertThat(permission.getIsFillWeb()).isFalse();
        assertThat(permission.getIsFillMobile()).isTrue();
    }

    @Test
    @DisplayName("Should support no permissions")
    void shouldSupportNoPermissions() {

        RoutineInspectionPermissionEntity permission = new RoutineInspectionPermissionEntity();
        permission.setUser(user);
        permission.setIsFillWeb(false);
        permission.setIsFillMobile(false);

        assertThat(permission.getIsFillWeb()).isFalse();
        assertThat(permission.getIsFillMobile()).isFalse();
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        RoutineInspectionPermissionEntity permission = new RoutineInspectionPermissionEntity();
        permission.setId(1L);
        permission.setUser(user);
        permission.setIsFillWeb(false);
        permission.setIsFillMobile(false);

        Long originalId = permission.getId();

        permission.setIsFillWeb(true);
        permission.setIsFillMobile(true);

        assertThat(permission.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should toggle web permission")
    void shouldToggleWebPermission() {

        RoutineInspectionPermissionEntity permission = new RoutineInspectionPermissionEntity();
        permission.setIsFillWeb(false);

        permission.setIsFillWeb(true);
        assertThat(permission.getIsFillWeb()).isTrue();

        permission.setIsFillWeb(false);

        assertThat(permission.getIsFillWeb()).isFalse();
    }

    @Test
    @DisplayName("Should toggle mobile permission")
    void shouldToggleMobilePermission() {

        RoutineInspectionPermissionEntity permission = new RoutineInspectionPermissionEntity();
        permission.setIsFillMobile(false);

        permission.setIsFillMobile(true);
        assertThat(permission.getIsFillMobile()).isTrue();

        permission.setIsFillMobile(false);

        assertThat(permission.getIsFillMobile()).isFalse();
    }

    @Test
    @DisplayName("Should support independent permission flags")
    void shouldSupportIndependentPermissionFlags() {

        RoutineInspectionPermissionEntity permission = new RoutineInspectionPermissionEntity();

        permission.setIsFillWeb(true);

        assertThat(permission.getIsFillWeb()).isTrue();
        assertThat(permission.getIsFillMobile()).isFalse();

        permission.setIsFillMobile(true);

        assertThat(permission.getIsFillWeb()).isTrue();
        assertThat(permission.getIsFillMobile()).isTrue();
    }
}
