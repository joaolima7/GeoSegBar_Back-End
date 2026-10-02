package com.geosegbar.infra.checklist_response.services;

import com.geosegbar.entities.AnswerEntity;
import com.geosegbar.infra.checklist_response.dtos.AnswerUpdateDTO;

/**
 * A validação de evidência e a gravação precisam enxergar a mesma localização.
 * Enquanto a validação olhava só o que estava no banco, uma resposta que exigia
 * localização e não tinha nenhuma não podia ser salva de jeito nenhum: a tela
 * pedia a localização e o backend descartava o que ela mandava.
 */
public final class AnswerLocationResolver {

    private AnswerLocationResolver() {
    }

    public static Double resolveLatitude(AnswerEntity answer, AnswerUpdateDTO update) {
        if (isClearRequested(update)) {
            return null;
        }
        return update.getLatitude() != null ? update.getLatitude() : answer.getLatitude();
    }

    public static Double resolveLongitude(AnswerEntity answer, AnswerUpdateDTO update) {
        if (isClearRequested(update)) {
            return null;
        }
        return update.getLongitude() != null ? update.getLongitude() : answer.getLongitude();
    }

    public static boolean isClearRequested(AnswerUpdateDTO update) {
        return Boolean.TRUE.equals(update.getClearLocation());
    }

    public static boolean hasLocation(AnswerEntity answer, AnswerUpdateDTO update) {
        return resolveLatitude(answer, update) != null && resolveLongitude(answer, update) != null;
    }
}
