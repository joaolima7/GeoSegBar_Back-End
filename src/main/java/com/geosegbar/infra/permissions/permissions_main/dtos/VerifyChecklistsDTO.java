package com.geosegbar.infra.permissions.permissions_main.dtos;

import java.util.List;

/**
 * Verificação de permissão de checklist para uma barragem inteira, de uma vez.
 *
 * A rota de hoje responde por um par (damId, checklistId); pintar "o que posso
 * responder" custava uma chamada por par.
 *
 * Os motivos de negação vêm com CÓDIGO além do texto, de propósito: são cinco
 * motivos distintos, cada um apontando um cadastro diferente, e é essa
 * granularidade que permite dizer ao inspetor o que fazer em vez de só "sem
 * permissão". O código é estável; o texto é de quem desenha a tela.
 */
public record VerifyChecklistsDTO(
        Long damId,
        List<ChecklistPermission> checklists) {

    public record ChecklistPermission(
            Long checklistId,
            String checklistName,
            boolean allowed,
            /**
             * Nulo quando allowed. Um de: NOT_IN_CLIENT, NO_DAM_ACCESS,
             * CHECKLIST_NOT_IN_DAM, NO_ROUTINE_PERMISSION, NO_MOBILE_FILL,
             * NO_WEB_FILL.
             */
            String reasonCode,
            String reason) {

    }
}
