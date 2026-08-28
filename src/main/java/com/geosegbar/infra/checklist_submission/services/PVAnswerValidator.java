package com.geosegbar.infra.checklist_submission.services;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.geosegbar.infra.checklist_submission.dtos.AnswerSubmissionDTO;

import lombok.RequiredArgsConstructor;

/**
 * Identifica se uma resposta marca uma anomalia nova (PV — Primeira Vez).
 *
 * A validação dos campos obrigatórios do PV NÃO mora mais aqui: ela é um caso
 * da tabela única de
 * {@link com.geosegbar.common.utils.ChecklistOptionTransitionValidator}, junto
 * com NE, NI e as opções de evolução. Manter duas listas de campos obrigatórios
 * do PV em lugares diferentes foi o que deixou "Observação" de fora por muito
 * tempo — o app sempre mandava, então ninguém percebeu.
 *
 * O que sobrou aqui é a pergunta que o fluxo de persistência precisa fazer:
 * esta resposta gera uma anomalia?
 */
@Component
@RequiredArgsConstructor
public class PVAnswerValidator {

    public boolean isPVAnswer(AnswerSubmissionDTO answerDto, Map<Long, String> optionsCache) {
        if (answerDto.getSelectedOptionIds() == null || answerDto.getSelectedOptionIds().isEmpty()) {
            return false;
        }

        for (Long optionId : answerDto.getSelectedOptionIds()) {
            if ("PV".equals(optionsCache.get(optionId))) {
                return true;
            }
        }

        return false;
    }
}
