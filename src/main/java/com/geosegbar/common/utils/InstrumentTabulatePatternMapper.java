package com.geosegbar.common.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.geosegbar.entities.InstrumentTabulateAssociationEntity;
import com.geosegbar.entities.InstrumentTabulateOutputAssociationEntity;
import com.geosegbar.entities.InstrumentTabulatePatternEntity;
import com.geosegbar.infra.instrument_tabulate_pattern.dtos.TabulatePatternResponseDTO;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InstrumentTabulatePatternMapper {

    public TabulatePatternResponseDTO mapToResponseDTO(InstrumentTabulatePatternEntity pattern) {
        TabulatePatternResponseDTO dto = new TabulatePatternResponseDTO();
        dto.setId(pattern.getId());
        dto.setName(pattern.getName());

        // Dam
        if (pattern.getDam() != null) {
            dto.setDam(new TabulatePatternResponseDTO.DamSummaryDTO(
                    pattern.getDam().getId(),
                    pattern.getDam().getName()));
        }

        // Folder
        if (pattern.getFolder() != null) {
            dto.setFolder(new TabulatePatternResponseDTO.FolderSummaryDTO(
                    pattern.getFolder().getId(),
                    pattern.getFolder().getName()));
        }

        // Associations
        List<TabulatePatternResponseDTO.InstrumentAssociationDTO> associationDTOs = new ArrayList<>();

        for (InstrumentTabulateAssociationEntity association : pattern.getAssociations()) {
            TabulatePatternResponseDTO.InstrumentAssociationDTO assocDTO = new TabulatePatternResponseDTO.InstrumentAssociationDTO();
            assocDTO.setId(association.getId());
            assocDTO.setInstrumentId(association.getInstrument().getId());
            assocDTO.setInstrumentName(association.getInstrument().getName());
            assocDTO.setIsDateEnable(association.getIsDateEnable());
            assocDTO.setDateIndex(association.getDateIndex());
            assocDTO.setIsHourEnable(association.getIsHourEnable());
            assocDTO.setHourIndex(association.getHourIndex());
            assocDTO.setIsUserEnable(association.getIsUserEnable());
            assocDTO.setUserIndex(association.getUserIndex());
            assocDTO.setIsReadEnable(association.getIsReadEnable());

            // Output associations
            List<TabulatePatternResponseDTO.OutputAssociationDTO> outputDTOs = new ArrayList<>();

            for (InstrumentTabulateOutputAssociationEntity outputAssoc : association.getOutputAssociations()) {
                TabulatePatternResponseDTO.OutputAssociationDTO outputDTO = new TabulatePatternResponseDTO.OutputAssociationDTO();
                outputDTO.setId(outputAssoc.getId());
                outputDTO.setOutputId(outputAssoc.getOutput().getId());
                outputDTO.setOutputName(outputAssoc.getOutput().getName());
                outputDTO.setOutputAcronym(outputAssoc.getOutput().getAcronym());
                outputDTO.setOutputIndex(outputAssoc.getOutputIndex());

                if (outputAssoc.getOutput().getMeasurementUnit() != null) {
                    outputDTO.setMeasurementUnit(new TabulatePatternResponseDTO.MeasurementUnitDTO(
                            outputAssoc.getOutput().getMeasurementUnit().getId(),
                            outputAssoc.getOutput().getMeasurementUnit().getName(),
                            outputAssoc.getOutput().getMeasurementUnit().getAcronym()));
                }

                outputDTOs.add(outputDTO);
            }

            // Ordenar outputs por índice
            outputDTOs.sort(Comparator.comparing(TabulatePatternResponseDTO.OutputAssociationDTO::getOutputIndex));
            assocDTO.setOutputAssociations(outputDTOs);

            associationDTOs.add(assocDTO);
        }

        dto.setAssociations(associationDTOs);

        return dto;
    }
}
