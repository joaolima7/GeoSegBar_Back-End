package com.geosegbar.infra.instrument.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.ConstantEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.DeterministicLimitEntity;
import com.geosegbar.entities.InputEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.InstrumentTypeEntity;
import com.geosegbar.entities.MeasurementUnitEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.entities.SectionEntity;
import com.geosegbar.entities.StatisticalLimitEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.constant.persistence.jpa.ConstantRepository;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.deterministic_limit.persistence.jpa.DeterministicLimitRepository;
import com.geosegbar.infra.input.persistence.jpa.InputRepository;
import com.geosegbar.infra.instrument.dtos.ConstantDTO;
import com.geosegbar.infra.instrument.dtos.CreateInstrumentRequest;
import com.geosegbar.infra.instrument.dtos.DeterministicLimitDTO;
import com.geosegbar.infra.instrument.dtos.InputDTO;
import com.geosegbar.infra.instrument.dtos.InstrumentResponseDTO;
import com.geosegbar.infra.instrument.dtos.OutputDTO;
import com.geosegbar.infra.instrument.dtos.StatisticalLimitDTO;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
import com.geosegbar.infra.instrument_type.persistence.jpa.InstrumentTypeRepository;
import com.geosegbar.infra.math.ExpressionEvaluator;
import com.geosegbar.infra.measurement_unit.persistence.jpa.MeasurementUnitRepository;
import com.geosegbar.infra.output.persistence.jpa.OutputRepository;
import com.geosegbar.infra.section.persistence.jpa.SectionRepository;
import com.geosegbar.infra.statistical_limit.persistence.jpa.StatisticalLimitRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstrumentService {

    private final InstrumentRepository instrumentRepository;
    private final DamRepository damRepository;
    private final InstrumentTypeRepository instrumentTypeRepository;
    private final SectionRepository sectionRepository;
    private final MeasurementUnitRepository measurementUnitRepository;
    private final StatisticalLimitRepository statisticalLimitRepository;
    private final DeterministicLimitRepository deterministicLimitRepository;
    private final InputRepository inputRepository;
    private final ConstantRepository constantRepository;
    private final OutputRepository outputRepository;

    public List<InstrumentEntity> findAll() {
        return instrumentRepository.findAllByOrderByNameAsc();
    }

    public List<InstrumentEntity> findByDamId(Long damId) {
        return instrumentRepository.findByDamId(damId);
    }

    public InstrumentEntity findById(Long id) {
        return instrumentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Instrumento não encontrado com ID: " + id));
    }

    public InstrumentEntity findWithAllDetails(Long id) {
        return instrumentRepository.findWithAllDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Instrumento não encontrado com ID: " + id));
    }

    @Transactional
    public InstrumentEntity createComplete(CreateInstrumentRequest request) {
        validateRequest(request);

        if (instrumentRepository.existsByNameAndDamId(request.getName(), request.getDamId())) {
            throw new DuplicateResourceException("Já existe um instrumento com o nome '" + request.getName() + "' na mesma barragem");
        }

        DamEntity dam = damRepository.findById(request.getDamId())
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada com ID: " + request.getDamId()));

        InstrumentTypeEntity instrumentType = instrumentTypeRepository.findById(request.getInstrumentTypeId())
                .orElseThrow(() -> new NotFoundException("Tipo de instrumento não encontrado com ID: " + request.getInstrumentTypeId()));

        SectionEntity section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new NotFoundException("Seção não encontrada com ID: " + request.getSectionId()));

        if (instrumentRepository.existsByNameAndDamId(request.getName(), request.getDamId())) {
            throw new DuplicateResourceException("Já existe um instrumento com esse nome nesta barragem");
        }

        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setName(request.getName());
        instrument.setLocation(request.getLocation());
        instrument.setDistanceOffset(request.getDistanceOffset());
        instrument.setLatitude(request.getLatitude());
        instrument.setLongitude(request.getLongitude());
        instrument.setNoLimit(request.getNoLimit());
        instrument.setDam(dam);
        instrument.setInstrumentType(instrumentType);
        instrument.setSection(section);

        InstrumentEntity savedInstrument = instrumentRepository.save(instrument);

        processLimits(savedInstrument, request);
        processInputs(savedInstrument, request.getInputs());

        if (request.getConstants() != null && !request.getConstants().isEmpty()) {
            processConstants(savedInstrument, request.getConstants());
        }

        processOutputs(savedInstrument, request.getOutputs());

        return instrumentRepository.findWithAllDetailsById(savedInstrument.getId())
                .orElseThrow(() -> new NotFoundException("Instrumento não encontrado após criação"));
    }

    private void validateRequest(CreateInstrumentRequest request) {
        if (request.getInputs() == null || request.getInputs().isEmpty()) {
            throw new InvalidInputException("Pelo menos um input é obrigatório");
        }

        if (request.getOutputs() == null || request.getOutputs().isEmpty()) {
            throw new InvalidInputException("Pelo menos um output é obrigatório");
        }

        if (Boolean.TRUE.equals(request.getNoLimit())) {
            if (request.getStatisticalLimit() != null || request.getDeterministicLimit() != null) {
                throw new InvalidInputException("Quando 'Sem Limites' está marcado, não deve haver limites estatísticos ou determinísticos");
            }
        } else {
            boolean hasStatistical = request.getStatisticalLimit() != null;
            boolean hasDeterministic = request.getDeterministicLimit() != null;

            if (!hasStatistical && !hasDeterministic) {
                throw new InvalidInputException("Quando 'Sem Limites' não está marcado, deve haver um tipo de limite");
            }

            if (hasStatistical && hasDeterministic) {
                throw new InvalidInputException("Apenas um tipo de limite (estatístico ou determinístico) deve ser fornecido, não ambos");
            }
        }
    }

    private void processLimits(InstrumentEntity instrument, CreateInstrumentRequest request) {
        if (!request.getNoLimit()) {
            if (request.getStatisticalLimit() != null) {
                StatisticalLimitEntity statisticalLimit = new StatisticalLimitEntity();
                statisticalLimit.setInstrument(instrument);
                statisticalLimit.setLowerValue(request.getStatisticalLimit().getLowerValue());
                statisticalLimit.setUpperValue(request.getStatisticalLimit().getUpperValue());
                statisticalLimitRepository.save(statisticalLimit);
                instrument.setStatisticalLimit(statisticalLimit);
            }

            if (request.getDeterministicLimit() != null) {
                DeterministicLimitEntity deterministicLimit = new DeterministicLimitEntity();
                deterministicLimit.setInstrument(instrument);
                deterministicLimit.setAttentionValue(request.getDeterministicLimit().getAttentionValue());
                deterministicLimit.setAlertValue(request.getDeterministicLimit().getAlertValue());
                deterministicLimit.setEmergencyValue(request.getDeterministicLimit().getEmergencyValue());
                deterministicLimitRepository.save(deterministicLimit);
                instrument.setDeterministicLimit(deterministicLimit);
            }
        }
    }

    private void processInputs(InstrumentEntity instrument, List<InputDTO> inputDTOs) {
        Set<String> acronyms = new HashSet<>();
        Set<String> names = new HashSet<>();

        for (InputDTO inputDTO : inputDTOs) {
            if (!acronyms.add(inputDTO.getAcronym())) {
                throw new DuplicateResourceException("Sigla de input duplicada: " + inputDTO.getAcronym());
            }

            if (!names.add(inputDTO.getName())) {
                throw new DuplicateResourceException("Nome de input duplicado: " + inputDTO.getName());
            }

            MeasurementUnitEntity measurementUnit = measurementUnitRepository.findById(inputDTO.getMeasurementUnitId())
                    .orElseThrow(() -> new NotFoundException("Unidade de medida não encontrada com ID: " + inputDTO.getMeasurementUnitId()));

            InputEntity input = new InputEntity();
            input.setAcronym(inputDTO.getAcronym());
            input.setName(inputDTO.getName());
            input.setPrecision(inputDTO.getPrecision());
            input.setMeasurementUnit(measurementUnit);
            input.setInstrument(instrument);

            inputRepository.save(input);
            instrument.getInputs().add(input);
        }
    }

    private void processConstants(InstrumentEntity instrument, List<ConstantDTO> constantDTOs) {
        Set<String> acronyms = new HashSet<>();
        Set<String> names = new HashSet<>();

        for (ConstantDTO constantDTO : constantDTOs) {
            if (!acronyms.add(constantDTO.getAcronym())) {
                throw new DuplicateResourceException("Sigla de constante duplicada: " + constantDTO.getAcronym());
            }

            if (!names.add(constantDTO.getName())) {
                throw new DuplicateResourceException("Nome de constante duplicado: " + constantDTO.getName());
            }

            MeasurementUnitEntity measurementUnit = measurementUnitRepository.findById(constantDTO.getMeasurementUnitId())
                    .orElseThrow(() -> new NotFoundException("Unidade de medida não encontrada com ID: " + constantDTO.getMeasurementUnitId()));

            ConstantEntity constant = new ConstantEntity();
            constant.setAcronym(constantDTO.getAcronym());
            constant.setName(constantDTO.getName());
            constant.setPrecision(constantDTO.getPrecision());
            constant.setValue(constantDTO.getValue());
            constant.setMeasurementUnit(measurementUnit);
            constant.setInstrument(instrument);

            constantRepository.save(constant);
            instrument.getConstants().add(constant);
        }
    }

    private void processOutputs(InstrumentEntity instrument, List<OutputDTO> outputDTOs) {
        Set<String> acronyms = new HashSet<>();
        Set<String> names = new HashSet<>();

        // Crie conjuntos de acrônimos disponíveis para validação
        Set<String> inputAcronyms = instrument.getInputs().stream()
                .map(InputEntity::getAcronym)
                .collect(Collectors.toSet());

        Set<String> constantAcronyms = instrument.getConstants().stream()
                .map(ConstantEntity::getAcronym)
                .collect(Collectors.toSet());

        for (OutputDTO outputDTO : outputDTOs) {
            // Verificar duplicatas no payload atual
            if (!acronyms.add(outputDTO.getAcronym())) {
                throw new DuplicateResourceException("Sigla de output duplicada: " + outputDTO.getAcronym());
            }

            if (!names.add(outputDTO.getName())) {
                throw new DuplicateResourceException("Nome de output duplicado: " + outputDTO.getName());
            }

            // Validar a equação para garantir que usa apenas inputs e constants existentes
            validateEquation(outputDTO.getEquation(), inputAcronyms, constantAcronyms);

            MeasurementUnitEntity measurementUnit = measurementUnitRepository.findById(outputDTO.getMeasurementUnitId())
                    .orElseThrow(() -> new NotFoundException("Unidade de medida não encontrada com ID: " + outputDTO.getMeasurementUnitId()));

            OutputEntity output = new OutputEntity();
            output.setAcronym(outputDTO.getAcronym());
            output.setName(outputDTO.getName());
            output.setEquation(outputDTO.getEquation());
            output.setPrecision(outputDTO.getPrecision());
            output.setMeasurementUnit(measurementUnit);
            output.setInstrument(instrument);

            outputRepository.save(output);
            instrument.getOutputs().add(output);
        }
    }

    @Transactional
    public void delete(Long id) {
        InstrumentEntity instrument = findById(id);

        inputRepository.deleteByInstrumentId(id);
        constantRepository.deleteByInstrumentId(id);
        outputRepository.deleteByInstrumentId(id);

        if (statisticalLimitRepository.existsByInstrumentId(id)) {
            statisticalLimitRepository.deleteByInstrumentId(id);
        }

        if (deterministicLimitRepository.existsByInstrumentId(id)) {
            deterministicLimitRepository.deleteByInstrumentId(id);
        }

        instrumentRepository.delete(instrument);
    }

    private void validateEquation(String equation, Set<String> inputAcronyms, Set<String> constantAcronyms) {
        String cleanEquation = equation.replaceAll("\\s+", "");

        Pattern pattern = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");
        Matcher matcher = pattern.matcher(cleanEquation);

        Set<String> variablesInEquation = new HashSet<>();
        while (matcher.find()) {
            String var = matcher.group();
            if (!isKnownMathFunction(var)) {
                variablesInEquation.add(var);
            }
        }

        for (String variable : variablesInEquation) {
            if (!inputAcronyms.contains(variable) && !constantAcronyms.contains(variable)) {
                throw new InvalidInputException("Variável '" + variable + "' na equação não existe como input ou constante");
            }
        }

        try {
            ExpressionEvaluator.validateSyntax(cleanEquation);
        } catch (Exception e) {
            throw new InvalidInputException("Erro de sintaxe na equação: " + e.getMessage());
        }
    }

    private boolean isKnownMathFunction(String name) {
        Set<String> mathFunctions = Set.of(
                "sin", "cos", "tan", "asin", "acos", "atan", "atan2",
                "sinh", "cosh", "tanh", "exp", "log", "log10", "pow",
                "sqrt", "cbrt", "abs", "min", "max", "floor", "ceil", "round"
        );
        return mathFunctions.contains(name.toLowerCase());
    }

    public InstrumentResponseDTO mapToResponseDTO(InstrumentEntity instrument) {
        InstrumentResponseDTO dto = new InstrumentResponseDTO();
        dto.setId(instrument.getId());
        dto.setName(instrument.getName());
        dto.setLocation(instrument.getLocation());
        dto.setDistanceOffset(instrument.getDistanceOffset());
        dto.setLatitude(instrument.getLatitude());
        dto.setLongitude(instrument.getLongitude());
        dto.setNoLimit(instrument.getNoLimit());
        dto.setDamId(instrument.getDam().getId());
        dto.setDamName(instrument.getDam().getName());
        dto.setInstrumentTypeId(instrument.getInstrumentType().getId());
        dto.setInstrumentTypeName(instrument.getInstrumentType().getName());
        dto.setSectionId(instrument.getSection().getId());
        dto.setSectionName(instrument.getSection().getName());

        if (instrument.getStatisticalLimit() != null) {
            StatisticalLimitDTO limitDTO = new StatisticalLimitDTO();
            limitDTO.setLowerValue(instrument.getStatisticalLimit().getLowerValue());
            limitDTO.setUpperValue(instrument.getStatisticalLimit().getUpperValue());
            dto.setStatisticalLimit(limitDTO);
        }

        if (instrument.getDeterministicLimit() != null) {
            DeterministicLimitDTO limitDTO = new DeterministicLimitDTO();
            limitDTO.setAttentionValue(instrument.getDeterministicLimit().getAttentionValue());
            limitDTO.setAlertValue(instrument.getDeterministicLimit().getAlertValue());
            limitDTO.setEmergencyValue(instrument.getDeterministicLimit().getEmergencyValue());
            dto.setDeterministicLimit(limitDTO);
        }

        List<InputDTO> inputDTOs = new ArrayList<>();
        for (InputEntity input : instrument.getInputs()) {
            InputDTO inputDTO = new InputDTO();
            inputDTO.setAcronym(input.getAcronym());
            inputDTO.setName(input.getName());
            inputDTO.setPrecision(input.getPrecision());
            inputDTO.setMeasurementUnitId(input.getMeasurementUnit().getId());

            inputDTO.setMeasurementUnitName(input.getMeasurementUnit().getName());
            inputDTO.setMeasurementUnitAcronym(input.getMeasurementUnit().getAcronym());

            inputDTOs.add(inputDTO);
        }
        dto.setInputs(inputDTOs);

        List<ConstantDTO> constantDTOs = new ArrayList<>();
        for (ConstantEntity constant : instrument.getConstants()) {
            ConstantDTO constantDTO = new ConstantDTO();
            constantDTO.setAcronym(constant.getAcronym());
            constantDTO.setName(constant.getName());
            constantDTO.setPrecision(constant.getPrecision());
            constantDTO.setValue(constant.getValue());
            constantDTO.setMeasurementUnitId(constant.getMeasurementUnit().getId());
            constantDTO.setMeasurementUnitName(constant.getMeasurementUnit().getName());
            constantDTO.setMeasurementUnitAcronym(constant.getMeasurementUnit().getAcronym());

            constantDTOs.add(constantDTO);
        }
        dto.setConstants(constantDTOs);

        List<OutputDTO> outputDTOs = new ArrayList<>();
        for (OutputEntity output : instrument.getOutputs()) {
            OutputDTO outputDTO = new OutputDTO();
            outputDTO.setAcronym(output.getAcronym());
            outputDTO.setName(output.getName());
            outputDTO.setEquation(output.getEquation());
            outputDTO.setPrecision(output.getPrecision());
            outputDTO.setMeasurementUnitId(output.getMeasurementUnit().getId());
            outputDTO.setMeasurementUnitName(output.getMeasurementUnit().getName());
            outputDTO.setMeasurementUnitAcronym(output.getMeasurementUnit().getAcronym());

            outputDTOs.add(outputDTO);
        }
        dto.setOutputs(outputDTOs);

        return dto;
    }

    public List<InstrumentResponseDTO> mapToResponseDTOList(List<InstrumentEntity> instruments) {
        return instruments.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }
}
