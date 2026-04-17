package com.geosegbar.infra.pae.dtos;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PAEResponseDTO {

    private Long id;

    private Long damId;

    private String damName;

    private String coordinatorName;

    private String coordinatorPhone;

    private String coordinatorEmail;

    private String substituteCoordinatorName;

    private String substituteCoordinatorPhone;

    private String substituteCoordinatorEmail;

    private Integer residences;

    private Integer people;

    private Integer sensiblePoints;

    private LocalDate lastCadastralSurvey;

    private Integer simulationParticipants;

    private LocalDate lastSimulationDate;

    private List<PAEProtectionElementDTO> protectionElements;

    private List<PAEContactDTO> zasContacts;

    private List<PAEContactDTO> zssContacts;
}
