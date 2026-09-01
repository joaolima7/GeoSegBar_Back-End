package com.geosegbar.infra.me.services;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.DamPermissionEntity;
import com.geosegbar.entities.RoutineInspectionPermissionEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.me.dtos.MePermissionsDTO;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * O que o usuário do token pode fazer, enxuto.
 *
 * A rota que já existia — /user-permissions/user/{userId} — serializa
 * entidades JPA inteiras e traz documentationPermission,
 * attributionsPermission e instrumentationPermission, que o app não usa, além
 * de exigir que ele carregue o userId na mão.
 */
@Service
@RequiredArgsConstructor
public class MeService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public MePermissionsDTO currentUserPermissions() {
        UserEntity user = userRepository
                .findByIdWithPermissions(AuthenticatedUserUtil.getCurrentUser().getId())
                .orElseThrow(() -> new NotFoundException("Usuário logado não encontrado"));

        RoutineInspectionPermissionEntity routine = user.getRoutineInspectionPermission();

        MePermissionsDTO.RoutineInspection routineDTO = routine == null
                ? new MePermissionsDTO.RoutineInspection(false, false)
                : new MePermissionsDTO.RoutineInspection(
                        Boolean.TRUE.equals(routine.getIsFillMobile()),
                        Boolean.TRUE.equals(routine.getIsFillWeb()));

        List<DamPermissionEntity> permissions = user.getDamPermissions() == null
                ? List.of()
                : user.getDamPermissions().stream()
                        .filter(p -> p.getDam() != null)
                        .sorted(Comparator.comparing(p -> p.getDam().getId()))
                        .toList();

        List<MePermissionsDTO.DamAccess> dams = permissions.stream()
                .map(p -> new MePermissionsDTO.DamAccess(
                p.getDam().getId(),
                p.getDam().getName(),
                p.getClient() != null ? p.getClient().getId() : null,
                Boolean.TRUE.equals(p.getHasAccess())))
                .toList();

        return new MePermissionsDTO(
                user.getId(),
                user.getRole() != null ? user.getRole().getName().name() : null,
                routineDTO,
                dams,
                lastChangedAt(permissions));
    }

    /**
     * A mais recente entre as datas de alteração das permissões de barragem.
     * A permissão de inspeção de rotina não tem carimbo de alteração, então
     * não entra na conta — é melhor o app saber a data de algo do que não
     * saber a de nada.
     */
    private LocalDateTime lastChangedAt(List<DamPermissionEntity> permissions) {
        return permissions.stream()
                .map(p -> p.getUpdatedAt() != null ? p.getUpdatedAt() : p.getCreatedAt())
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }
}
