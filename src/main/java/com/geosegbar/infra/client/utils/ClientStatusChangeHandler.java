package com.geosegbar.infra.client.utils;

import org.springframework.stereotype.Component;

import com.geosegbar.common.enums.StatusEnum;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.StatusEntity;
import com.geosegbar.infra.dam.services.DamService;
import com.geosegbar.infra.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClientStatusChangeHandler {

    private final DamService damService;
    private final UserService userService;

    public boolean handleStatusChange(ClientEntity client, StatusEntity newStatus) {

        if (client.getStatus() != null
                && client.getStatus().getId().equals(newStatus.getId())) {
            log.debug("Status do cliente {} não mudou. Nenhuma ação necessária.", client.getId());
            return false;
        }

        StatusEnum oldStatusEnum = client.getStatus() != null
                ? client.getStatus().getStatus() : null;
        StatusEnum newStatusEnum = newStatus.getStatus();

        log.info("Detectada mudança de status do cliente {} de {} para {}",
                client.getId(), oldStatusEnum, newStatusEnum);

        if (isStatusSyncRequired(oldStatusEnum, newStatusEnum)) {

            int updatedDams = damService.synchronizeClientDamsStatus(client.getId(), newStatus);

            log.info("Status do cliente {} alterado. {} barragem(ns) sincronizada(s).",
                    client.getId(), updatedDams);

            if (newStatusEnum == StatusEnum.DISABLED) {
                log.info("Cliente {} desativado. Iniciando desativação assíncrona de usuários...", client.getId());
                userService.deactivateClientUsersAsync(client.getId(), newStatus.getId());
            }

            return true;
        }

        return false;
    }

    private boolean isStatusSyncRequired(StatusEnum oldStatus, StatusEnum newStatus) {
        if (oldStatus == null) {
            return false;
        }

        return (oldStatus == StatusEnum.ACTIVE && newStatus == StatusEnum.DISABLED)
                || (oldStatus == StatusEnum.DISABLED && newStatus == StatusEnum.ACTIVE);
    }
}
