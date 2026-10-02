package com.geosegbar.infra.checklist_response.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import com.geosegbar.entities.AnomalyEntity;
import com.geosegbar.exceptions.InvalidInputException;

/**
 * A engenharia preenche checklist no papel em campo e lança no sistema semanas
 * depois, então a data da inspeção é apontada à mão. Duas regras seguram isso:
 *
 * <p>Data no futuro é recusada — uma inspeção adiante no tempo viraria a mais
 * recente da barragem e passaria a ditar quais rótulos a próxima inspeção pode
 * usar, pela cadeia de regras de monitoramento.
 *
 * <p>As anomalias geradas pela inspeção acompanham a mudança preservando o
 * deslocamento entre elas: a que foi registrada cinco minutos depois do início
 * continua cinco minutos depois. Jogar todas no mesmo instante embaralharia a
 * ordem em que foram apontadas.
 */
public final class InspectionDateChange {

    private InspectionDateChange() {
    }

    public static void requireNotFuture(LocalDateTime inspectionDate) {
        if (inspectionDate != null && inspectionDate.isAfter(LocalDateTime.now())) {
            throw new InvalidInputException("A data da inspeção não pode estar no futuro.");
        }
    }

    public static void shiftAnomalies(List<AnomalyEntity> anomalies,
            LocalDateTime previousDate, LocalDateTime newDate) {
        if (previousDate == null || newDate == null) {
            return;
        }

        Duration shift = Duration.between(previousDate, newDate);
        if (shift.isZero()) {
            return;
        }

        for (AnomalyEntity anomaly : anomalies) {
            if (anomaly.getCreatedAt() != null) {
                anomaly.setCreatedAt(anomaly.getCreatedAt().plus(shift));
            }
        }
    }
}
