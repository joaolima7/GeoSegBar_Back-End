package com.geosegbar.infra.pae.dtos;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PAEDTO {

    private Long id;

    @NotNull(message = "ID da barragem é obrigatório!")
    private Long damId;

    private String coordinatorName;

    private String coordinatorPhone;

    @Email(message = "Email do coordenador inválido!")
    private String coordinatorEmail;

    private String substituteCoordinatorName;

    private String substituteCoordinatorPhone;

    @Email(message = "Email do coordenador substituto inválido!")
    private String substituteCoordinatorEmail;

    private Integer residences;

    private Integer people;

    private Integer sensiblePoints;

    private LocalDate lastCadastralSurvey;

    private Integer simulationParticipants;

    private LocalDate lastSimulationDate;

    // null = manter existentes; lista (vazia ou com itens) = substituir todos
    @Valid
    private List<PAEProtectionElementDTO> protectionElements;

    // null = manter existentes; lista (vazia ou com itens) = substituir todos
    @Valid
    private List<PAEContactDTO> contacts;
}
