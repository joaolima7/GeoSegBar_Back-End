package com.geosegbar.common.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.geosegbar.entities.RoutineInspectionPermissionEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.UnauthorizedException;

@Component
public class AuthenticatedUserUtil {

    public static UserEntity getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null) {
            throw new UnauthorizedException("Usuário não autenticado");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserEntity) {
            return (UserEntity) principal;
        }

        throw new UnauthorizedException("Erro ao obter usuário autenticado");
    }

    public static boolean isAdmin() {
        UserEntity user = getCurrentUser();
        return user.getRole().getName().toString().equals("ADMIN");
    }

    public static void checkAdminPermission() {
        if (!isAdmin()) {
            throw new UnauthorizedException("Acesso negado. Permissão de administrador necessária.");
        }
    }

    public static void checkRole(String... roles) {
        UserEntity user = getCurrentUser();
        String userRole = user.getRole().getName().toString();

        for (String role : roles) {
            if (userRole.equals(role)) {
                return; // Tem a permissão
            }
        }

        throw new UnauthorizedException("Acesso negado. Permissão insuficiente para esta operação.");
    }

    public static boolean hasRoutineInspectionPermission(boolean isMobile) {
        UserEntity currentUser = getCurrentUser();

        if (currentUser.getRole().getName().toString().equals("ADMIN")) {
            return true;
        }

        RoutineInspectionPermissionEntity permissions = currentUser.getRoutineInspectionPermission();
        return permissions != null;
    }
}
