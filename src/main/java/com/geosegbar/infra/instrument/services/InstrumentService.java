package com.geosegbar.infra.instrument.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.utils.ExpressionEvaluator;
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
import com.geosegbar.infra.instrument.dtos.UpdateInstrumentRequest;
import com.geosegbar.infra.instrument.events.InstrumentCreatedEvent;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
import com.geosegbar.infra.instrument_type.persistence.jpa.InstrumentTypeRepository;
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
    private final InstrumentTypeRepository instrumentTypeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Cacheable(value = "allInstruments", cacheManager = "instrumentCacheManager")
    public List<InstrumentEntity> findAll() {
        return instrumentRepository.findAllByOrderByNameAsc();
    }

    @Cacheable(value = "instrumentsByDam", key = "#damId", cacheManager = "instrumentCacheManager")
    public List<InstrumentEntity> findByDamId(Long damId) {
        return instrumentRepository.findByDamId(damId);
    }

    @Cacheable(value = "instrumentById", key = "#id", cacheManager = "instrumentCacheManager")
    public InstrumentEntity findById(Long id) {
        return instrumentRepository.findByIdWithBasicRelations(id)
                .orElseThrow(() -> new NotFoundException("Instrumento não encontrado com ID: " + id));
    }

    @Cacheable(value = "instrumentWithDetails", key = "#id", cacheManager = "instrumentCacheManager")
    public InstrumentEntity findWithAllDetails(Long id) {
        return instrumentRepository.findWithCompleteDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Instrumento não encontrado com ID: " + id));
    }

    @Cacheable(
            value = "instrumentsByClient",
            key = "#clientId + '_' + #active",
            cacheManager = "instrumentCacheManager"
    )
    public List<InstrumentEntity> findByClientId(Long clientId, Boolean active) {
        return instrumentRepository.findByClientIdOptimized(clientId, active);
    }

    @Transactional
    @CacheEvict(
            value = {
                "instrumentById", "instrumentWithDetails", "instrumentsByClient",
                "instrumentsByFilters", "instrumentsByDam", "allInstruments",
                "instrumentResponseDTO"
            },
            allEntries = true,
            cacheManager = "instrumentCacheManager"
    )
    public InstrumentEntity createComplete(CreateInstrumentRequest request) {
        // Se for régua linimétrica, fazemos validações específicas e garantimos noLimit = true
        if (Boolean.TRUE.equals(request.getIsLinimetricRuler())) {
            // Forçar noLimit = true para réguas linimétricas
            request.setNoLimit(true);

            // Se fornecido um código, verificar se já existe outra régua com o mesmo código
            if (request.getLinimetricRulerCode() != null
                    && instrumentRepository.findByLinimetricRulerCode(request.getLinimetricRulerCode()).isPresent()) {
                throw new DuplicateResourceException("Já existe uma régua linimétrica com o código " + request.getLinimetricRulerCode());
            }
        } else {
            // Para instrumentos normais, validamos as entradas
            validateRequest(request);
            validateUniqueAcronymsAcrossComponents(
                    request.getInputs(),
                    request.getConstants(),
                    request.getOutputs()
            );
        }

        if (instrumentRepository.existsByNameAndDamId(request.getName(), request.getDamId())) {
            throw new DuplicateResourceException("Já existe um instrumento com o nome '" + request.getName() + "' na mesma barragem");
        }

        DamEntity dam = damRepository.findById(request.getDamId())
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada com ID: " + request.getDamId()));

        SectionEntity section = null;
        if (request.getSectionId() != null) {
            section = sectionRepository.findById(request.getSectionId())
                    .orElseThrow(() -> new NotFoundException("Seção não encontrada com ID: " + request.getSectionId()));
        }

        InstrumentTypeEntity instrumentType = instrumentTypeRepository.findById(request.getInstrumentTypeId())
                .orElseThrow(() -> new NotFoundException("Tipo de instrumento não encontrado com ID: " + request.getInstrumentTypeId()));

        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setName(request.getName().toUpperCase());
        instrument.setLocation(request.getLocation());
        instrument.setDistanceOffset(request.getDistanceOffset());
        instrument.setLatitude(request.getLatitude());
        instrument.setLongitude(request.getLongitude());
        instrument.setNoLimit(request.getNoLimit());
        instrument.setInstrumentType(instrumentType);
        instrument.setDam(dam);
        instrument.setSection(section);
        instrument.setActive(true);
        instrument.setActiveForSection(request.getActiveForSection());

        // Definindo os campos relacionados à régua linimétrica
        instrument.setIsLinimetricRuler(request.getIsLinimetricRuler());
        instrument.setLinimetricRulerCode(request.getLinimetricRulerCode());

        InstrumentEntity savedInstrument = instrumentRepository.save(instrument);

        // Se for régua linimétrica, criamos components padrão
        if (Boolean.TRUE.equals(request.getIsLinimetricRuler())) {
            createLinimetricRulerComponents(savedInstrument);
        } else {
            // Caso contrário, processamos os componentes normalmente
            processInputs(savedInstrument, request.getInputs());

            if (request.getConstants() != null && !request.getConstants().isEmpty()) {
                processConstants(savedInstrument, request.getConstants());
            }

            processOutputs(savedInstrument, request.getOutputs());
        }

        savedInstrument = instrumentRepository.findWithActiveOutputsById(savedInstrument.getId())
                .orElseThrow(() -> new NotFoundException("Instrumento não encontrado após criação"));

        // Publicar evento em vez de chamar diretamente o serviço
        if (Boolean.FALSE.equals(savedInstrument.getIsLinimetricRuler())) {
            eventPublisher.publishEvent(new InstrumentCreatedEvent(savedInstrument));
        }

        return savedInstrument;
    }

    private void validateRequest(CreateInstrumentRequest request) {
        if (Boolean.TRUE.equals(request.getIsLinimetricRuler())) {
            return;
        }

        // Validações padrão
        if (request.getInputs() == null || request.getInputs().isEmpty()) {
            throw new InvalidInputException("Pelo menos um input é obrigatório para instrumentos normais");
        }

        if (request.getOutputs() == null || request.getOutputs().isEmpty()) {
            throw new InvalidInputException("Pelo menos um output é obrigatório para instrumentos normais");
        }

        // Validar que quando noLimit = false, todos os outputs têm o mesmo tipo de limite
        if (!request.getNoLimit() && request.getOutputs().size() > 1) {
            boolean hasStatistical = request.getOutputs().get(0).getStatisticalLimit() != null;
            String firstLimitType = hasStatistical ? "estatístico" : "determinístico";

            for (int i = 1; i < request.getOutputs().size(); i++) {
                OutputDTO output = request.getOutputs().get(i);
                boolean currentHasStatistical = output.getStatisticalLimit() != null;

                if (hasStatistical != currentHasStatistical) {
                    throw new InvalidInputException(
                            "Todos os outputs de um instrumento devem ter o mesmo tipo de limite. "
                            + "O primeiro output usa limite " + firstLimitType + ", mas o output '"
                            + output.getName() + "' usa um tipo diferente."
                    );
                }
            }
        }

        // Validar cada output individualmente
        for (OutputDTO outputDTO : request.getOutputs()) {
            validateOutputRequest(outputDTO, request.getNoLimit());
        }
    }

    private void validateRequest(UpdateInstrumentRequest request) {
        // Se for régua linimétrica, não precisamos validar inputs/outputs
        if (Boolean.TRUE.equals(request.getIsLinimetricRuler())) {
            return;
        }

        // Validações padrão
        if (request.getInputs() == null || request.getInputs().isEmpty()) {
            throw new InvalidInputException("Pelo menos um input é obrigatório para instrumentos normais");
        }

        if (request.getOutputs() == null || request.getOutputs().isEmpty()) {
            throw new InvalidInputException("Pelo menos um output é obrigatório para instrumentos normais");
        }

        // Validar que quando noLimit = false, todos os outputs têm o mesmo tipo de limite
        if (!request.getNoLimit() && request.getOutputs().size() > 1) {
            boolean hasStatistical = request.getOutputs().get(0).getStatisticalLimit() != null;
            String firstLimitType = hasStatistical ? "estatístico" : "determinístico";

            for (int i = 1; i < request.getOutputs().size(); i++) {
                OutputDTO output = request.getOutputs().get(i);
                boolean currentHasStatistical = output.getStatisticalLimit() != null;

                if (hasStatistical != currentHasStatistical) {
                    throw new InvalidInputException(
                            "Todos os outputs de um instrumento devem ter o mesmo tipo de limite. "
                            + "O primeiro output usa limite " + firstLimitType + ", mas o output '"
                            + output.getName() + "' usa um tipo diferente."
                    );
                }
            }
        }

        // Validar cada output individualmente
        for (OutputDTO outputDTO : request.getOutputs()) {
            validateOutputRequest(outputDTO, request.getNoLimit());
        }
    }

    private void validateOutputRequest(OutputDTO outputDTO, Boolean instrumentNoLimit) {
        if (Boolean.TRUE.equals(instrumentNoLimit)) {
            if (outputDTO.getStatisticalLimit() != null || outputDTO.getDeterministicLimit() != null) {
                throw new InvalidInputException("Quando o instrumento está marcado como 'Sem Limites', seus outputs não devem ter limites estatísticos ou determinísticos");
            }
        } else {
            boolean hasStatistical = outputDTO.getStatisticalLimit() != null;
            boolean hasDeterministic = outputDTO.getDeterministicLimit() != null;

            if (!hasStatistical && !hasDeterministic) {
                throw new InvalidInputException("Quando o instrumento não está marcado como 'Sem Limites', cada output deve ter um tipo de limite");
            }

            if (hasStatistical && hasDeterministic) {
                throw new InvalidInputException("Apenas um tipo de limite (estatístico ou determinístico) deve ser fornecido para um output, não ambos");
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
            input.setAcronym(inputDTO.getAcronym().toUpperCase());
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
            constant.setAcronym(constantDTO.getAcronym().toUpperCase());
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
            // Validações existentes
            if (!acronyms.add(outputDTO.getAcronym())) {
                throw new DuplicateResourceException("Sigla de output duplicada: " + outputDTO.getAcronym());
            }

            if (!names.add(outputDTO.getName())) {
                throw new DuplicateResourceException("Nome de output duplicado: " + outputDTO.getName());
            }

            validateEquation(outputDTO.getEquation(), inputAcronyms, constantAcronyms);
            validateOutputRequest(outputDTO, instrument.getNoLimit());

            MeasurementUnitEntity measurementUnit = measurementUnitRepository.findById(outputDTO.getMeasurementUnitId())
                    .orElseThrow(() -> new NotFoundException("Unidade de medida não encontrada com ID: " + outputDTO.getMeasurementUnitId()));

            OutputEntity output = new OutputEntity();
            output.setAcronym(outputDTO.getAcronym().toUpperCase());
            output.setName(outputDTO.getName());
            output.setEquation(outputDTO.getEquation());
            output.setPrecision(outputDTO.getPrecision());
            output.setMeasurementUnit(measurementUnit);
            output.setActive(true);
            output.setInstrument(instrument);

            OutputEntity savedOutput = outputRepository.save(output);

            // Criar os limites para o output apenas se o instrumento não for noLimit
            if (!instrument.getNoLimit()) {
                if (outputDTO.getStatisticalLimit() != null) {
                    StatisticalLimitEntity statisticalLimit = new StatisticalLimitEntity();
                    statisticalLimit.setOutput(savedOutput);
                    statisticalLimit.setLowerValue(outputDTO.getStatisticalLimit().getLowerValue());
                    statisticalLimit.setUpperValue(outputDTO.getStatisticalLimit().getUpperValue());
                    statisticalLimitRepository.save(statisticalLimit);
                    savedOutput.setStatisticalLimit(statisticalLimit);
                }

                if (outputDTO.getDeterministicLimit() != null) {
                    DeterministicLimitEntity deterministicLimit = new DeterministicLimitEntity();
                    deterministicLimit.setOutput(savedOutput);
                    deterministicLimit.setAttentionValue(outputDTO.getDeterministicLimit().getAttentionValue());
                    deterministicLimit.setAlertValue(outputDTO.getDeterministicLimit().getAlertValue());
                    deterministicLimit.setEmergencyValue(outputDTO.getDeterministicLimit().getEmergencyValue());
                    deterministicLimitRepository.save(deterministicLimit);
                    savedOutput.setDeterministicLimit(deterministicLimit);
                }
            }

            instrument.getOutputs().add(savedOutput);
        }
    }

    @Transactional
    @CacheEvict(
            value = {
                "instrumentById", "instrumentWithDetails", "instrumentsByClient",
                "instrumentsByFilters", "instrumentsByDam", "allInstruments",
                "instrumentResponseDTO"
            },
            allEntries = true,
            cacheManager = "instrumentCacheManager"
    )
    public InstrumentEntity update(Long id, UpdateInstrumentRequest request) {
        InstrumentEntity oldInstrument = findById(id);

        // Verificar se está tentando mudar o tipo do instrumento (linimétrico/normal)
        if (!oldInstrument.getIsLinimetricRuler().equals(request.getIsLinimetricRuler())) {
            throw new InvalidInputException("Não é permitido alterar o tipo de instrumento. Uma vez criado como régua linimétrica ou instrumento normal, este atributo não pode ser modificado.");
        }

        // Se for régua linimétrica, fazemos validações específicas e garantimos noLimit = true
        if (Boolean.TRUE.equals(request.getIsLinimetricRuler())) {
            // Forçar noLimit = true para réguas linimétricas
            request.setNoLimit(true);

            // Se fornecido um código, verificar se já existe outra régua com o mesmo código (diferente desta)
            if (request.getLinimetricRulerCode() != null
                    && instrumentRepository.existsByLinimetricRulerCodeAndIdNot(request.getLinimetricRulerCode(), id)) {
                throw new DuplicateResourceException("Já existe uma régua linimétrica com o código " + request.getLinimetricRulerCode());
            }
        } else {
            // Para instrumentos normais, validamos as entradas
            validateRequest(request);
            validateUniqueAcronymsAcrossComponents(
                    request.getInputs(),
                    request.getConstants(),
                    request.getOutputs()
            );
        }

        if (instrumentRepository.existsByNameAndDamIdAndIdNot(request.getName(), request.getDamId(), id)) {
            throw new DuplicateResourceException("Já existe um instrumento com esse nome nesta barragem");
        }

        // Não precisamos mais verificar se está mudando de normal para régua linimétrica ou vice-versa
        // pois não permitimos mais essa mudança
        if (!Boolean.TRUE.equals(request.getIsLinimetricRuler())) {
            // Se continua sendo instrumento normal, mapeia os componentes existentes
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

            // Atualizar os campos básicos
            updateInstrumentBasicFields(oldInstrument, request);
            InstrumentEntity savedInstrument = instrumentRepository.save(oldInstrument);

            // Processar os componentes para atualização
            processInputsForUpdate(savedInstrument, request.getInputs(), existingInputsByAcronym);
            if (request.getConstants() != null && !request.getConstants().isEmpty()) {
                processConstantsForUpdate(savedInstrument, request.getConstants(), existingConstantsByAcronym);
            }
            processOutputsForUpdate(savedInstrument, request.getOutputs(), existingOutputsByAcronym);

            // Remover componentes não utilizados
            deleteUnusedComponents(existingInputsByAcronym, existingConstantsByAcronym);

            log.info("Instrumento normal atualizado com sucesso");

            return instrumentRepository.findWithActiveOutputsById(id)
                    .orElseThrow(() -> new NotFoundException("Instrumento não encontrado após atualização"));
        } else {
            // Atualizar os campos básicos do instrumento linimétrico
            updateInstrumentBasicFields(oldInstrument, request);
            instrumentRepository.save(oldInstrument);

            log.info("Instrumento linimétrico atualizado com sucesso");

            return instrumentRepository.findWithActiveOutputsById(id)
                    .orElseThrow(() -> new NotFoundException("Instrumento não encontrado após atualização"));
        }
    }

    private void validateUniqueAcronymsAcrossComponents(List<InputDTO> inputs, List<ConstantDTO> constants, List<OutputDTO> outputs) {
        Map<String, String> acronymMap = new HashMap<>();

        if (inputs != null) {
            for (InputDTO input : inputs) {
                String acronym = input.getAcronym();
                if (acronymMap.containsKey(acronym)) {
                    throw new DuplicateResourceException(
                            "Acrônimo '" + acronym + "' duplicado: já existe como " + acronymMap.get(acronym)
                    );
                }
                acronymMap.put(acronym, "input");
            }
        }

        if (constants != null) {
            for (ConstantDTO constant : constants) {
                String acronym = constant.getAcronym();
                if (acronymMap.containsKey(acronym)) {
                    throw new DuplicateResourceException(
                            "Acrônimo '" + acronym + "' duplicado: já existe como " + acronymMap.get(acronym)
                    );
                }
                acronymMap.put(acronym, "constant");
            }
        }

        if (outputs != null) {
            for (OutputDTO output : outputs) {
                String acronym = output.getAcronym();
                if (acronymMap.containsKey(acronym)) {
                    throw new DuplicateResourceException(
                            "Acrônimo '" + acronym + "' duplicado: já existe como " + acronymMap.get(acronym)
                    );
                }
                acronymMap.put(acronym, "output");
            }
        }
    }

    private void createLinimetricRulerComponents(InstrumentEntity instrument) {
        // Obter a unidade de medida com ID 1 (metros)
        MeasurementUnitEntity measurementUnit = measurementUnitRepository.findById(1L)
                .orElseThrow(() -> new NotFoundException("Unidade de medida 'metros' não encontrada com ID: 1"));

        // Criar o input LEI
        InputEntity input = new InputEntity();
        input.setAcronym("LEI");
        input.setName("Leitura");
        input.setPrecision(6);
        input.setMeasurementUnit(measurementUnit);
        input.setInstrument(instrument);

        inputRepository.save(input);
        instrument.getInputs().add(input);

        // Criar o output NVL
        OutputEntity output = new OutputEntity();
        output.setAcronym("NVL");
        output.setName("Nivel");
        output.setEquation("LEI * 1");
        output.setPrecision(6);
        output.setMeasurementUnit(measurementUnit);
        output.setActive(true);
        output.setInstrument(instrument);

        outputRepository.save(output);
        instrument.getOutputs().add(output);

        log.info("Componentes da régua linimétrica criados com sucesso para o instrumento ID: {}", instrument.getId());
    }

    private void updateInstrumentBasicFields(InstrumentEntity instrument, UpdateInstrumentRequest request) {
        DamEntity dam = damRepository.findById(request.getDamId())
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada com ID: " + request.getDamId()));

        SectionEntity section = null;
        if (request.getSectionId() != null) {
            section = sectionRepository.findById(request.getSectionId())
                    .orElseThrow(() -> new NotFoundException("Seção não encontrada com ID: " + request.getSectionId()));
        }
        InstrumentTypeEntity instrumentType = instrumentTypeRepository.findById(request.getInstrumentTypeId())
                .orElseThrow(() -> new NotFoundException("Tipo de instrumento não encontrado com ID: " + request.getInstrumentTypeId()));

        instrument.setName(request.getName().toUpperCase());
        instrument.setLocation(request.getLocation());
        instrument.setDistanceOffset(request.getDistanceOffset());
        instrument.setLatitude(request.getLatitude());
        instrument.setLongitude(request.getLongitude());
        instrument.setNoLimit(request.getNoLimit());
        instrument.setInstrumentType(instrumentType);
        instrument.setDam(dam);
        instrument.setSection(section);
        instrument.setActiveForSection(request.getActiveForSection());

        // Apenas atualizar o código da régua linimétrica, mas nunca o status
        if (Boolean.TRUE.equals(instrument.getIsLinimetricRuler())) {
            instrument.setLinimetricRulerCode(request.getLinimetricRulerCode());
        }

        // Não atualizamos o campo isLinimetricRuler, pois ele é imutável após a criação
    }

    @Transactional
    @CacheEvict(
            value = {
                "instrumentById", "instrumentWithDetails", "instrumentsByClient",
                "instrumentsByFilters", "instrumentsByDam", "allInstruments",
                "instrumentResponseDTO"
            },
            allEntries = true,
            cacheManager = "instrumentCacheManager"
    )
    public void delete(Long id) {
        InstrumentEntity instrument = findById(id);

        for (OutputEntity output : instrument.getOutputs()) {
            if (output.getStatisticalLimit() != null) {
                statisticalLimitRepository.delete(output.getStatisticalLimit());
            }
            if (output.getDeterministicLimit() != null) {
                deterministicLimitRepository.delete(output.getDeterministicLimit());
            }
        }

        inputRepository.deleteByInstrumentId(id);
        constantRepository.deleteByInstrumentId(id);
        outputRepository.deleteByInstrumentId(id);

        instrumentRepository.delete(instrument);
    }

    @Transactional
    @CacheEvict(
            value = {
                "instrumentById", "instrumentWithDetails", "instrumentsByClient",
                "instrumentsByFilters", "instrumentsByDam", "allInstruments",
                "instrumentResponseDTO"
            },
            allEntries = true,
            cacheManager = "instrumentCacheManager"
    )
    public InstrumentEntity toggleActiveInstrument(Long id, Boolean active) {
        InstrumentEntity instrument = findById(id);
        instrument.setActive(active);
        return instrumentRepository.save(instrument);
    }

    @Cacheable(
            value = "instrumentsByFilters",
            key = "#damId + '_' + #instrumentTypeId + '_' + #sectionId + '_' + #active + '_' + #clientId",
            cacheManager = "instrumentCacheManager"
    )
    public List<InstrumentEntity> findByFilters(Long damId, Long instrumentTypeId, Long sectionId, Boolean active, Long clientId) {
        log.debug("Cache miss: findByFilters({}, {}, {}, {}, {})", damId, instrumentTypeId, sectionId, active, clientId);
        return instrumentRepository.findByFiltersOptimized(damId, instrumentTypeId, sectionId, active, clientId);
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
                input.setAcronym(inputDTO.getAcronym().toUpperCase());
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
                constant.setAcronym(constantDTO.getAcronym().toUpperCase());
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

        Set<String> inputAcronyms = instrument.getInputs().stream()
                .map(InputEntity::getAcronym)
                .collect(Collectors.toSet());

        Set<String> constantAcronyms = instrument.getConstants().stream()
                .map(ConstantEntity::getAcronym)
                .collect(Collectors.toSet());

        int updatedCount = 0;
        int createdCount = 0;

        // Validar que todos os outputs usam o mesmo tipo de limite
        if (!instrument.getNoLimit() && outputDTOs.size() > 1) {
            boolean hasStatistical = outputDTOs.get(0).getStatisticalLimit() != null;
            String firstLimitType = hasStatistical ? "estatístico" : "determinístico";

            for (int i = 1; i < outputDTOs.size(); i++) {
                OutputDTO output = outputDTOs.get(i);
                boolean currentHasStatistical = output.getStatisticalLimit() != null;

                if (hasStatistical != currentHasStatistical) {
                    throw new InvalidInputException(
                            "Todos os outputs de um instrumento devem ter o mesmo tipo de limite. "
                            + "O primeiro output usa limite " + firstLimitType + ", mas o output '"
                            + output.getName() + "' usa um tipo diferente."
                    );
                }
            }
        }

        for (OutputDTO outputDTO : outputDTOs) {
            if (!acronyms.add(outputDTO.getAcronym())) {
                throw new DuplicateResourceException("Sigla de output duplicada: " + outputDTO.getAcronym());
            }

            if (!names.add(outputDTO.getName())) {
                throw new DuplicateResourceException("Nome de output duplicado: " + outputDTO.getName());
            }

            validateEquation(outputDTO.getEquation(), inputAcronyms, constantAcronyms);
            validateOutputRequest(outputDTO, instrument.getNoLimit());

            MeasurementUnitEntity measurementUnit = measurementUnitRepository.findById(outputDTO.getMeasurementUnitId())
                    .orElseThrow(() -> new NotFoundException("Unidade de medida não encontrada com ID: " + outputDTO.getMeasurementUnitId()));

            OutputEntity output = existingOutputsByAcronym.get(outputDTO.getAcronym());

            if (output == null) {
                // Criar novo output
                output = new OutputEntity();
                output.setAcronym(outputDTO.getAcronym().toUpperCase());
                output.setName(outputDTO.getName());
                output.setEquation(outputDTO.getEquation());
                output.setPrecision(outputDTO.getPrecision());
                output.setMeasurementUnit(measurementUnit);
                output.setActive(true);
                output.setInstrument(instrument);

                OutputEntity savedOutput = outputRepository.save(output);

                // Criar os limites para o output apenas se o instrumento não for noLimit
                if (!instrument.getNoLimit()) {
                    if (outputDTO.getStatisticalLimit() != null) {
                        StatisticalLimitEntity statisticalLimit = new StatisticalLimitEntity();
                        statisticalLimit.setOutput(savedOutput);
                        statisticalLimit.setLowerValue(outputDTO.getStatisticalLimit().getLowerValue());
                        statisticalLimit.setUpperValue(outputDTO.getStatisticalLimit().getUpperValue());
                        statisticalLimitRepository.save(statisticalLimit);
                        savedOutput.setStatisticalLimit(statisticalLimit);
                    }

                    if (outputDTO.getDeterministicLimit() != null) {
                        DeterministicLimitEntity deterministicLimit = new DeterministicLimitEntity();
                        deterministicLimit.setOutput(savedOutput);
                        deterministicLimit.setAttentionValue(outputDTO.getDeterministicLimit().getAttentionValue());
                        deterministicLimit.setAlertValue(outputDTO.getDeterministicLimit().getAlertValue());
                        deterministicLimit.setEmergencyValue(outputDTO.getDeterministicLimit().getEmergencyValue());
                        deterministicLimitRepository.save(deterministicLimit);
                        savedOutput.setDeterministicLimit(deterministicLimit);
                    }
                }

                instrument.getOutputs().add(savedOutput);
                createdCount++;
            } else {
                // Atualizar output existente
                output.setName(outputDTO.getName());
                output.setEquation(outputDTO.getEquation());
                output.setPrecision(outputDTO.getPrecision());
                output.setMeasurementUnit(measurementUnit);
                output.setActive(true);

                // Atualizar limites
                if (instrument.getNoLimit()) {
                    // Se o instrumento for noLimit, remover qualquer limite existente
                    if (output.getStatisticalLimit() != null) {
                        statisticalLimitRepository.delete(output.getStatisticalLimit());
                        output.setStatisticalLimit(null);
                    }
                    if (output.getDeterministicLimit() != null) {
                        deterministicLimitRepository.delete(output.getDeterministicLimit());
                        output.setDeterministicLimit(null);
                    }
                } else {
                    // Se o instrumento não for noLimit, atualizar os limites
                    if (outputDTO.getStatisticalLimit() != null) {
                        // Se já existe um limite determinístico, removê-lo
                        if (output.getDeterministicLimit() != null) {
                            deterministicLimitRepository.delete(output.getDeterministicLimit());
                            output.setDeterministicLimit(null);
                        }

                        // Atualizar ou criar limite estatístico
                        if (output.getStatisticalLimit() != null) {
                            output.getStatisticalLimit().setLowerValue(outputDTO.getStatisticalLimit().getLowerValue());
                            output.getStatisticalLimit().setUpperValue(outputDTO.getStatisticalLimit().getUpperValue());
                        } else {
                            StatisticalLimitEntity statisticalLimit = new StatisticalLimitEntity();
                            statisticalLimit.setOutput(output);
                            statisticalLimit.setLowerValue(outputDTO.getStatisticalLimit().getLowerValue());
                            statisticalLimit.setUpperValue(outputDTO.getStatisticalLimit().getUpperValue());
                            statisticalLimitRepository.save(statisticalLimit);
                            output.setStatisticalLimit(statisticalLimit);
                        }
                    } else if (outputDTO.getDeterministicLimit() != null) {
                        // Se já existe um limite estatístico, removê-lo
                        if (output.getStatisticalLimit() != null) {
                            statisticalLimitRepository.delete(output.getStatisticalLimit());
                            output.setStatisticalLimit(null);
                        }

                        // Atualizar ou criar limite determinístico
                        if (output.getDeterministicLimit() != null) {
                            output.getDeterministicLimit().setAttentionValue(outputDTO.getDeterministicLimit().getAttentionValue());
                            output.getDeterministicLimit().setAlertValue(outputDTO.getDeterministicLimit().getAlertValue());
                            output.getDeterministicLimit().setEmergencyValue(outputDTO.getDeterministicLimit().getEmergencyValue());
                        } else {
                            DeterministicLimitEntity deterministicLimit = new DeterministicLimitEntity();
                            deterministicLimit.setOutput(output);
                            deterministicLimit.setAttentionValue(outputDTO.getDeterministicLimit().getAttentionValue());
                            deterministicLimit.setAlertValue(outputDTO.getDeterministicLimit().getAlertValue());
                            deterministicLimit.setEmergencyValue(outputDTO.getDeterministicLimit().getEmergencyValue());
                            deterministicLimitRepository.save(deterministicLimit);
                            output.setDeterministicLimit(deterministicLimit);
                        }
                    }
                }

                outputRepository.save(output);
                existingOutputsByAcronym.remove(output.getAcronym());
                updatedCount++;
            }
        }

        // Desativar outputs não incluídos na atualização
        for (OutputEntity unusedOutput : existingOutputsByAcronym.values()) {
            unusedOutput.setActive(false);
            outputRepository.save(unusedOutput);
        }

        log.info("Outputs processados: {} atualizados, {} criados, {} desativados",
                updatedCount, createdCount, existingOutputsByAcronym.size());
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

    @Transactional
    @CacheEvict(
            value = {
                "instrumentById", "instrumentWithDetails", "instrumentsByClient",
                "instrumentsByFilters", "instrumentsByDam", "allInstruments",
                "instrumentResponseDTO"
            },
            allEntries = true,
            cacheManager = "instrumentCacheManager"
    )
    public InstrumentEntity toggleSectionVisibility(Long id, Boolean active) {
        InstrumentEntity instrument = findById(id);
        instrument.setActiveForSection(active);
        return instrumentRepository.save(instrument);
    }

    @Cacheable(
            value = "instrumentResponseDTO",
            key = "#instrument.id",
            cacheManager = "instrumentCacheManager"
    )
    public InstrumentResponseDTO mapToResponseDTO(InstrumentEntity instrument) {
        InstrumentResponseDTO dto = new InstrumentResponseDTO();
        dto.setId(instrument.getId());
        dto.setName(instrument.getName());
        dto.setLocation(instrument.getLocation());
        dto.setDistanceOffset(instrument.getDistanceOffset());
        dto.setLatitude(instrument.getLatitude());
        dto.setLongitude(instrument.getLongitude());
        dto.setNoLimit(instrument.getNoLimit());

        // Novos campos
        dto.setIsLinimetricRuler(instrument.getIsLinimetricRuler());
        dto.setLinimetricRulerCode(instrument.getLinimetricRulerCode());

        // Relações diretas
        DamEntity dam = instrument.getDam();
        dto.setDamId(dam.getId());
        dto.setDamName(dam.getName());

        InstrumentTypeEntity type = instrument.getInstrumentType();
        dto.setInstrumentTypeId(type.getId());
        dto.setInstrumentType(type.getName());

        SectionEntity section = instrument.getSection();
        if (section != null) {
            dto.setSectionId(section.getId());
            dto.setSectionName(section.getName());
        }

        dto.setActiveForSection(instrument.getActiveForSection());

        // Inputs - usar inicialização eager para evitar N+1
        List<InputDTO> inputDTOs = new ArrayList<>(instrument.getInputs().size());
        for (InputEntity input : instrument.getInputs()) {
            InputDTO inputDTO = new InputDTO();
            inputDTO.setId(input.getId());
            inputDTO.setAcronym(input.getAcronym());
            inputDTO.setName(input.getName());
            inputDTO.setPrecision(input.getPrecision());

            MeasurementUnitEntity unit = input.getMeasurementUnit();
            inputDTO.setMeasurementUnitId(unit.getId());
            inputDTO.setMeasurementUnitName(unit.getName());
            inputDTO.setMeasurementUnitAcronym(unit.getAcronym());

            inputDTOs.add(inputDTO);
        }
        dto.setInputs(inputDTOs);

        // Constants
        List<ConstantDTO> constantDTOs = new ArrayList<>(instrument.getConstants().size());
        for (ConstantEntity constant : instrument.getConstants()) {
            ConstantDTO constantDTO = new ConstantDTO();
            constantDTO.setId(constant.getId());
            constantDTO.setAcronym(constant.getAcronym());
            constantDTO.setName(constant.getName());
            constantDTO.setPrecision(constant.getPrecision());
            constantDTO.setValue(constant.getValue());

            MeasurementUnitEntity unit = constant.getMeasurementUnit();
            constantDTO.setMeasurementUnitId(unit.getId());
            constantDTO.setMeasurementUnitName(unit.getName());
            constantDTO.setMeasurementUnitAcronym(unit.getAcronym());

            constantDTOs.add(constantDTO);
        }
        dto.setConstants(constantDTOs);

        // Outputs - filtrar ativos primeiro para melhorar performance
        List<OutputEntity> activeOutputs = instrument.getOutputs().stream()
                .filter(OutputEntity::getActive)
                .collect(Collectors.toList());

        List<OutputDTO> outputDTOs = new ArrayList<>(activeOutputs.size());
        for (OutputEntity output : activeOutputs) {
            OutputDTO outputDTO = new OutputDTO();
            outputDTO.setId(output.getId());
            outputDTO.setAcronym(output.getAcronym());
            outputDTO.setName(output.getName());
            outputDTO.setEquation(output.getEquation());
            outputDTO.setPrecision(output.getPrecision());

            MeasurementUnitEntity unit = output.getMeasurementUnit();
            outputDTO.setMeasurementUnitId(unit.getId());
            outputDTO.setMeasurementUnitName(unit.getName());
            outputDTO.setMeasurementUnitAcronym(unit.getAcronym());

            // Limites estatísticos
            StatisticalLimitEntity statLimit = output.getStatisticalLimit();
            if (statLimit != null) {
                StatisticalLimitDTO limitDTO = new StatisticalLimitDTO();
                limitDTO.setId(statLimit.getId());
                limitDTO.setLowerValue(statLimit.getLowerValue());
                limitDTO.setUpperValue(statLimit.getUpperValue());
                outputDTO.setStatisticalLimit(limitDTO);
            }

            // Limites determinísticos
            DeterministicLimitEntity detLimit = output.getDeterministicLimit();
            if (detLimit != null) {
                DeterministicLimitDTO limitDTO = new DeterministicLimitDTO();
                limitDTO.setId(detLimit.getId());
                limitDTO.setAttentionValue(detLimit.getAttentionValue());
                limitDTO.setAlertValue(detLimit.getAlertValue());
                limitDTO.setEmergencyValue(detLimit.getEmergencyValue());
                outputDTO.setDeterministicLimit(limitDTO);
            }

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
