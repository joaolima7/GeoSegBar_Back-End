package com.geosegbar.infra.instrument.dtos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

    private Double latitude;

    private Double longitude;

    @NotNull(message = "Campo 'Sem limites' é obrigatório")
    private Boolean noLimit;

    private Boolean activeForSection = true;

    @NotNull(message = "Campo 'É Régua Linimétrica' é obrigatório")
    private Boolean isLinimetricRuler = false;

    private Long linimetricRulerCode;

    @NotNull(message = "ID da barragem é obrigatório")
    private Long damId;

    @NotNull(message = "ID do tipo de instrumento é obrigatório")
    private Long instrumentTypeId;

    private Long sectionId;

    @Valid
    private List<InputDTO> inputs;

    private List<ConstantDTO> constants;

    @Valid
    private List<OutputDTO> outputs;
}
