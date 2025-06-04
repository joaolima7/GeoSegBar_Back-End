package com.geosegbar.infra.instrument.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import com.geosegbar.infra.instrument.dtos.UpdateInstrumentRequest;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
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

        SectionEntity section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new NotFoundException("Seção não encontrada com ID: " + request.getSectionId()));

        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setName(request.getName());
        instrument.setLocation(request.getLocation());
        instrument.setDistanceOffset(request.getDistanceOffset());
        instrument.setLatitude(request.getLatitude());
        instrument.setLongitude(request.getLongitude());
        instrument.setNoLimit(request.getNoLimit());
        instrument.setInstrumentType(request.getInstrumentType());
        instrument.setDam(dam);
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
        validateLimits(request.getNoLimit(), request.getStatisticalLimit(), request.getDeterministicLimit());

        if (request.getInputs() == null || request.getInputs().isEmpty()) {
            throw new InvalidInputException("Pelo menos um input é obrigatório");
        }

        if (request.getOutputs() == null || request.getOutputs().isEmpty()) {
            throw new InvalidInputException("Pelo menos um output é obrigatório");
        }
    }

    private void validateRequest(UpdateInstrumentRequest request) {
        validateLimits(request.getNoLimit(), request.getStatisticalLimit(), request.getDeterministicLimit());

        if (request.getInputs() == null || request.getInputs().isEmpty()) {
            throw new InvalidInputException("Pelo menos um input é obrigatório");
        }

        if (request.getOutputs() == null || request.getOutputs().isEmpty()) {
            throw new InvalidInputException("Pelo menos um output é obrigatório");
        }
    }

    private void validateLimits(Boolean noLimit, StatisticalLimitDTO statisticalLimit, DeterministicLimitDTO deterministicLimit) {
        if (Boolean.TRUE.equals(noLimit)) {
            if (statisticalLimit != null || deterministicLimit != null) {
                throw new InvalidInputException("Quando 'Sem Limites' está marcado, não deve haver limites estatísticos ou determinísticos");
            }
        } else {
            boolean hasStatistical = statisticalLimit != null;
            boolean hasDeterministic = deterministicLimit != null;

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

        Set<String> inputAcronyms = instrument.getInputs().stream()
                .map(InputEntity::getAcronym)
                .collect(Collectors.toSet());

        Set<String> constantAcronyms = instrument.getConstants().stream()
                .map(ConstantEntity::getAcronym)
                .collect(Collectors.toSet());

        for (OutputDTO outputDTO : outputDTOs) {
            if (!acronyms.add(outputDTO.getAcronym())) {
                throw new DuplicateResourceException("Sigla de output duplicada: " + outputDTO.getAcronym());
            }

            if (!names.add(outputDTO.getName())) {
                throw new DuplicateResourceException("Nome de output duplicado: " + outputDTO.getName());
            }

            validateEquation(outputDTO.getEquation(), inputAcronyms, constantAcronyms);

            MeasurementUnitEntity measurementUnit = measurementUnitRepository.findById(outputDTO.getMeasurementUnitId())
                    .orElseThrow(() -> new NotFoundException("Unidade de medida não encontrada com ID: " + outputDTO.getMeasurementUnitId()));

            OutputEntity output = new OutputEntity();
            output.setAcronym(outputDTO.getAcronym());
            output.setName(outputDTO.getName());
            output.setEquation(outputDTO.getEquation());
            output.setPrecision(outputDTO.getPrecision());
            output.setMeasurementUnit(measurementUnit);
            output.setActive(true);
            output.setInstrument(instrument);

            outputRepository.save(output);
            instrument.getOutputs().add(output);
        }
    }

    @Transactional
    public InstrumentEntity update(Long id, UpdateInstrumentRequest request) {
        validateRequest(request);

        if (instrumentRepository.existsByNameAndDamIdAndIdNot(request.getName(), request.getDamId(), id)) {
            throw new DuplicateResourceException("Já existe um instrumento com esse nome nesta barragem");
        }

        InstrumentEntity oldInstrument = findById(id);

        Map<String, InputEntity> existingInputsByAcronym = oldInstrument.getInputs().stream()
                .collect(Collectors.toMap(
                        InputEntity::getAcronym,
                        input -> input,
                        (existing, replacement) -> {
                            log.warn("Encontrado input duplicado com acrônimo: {} (ids: {} e {})",
                                    existing.getAcronym(), existing.getId(), replacement.getId());
                            return existing;
                        }
                ));

        Map<String, ConstantEntity> existingConstantsByAcronym = oldInstrument.getConstants().stream()
                .collect(Collectors.toMap(
                        ConstantEntity::getAcronym,
                        constant -> constant,
                        (existing, replacement) -> {
                            log.warn("Encontrada constante duplicada com acrônimo: {} (ids: {} e {})",
                                    existing.getAcronym(), existing.getId(), replacement.getId());
                            return existing;
                        }
                ));

        Map<String, OutputEntity> existingOutputsByAcronym = oldInstrument.getOutputs().stream()
                .filter(OutputEntity::getActive)
                .collect(Collectors.toMap(
                        OutputEntity::getAcronym,
                        output -> output,
                        (existing, replacement) -> {
                            log.warn("Encontrado output duplicado com acrônimo: {} (ids: {} e {})",
                                    existing.getAcronym(), existing.getId(), replacement.getId());
                            return existing;
                        }
                ));

        for (OutputEntity output : oldInstrument.getOutputs()) {
            output.setActive(false);
            outputRepository.save(output);
        }

        updateInstrumentBasicFields(oldInstrument, request);

        updateLimits(oldInstrument, request);

        InstrumentEntity savedInstrument = instrumentRepository.save(oldInstrument);

        savedInstrument.getInputs().clear();
        savedInstrument.getConstants().clear();
        savedInstrument.getOutputs().clear();

        processInputsForUpdate(savedInstrument, request.getInputs(), existingInputsByAcronym);
        if (request.getConstants() != null && !request.getConstants().isEmpty()) {
            processConstantsForUpdate(savedInstrument, request.getConstants(), existingConstantsByAcronym);
        }
        processOutputsForUpdate(savedInstrument, request.getOutputs(), existingOutputsByAcronym);

        deleteUnusedComponents(existingInputsByAcronym, existingConstantsByAcronym);

        log.info("Instrumento atualizado com sucesso");

        return instrumentRepository.findWithAllDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Instrumento não encontrado após atualização"));
    }

    private void updateInstrumentBasicFields(InstrumentEntity instrument, UpdateInstrumentRequest request) {
        DamEntity dam = damRepository.findById(request.getDamId())
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada com ID: " + request.getDamId()));

        SectionEntity section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new NotFoundException("Seção não encontrada com ID: " + request.getSectionId()));

        instrument.setName(request.getName());
        instrument.setLocation(request.getLocation());
        instrument.setDistanceOffset(request.getDistanceOffset());
        instrument.setLatitude(request.getLatitude());
        instrument.setLongitude(request.getLongitude());
        instrument.setNoLimit(request.getNoLimit());
        instrument.setInstrumentType(request.getInstrumentType());
        instrument.setDam(dam);
        instrument.setSection(section);
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

    @Transactional
    public InstrumentEntity toggleActiveInstrument(Long id, Boolean active) {
        InstrumentEntity instrument = findById(id);
        instrument.setActive(active);
        return instrumentRepository.save(instrument);
    }

    public List<InstrumentEntity> findByFilters(Long damId, String instrumentType, Long sectionId, Boolean active) {
        return instrumentRepository.findByFilters(damId, instrumentType, sectionId, active);
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

    private void processInputsForUpdate(InstrumentEntity instrument, List<InputDTO> inputDTOs,
            Map<String, InputEntity> existingInputsByAcronym) {
        Set<String> acronyms = new HashSet<>();
        Set<String> names = new HashSet<>();

        int updatedCount = 0;
        int createdCount = 0;

        for (InputDTO inputDTO : inputDTOs) {
            if (!acronyms.add(inputDTO.getAcronym())) {
                throw new DuplicateResourceException("Sigla de input duplicada: " + inputDTO.getAcronym());
            }

            if (!names.add(inputDTO.getName())) {
                throw new DuplicateResourceException("Nome de input duplicado: " + inputDTO.getName());
            }

            MeasurementUnitEntity measurementUnit = measurementUnitRepository.findById(inputDTO.getMeasurementUnitId())
                    .orElseThrow(() -> new NotFoundException("Unidade de medida não encontrada com ID: " + inputDTO.getMeasurementUnitId()));

            InputEntity input = existingInputsByAcronym.get(inputDTO.getAcronym());

            if (input != null) {
                input.setName(inputDTO.getName());
                input.setPrecision(inputDTO.getPrecision());
                input.setMeasurementUnit(measurementUnit);
                inputRepository.save(input);
                instrument.getInputs().add(input);

                existingInputsByAcronym.remove(inputDTO.getAcronym());
                updatedCount++;
            } else {
                input = new InputEntity();
                input.setAcronym(inputDTO.getAcronym());
                input.setName(inputDTO.getName());
                input.setPrecision(inputDTO.getPrecision());
                input.setMeasurementUnit(measurementUnit);
                input.setInstrument(instrument);
                inputRepository.save(input);
                instrument.getInputs().add(input);
                createdCount++;
            }
        }

        log.info("Inputs processados: {} atualizados, {} criados", updatedCount, createdCount);
    }

    private void processConstantsForUpdate(InstrumentEntity instrument, List<ConstantDTO> constantDTOs,
            Map<String, ConstantEntity> existingConstantsByAcronym) {
        Set<String> acronyms = new HashSet<>();
        Set<String> names = new HashSet<>();

        int updatedCount = 0;
        int createdCount = 0;

        for (ConstantDTO constantDTO : constantDTOs) {
            if (!acronyms.add(constantDTO.getAcronym())) {
                throw new DuplicateResourceException("Sigla de constante duplicada: " + constantDTO.getAcronym());
            }

            if (!names.add(constantDTO.getName())) {
                throw new DuplicateResourceException("Nome de constante duplicado: " + constantDTO.getName());
            }

            MeasurementUnitEntity measurementUnit = measurementUnitRepository.findById(constantDTO.getMeasurementUnitId())
                    .orElseThrow(() -> new NotFoundException("Unidade de medida não encontrada com ID: " + constantDTO.getMeasurementUnitId()));

            ConstantEntity constant = existingConstantsByAcronym.get(constantDTO.getAcronym());

            if (constant != null) {
                constant.setName(constantDTO.getName());
                constant.setPrecision(constantDTO.getPrecision());
                constant.setValue(constantDTO.getValue());
                constant.setMeasurementUnit(measurementUnit);
                constantRepository.save(constant);
                instrument.getConstants().add(constant);

                existingConstantsByAcronym.remove(constantDTO.getAcronym());
                updatedCount++;
            } else {
                constant = new ConstantEntity();
                constant.setAcronym(constantDTO.getAcronym());
                constant.setName(constantDTO.getName());
                constant.setPrecision(constantDTO.getPrecision());
                constant.setValue(constantDTO.getValue());
                constant.setMeasurementUnit(measurementUnit);
                constant.setInstrument(instrument);
                constantRepository.save(constant);
                instrument.getConstants().add(constant);
                createdCount++;
            }
        }

        log.info("Constants processadas: {} atualizadas, {} criadas", updatedCount, createdCount);
    }

    private void processOutputsForUpdate(InstrumentEntity instrument, List<OutputDTO> outputDTOs,
            Map<String, OutputEntity> existingOutputsByAcronym) {
        Set<String> acronyms = new HashSet<>();
        Set<String> names = new HashSet<>();

        Map<String, OutputEntity> existingOutputsByName = instrument.getOutputs().stream()
                .filter(OutputEntity::getActive)
                .collect(Collectors.toMap(
                        OutputEntity::getName,
                        output -> output,
                        (existing, replacement) -> existing
                ));

        Set<String> inputAcronyms = instrument.getInputs().stream()
                .map(InputEntity::getAcronym)
                .collect(Collectors.toSet());

        Set<String> constantAcronyms = instrument.getConstants().stream()
                .map(ConstantEntity::getAcronym)
                .collect(Collectors.toSet());

        int updatedCount = 0;
        int createdCount = 0;

        for (OutputDTO outputDTO : outputDTOs) {
            if (!acronyms.add(outputDTO.getAcronym())) {
                throw new DuplicateResourceException("Sigla de output duplicada: " + outputDTO.getAcronym());
            }

            if (!names.add(outputDTO.getName())) {
                throw new DuplicateResourceException("Nome de output duplicado: " + outputDTO.getName());
            }

            validateEquation(outputDTO.getEquation(), inputAcronyms, constantAcronyms);

            MeasurementUnitEntity measurementUnit = measurementUnitRepository.findById(outputDTO.getMeasurementUnitId())
                    .orElseThrow(() -> new NotFoundException("Unidade de medida não encontrada com ID: " + outputDTO.getMeasurementUnitId()));

            OutputEntity output = existingOutputsByAcronym.get(outputDTO.getAcronym());

            if (output == null) {
                output = existingOutputsByName.get(outputDTO.getName());
                if (output != null) {
                    existingOutputsByAcronym.remove(output.getAcronym());
                }
            } else {
                existingOutputsByName.remove(output.getName());
            }

            if (output != null) {
                output.setName(outputDTO.getName());
                output.setAcronym(outputDTO.getAcronym());
                output.setEquation(outputDTO.getEquation());
                output.setPrecision(outputDTO.getPrecision());
                output.setMeasurementUnit(measurementUnit);
                output.setActive(true); // Reativar o output
                outputRepository.save(output);
                instrument.getOutputs().add(output);
                updatedCount++;
                log.debug("Output atualizado: {} (antigo acrônimo/nome: {}/{})",
                        outputDTO.getAcronym(), output.getAcronym(), output.getName());
            } else {
                output = new OutputEntity();
                output.setAcronym(outputDTO.getAcronym());
                output.setName(outputDTO.getName());
                output.setEquation(outputDTO.getEquation());
                output.setPrecision(outputDTO.getPrecision());
                output.setMeasurementUnit(measurementUnit);
                output.setActive(true);
                output.setInstrument(instrument);
                outputRepository.save(output);
                instrument.getOutputs().add(output);
                createdCount++;
            }
        }

        log.info("Outputs processados: {} atualizados, {} criados", updatedCount, createdCount);
    }

    private void deleteUnusedComponents(Map<String, InputEntity> unusedInputs, Map<String, ConstantEntity> unusedConstants) {
        for (InputEntity input : unusedInputs.values()) {
            inputRepository.delete(input);
        }

        for (ConstantEntity constant : unusedConstants.values()) {
            constantRepository.delete(constant);
        }

        log.info("Componentes não utilizados excluídos: {} inputs, {} constants",
                unusedInputs.size(), unusedConstants.size());
    }

    private void updateLimits(InstrumentEntity instrument, UpdateInstrumentRequest request) {
        if (!request.getNoLimit() && request.getStatisticalLimit() != null) {
            if (instrument.getStatisticalLimit() != null) {
                instrument.getStatisticalLimit().setLowerValue(request.getStatisticalLimit().getLowerValue());
                instrument.getStatisticalLimit().setUpperValue(request.getStatisticalLimit().getUpperValue());
                statisticalLimitRepository.save(instrument.getStatisticalLimit());
            } else {
                StatisticalLimitEntity statisticalLimit = new StatisticalLimitEntity();
                statisticalLimit.setInstrument(instrument);
                statisticalLimit.setLowerValue(request.getStatisticalLimit().getLowerValue());
                statisticalLimit.setUpperValue(request.getStatisticalLimit().getUpperValue());
                statisticalLimitRepository.save(statisticalLimit);
                instrument.setStatisticalLimit(statisticalLimit);
            }
        } else if (instrument.getStatisticalLimit() != null) {
            statisticalLimitRepository.delete(instrument.getStatisticalLimit());
            instrument.setStatisticalLimit(null);
        }

        if (!request.getNoLimit() && request.getDeterministicLimit() != null) {
            if (instrument.getDeterministicLimit() != null) {
                instrument.getDeterministicLimit().setAttentionValue(request.getDeterministicLimit().getAttentionValue());
                instrument.getDeterministicLimit().setAlertValue(request.getDeterministicLimit().getAlertValue());
                instrument.getDeterministicLimit().setEmergencyValue(request.getDeterministicLimit().getEmergencyValue());
                deterministicLimitRepository.save(instrument.getDeterministicLimit());
            } else {
                DeterministicLimitEntity deterministicLimit = new DeterministicLimitEntity();
                deterministicLimit.setInstrument(instrument);
                deterministicLimit.setAttentionValue(request.getDeterministicLimit().getAttentionValue());
                deterministicLimit.setAlertValue(request.getDeterministicLimit().getAlertValue());
                deterministicLimit.setEmergencyValue(request.getDeterministicLimit().getEmergencyValue());
                deterministicLimitRepository.save(deterministicLimit);
                instrument.setDeterministicLimit(deterministicLimit);
            }
        } else if (instrument.getDeterministicLimit() != null) {
            deterministicLimitRepository.delete(instrument.getDeterministicLimit());
            instrument.setDeterministicLimit(null);
        }
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
        dto.setInstrumentType(instrument.getInstrumentType());
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
            if (output.getActive()) {
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
