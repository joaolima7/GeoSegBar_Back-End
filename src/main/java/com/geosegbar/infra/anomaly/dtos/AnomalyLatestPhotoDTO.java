package com.geosegbar.infra.anomaly.dtos;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Foto mais recente do ponto de uma anomalia vinda de checklist: as fotos da
 * resposta mais recente (pela data da inspeção) à mesma pergunta, do mesmo
 * questionário, na mesma barragem. photoDate é a data dessa inspeção.
 */
public record AnomalyLatestPhotoDTO(
        Long anomalyId,
        Long checklistResponseId,
        LocalDateTime photoDate,
        List<String> photoPaths) {

}
