package com.geosegbar.exceptions;

import com.geosegbar.infra.checklist_response.dtos.InspectionDateDivergenceDTO;

import lombok.Getter;

/**
 * Mover a inspecao de data invalidaria respostas de outras inspecoes ja
 * gravadas. Carrega a lista do que mudaria para a tela poder mostrar antes de
 * pedir confirmacao, em vez de so dizer que nao deu.
 */
@Getter
public class InspectionDateDivergenceException extends RuntimeException {

    private final transient InspectionDateDivergenceDTO preview;

    public InspectionDateDivergenceException(InspectionDateDivergenceDTO preview) {
        super("A mudança de data torna irregulares respostas de inspeções posteriores. "
                + "Confirme para prosseguir.");
        this.preview = preview;
    }
}
