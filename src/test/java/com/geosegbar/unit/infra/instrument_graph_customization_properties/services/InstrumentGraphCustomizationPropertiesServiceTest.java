package com.geosegbar.unit.infra.instrument_graph_customization_properties.services;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.geosegbar.common.enums.CustomizationTypeEnum;
import com.geosegbar.common.enums.LimitValueTypeEnum;
import com.geosegbar.common.enums.LineTypeEnum;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.InstrumentGraphCustomizationPropertiesEntity;
import com.geosegbar.entities.InstrumentGraphPatternEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.GraphPropertiesResponseDTO;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.PropertyResponseDTO;
import com.geosegbar.infra.instrument_graph_customization_properties.dtos.UpdatePropertyRequestDTO;
import com.geosegbar.infra.instrument_graph_customization_properties.persistence.jpa.InstrumentGraphCustomizationPropertiesRepository;
import com.geosegbar.infra.instrument_graph_customization_properties.services.InstrumentGraphCustomizationPropertiesService;
import com.geosegbar.infra.instrument_graph_pattern.services.InstrumentGraphPatternService;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class InstrumentGraphCustomizationPropertiesServiceTest {

    @Mock
    private InstrumentGraphCustomizationPropertiesRepository propertiesRepository;

    @Mock
    private InstrumentGraphPatternService patternService;

    @InjectMocks
    private InstrumentGraphCustomizationPropertiesService service;

    private InstrumentGraphCustomizationPropertiesEntity propertyEntity;
    private InstrumentGraphPatternEntity patternEntity;
    private InstrumentEntity instrumentEntity;
    private OutputEntity outputEntity;

    @BeforeEach
    void setUp() {
        instrumentEntity = new InstrumentEntity();
        instrumentEntity.setId(1L);
        instrumentEntity.setName("Test Instrument");

        patternEntity = new InstrumentGraphPatternEntity();
        patternEntity.setId(1L);
        patternEntity.setName("Test Pattern");
        patternEntity.setInstrument(instrumentEntity);

        outputEntity = new OutputEntity();
        outputEntity.setId(1L);
        outputEntity.setName("Test Output");
        outputEntity.setAcronym("TO");

        propertyEntity = new InstrumentGraphCustomizationPropertiesEntity();
        propertyEntity.setId(1L);
        propertyEntity.setName("Test Property");
        propertyEntity.setCustomizationType(CustomizationTypeEnum.OUTPUT);
        propertyEntity.setFillColor("#FF0000");
        propertyEntity.setLineType(LineTypeEnum.SOLID);
        propertyEntity.setLabelEnable(true);
        propertyEntity.setIsPrimaryOrdinate(true);
        propertyEntity.setPattern(patternEntity);
        propertyEntity.setOutput(outputEntity);
    }

    @Test
    void shouldFindPropertyByIdSuccessfully() {
        when(propertiesRepository.findById(1L)).thenReturn(Optional.of(propertyEntity));

        PropertyResponseDTO result = service.findPropertyById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Property", result.getName());
        assertEquals(CustomizationTypeEnum.OUTPUT, result.getCustomizationType());
        assertEquals("#FF0000", result.getFillColor());
        assertEquals(LineTypeEnum.SOLID, result.getLineType());
        assertTrue(result.getLabelEnable());
        assertTrue(result.getIsPrimaryOrdinate());
        assertEquals(1L, result.getPatternId());
        assertEquals(1L, result.getOutputId());

        verify(propertiesRepository).findById(1L);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenPropertyNotFoundById() {
        when(propertiesRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            service.findPropertyById(999L);
        });

        assertEquals("Propriedade não encontrada com ID: 999", exception.getMessage());
        verify(propertiesRepository).findById(999L);
    }

    @Test
    void shouldFindPropertiesByPatternIdSuccessfully() {
        InstrumentGraphCustomizationPropertiesEntity property2 = new InstrumentGraphCustomizationPropertiesEntity();
        property2.setId(2L);
        property2.setName("Property 2");
        property2.setCustomizationType(CustomizationTypeEnum.INSTRUMENT);
        property2.setFillColor("#0000FF");
        property2.setLineType(LineTypeEnum.DASHED);
        property2.setLabelEnable(false);
        property2.setIsPrimaryOrdinate(false);
        property2.setPattern(patternEntity);
        property2.setInstrument(instrumentEntity);

        List<InstrumentGraphCustomizationPropertiesEntity> properties = Arrays.asList(propertyEntity, property2);
        when(propertiesRepository.findByPatternId(1L)).thenReturn(properties);

        List<PropertyResponseDTO> result = service.findPropertiesByPatternId(1L);

        assertNotNull(result);
        assertEquals(2, result.size());

        PropertyResponseDTO dto1 = result.get(0);
        assertEquals(1L, dto1.getId());
        assertEquals("Test Property", dto1.getName());
        assertEquals(CustomizationTypeEnum.OUTPUT, dto1.getCustomizationType());

        PropertyResponseDTO dto2 = result.get(1);
        assertEquals(2L, dto2.getId());
        assertEquals("Property 2", dto2.getName());
        assertEquals(CustomizationTypeEnum.INSTRUMENT, dto2.getCustomizationType());

        verify(propertiesRepository).findByPatternId(1L);
    }

    @Test
    void shouldReturnEmptyListWhenNoPropertiesFoundByPatternId() {
        when(propertiesRepository.findByPatternId(999L)).thenReturn(Arrays.asList());

        List<PropertyResponseDTO> result = service.findPropertiesByPatternId(999L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(propertiesRepository).findByPatternId(999L);
    }

    @Test
    void shouldFindByPatternIdWithDetailsSuccessfully() {
        List<InstrumentGraphCustomizationPropertiesEntity> properties = Arrays.asList(propertyEntity);

        when(patternService.findById(1L)).thenReturn(patternEntity);
        when(propertiesRepository.findByPatternId(1L)).thenReturn(properties);

        GraphPropertiesResponseDTO result = service.findByPatternId(1L);

        assertNotNull(result);
        assertEquals(1L, result.getPatternId());
        assertNotNull(result.getProperties());
        assertEquals(1, result.getProperties().size());

        GraphPropertiesResponseDTO.PropertyDetailDTO detailDTO = result.getProperties().get(0);
        assertEquals(1L, detailDTO.getId());
        assertEquals("Test Property", detailDTO.getName());
        assertEquals(CustomizationTypeEnum.OUTPUT, detailDTO.getCustomizationType());
        assertEquals("#FF0000", detailDTO.getFillColor());
        assertEquals(LineTypeEnum.SOLID, detailDTO.getLineType());
        assertTrue(detailDTO.getLabelEnable());
        assertTrue(detailDTO.getIsPrimaryOrdinate());
        assertNotNull(detailDTO.getOutput());
        assertEquals(1L, detailDTO.getOutput().getId());
        assertEquals("TO", detailDTO.getOutput().getAcronym());

        verify(patternService).findById(1L);
        verify(propertiesRepository).findByPatternId(1L);
    }

    @Test
    void shouldUpdatePropertySuccessfully() {
        UpdatePropertyRequestDTO requestDTO = new UpdatePropertyRequestDTO();
        requestDTO.setName("Updated Property");
        requestDTO.setFillColor("#00FF00");
        requestDTO.setLineType(LineTypeEnum.DOTTED);
        requestDTO.setLabelEnable(false);
        requestDTO.setIsPrimaryOrdinate(false);

        when(propertiesRepository.findById(1L)).thenReturn(Optional.of(propertyEntity));
        when(propertiesRepository.save(any(InstrumentGraphCustomizationPropertiesEntity.class))).thenReturn(propertyEntity);

        PropertyResponseDTO result = service.updateProperty(1L, requestDTO);

        assertNotNull(result);
        assertEquals(1L, result.getId());

        verify(propertiesRepository).findById(1L);
        verify(propertiesRepository).save(propertyEntity);

        assertEquals("Updated Property", propertyEntity.getName());
        assertEquals("#00FF00", propertyEntity.getFillColor());
        assertEquals(LineTypeEnum.DOTTED, propertyEntity.getLineType());
        assertFalse(propertyEntity.getLabelEnable());
        assertFalse(propertyEntity.getIsPrimaryOrdinate());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUpdatingNonExistentProperty() {
        UpdatePropertyRequestDTO requestDTO = new UpdatePropertyRequestDTO();
        requestDTO.setName("Updated Property");
        requestDTO.setFillColor("#00FF00");
        requestDTO.setLineType(LineTypeEnum.SOLID);
        requestDTO.setLabelEnable(true);
        requestDTO.setIsPrimaryOrdinate(true);

        when(propertiesRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            service.updateProperty(999L, requestDTO);
        });

        assertEquals("Propriedade não encontrada com ID: 999", exception.getMessage());
        verify(propertiesRepository).findById(999L);
        verify(propertiesRepository, never()).save(any());
    }

    @Test
    void shouldMapEntityToPropertyResponseDTOCorrectly() {
        propertyEntity.setLimitValueType(LimitValueTypeEnum.STATISTICAL_LOWER);

        when(propertiesRepository.findById(1L)).thenReturn(Optional.of(propertyEntity));

        PropertyResponseDTO result = service.findPropertyById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Property", result.getName());
        assertEquals(CustomizationTypeEnum.OUTPUT, result.getCustomizationType());
        assertEquals("#FF0000", result.getFillColor());
        assertEquals(LineTypeEnum.SOLID, result.getLineType());
        assertTrue(result.getLabelEnable());
        assertTrue(result.getIsPrimaryOrdinate());
        assertEquals(1L, result.getPatternId());
        assertEquals(LimitValueTypeEnum.STATISTICAL_LOWER, result.getLimitValueType());
        assertEquals(1L, result.getOutputId());
        assertNull(result.getInstrumentId());
        assertNull(result.getConstantId());
        assertNull(result.getStatisticalLimitId());
        assertNull(result.getDeterministicLimitId());
    }
}
