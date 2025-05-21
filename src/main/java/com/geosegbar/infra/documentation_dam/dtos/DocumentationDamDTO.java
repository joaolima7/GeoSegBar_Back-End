package com.geosegbar.infra.documentation_dam.dtos;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentationDamDTO {

    private Long id;

    @NotNull(message = "O ID da barragem é obrigatório")
    private Long damId;

    private LocalDate lastUpdatePAE;
    private LocalDate nextUpdatePAE;
    private LocalDate lastUpdatePSB;
    private LocalDate nextUpdatePSB;
    private LocalDate lastUpdateRPSB;
    private LocalDate nextUpdateRPSB;
    private LocalDate lastAchievementISR;
    private LocalDate nextAchievementISR;
    private LocalDate lastAchievementChecklist;
    private LocalDate nextAchievementChecklist;
    private LocalDate lastFillingFSB;
    private LocalDate nextFillingFSB;
    private LocalDate lastInternalSimulation;
    private LocalDate nextInternalSimulation;
    private LocalDate lastExternalSimulation;
    private LocalDate nextExternalSimulation;
}
