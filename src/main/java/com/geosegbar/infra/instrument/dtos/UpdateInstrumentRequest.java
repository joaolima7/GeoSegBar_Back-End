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
public class UpdateInstrumentRequest {

    @NotBlank(message = "Nome do instrumento é obrigatório")
    private String name;

    private String location;

    private Double distanceOffset;

    @NotNull(message = "Latitude é obrigatória")
    private Double latitude;

    @NotNull(message = "Longitude é obrigatória")
    private Double longitude;

    @NotNull(message = "Campo 'sem limites' é obrigatório")
    private Boolean noLimit;

    private Boolean activeForSection = true;

    @NotNull(message = "ID da barragem é obrigatório")
    private Long damId;

    @NotNull(message = "ID do tipo de instrumento é obrigatório")
    private Long instrumentTypeId;

    private Long sectionId;

    @Valid
    @NotNull(message = "Lista de inputs é obrigatória")
    private List<InputDTO> inputs;

    private List<ConstantDTO> constants;

    @Valid
    @NotNull(message = "Lista de outputs é obrigatória")
    private List<OutputDTO> outputs;
}
