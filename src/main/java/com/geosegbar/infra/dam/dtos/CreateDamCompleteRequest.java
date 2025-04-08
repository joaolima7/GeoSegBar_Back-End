package com.geosegbar.infra.dam.dtos;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
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
public class CreateDamCompleteRequest {
    // Dam fields
    @NotBlank(message = "Nome é obrigatório!")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "O campo não pode conter números!")
    private String name;
    
    @NotNull(message = "Latitude é obrigatório!")
    private Double latitude;
    
    @NotNull(message = "Longitude é obrigatório!")
    private Double longitude;
    
    @NotBlank(message = "O nome da rua é obrigatório!")
    private String street;
    
    @NotBlank(message = "O nome do bairro é obrigatório!")
    @Size(max = 100, message = "O nome do bairro deve ter no máximo 100 caracteres!")
    private String neighborhood;
    
    @Pattern(regexp = "^[0-9]*$", message = "O número do endereço deve conter apenas números!")
    @Size(max = 10, message = "O número do endereço deve ter no máximo 10 caracteres!")
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
    
    @NotNull(message = "ID do cliente é obrigatório!")
    private Long clientId;
    
    @NotNull(message = "ID do status é obrigatório!")
    private Long statusId;
    
    private String logoBase64;
    private String damImageBase64;
    private String linkPSB;
    private String linkLegislation;
    
    // DocumentationDam fields
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
    
    // RegulatoryDam fields
    private Boolean framePNSB = false;
    private String representativeName;
    
    @Email(message = "Email do representante inválido!")
    private String representativeEmail;
    
    @Pattern(regexp = "^(\\d{10,11})?$", message = "O telefone do representante deve conter 10 ou 11 dígitos numéricos ou ser vazio!")
    private String representativePhone;
    
    private String technicalManagerName;
    
    @Email(message = "Email do responsável técnico inválido!")
    private String technicalManagerEmail;
    
    @Pattern(regexp = "^(\\d{10,11})?$", message = "O telefone do responsável técnico deve conter 10 ou 11 dígitos numéricos ou ser vazio!")
    private String technicalManagerPhone;
    
    private Long securityLevelId;
    private Long supervisoryBodyId;
    private Long riskCategoryId;
    private Long potentialDamageId;
    private Long classificationDamId;
}