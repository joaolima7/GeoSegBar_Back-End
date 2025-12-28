package com.geosegbar.unit.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.geosegbar.common.utils.InstrumentTabulatePatternMapper;
import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.InstrumentTabulateAssociationEntity;
import com.geosegbar.entities.InstrumentTabulateOutputAssociationEntity;
import com.geosegbar.entities.InstrumentTabulatePatternEntity;
import com.geosegbar.entities.InstrumentTabulatePatternFolder;
import com.geosegbar.entities.MeasurementUnitEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.infra.instrument_tabulate_pattern.dtos.TabulatePatternResponseDTO;

@Tag("unit")
class InstrumentTabulatePatternMapperTest extends BaseUnitTest {

    private InstrumentTabulatePatternMapper mapper;
    private InstrumentTabulatePatternEntity pattern;
    private DamEntity dam;
    private InstrumentTabulatePatternFolder folder;

    @BeforeEach
    void setUp() {
        mapper = new InstrumentTabulatePatternMapper();
        pattern = new InstrumentTabulatePatternEntity();
        dam = mock(DamEntity.class);
        folder = mock(InstrumentTabulatePatternFolder.class);

        pattern.setId(1L);
        pattern.setName("Pattern Test");
        pattern.setAssociations(new HashSet<>());
    }

    @Test
    @DisplayName("Should map pattern with complete data including dam and folder")
    void shouldMapPatternWithCompleteDataIncludingDamAndFolder() {
        // Given
        when(dam.getId()).thenReturn(1L);
        when(dam.getName()).thenReturn("Barragem Principal");
        when(folder.getId()).thenReturn(10L);
        when(folder.getName()).thenReturn("Folder Test");

        pattern.setDam(dam);
        pattern.setFolder(folder);

        // When
        TabulatePatternResponseDTO dto = mapper.mapToResponseDTO(pattern);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Pattern Test");
        assertThat(dto.getDam()).isNotNull();
        assertThat(dto.getDam().getId()).isEqualTo(1L);
        assertThat(dto.getDam().getName()).isEqualTo("Barragem Principal");
        assertThat(dto.getFolder()).isNotNull();
        assertThat(dto.getFolder().getId()).isEqualTo(10L);
        assertThat(dto.getFolder().getName()).isEqualTo("Folder Test");
    }

    @Test
    @DisplayName("Should map pattern without folder when folder is null")
    void shouldMapPatternWithoutFolderWhenFolderIsNull() {
        // Given
        when(dam.getId()).thenReturn(1L);
        when(dam.getName()).thenReturn("Barragem Principal");

        pattern.setDam(dam);
        pattern.setFolder(null);

        // When
        TabulatePatternResponseDTO dto = mapper.mapToResponseDTO(pattern);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getFolder()).isNull();
    }

    @Test
    @DisplayName("Should map pattern with single association")
    void shouldMapPatternWithSingleAssociation() {
        // Given
        when(dam.getId()).thenReturn(1L);
        when(dam.getName()).thenReturn("Barragem Principal");
        pattern.setDam(dam);

        InstrumentEntity instrument = mock(InstrumentEntity.class);
        when(instrument.getId()).thenReturn(100L);
        when(instrument.getName()).thenReturn("Piezômetro P1");

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setId(5L);
        association.setInstrument(instrument);
        association.setIsDateEnable(true);
        association.setDateIndex(0);
        association.setIsHourEnable(true);
        association.setHourIndex(1);
        association.setIsUserEnable(false);
        association.setIsReadEnable(true);
        association.setOutputAssociations(new HashSet<>());

        pattern.getAssociations().add(association);

        // When
        TabulatePatternResponseDTO dto = mapper.mapToResponseDTO(pattern);

        // Then
        assertThat(dto.getAssociations()).hasSize(1);
        TabulatePatternResponseDTO.InstrumentAssociationDTO assocDTO = dto.getAssociations().get(0);
        assertThat(assocDTO.getId()).isEqualTo(5L);
        assertThat(assocDTO.getInstrumentId()).isEqualTo(100L);
        assertThat(assocDTO.getInstrumentName()).isEqualTo("Piezômetro P1");
        assertThat(assocDTO.getIsDateEnable()).isTrue();
        assertThat(assocDTO.getDateIndex()).isEqualTo(0);
        assertThat(assocDTO.getIsHourEnable()).isTrue();
        assertThat(assocDTO.getHourIndex()).isEqualTo(1);
        assertThat(assocDTO.getIsUserEnable()).isFalse();
        assertThat(assocDTO.getIsReadEnable()).isTrue();
    }

    @Test
    @DisplayName("Should map pattern with multiple associations")
    void shouldMapPatternWithMultipleAssociations() {
        // Given
        when(dam.getId()).thenReturn(1L);
        when(dam.getName()).thenReturn("Barragem Principal");
        pattern.setDam(dam);

        InstrumentEntity instrument1 = mock(InstrumentEntity.class);
        when(instrument1.getId()).thenReturn(100L);
        when(instrument1.getName()).thenReturn("Piezômetro P1");

        InstrumentEntity instrument2 = mock(InstrumentEntity.class);
        when(instrument2.getId()).thenReturn(200L);
        when(instrument2.getName()).thenReturn("Inclinômetro I1");

        InstrumentTabulateAssociationEntity association1 = createAssociation(5L, instrument1);
        InstrumentTabulateAssociationEntity association2 = createAssociation(6L, instrument2);

        pattern.getAssociations().add(association1);
        pattern.getAssociations().add(association2);

        // When
        TabulatePatternResponseDTO dto = mapper.mapToResponseDTO(pattern);

        // Then
        assertThat(dto.getAssociations()).hasSize(2);
    }

    @Test
    @DisplayName("Should map association with single output")
    void shouldMapAssociationWithSingleOutput() {
        // Given
        when(dam.getId()).thenReturn(1L);
        when(dam.getName()).thenReturn("Barragem Principal");
        pattern.setDam(dam);

        InstrumentEntity instrument = mock(InstrumentEntity.class);
        when(instrument.getId()).thenReturn(100L);
        when(instrument.getName()).thenReturn("Piezômetro P1");

        MeasurementUnitEntity measurementUnit = mock(MeasurementUnitEntity.class);
        when(measurementUnit.getId()).thenReturn(50L);
        when(measurementUnit.getName()).thenReturn("Metro");
        when(measurementUnit.getAcronym()).thenReturn("m");

        OutputEntity output = mock(OutputEntity.class);
        when(output.getId()).thenReturn(300L);
        when(output.getName()).thenReturn("Deslocamento");
        when(output.getAcronym()).thenReturn("DESL");
        when(output.getMeasurementUnit()).thenReturn(measurementUnit);

        InstrumentTabulateAssociationEntity association = createAssociation(5L, instrument);
        InstrumentTabulateOutputAssociationEntity outputAssoc = createOutputAssociation(10L, output, 0);

        association.getOutputAssociations().add(outputAssoc);
        pattern.getAssociations().add(association);

        // When
        TabulatePatternResponseDTO dto = mapper.mapToResponseDTO(pattern);

        // Then
        assertThat(dto.getAssociations()).hasSize(1);
        List<TabulatePatternResponseDTO.OutputAssociationDTO> outputs = dto.getAssociations().get(0).getOutputAssociations();
        assertThat(outputs).hasSize(1);
        assertThat(outputs.get(0).getId()).isEqualTo(10L);
        assertThat(outputs.get(0).getOutputId()).isEqualTo(300L);
        assertThat(outputs.get(0).getOutputName()).isEqualTo("Deslocamento");
        assertThat(outputs.get(0).getOutputAcronym()).isEqualTo("DESL");
        assertThat(outputs.get(0).getOutputIndex()).isEqualTo(0);
        assertThat(outputs.get(0).getMeasurementUnit()).isNotNull();
        assertThat(outputs.get(0).getMeasurementUnit().getId()).isEqualTo(50L);
        assertThat(outputs.get(0).getMeasurementUnit().getName()).isEqualTo("Metro");
        assertThat(outputs.get(0).getMeasurementUnit().getAcronym()).isEqualTo("m");
    }

    @Test
    @DisplayName("Should map association with multiple outputs sorted by outputIndex")
    void shouldMapAssociationWithMultipleOutputsSortedByOutputIndex() {
        // Given
        when(dam.getId()).thenReturn(1L);
        when(dam.getName()).thenReturn("Barragem Principal");
        pattern.setDam(dam);

        InstrumentEntity instrument = mock(InstrumentEntity.class);
        when(instrument.getId()).thenReturn(100L);
        when(instrument.getName()).thenReturn("Piezômetro P1");

        OutputEntity output1 = createMockOutput(301L, "Output 1", "O1");
        OutputEntity output2 = createMockOutput(302L, "Output 2", "O2");
        OutputEntity output3 = createMockOutput(303L, "Output 3", "O3");

        InstrumentTabulateAssociationEntity association = createAssociation(5L, instrument);

        // Add outputs in non-sequential order (2, 0, 1)
        association.getOutputAssociations().add(createOutputAssociation(12L, output2, 2));
        association.getOutputAssociations().add(createOutputAssociation(10L, output1, 0));
        association.getOutputAssociations().add(createOutputAssociation(11L, output3, 1));

        pattern.getAssociations().add(association);

        // When
        TabulatePatternResponseDTO dto = mapper.mapToResponseDTO(pattern);

        // Then
        List<TabulatePatternResponseDTO.OutputAssociationDTO> outputs = dto.getAssociations().get(0).getOutputAssociations();
        assertThat(outputs).hasSize(3);

        // Should be sorted by outputIndex (0, 1, 2)
        assertThat(outputs.get(0).getOutputIndex()).isEqualTo(0);
        assertThat(outputs.get(0).getOutputName()).isEqualTo("Output 1");

        assertThat(outputs.get(1).getOutputIndex()).isEqualTo(1);
        assertThat(outputs.get(1).getOutputName()).isEqualTo("Output 3");

        assertThat(outputs.get(2).getOutputIndex()).isEqualTo(2);
        assertThat(outputs.get(2).getOutputName()).isEqualTo("Output 2");
    }

    @Test
    @DisplayName("Should map output without measurement unit when null")
    void shouldMapOutputWithoutMeasurementUnitWhenNull() {
        // Given
        when(dam.getId()).thenReturn(1L);
        when(dam.getName()).thenReturn("Barragem Principal");
        pattern.setDam(dam);

        InstrumentEntity instrument = mock(InstrumentEntity.class);
        when(instrument.getId()).thenReturn(100L);
        when(instrument.getName()).thenReturn("Piezômetro P1");

        OutputEntity output = mock(OutputEntity.class);
        when(output.getId()).thenReturn(300L);
        when(output.getName()).thenReturn("Deslocamento");
        when(output.getAcronym()).thenReturn("DESL");
        when(output.getMeasurementUnit()).thenReturn(null);

        InstrumentTabulateAssociationEntity association = createAssociation(5L, instrument);
        association.getOutputAssociations().add(createOutputAssociation(10L, output, 0));

        pattern.getAssociations().add(association);

        // When
        TabulatePatternResponseDTO dto = mapper.mapToResponseDTO(pattern);

        // Then
        List<TabulatePatternResponseDTO.OutputAssociationDTO> outputs = dto.getAssociations().get(0).getOutputAssociations();
        assertThat(outputs.get(0).getMeasurementUnit()).isNull();
    }

    @Test
    @DisplayName("Should map pattern with all enable flags set to true")
    void shouldMapPatternWithAllEnableFlagsSetToTrue() {
        // Given
        when(dam.getId()).thenReturn(1L);
        when(dam.getName()).thenReturn("Barragem Principal");
        pattern.setDam(dam);

        InstrumentEntity instrument = mock(InstrumentEntity.class);
        when(instrument.getId()).thenReturn(100L);
        when(instrument.getName()).thenReturn("Piezômetro P1");

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setId(5L);
        association.setInstrument(instrument);
        association.setIsDateEnable(true);
        association.setDateIndex(0);
        association.setIsHourEnable(true);
        association.setHourIndex(1);
        association.setIsUserEnable(true);
        association.setUserIndex(2);
        association.setIsReadEnable(true);
        association.setOutputAssociations(new HashSet<>());

        pattern.getAssociations().add(association);

        // When
        TabulatePatternResponseDTO dto = mapper.mapToResponseDTO(pattern);

        // Then
        TabulatePatternResponseDTO.InstrumentAssociationDTO assocDTO = dto.getAssociations().get(0);
        assertThat(assocDTO.getIsDateEnable()).isTrue();
        assertThat(assocDTO.getIsHourEnable()).isTrue();
        assertThat(assocDTO.getIsUserEnable()).isTrue();
        assertThat(assocDTO.getIsReadEnable()).isTrue();
    }

    @Test
    @DisplayName("Should map pattern with all enable flags set to false")
    void shouldMapPatternWithAllEnableFlagsSetToFalse() {
        // Given
        when(dam.getId()).thenReturn(1L);
        when(dam.getName()).thenReturn("Barragem Principal");
        pattern.setDam(dam);

        InstrumentEntity instrument = mock(InstrumentEntity.class);
        when(instrument.getId()).thenReturn(100L);
        when(instrument.getName()).thenReturn("Piezômetro P1");

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setId(5L);
        association.setInstrument(instrument);
        association.setIsDateEnable(false);
        association.setIsHourEnable(false);
        association.setIsUserEnable(false);
        association.setIsReadEnable(false);
        association.setOutputAssociations(new HashSet<>());

        pattern.getAssociations().add(association);

        // When
        TabulatePatternResponseDTO dto = mapper.mapToResponseDTO(pattern);

        // Then
        TabulatePatternResponseDTO.InstrumentAssociationDTO assocDTO = dto.getAssociations().get(0);
        assertThat(assocDTO.getIsDateEnable()).isFalse();
        assertThat(assocDTO.getIsHourEnable()).isFalse();
        assertThat(assocDTO.getIsUserEnable()).isFalse();
        assertThat(assocDTO.getIsReadEnable()).isFalse();
    }

    @Test
    @DisplayName("Should map pattern with various index values")
    void shouldMapPatternWithVariousIndexValues() {
        // Given
        when(dam.getId()).thenReturn(1L);
        when(dam.getName()).thenReturn("Barragem Principal");
        pattern.setDam(dam);

        InstrumentEntity instrument = mock(InstrumentEntity.class);
        when(instrument.getId()).thenReturn(100L);
        when(instrument.getName()).thenReturn("Piezômetro P1");

        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setId(5L);
        association.setInstrument(instrument);
        association.setIsDateEnable(true);
        association.setDateIndex(5);
        association.setIsHourEnable(true);
        association.setHourIndex(10);
        association.setIsUserEnable(true);
        association.setUserIndex(15);
        association.setIsReadEnable(true);
        association.setOutputAssociations(new HashSet<>());

        pattern.getAssociations().add(association);

        // When
        TabulatePatternResponseDTO dto = mapper.mapToResponseDTO(pattern);

        // Then
        TabulatePatternResponseDTO.InstrumentAssociationDTO assocDTO = dto.getAssociations().get(0);
        assertThat(assocDTO.getDateIndex()).isEqualTo(5);
        assertThat(assocDTO.getHourIndex()).isEqualTo(10);
        assertThat(assocDTO.getUserIndex()).isEqualTo(15);
    }

    @Test
    @DisplayName("Should preserve pattern name and id during mapping")
    void shouldPreservePatternNameAndIdDuringMapping() {
        // Given
        when(dam.getId()).thenReturn(1L);
        when(dam.getName()).thenReturn("Barragem Principal");
        pattern.setDam(dam);
        pattern.setId(999L);
        pattern.setName("Complex Pattern Name with Special Chars àéç");

        // When
        TabulatePatternResponseDTO dto = mapper.mapToResponseDTO(pattern);

        // Then
        assertThat(dto.getId()).isEqualTo(999L);
        assertThat(dto.getName()).isEqualTo("Complex Pattern Name with Special Chars àéç");
    }

    @Test
    @DisplayName("Should map empty associations list")
    void shouldMapEmptyAssociationsList() {
        // Given
        when(dam.getId()).thenReturn(1L);
        when(dam.getName()).thenReturn("Barragem Principal");
        pattern.setDam(dam);
        pattern.setAssociations(new HashSet<>());

        // When
        TabulatePatternResponseDTO dto = mapper.mapToResponseDTO(pattern);

        // Then
        assertThat(dto.getAssociations()).isEmpty();
    }

    // Helper methods
    private InstrumentTabulateAssociationEntity createAssociation(Long id, InstrumentEntity instrument) {
        InstrumentTabulateAssociationEntity association = new InstrumentTabulateAssociationEntity();
        association.setId(id);
        association.setInstrument(instrument);
        association.setIsDateEnable(true);
        association.setDateIndex(0);
        association.setIsHourEnable(true);
        association.setHourIndex(1);
        association.setIsUserEnable(false);
        association.setIsReadEnable(true);
        association.setOutputAssociations(new HashSet<>());
        return association;
    }

    private InstrumentTabulateOutputAssociationEntity createOutputAssociation(Long id, OutputEntity output, Integer outputIndex) {
        InstrumentTabulateOutputAssociationEntity outputAssoc = new InstrumentTabulateOutputAssociationEntity();
        outputAssoc.setId(id);
        outputAssoc.setOutput(output);
        outputAssoc.setOutputIndex(outputIndex);
        return outputAssoc;
    }

    private OutputEntity createMockOutput(Long id, String name, String acronym) {
        OutputEntity output = mock(OutputEntity.class);
        when(output.getId()).thenReturn(id);
        when(output.getName()).thenReturn(name);
        when(output.getAcronym()).thenReturn(acronym);
        when(output.getMeasurementUnit()).thenReturn(null);
        return output;
    }
}
