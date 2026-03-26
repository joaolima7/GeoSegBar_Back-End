package com.geosegbar.infra.checklist_response.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerUpdateDTO {

    @NotNull(message = "ID da resposta é obrigatório!")
    @Positive(message = "ID da resposta deve ser um número positivo!")
    private Long answerId;

    @NotNull(message = "ID da opção selecionada é obrigatório!")
    @Positive(message = "ID da opção deve ser um número positivo!")
    private Long selectedOptionId;
}
