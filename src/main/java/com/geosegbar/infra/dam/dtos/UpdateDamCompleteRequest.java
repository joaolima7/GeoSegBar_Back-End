package com.geosegbar.infra.dam.dtos;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDamCompleteRequest {

    // ==========================================
    // DADOS BÁSICOS DA BARRAGEM
    // ==========================================
    @NotBlank(message = "Nome da barragem é obrigatório")
    private String name;

    private Double latitude;
    private Double longitude;
    private String street;
    private String neighborhood;
    private String numberAddress;
    private String city;
    private String state;
    private String zipCode;
    private String linkPSB;
    private String linkLegislation;

    @NotNull(message = "Cliente é obrigatório")
    private Long clientId;

    @NotNull(message = "Status é obrigatório")
    private Long statusId;

    // Base64 das imagens (opcional - se não enviar, mantém a existente)
    private String logoBase64;
    private String damImageBase64;

    // ==========================================
    // DOCUMENTATION DAM
    // ==========================================
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
    private LocalDate lastInternalSimulation;
    private LocalDate nextInternalSimulation;
    private LocalDate lastExternalSimulation;
    private LocalDate nextExternalSimulation;

    // ==========================================
    // REGULATORY DAM
    // ==========================================
    @NotNull(message = "Frame PNSB é obrigatório")
    private Boolean framePNSB;

    private String representativeName;

    @Email(message = "Email do representante inválido")
    private String representativeEmail;

    private String representativePhone;
    private String technicalManagerName;

    @Email(message = "Email do gestor técnico inválido")
    private String technicalManagerEmail;

    private String technicalManagerPhone;
    private String supervisoryBodyName;

    // IDs das entidades relacionadas (opcionais)
    private Long securityLevelId;
    private Long riskCategoryId;
    private Long potentialDamageId;
    private Long classificationDamId;

    // ==========================================
    // RESERVOIRS
    // ==========================================
    @Valid
    private List<ReservoirRequestDTO> reservoirs;
}
