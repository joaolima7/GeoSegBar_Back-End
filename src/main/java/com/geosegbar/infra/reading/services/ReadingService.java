package com.geosegbar.infra.reading.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.enums.LimitStatusEnum;
import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.DeterministicLimitEntity;
import com.geosegbar.entities.InputEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.entities.ReadingEntity;
import com.geosegbar.entities.ReadingInputValueEntity;
import com.geosegbar.entities.StatisticalLimitEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.exceptions.UnauthorizedException;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
import com.geosegbar.infra.reading.dtos.BulkToggleActiveResponseDTO;
import com.geosegbar.infra.reading.dtos.InstrumentLimitStatusDTO;
import com.geosegbar.infra.reading.dtos.PagedReadingResponseDTO;
import com.geosegbar.infra.reading.dtos.ReadingRequestDTO;
import com.geosegbar.infra.reading.dtos.ReadingResponseDTO;
import com.geosegbar.infra.reading.dtos.UpdateReadingRequestDTO;
import com.geosegbar.infra.reading.persistence.jpa.ReadingRepository;
import com.geosegbar.infra.reading_input_value.dtos.ReadingInputValueDTO;
import com.geosegbar.infra.reading_input_value.persistence.jpa.ReadingInputValueRepository;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReadingService {

    private final ReadingRepository readingRepository;
    private final ReadingInputValueRepository readingInputValueRepository;
    private final InstrumentRepository instrumentRepository;
    private final OutputCalculationService outputCalculationService;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    public List<ReadingResponseDTO> findByInstrumentId(Long instrumentId) {

        List<ReadingEntity> readings = readingRepository.findByInstrumentIdOptimized(instrumentId);
        return readings.stream()
                .map(this::mapToResponseDTOOptimized)
                .collect(Collectors.toList());
    }

    public boolean existsByInstrumentAndDate(Long instrumentId, LocalDate date) {
        return readingRepository.existsByInstrumentIdAndDate(instrumentId, date);
    }

    public InstrumentLimitStatusDTO getInstrumentLimitStatus(Long instrumentId, int limit) {
        InstrumentEntity instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new NotFoundException("Instrumento não encontrado com ID: " + instrumentId));

        Pageable pageable = PageRequest.of(0, limit);
        List<ReadingEntity> recentReadings = readingRepository.findTopNByInstrumentIdOptimized(instrumentId, pageable);

        if (recentReadings.isEmpty()) {
            return createInstrumentLimitStatusDTO(instrument, LimitStatusEnum.NORMAL, null);
        }

        LocalDate mostRecentDate = recentReadings.stream()
                .map(ReadingEntity::getDate)
                .max(LocalDate::compareTo)
                .orElse(null);

        if (mostRecentDate != null) {
            List<ReadingEntity> readingsOfMostRecentDate = recentReadings.stream()
                    .filter(r -> r.getDate().equals(mostRecentDate))
                    .collect(Collectors.toList());

            LocalTime mostRecentHour = readingsOfMostRecentDate.stream()
                    .map(ReadingEntity::getHour)
                    .max(LocalTime::compareTo)
                    .orElse(null);

            if (mostRecentHour != null) {
                List<ReadingEntity> mostRecentReadings = readingsOfMostRecentDate.stream()
                        .filter(r -> r.getHour().equals(mostRecentHour))
                        .collect(Collectors.toList());

                LimitStatusEnum mostCriticalStatus = findMostCriticalStatus(mostRecentReadings);

                String formattedDateTime = mostRecentDate + " " + mostRecentHour;

                return createInstrumentLimitStatusDTO(instrument, mostCriticalStatus, formattedDateTime);
            }
        }

        ReadingEntity firstReading = recentReadings.get(0);
        return createInstrumentLimitStatusDTO(
                instrument,
                firstReading.getLimitStatus(),
                firstReading.getDate() + " " + firstReading.getHour());
    }

    @Transactional(readOnly = true)
    public PagedReadingResponseDTO<ReadingResponseDTO> findByMultipleInstruments(
            List<Long> instrumentIds,
            LocalDate startDate,
            LocalDate endDate,
            LimitStatusEnum limitStatus,
            Boolean active,
            Pageable pageable) {

        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getInstrumentationPermission().getViewRead()) {
                throw new UnauthorizedException("Usuário não tem permissão para visualizar leituras!");
            }
        }

        if (instrumentIds == null || instrumentIds.isEmpty()) {
            throw new InvalidInputException("É necessário fornecer pelo menos um ID de instrumento!");
        }

        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "date", "hour")
            );
        }

        Boolean activeFilter = active != null ? active : true;

        Page<ReadingEntity> readings = readingRepository.findByMultipleInstrumentsWithFilters(
                instrumentIds, startDate, endDate, limitStatus, activeFilter, pageable);

        Page<ReadingResponseDTO> dtoPage = readings.map(this::mapToResponseDTOOptimized);

        return new PagedReadingResponseDTO<>(
                dtoPage.getContent(),
                dtoPage.getNumber(),
                dtoPage.getSize(),
                dtoPage.getTotalElements(),
                dtoPage.getTotalPages(),
                dtoPage.isLast(),
                dtoPage.isFirst()
        );
    }

    @Transactional(readOnly = true)
    public PagedReadingResponseDTO<ReadingResponseDTO> findGroupedReadingsFlatByMultipleInstruments(
            List<Long> instrumentIds, Pageable pageable) {

        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getInstrumentationPermission().getViewRead()) {
                throw new UnauthorizedException("Usuário não tem permissão para visualizar leituras");
            }
        }

        if (instrumentIds == null || instrumentIds.isEmpty()) {
            throw new InvalidInputException("É necessário fornecer pelo menos um ID de instrumento");
        }

        Page<Object[]> dateHourPage = readingRepository.findDistinctDateHourByMultipleInstrumentIds(instrumentIds, pageable);

        List<ReadingResponseDTO> allReadings = new ArrayList<>();

        for (Object[] dh : dateHourPage.getContent()) {
            LocalDate date = (LocalDate) dh[0];
            LocalTime hour = (LocalTime) dh[1];

            List<ReadingEntity> readings = readingRepository.findByMultipleInstrumentIdsAndDateAndHourAndActiveTrue(
                    instrumentIds, date, hour);

            List<ReadingResponseDTO> dtos = readings.stream()
                    .map(this::mapToResponseDTOOptimized)
                    .collect(Collectors.toList());

            allReadings.addAll(dtos);
        }

        return new PagedReadingResponseDTO<>(
                allReadings,
                dateHourPage.getNumber(),
                dateHourPage.getSize(),
                dateHourPage.getTotalElements(),
                dateHourPage.getTotalPages(),
                dateHourPage.isLast(),
                dateHourPage.isFirst()
        );
    }

    public List<InstrumentLimitStatusDTO> getAllInstrumentLimitStatusesByClientId(Long clientId, int limit) {
        clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado com ID: " + clientId));

        List<InstrumentLimitStatusDTO> results = new ArrayList<>();

        List<InstrumentEntity> activeInstruments = instrumentRepository.findByFiltersOptimized(
                null,
                null,
                null,
                true,
                clientId
        );

        for (InstrumentEntity instrument : activeInstruments) {
            Pageable pageable = PageRequest.of(0, limit);
            List<ReadingEntity> recentReadings = readingRepository.findTopNByInstrumentIdOrderByDateDescHourDesc(
                    instrument.getId(), pageable);

            if (recentReadings.isEmpty()) {
                results.add(createInstrumentLimitStatusDTO(instrument, LimitStatusEnum.NORMAL, null));
            } else {
                LocalDate mostRecentDate = recentReadings.stream()
                        .map(ReadingEntity::getDate)
                        .max(LocalDate::compareTo)
                        .orElse(null);

                if (mostRecentDate != null) {
                    List<ReadingEntity> readingsOfMostRecentDate = recentReadings.stream()
                            .filter(r -> r.getDate().equals(mostRecentDate))
                            .collect(Collectors.toList());

                    LocalTime mostRecentHour = readingsOfMostRecentDate.stream()
                            .map(ReadingEntity::getHour)
                            .max(LocalTime::compareTo)
                            .orElse(null);

                    if (mostRecentHour != null) {
                        List<ReadingEntity> mostRecentReadings = readingsOfMostRecentDate.stream()
                                .filter(r -> r.getHour().equals(mostRecentHour))
                                .collect(Collectors.toList());

                        LimitStatusEnum mostCriticalStatus = findMostCriticalStatus(mostRecentReadings);

                        String formattedDateTime = mostRecentDate + " " + mostRecentHour;

                        results.add(createInstrumentLimitStatusDTO(
                                instrument, mostCriticalStatus, formattedDateTime));
                    }
                }
            }
        }

        return results;
    }

    private LimitStatusEnum findMostCriticalStatus(List<ReadingEntity> readings) {
        if (readings.stream().anyMatch(r -> r.getLimitStatus() == LimitStatusEnum.EMERGENCIA)) {
            return LimitStatusEnum.EMERGENCIA;
        }
        if (readings.stream().anyMatch(r -> r.getLimitStatus() == LimitStatusEnum.ALERTA)) {
            return LimitStatusEnum.ALERTA;
        }
        if (readings.stream().anyMatch(r -> r.getLimitStatus() == LimitStatusEnum.ATENCAO)) {
            return LimitStatusEnum.ATENCAO;
        }
        if (readings.stream().anyMatch(r -> r.getLimitStatus() == LimitStatusEnum.INFERIOR)) {
            return LimitStatusEnum.INFERIOR;
        }
        if (readings.stream().anyMatch(r -> r.getLimitStatus() == LimitStatusEnum.SUPERIOR)) {
            return LimitStatusEnum.SUPERIOR;
        }
        return LimitStatusEnum.NORMAL;
    }

    private InstrumentLimitStatusDTO createInstrumentLimitStatusDTO(
            InstrumentEntity instrument, LimitStatusEnum status, String lastReadingDate) {

        InstrumentLimitStatusDTO dto = new InstrumentLimitStatusDTO();
        dto.setInstrumentId(instrument.getId());
        dto.setInstrumentName(instrument.getName());
        dto.setInstrumentType(instrument.getInstrumentType().getName());// Adicionado
        dto.setDamId(instrument.getDam().getId());
        dto.setDamName(instrument.getDam().getName());
        dto.setClientId(instrument.getDam().getClient().getId());
        dto.setClientName(instrument.getDam().getClient().getName());
        dto.setLimitStatus(status);
        dto.setLastReadingDate(lastReadingDate);

        return dto;
    }

    public PagedReadingResponseDTO<ReadingResponseDTO> findByInstrumentId(Long instrumentId, Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "date", "hour")
            );
        }

        Page<ReadingEntity> readings = readingRepository.findByInstrumentIdOptimized(instrumentId, pageable);
        Page<ReadingResponseDTO> dtoPage = readings.map(this::mapToResponseDTOOptimized);

        return new PagedReadingResponseDTO<>(
                dtoPage.getContent(),
                dtoPage.getNumber(),
                dtoPage.getSize(),
                dtoPage.getTotalElements(),
                dtoPage.getTotalPages(),
                dtoPage.isLast(),
                dtoPage.isFirst()
        );
    }

    public List<ReadingResponseDTO> findByOutputId(Long outputId) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getInstrumentationPermission().getViewRead()) {
                throw new UnauthorizedException("Usuário não autorizado a visualizar leituras!");
            }
        }

        List<ReadingEntity> readings = readingRepository.findByOutputIdOptimized(outputId);
        return readings.stream()
                .map(this::mapToResponseDTOOptimized)
                .collect(Collectors.toList());
    }

    public PagedReadingResponseDTO<ReadingResponseDTO> findByFilters(Long instrumentId, Long outputId, LocalDate startDate, LocalDate endDate,
            LimitStatusEnum limitStatus, Boolean active, Pageable pageable) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getInstrumentationPermission().getViewRead()) {
                throw new UnauthorizedException("Usuário não autorizado a visualizar leituras!");
            }
        }
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "date", "hour")
            );
        }

        Boolean activeFilter = active != null ? active : true;

        Page<ReadingEntity> readings = readingRepository.findByFiltersOptimized(instrumentId, outputId, startDate, endDate, limitStatus, activeFilter, pageable);
        Page<ReadingResponseDTO> dtoPage = readings.map(this::mapToResponseDTOOptimized);

        return new PagedReadingResponseDTO<>(
                dtoPage.getContent(),
                dtoPage.getNumber(),
                dtoPage.getSize(),
                dtoPage.getTotalElements(),
                dtoPage.getTotalPages(),
                dtoPage.isLast(),
                dtoPage.isFirst()
        );
    }

    public ReadingEntity findById(Long id) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getInstrumentationPermission().getViewRead()) {
                throw new UnauthorizedException("Usuário não autorizado a visualizar leituras!");
            }
        }
        ReadingEntity reading = readingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Leitura não encontrada com ID: " + id));

        List<ReadingInputValueEntity> inputValues = readingInputValueRepository.findByReadingId(id);
        reading.setInputValues(new HashSet<>(inputValues));

        return reading;
    }

    @Transactional
    public List<ReadingResponseDTO> create(Long instrumentId, ReadingRequestDTO request) {
        UserEntity currentUser = AuthenticatedUserUtil.getCurrentUser();

        if (!AuthenticatedUserUtil.isAdmin()) {
            if (!currentUser.getInstrumentationPermission().getEditRead()) {
                throw new UnauthorizedException("Usuário não autorizado a criar leituras!");
            }
        }

        InstrumentEntity instrument = instrumentRepository.findWithActiveOutputsById(instrumentId)
                .orElseThrow(() -> new NotFoundException("Instrumento não encontrado com ID: " + instrumentId));

        List<OutputEntity> activeOutputs = instrument.getOutputs().stream()
                .filter(OutputEntity::getActive)
                .collect(Collectors.toList());

        if (activeOutputs.isEmpty()) {
            throw new NotFoundException("O instrumento não possui outputs ativos para calcular leituras");
        }

        validateInputValues(instrument, request.getInputValues());

        // Formatar valores de entrada com a precisão correta de cada input
        Map<String, Double> formattedInputValues = new HashMap<>();

        for (InputEntity input : instrument.getInputs()) {
            Double inputValue = request.getInputValues().get(input.getAcronym());
            if (inputValue != null) {
                Double formattedValue = formatToSpecificPrecision(inputValue, input.getPrecision());
                formattedInputValues.put(input.getAcronym(), formattedValue);
            }
        }

        List<ReadingEntity> createdReadings = new ArrayList<>();

        Map<String, String> inputNames = instrument.getInputs().stream()
                .collect(Collectors.toMap(InputEntity::getAcronym, InputEntity::getName));

        for (OutputEntity output : activeOutputs) {
            // Calcular com os valores formatados
            Double calculatedValue = outputCalculationService.calculateOutput(output, request, formattedInputValues);

            ReadingEntity reading = new ReadingEntity();
            reading.setDate(request.getDate());
            reading.setHour(request.getHour());
            reading.setCalculatedValue(calculatedValue);
            reading.setInstrument(instrument);
            reading.setOutput(output);
            reading.setUser(currentUser);
            reading.setActive(true);
            reading.setComment(request.getComment());

            LimitStatusEnum limitStatus = determineLimitStatus(instrument, calculatedValue, output);
            reading.setLimitStatus(limitStatus);

            ReadingEntity savedReading = readingRepository.save(reading);

            for (Map.Entry<String, Double> entry : formattedInputValues.entrySet()) {
                ReadingInputValueEntity inputValue = new ReadingInputValueEntity();
                inputValue.setReading(savedReading);
                inputValue.setInputAcronym(entry.getKey());
                inputValue.setInputName(inputNames.get(entry.getKey()));
                inputValue.setValue(entry.getValue());

                readingInputValueRepository.save(inputValue);
                savedReading.getInputValues().add(inputValue);
            }

            createdReadings.add(savedReading);
        }

        return createdReadings.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    private void validateInputValues(InstrumentEntity instrument, Map<String, Double> inputValues) {
        if (inputValues == null || inputValues.isEmpty()) {
            throw new InvalidInputException("É necessário fornecer valores para os inputs");
        }

        Set<String> requiredInputs = instrument.getInputs().stream()
                .map(InputEntity::getAcronym)
                .collect(Collectors.toSet());

        for (String inputAcronym : requiredInputs) {
            if (!inputValues.containsKey(inputAcronym)) {
                throw new InvalidInputException("Valor não fornecido para o input '" + inputAcronym + "'");
            }
        }

        for (String providedInput : inputValues.keySet()) {
            if (!requiredInputs.contains(providedInput)) {
                throw new InvalidInputException("Input '" + providedInput + "' não existe neste instrumento");
            }
        }
    }

    private List<ReadingInputValueDTO> getInputValuesForReading(ReadingEntity reading) {
        List<ReadingInputValueEntity> inputValues = readingInputValueRepository.findByReadingId(reading.getId());
        return inputValues.stream()
                .map(this::mapToInputValueDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReadingResponseDTO updateReading(Long id, UpdateReadingRequestDTO request) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getInstrumentationPermission().getEditRead()) {
                throw new UnauthorizedException("Usuário não autorizado a editar leituras!");
            }
        }

        ReadingEntity reading = readingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Leitura não encontrada com ID: " + id));

        if (!reading.getActive()) {
            throw new InvalidInputException("Não é possível editar uma leitura inativa");
        }

        UserEntity originalUser = reading.getUser();

        if (request.getDate() != null) {
            reading.setDate(request.getDate());
        }

        if (request.getHour() != null) {
            reading.setHour(request.getHour());
        }

        UserEntity newUser = null;
        if (request.getUserId() != null) {
            newUser = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + request.getUserId()));
            reading.setUser(newUser);
        }

        ReadingEntity savedReading = readingRepository.save(reading);

        if (newUser != null && !newUser.equals(originalUser)) {
            updateRelatedReadingsUser(savedReading, newUser);
        }

        return mapToResponseDTO(savedReading);
    }

    private void updateRelatedReadingsUser(ReadingEntity baseReading, UserEntity newUser) {

        List<ReadingEntity> relatedReadings = readingRepository.findByInstrumentAndDateAndHourAndUser(
                baseReading.getInstrument().getId(),
                baseReading.getDate(),
                baseReading.getHour(),
                baseReading.getUser().getId()
        );

        for (ReadingEntity relatedReading : relatedReadings) {
            if (!relatedReading.getId().equals(baseReading.getId())) {
                relatedReading.setUser(newUser);
                readingRepository.save(relatedReading);
            }
        }
    }

    private ReadingInputValueDTO mapToInputValueDTO(ReadingInputValueEntity entity) {
        ReadingInputValueDTO dto = new ReadingInputValueDTO();
        dto.setInputAcronym(entity.getInputAcronym());
        dto.setInputName(entity.getInputName());
        dto.setValue(entity.getValue());
        return dto;
    }

    @Transactional
    public BulkToggleActiveResponseDTO bulkToggleActive(Boolean active, List<Long> readingIds) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getInstrumentationPermission().getEditRead()) {
                throw new UnauthorizedException("Usuário não autorizado a alterar status de leituras!");
            }
        }

        List<Long> successfulIds = new ArrayList<>();
        List<BulkToggleActiveResponseDTO.FailedOperation> failedOperations = new ArrayList<>();

        for (Long readingId : readingIds) {
            try {
                ReadingEntity reading = readingRepository.findById(readingId)
                        .orElseThrow(() -> new NotFoundException("Leitura não encontrada com ID: " + readingId));

                reading.setActive(active);
                readingRepository.save(reading);
                successfulIds.add(readingId);

            } catch (Exception e) {
                String errorMessage = e.getMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "Erro interno do servidor";
                }

                failedOperations.add(new BulkToggleActiveResponseDTO.FailedOperation(
                        readingId,
                        errorMessage
                ));
            }
        }

        BulkToggleActiveResponseDTO response = new BulkToggleActiveResponseDTO();
        response.setSuccessfulIds(successfulIds);
        response.setFailedOperations(failedOperations);
        response.setTotalProcessed(readingIds.size());
        response.setSuccessCount(successfulIds.size());
        response.setFailureCount(failedOperations.size());

        return response;
    }

    @Transactional
    public void deactivate(Long id) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getInstrumentationPermission().getEditRead()) {
                throw new UnauthorizedException("Usuário não autorizado a desativar leituras!");
            }
        }
        ReadingEntity reading = readingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Leitura não encontrada com ID: " + id));

        reading.setActive(false);
        readingRepository.save(reading);
    }

    @Transactional
    public ReadingResponseDTO updateComment(Long id, String comment) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getInstrumentationPermission().getEditRead()) {
                throw new UnauthorizedException("Usuário não autorizado a editar leituras!");
            }
        }

        ReadingEntity reading = readingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Leitura não encontrada com ID: " + id));

        if (!reading.getActive()) {
            throw new InvalidInputException("Não é possível editar comentário de uma leitura inativa");
        }

        reading.setComment(comment);
        ReadingEntity savedReading = readingRepository.save(reading);

        return mapToResponseDTO(savedReading);
    }

    @Transactional
    public void delete(Long id) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getInstrumentationPermission().getEditRead()) {
                throw new UnauthorizedException("Usuário não autorizado a excluir leituras!");
            }
        }
        ReadingEntity reading = findById(id);
        readingInputValueRepository.deleteByReadingId(id);
        readingRepository.delete(reading);
        log.info("Leitura excluída: ID {}", id);
    }

    private LimitStatusEnum determineLimitStatus(InstrumentEntity instrument, Double value, OutputEntity output) {

        if (Boolean.TRUE.equals(instrument.getNoLimit())) {
            return LimitStatusEnum.NORMAL;
        }

        StatisticalLimitEntity statisticalLimit = output.getStatisticalLimit();
        if (statisticalLimit != null) {
            if (statisticalLimit.getLowerValue() != null && value < statisticalLimit.getLowerValue()) {
                return LimitStatusEnum.INFERIOR;
            }
            if (statisticalLimit.getUpperValue() != null && value > statisticalLimit.getUpperValue()) {
                return LimitStatusEnum.SUPERIOR;
            }
            return LimitStatusEnum.NORMAL;
        }

        DeterministicLimitEntity deterministicLimit = output.getDeterministicLimit();
        if (deterministicLimit != null) {
            if (deterministicLimit.getEmergencyValue() != null && value >= deterministicLimit.getEmergencyValue()) {
                return LimitStatusEnum.EMERGENCIA;
            }
            if (deterministicLimit.getAlertValue() != null && value >= deterministicLimit.getAlertValue()) {
                return LimitStatusEnum.ALERTA;
            }
            if (deterministicLimit.getAttentionValue() != null && value >= deterministicLimit.getAttentionValue()) {
                return LimitStatusEnum.ATENCAO;
            }
            return LimitStatusEnum.NORMAL;
        }

        return LimitStatusEnum.NORMAL;
    }

    public ReadingResponseDTO mapToResponseDTO(ReadingEntity reading) {
        ReadingResponseDTO dto = new ReadingResponseDTO();
        dto.setId(reading.getId());
        dto.setDate(reading.getDate());
        dto.setHour(reading.getHour());
        dto.setCalculatedValue(reading.getCalculatedValue());
        dto.setLimitStatus(reading.getLimitStatus());
        dto.setInstrumentId(reading.getInstrument().getId());
        dto.setInstrumentName(reading.getInstrument().getName());
        dto.setOutputId(reading.getOutput().getId());
        dto.setOutputName(reading.getOutput().getName());
        dto.setOutputAcronym(reading.getOutput().getAcronym());
        dto.setComment(reading.getComment());

        if (reading.getUser() != null) {
            dto.setCreatedBy(new ReadingResponseDTO.UserInfoDTO(
                    reading.getUser().getId(),
                    reading.getUser().getName(),
                    reading.getUser().getEmail()
            ));
        }

        dto.setInputValues(getInputValuesForReading(reading));

        return dto;
    }

    public PagedReadingResponseDTO<ReadingResponseDTO> findGroupedReadingsFlatByInstrument(Long instrumentId, Pageable pageable) {
        Page<Object[]> dateHourPage = readingRepository.findDistinctDateHourByInstrumentId(instrumentId, pageable);

        List<ReadingResponseDTO> allReadings = new ArrayList<>();
        for (Object[] dh : dateHourPage.getContent()) {
            LocalDate date = (LocalDate) dh[0];
            LocalTime hour = (LocalTime) dh[1];
            List<ReadingEntity> readings = readingRepository.findByInstrumentIdAndDateAndHourAndActiveTrue(instrumentId, date, hour);
            List<ReadingResponseDTO> dtos = readings.stream().map(this::mapToResponseDTO).collect(Collectors.toList());
            allReadings.addAll(dtos);
        }

        return new PagedReadingResponseDTO<>(
                allReadings,
                dateHourPage.getNumber(),
                dateHourPage.getSize(),
                dateHourPage.getTotalElements(),
                dateHourPage.getTotalPages(),
                dateHourPage.isLast(),
                dateHourPage.isFirst()
        );
    }

    private ReadingResponseDTO mapToResponseDTOOptimized(ReadingEntity reading) {
        ReadingResponseDTO dto = new ReadingResponseDTO();
        dto.setId(reading.getId());
        dto.setDate(reading.getDate());
        dto.setHour(reading.getHour());
        dto.setCalculatedValue(reading.getCalculatedValue());
        dto.setLimitStatus(reading.getLimitStatus());
        dto.setInstrumentId(reading.getInstrument().getId());
        dto.setInstrumentName(reading.getInstrument().getName());
        dto.setOutputId(reading.getOutput().getId());
        dto.setOutputName(reading.getOutput().getName());
        dto.setOutputAcronym(reading.getOutput().getAcronym());
        dto.setComment(reading.getComment());

        if (reading.getUser() != null) {
            dto.setCreatedBy(new ReadingResponseDTO.UserInfoDTO(
                    reading.getUser().getId(),
                    reading.getUser().getName(),
                    reading.getUser().getEmail()
            ));
        }

        List<ReadingInputValueDTO> inputValueDTOs = reading.getInputValues().stream()
                .map(this::mapToInputValueDTO)
                .collect(Collectors.toList());
        dto.setInputValues(inputValueDTOs);

        return dto;
    }

    private Double formatToSpecificPrecision(Double value, Integer precision) {
        if (value == null || precision == null) {
            return value;
        }

        // Usando BigDecimal para evitar problemas de arredondamento
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(precision, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
