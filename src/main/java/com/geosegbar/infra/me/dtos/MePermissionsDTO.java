package com.geosegbar.infra.me.dtos;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tudo que o app precisa saber sobre o próprio acesso, numa chamada.
 *
 * Existe porque o LoginResponseDTO não devolve permissão nenhuma: logo após o
 * login o app não sabia se podia preencher checklist nem quais barragens via.
 * Sem onde guardar isso, ele não conseguia responder "pode?" sem rede — e daí
 * saía a contradição que o usuário relatou: online dizia que não tinha
 * permissão, offline deixava preencher.
 */
public record MePermissionsDTO(
        Long userId,
        String role,
        RoutineInspection routineInspection,
        List<DamAccess> dams,
        /**
         * Quando a permissão foi alterada pela última vez. Permite ao app
         * dizer "permissões conferidas há 2 dias" quando estiver offline, em
         * vez de fingir certeza.
         */
        LocalDateTime updatedAt) {

    public record RoutineInspection(Boolean isFillMobile, Boolean isFillWeb) {

    }

    /**
     * As barragens sem acesso vêm na lista, com hasAccess = false de
     * propósito: o app mostra a barragem bloqueada com o motivo, em vez de
     * sumir com ela e deixar o inspetor sem entender o que aconteceu.
     */
    public record DamAccess(Long damId, String damName, Long clientId, Boolean hasAccess) {

    }
}
