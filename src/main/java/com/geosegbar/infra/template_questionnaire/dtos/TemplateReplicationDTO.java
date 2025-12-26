package com.geosegbar.infra.template_questionnaire.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateReplicationDTO {

    @NotNull(message = "ID do template de origem é obrigatório!")
    @Positive(message = "ID do template de origem deve ser um número positivo!")
    private Long sourceTemplateId;

    @NotNull(message = "ID da barragem de destino é obrigatório!")
    @Positive(message = "ID da barragem de destino deve ser um número positivo!")
    private Long targetDamId;
}
