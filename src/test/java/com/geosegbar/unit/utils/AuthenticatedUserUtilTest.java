package com.geosegbar.unit.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.geosegbar.common.enums.RoleEnum;
import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.RoleEntity;
import com.geosegbar.entities.RoutineInspectionPermissionEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.UnauthorizedException;

@Tag("unit")
class AuthenticatedUserUtilTest extends BaseUnitTest {

    private SecurityContext securityContext;
    private Authentication authentication;
    private UserEntity mockUser;
    private RoleEntity mockRole;

    @BeforeEach
    void setUp() {
        securityContext = mock(SecurityContext.class);
        authentication = mock(Authentication.class);
        SecurityContextHolder.setContext(securityContext);

        mockUser = new UserEntity();
        mockUser.setId(1L);
        mockUser.setName("Test User");
        mockUser.setEmail("test@example.com");

        mockRole = new RoleEntity();
        mockRole.setId(1L);
        mockUser.setRole(mockRole);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should get current user when authenticated")
    void shouldGetCurrentUserWhenAuthenticated() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockUser);

        // When
        UserEntity currentUser = AuthenticatedUserUtil.getCurrentUser();

        // Then
        assertThat(currentUser).isNotNull();
        assertThat(currentUser.getId()).isEqualTo(1L);
        assertThat(currentUser.getName()).isEqualTo("Test User");
        assertThat(currentUser.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should throw exception when authentication is null")
    void shouldThrowExceptionWhenAuthenticationIsNull() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> AuthenticatedUserUtil.getCurrentUser())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Usuário não autenticado");
    }

    @Test
    @DisplayName("Should throw exception when user is not authenticated")
    void shouldThrowExceptionWhenUserIsNotAuthenticated() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> AuthenticatedUserUtil.getCurrentUser())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Usuário não autenticado");
    }

    @Test
    @DisplayName("Should throw exception when principal is null")
    void shouldThrowExceptionWhenPrincipalIsNull() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> AuthenticatedUserUtil.getCurrentUser())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Usuário não autenticado");
    }

    @Test
    @DisplayName("Should throw exception when principal is not UserEntity")
    void shouldThrowExceptionWhenPrincipalIsNotUserEntity() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("not-a-user-entity");

        // When & Then
        assertThatThrownBy(() -> AuthenticatedUserUtil.getCurrentUser())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Erro ao obter usuário autenticado");
    }

    @Test
    @DisplayName("Should return true when user is admin")
    void shouldReturnTrueWhenUserIsAdmin() {
        // Given
        mockRole.setName(RoleEnum.ADMIN);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockUser);

        // When
        boolean isAdmin = AuthenticatedUserUtil.isAdmin();

        // Then
        assertThat(isAdmin).isTrue();
    }

    @Test
    @DisplayName("Should return false when user is not admin")
    void shouldReturnFalseWhenUserIsNotAdmin() {
        // Given
        mockRole.setName(RoleEnum.COLLABORATOR);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockUser);

        // When
        boolean isAdmin = AuthenticatedUserUtil.isAdmin();

        // Then
        assertThat(isAdmin).isFalse();
    }

    @Test
    @DisplayName("Should pass check admin permission when user is admin")
    void shouldPassCheckAdminPermissionWhenUserIsAdmin() {
        // Given
        mockRole.setName(RoleEnum.ADMIN);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockUser);

        // When & Then (no exception thrown)
        AuthenticatedUserUtil.checkAdminPermission();
    }

    @Test
    @DisplayName("Should throw exception when check admin permission fails")
    void shouldThrowExceptionWhenCheckAdminPermissionFails() {
        // Given
        mockRole.setName(RoleEnum.COLLABORATOR);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockUser);

        // When & Then
        assertThatThrownBy(() -> AuthenticatedUserUtil.checkAdminPermission())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Acesso negado. Permissão de administrador necessária.");
    }

    @Test
    @DisplayName("Should pass check role when user has required role")
    void shouldPassCheckRoleWhenUserHasRequiredRole() {
        // Given
        mockRole.setName(RoleEnum.ADMIN);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockUser);

        // When & Then (no exception thrown)
        AuthenticatedUserUtil.checkRole("ADMIN");
    }

    @Test
    @DisplayName("Should pass check role when user has one of multiple allowed roles")
    void shouldPassCheckRoleWhenUserHasOneOfMultipleAllowedRoles() {
        // Given
        mockRole.setName(RoleEnum.COLLABORATOR);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockUser);

        // When & Then (no exception thrown)
        AuthenticatedUserUtil.checkRole("ADMIN", "COLLABORATOR");
    }

    @Test
    @DisplayName("Should throw exception when check role fails")
    void shouldThrowExceptionWhenCheckRoleFails() {
        // Given
        mockRole.setName(RoleEnum.COLLABORATOR);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockUser);

        // When & Then
        assertThatThrownBy(() -> AuthenticatedUserUtil.checkRole("ADMIN"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Acesso negado. Permissão insuficiente para esta operação.");
    }

    @Test
    @DisplayName("Should return true for routine inspection permission when user is admin")
    void shouldReturnTrueForRoutineInspectionPermissionWhenUserIsAdmin() {
        // Given
        mockRole.setName(RoleEnum.ADMIN);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockUser);

        // When
        boolean hasPermission = AuthenticatedUserUtil.hasRoutineInspectionPermission(false);

        // Then
        assertThat(hasPermission).isTrue();
    }

    @Test
    @DisplayName("Should return true when user has routine inspection permission")
    void shouldReturnTrueWhenUserHasRoutineInspectionPermission() {
        // Given
        mockRole.setName(RoleEnum.COLLABORATOR);
        RoutineInspectionPermissionEntity permission = new RoutineInspectionPermissionEntity();
        permission.setIsFillWeb(true);
        mockUser.setRoutineInspectionPermission(permission);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockUser);

        // When
        boolean hasPermission = AuthenticatedUserUtil.hasRoutineInspectionPermission(false);

        // Then
        assertThat(hasPermission).isTrue();
    }

    @Test
    @DisplayName("Should return false when user has no routine inspection permission")
    void shouldReturnFalseWhenUserHasNoRoutineInspectionPermission() {
        // Given
        mockRole.setName(RoleEnum.COLLABORATOR);
        mockUser.setRoutineInspectionPermission(null);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockUser);

        // When
        boolean hasPermission = AuthenticatedUserUtil.hasRoutineInspectionPermission(false);

        // Then
        assertThat(hasPermission).isFalse();
    }
}
