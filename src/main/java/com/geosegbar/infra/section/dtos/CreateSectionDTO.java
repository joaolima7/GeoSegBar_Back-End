package com.geosegbar.infra.section.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSectionDTO {

    @NotBlank(message = "Nome da Seção é obrigatório!")
    private String name;

    @NotNull(message = "Primeiro vértice da Latitude é obrigatório!")
    private Double firstVertexLatitude;

    @NotNull(message = "Segundo vértice da Latitude é obrigatório!")
    private Double secondVertexLatitude;

    @NotNull(message = "Primeiro vértice da Longitude é obrigatório!")
    private Double firstVertexLongitude;

    @NotNull(message = "Segundo vértice da Longitude é obrigatório!")
    private Double secondVertexLongitude;

    @NotNull(message = "ID da barragem é obrigatório")
    private Long damId;
}
