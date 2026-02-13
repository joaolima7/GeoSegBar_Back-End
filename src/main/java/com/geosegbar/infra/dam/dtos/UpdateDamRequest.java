package com.geosegbar.infra.dam.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDamRequest {

    @NotBlank(message = "Nome é obrigatório!")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "O campo não pode conter números!")
    private String name;

    @NotNull(message = "Latitude é obrigatório!")
    private Double latitude;

    @NotNull(message = "Longitude é obrigatório!")
    private Double longitude;

    @NotBlank(message = "O nome da rua é obrigatório!")
    private String street;

    @Size(max = 100, message = "O nome do bairro deve ter no máximo 100 caracteres!")
    private String neighborhood;

    @Size(max = 20, message = "O número do endereço deve ter no máximo 20 caracteres!")
    private String numberAddress;

    @NotBlank(message = "O nome da cidade é obrigatório!")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "Cidade não pode conter números!")
    @Size(max = 100, message = "O nome da cidade deve ter no máximo 100 caracteres!")
    private String city;

    @NotBlank(message = "O nome do estado é obrigatório!")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "Estado não pode conter números!")
    @Size(max = 100, message = "O nome do estado deve ter no máximo 100 caracteres!")
    private String state;

    @NotBlank(message = "CEP é obrigatório!")
    @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido!")
    private String zipCode;

    @Size(max = 100, message = "O complemento deve ter no máximo 100 caracteres!")
    private String complement;

    @NotNull(message = "ID do cliente é obrigatório!")
    private Long clientId;

    @NotNull(message = "ID do status é obrigatório!")
    private Long statusId;

    private Long psbLinkFolderId;

    private Long legislationLinkFolderId;

    private String logoBase64;
    private String damImageBase64;
}
