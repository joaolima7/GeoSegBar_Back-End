package com.geosegbar.infra.instrument_tabulate_pattern.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTabulatePatternRequestDTO {

    @NotBlank(message = "Nome do padrão é obrigatório!")
    private String name;

    @NotNull(message = "ID da barragem é obrigatório!")
    private Long damId;

    private Long folderId;

    @NotEmpty(message = "Pelo menos uma associação de instrumento é obrigatória!")
    @Size(min = 1, message = "Pelo menos uma associação de instrumento é obrigatória!")
    @Valid
    private List<InstrumentAssociationDTO> associations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstrumentAssociationDTO {

        @NotNull(message = "ID do instrumento é obrigatório!")
        private Long instrumentId;

        private Boolean isDateEnable;
        private Integer dateIndex;
        private Boolean isHourEnable;
        private Integer hourIndex;
        private Boolean isUserEnable;
        private Integer userIndex;
        private Boolean isReadEnable;

        @NotEmpty(message = "Pelo menos uma associação de output é obrigatória!")
        @Size(min = 1, message = "Pelo menos uma associação de output é obrigatória!")
        @Valid
        private List<OutputAssociationDTO> outputAssociations;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutputAssociationDTO {

        @NotNull(message = "ID do output é obrigatório!")
        private Long outputId;

        @NotNull(message = "Índice do output é obrigatório!")
        private Integer outputIndex;
    }
}
