package com.geosegbar.infra.regulatory_dam.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegulatoryDamDTO {
    
    private Long id;
    
    @NotNull(message = "O ID da barragem é obrigatório")
    private Long damId;
    
    private Boolean framePNSB;
    
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