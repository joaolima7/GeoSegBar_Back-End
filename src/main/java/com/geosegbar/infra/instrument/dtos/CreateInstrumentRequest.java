package com.geosegbar.infra.instrument.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateInstrumentRequest {

    @NotBlank(message = "Nome do instrumento é obrigatório")
    private String name;

    private String location;

    private Double distanceOffset;

    @NotNull(message = "Latitude é obrigatória")
    private Double latitude;

    @NotNull(message = "Longitude é obrigatória")
    private Double longitude;

    @NotNull(message = "Campo 'Sem limites' é obrigatório")
    private Boolean noLimit;

    @NotNull(message = "ID da barragem é obrigatório")
    private Long damId;

    @NotBlank(message = "Tipo de instrumento é obrigatório")
    private String instrumentType;

    @NotNull(message = "ID da seção é obrigatório")
    private Long sectionId;

    @NotEmpty(message = "Pelo menos um input é obrigatório")
    @Valid
    private List<InputDTO> inputs;

    private List<ConstantDTO> constants;

    @NotEmpty(message = "Pelo menos um output é obrigatório")
    @Valid
    private List<OutputDTO> outputs;
}
