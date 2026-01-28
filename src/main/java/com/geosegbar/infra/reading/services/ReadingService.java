package com.geosegbar.infra.reading.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.enums.LimitStatusEnum;
import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.common.utils.DateFormatter;
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
import com.geosegbar.infra.reading.dtos.InstrumentGroupedReadingsDTO;
import com.geosegbar.infra.reading.dtos.InstrumentGroupedReadingsDTO.GroupedDateHourReadingsDTO;
import com.geosegbar.infra.reading.dtos.InstrumentLimitStatusDTO;
import com.geosegbar.infra.reading.dtos.InstrumentReadingsDTO;
import com.geosegbar.infra.reading.dtos.InstrumentReadingsDTO.MultiInstrumentReadingsResponseDTO;
import com.geosegbar.infra.reading.dtos.PagedReadingResponseDTO;
import com.geosegbar.infra.reading.dtos.ReadingRequestDTO;
import com.geosegbar.infra.reading.dtos.ReadingResponseDTO;
import com.geosegbar.infra.reading.dtos.ReadingResponseDTO.UserInfoDTO;
import com.geosegbar.infra.reading.dtos.UpdateReadingRequestDTO;
import com.geosegbar.infra.reading.persistence.jpa.ReadingRepository;
import com.geosegbar.infra.reading.projections.InstrumentLimitStatusProjection;
import com.geosegbar.infra.reading_input_value.dtos.ReadingInputValueDTO;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReadingService {

    private final ReadingRepository readingRepository;
    private final InstrumentRepository instrumentRepository;
    private final OutputCalculationService outputCalculationService;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    private static final Map<LimitStatusEnum, Integer> STATUS_PRIORITY;

    static {
        STATUS_PRIORITY = new EnumMap<>(LimitStatusEnum.class);
        STATUS_PRIORITY.put(LimitStatusEnum.EMERGENCIA, 5);
        STATUS_PRIORITY.put(LimitStatusEnum.ALERTA, 4);
        STATUS_PRIORITY.put(LimitStatusEnum.ATENCAO, 3);
        STATUS_PRIORITY.put(LimitStatusEnum.INFERIOR, 2);
        STATUS_PRIORITY.put(LimitStatusEnum.SUPERIOR, 1);
        STATUS_PRIORITY.put(LimitStatusEnum.NORMAL, 0);
    }

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "date", "hour");

    @Transactional(readOnly = true)
    public List<ReadingResponseDTO> findByInstrumentId(Long instrumentId) {
        validateViewPermission();

        return readingRepository.findByInstrumentIdWithAllRelations(instrumentId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean existsByInstrumentAndDate(Long instrumentId, LocalDate date) {
        return readingRepository.existsByInstrumentIdAndDate(instrumentId, date);
    }

    @Transactional(readOnly = true)
    public InstrumentLimitStatusDTO getInstrumentLimitStatus(Long instrumentId, int limit) {
        InstrumentEntity instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new NotFoundException("Instrumento não encontrado com ID: " + instrumentId));

        List<ReadingEntity> recentReadings = readingRepository.findTopNByInstrumentIdOptimized(
                instrumentId, PageRequest.of(0, limit));

        if (recentReadings.isEmpty()) {
            return buildInstrumentLimitStatusDTO(instrument, LimitStatusEnum.NORMAL, null);
        }

        ReadingEntity latest = recentReadings.stream()
                .max(Comparator.comparing(ReadingEntity::getDate)
                        .thenComparing(ReadingEntity::getHour))
                .orElse(recentReadings.get(0));

        LocalDate latestDate = latest.getDate();
        LocalTime latestHour = latest.getHour();

        LimitStatusEnum mostCritical = recentReadings.stream()
                .filter(r -> r.getDate().equals(latestDate) && r.getHour().equals(latestHour))
                .map(ReadingEntity::getLimitStatus)
                .max(Comparator.comparingInt(s -> STATUS_PRIORITY.getOrDefault(s, 0)))
                .orElse(LimitStatusEnum.NORMAL);

        return buildInstrumentLimitStatusDTO(instrument, mostCritical, latestDate + " " + latestHour);
    }

    /**
     * ⭐ SUPER OTIMIZADO: Query única com Window Function
     */
    @Transactional(readOnly = true)
    public List<InstrumentLimitStatusDTO> getAllInstrumentLimitStatusesByClientId(Long clientId, int limit) {
        clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado com ID: " + clientId));

        List<InstrumentLimitStatusProjection> projections
                = readingRepository.findLatestLimitStatusByClientId(clientId, limit);

        List<InstrumentEntity> allActiveInstruments
                = instrumentRepository.findByFiltersOptimized(null, null, null, true, clientId);

        if (projections.isEmpty()) {
            return allActiveInstruments.stream()
                    .map(i -> buildInstrumentLimitStatusDTO(i, LimitStatusEnum.NORMAL, null))
                    .collect(Collectors.toList());
        }

        Map<Long, List<InstrumentLimitStatusProjection>> byInstrument = projections.stream()
                .collect(Collectors.groupingBy(InstrumentLimitStatusProjection::getInstrumentId));

        List<InstrumentLimitStatusDTO> results = new ArrayList<>(allActiveInstruments.size());

        for (Map.Entry<Long, List<InstrumentLimitStatusProjection>> entry : byInstrument.entrySet()) {
            List<InstrumentLimitStatusProjection> instrumentReadings = entry.getValue();
            InstrumentLimitStatusProjection first = instrumentReadings.get(0);

            LocalDate latestDate = first.getReadingDate();
            LocalTime latestHour = first.getReadingHour();

            LimitStatusEnum mostCritical = instrumentReadings.stream()
                    .filter(p -> p.getReadingDate().equals(latestDate) && p.getReadingHour().equals(latestHour))
                    .map(p -> parseLimitStatus(p.getLimitStatus()))
                    .max(Comparator.comparingInt(s -> STATUS_PRIORITY.getOrDefault(s, 0)))
                    .orElse(LimitStatusEnum.NORMAL);

            results.add(buildInstrumentLimitStatusDTOFromProjection(first, mostCritical, latestDate + " " + latestHour));
        }

        Set<Long> instrumentsWithReadings = byInstrument.keySet();
        for (InstrumentEntity instrument : allActiveInstruments) {
            if (!instrumentsWithReadings.contains(instrument.getId())) {
                results.add(buildInstrumentLimitStatusDTO(instrument, LimitStatusEnum.NORMAL, null));
            }
        }

        return results;
    }

    private LimitStatusEnum parseLimitStatus(String status) {
        if (status == null) {
            return LimitStatusEnum.NORMAL;
        }
        try {
            return LimitStatusEnum.valueOf(status);
        } catch (IllegalArgumentException e) {
            log.warn("LimitStatus inválido: {}", status);
            return LimitStatusEnum.NORMAL;
        }
    }

    @Transactional(readOnly = true)
    public List<ReadingResponseDTO> findByOutputId(Long outputId) {
        validateViewPermission();

        return readingRepository.findByOutputIdWithAllRelations(outputId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReadingEntity findById(Long id) {
        validateViewPermission();

        return readingRepository.findByIdWithAllRelations(id)
                .orElseThrow(() -> new NotFoundException("Leitura não encontrada com ID: " + id));
    }

    @Transactional(readOnly = true)
    public PagedReadingResponseDTO<ReadingResponseDTO> findGroupedReadingsFlatByInstrument(
            Long instrumentId, Boolean active, Pageable pageable) {

        validateViewPermission();

        Page<Object[]> dateHourPage = readingRepository.findDistinctDateHourByInstrumentIdAndActive(
                instrumentId, active, pageable);

        if (dateHourPage.isEmpty()) {
            return createEmptyPagedResponse(dateHourPage);
        }

        int size = dateHourPage.getContent().size();
        List<LocalDate> dates = new ArrayList<>(size);
        List<LocalTime> hours = new ArrayList<>(size);

        for (Object[] dh : dateHourPage.getContent()) {
            dates.add((LocalDate) dh[0]);
            hours.add((LocalTime) dh[1]);
        }

        List<ReadingResponseDTO> dtos = readingRepository
                .findByInstrumentIdAndDateHoursWithAllRelations(instrumentId, dates, hours, active)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());

        return createPagedResponse(dtos, dateHourPage);
    }

    /**
     * ⭐ SUPER OTIMIZADO: Mínimas queries possíveis
     */
    @Transactional(readOnly = true)
    public List<InstrumentGroupedReadingsDTO> findLatestGroupedReadingsByClientId(Long clientId, int limit) {
        List<Object[]> latestDateHours = readingRepository.findLatestDistinctDateHoursByClientId(clientId, limit);

        if (latestDateHours.isEmpty()) {
            return List.of();
        }

        Set<Long> instrumentIds = new HashSet<>();
        Map<Long, List<DateTimePair>> instrumentDateHoursMap = new HashMap<>();

        for (Object[] row : latestDateHours) {
            Long instrumentId = ((Number) row[0]).longValue();
            instrumentIds.add(instrumentId);

            LocalDate date = ((java.sql.Date) row[1]).toLocalDate();
            LocalTime hour = ((java.sql.Time) row[2]).toLocalTime();

            instrumentDateHoursMap
                    .computeIfAbsent(instrumentId, k -> new ArrayList<>())
                    .add(new DateTimePair(date, hour));
        }

        Map<Long, InstrumentEntity> instrumentsMap = instrumentRepository.findAllById(instrumentIds)
                .stream()
                .collect(Collectors.toMap(InstrumentEntity::getId, Function.identity()));

        List<ReadingEntity> allReadings = readingRepository
                .findByInstrumentIdsAndActiveTrueWithAllRelations(new ArrayList<>(instrumentIds));

        Map<Long, Map<String, List<ReadingEntity>>> readingsByInstrumentAndDateTime = new HashMap<>();
        for (ReadingEntity reading : allReadings) {
            Long instId = reading.getInstrument().getId();
            String dateTimeKey = reading.getDate() + " " + reading.getHour();

            readingsByInstrumentAndDateTime
                    .computeIfAbsent(instId, k -> new HashMap<>())
                    .computeIfAbsent(dateTimeKey, k -> new ArrayList<>())
                    .add(reading);
        }

        List<InstrumentGroupedReadingsDTO> result = new ArrayList<>(instrumentIds.size());

        for (Map.Entry<Long, List<DateTimePair>> entry : instrumentDateHoursMap.entrySet()) {
            Long instrumentId = entry.getKey();
            InstrumentEntity instrument = instrumentsMap.get(instrumentId);

            if (instrument == null) {
                continue;
            }

            InstrumentGroupedReadingsDTO instrumentDTO = new InstrumentGroupedReadingsDTO();
            instrumentDTO.setInstrumentId(instrument.getId());
            instrumentDTO.setInstrumentName(instrument.getName());
            instrumentDTO.setInstrumentType(instrument.getInstrumentType().getName());
            instrumentDTO.setDamId(instrument.getDam().getId());
            instrumentDTO.setDamName(instrument.getDam().getName());

            Map<String, List<ReadingEntity>> readingsByDateTime
                    = readingsByInstrumentAndDateTime.getOrDefault(instrumentId, Map.of());

            List<GroupedDateHourReadingsDTO> groupedReadings = new ArrayList<>();

            for (DateTimePair dtp : entry.getValue()) {
                String dateHourKey = dtp.date + " " + dtp.hour;
                List<ReadingEntity> readings = readingsByDateTime.getOrDefault(dateHourKey, List.of());

                if (!readings.isEmpty()) {
                    GroupedDateHourReadingsDTO group = new GroupedDateHourReadingsDTO();
                    group.setDateTime(dateHourKey);
                    group.setReadings(readings.stream()
                            .map(this::mapToResponseDTO)
                            .collect(Collectors.toList()));
                    groupedReadings.add(group);
                }
            }

            instrumentDTO.setGroupedReadings(groupedReadings);
            result.add(instrumentDTO);
        }

        return result;
    }

    @Transactional
    public List<ReadingResponseDTO> create(Long instrumentId, ReadingRequestDTO request, boolean skipPermissionCheck) {
        LocalTime truncatedHour = request.getHour().withNano(0);
        LocalDateTime readingDateTime = LocalDateTime.of(request.getDate(), truncatedHour);
        LocalDateTime now = LocalDateTime.now();

        if (readingDateTime.truncatedTo(ChronoUnit.MINUTES).isAfter(now.truncatedTo(ChronoUnit.MINUTES))) {
            throw new InvalidInputException("Não é possível criar leituras com data e hora futura. "
                    + "Data/hora informada: " + DateFormatter.formatDateTime(readingDateTime)
                    + ", Data/hora atual: " + DateFormatter.formatDateTime(now));
        }

        if (readingRepository.existsByInstrumentIdAndDateAndHourAndActive(
                instrumentId, request.getDate(), truncatedHour, true)) {
            throw new InvalidInputException("Já existe leitura registrada para este instrumento na mesma data e hora ("
                    + request.getDate() + " " + truncatedHour + ")");
        }

        request.setHour(truncatedHour);

        UserEntity currentUser = resolveCurrentUser(skipPermissionCheck);

        InstrumentEntity instrument = instrumentRepository.findWithActiveOutputsById(instrumentId)
                .orElseThrow(() -> new NotFoundException("Instrumento não encontrado com ID: " + instrumentId));

        List<OutputEntity> activeOutputs = instrument.getOutputs().stream()
                .filter(OutputEntity::getActive)
                .collect(Collectors.toList());

        if (activeOutputs.isEmpty()) {
            throw new NotFoundException("O instrumento não possui outputs ativos para calcular leituras");
        }

        validateInputValues(instrument, request.getInputValues());

        Map<String, Double> formattedInputValues = new HashMap<>();
        Map<String, String> inputNames = new HashMap<>();

        for (InputEntity input : instrument.getInputs()) {
            Double inputValue = request.getInputValues().get(input.getAcronym());
            if (inputValue != null) {
                formattedInputValues.put(input.getAcronym(),
                        formatToSpecificPrecision(inputValue, input.getPrecision()));
                inputNames.put(input.getAcronym(), input.getName());
            }
        }

        List<ReadingEntity> readingsToSave = new ArrayList<>(activeOutputs.size());

        for (OutputEntity output : activeOutputs) {
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
            reading.setLimitStatus(determineLimitStatus(instrument, calculatedValue, output));

            Set<ReadingInputValueEntity> inputValuesSet = new HashSet<>(formattedInputValues.size());
            for (Map.Entry<String, Double> entry : formattedInputValues.entrySet()) {
                ReadingInputValueEntity inputValue = new ReadingInputValueEntity();
                inputValue.setInputAcronym(entry.getKey());
                inputValue.setInputName(inputNames.get(entry.getKey()));
                inputValue.setValue(entry.getValue());
                inputValue.setReading(reading);
                inputValuesSet.add(inputValue);
            }
            reading.setInputValues(inputValuesSet);

            readingsToSave.add(reading);
        }

        List<ReadingEntity> createdReadings = readingRepository.saveAll(readingsToSave);

        return createdReadings.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReadingResponseDTO updateReading(Long id, UpdateReadingRequestDTO request) {
        validateEditPermission();

        ReadingEntity reading = readingRepository.findByIdWithAllRelations(id)
                .orElseThrow(() -> new NotFoundException("Leitura não encontrada com ID: " + id));

        if (!reading.getActive()) {
            throw new InvalidInputException("Não é possível editar uma leitura inativa");
        }

        InstrumentEntity instrument = reading.getInstrument();
        Long instrumentId = instrument.getId();
        LocalDate originalDate = reading.getDate();
        LocalTime originalHour = reading.getHour();

        boolean isUpdatingInputValues = request.getInputValues() != null && !request.getInputValues().isEmpty();

        if (isUpdatingInputValues) {
            LocalDateTime readingDateTime = LocalDateTime.of(originalDate, originalHour);
            if (instrument.getLastUpdateVariablesDate() != null
                    && instrument.getLastUpdateVariablesDate().isAfter(readingDateTime)) {
                throw new InvalidInputException(
                        "Não é possível editar os valores desta leitura pois as variáveis do instrumento foram alteradas após o registro.");
            }
        }

        LocalDate newDate = request.getDate() != null ? request.getDate() : originalDate;
        LocalTime newHour = request.getHour() != null ? request.getHour().withNano(0) : originalHour;

        boolean isDateTimeChanged = !Objects.equals(newDate, originalDate) || !Objects.equals(newHour, originalHour);

        if (isDateTimeChanged) {
            validateDateTimeChange(newDate, newHour, instrumentId, id);
        }

        UserEntity newUser = request.getUserId() != null
                ? userRepository.findById(request.getUserId())
                        .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + request.getUserId()))
                : null;

        if (isUpdatingInputValues) {
            updateInputValuesForGroup(instrumentId, originalDate, originalHour, request.getInputValues(), instrument);
        }

        List<ReadingEntity> groupReadings = readingRepository.findAllReadingsInGroupWithRelations(
                instrumentId, originalDate, originalHour);

        boolean hasChanges = updateGroupReadings(groupReadings, request, newDate, newHour, newUser, isDateTimeChanged);

        if (hasChanges) {
            readingRepository.saveAll(groupReadings);
        }

        return mapToResponseDTO(readingRepository.findByIdWithAllRelations(id)
                .orElseThrow(() -> new NotFoundException("Leitura não encontrada após atualização")));
    }

    @Transactional
    public BulkToggleActiveResponseDTO bulkToggleActive(Boolean active, List<Long> readingIds) {
        validateEditPermission();

        List<Long> successfulIds = new ArrayList<>(readingIds.size());
        List<BulkToggleActiveResponseDTO.FailedOperation> failedOperations = new ArrayList<>();

        List<ReadingEntity> readings = readingRepository.findAllByIdWithMinimalData(readingIds);
        Set<Long> foundIds = readings.stream().map(ReadingEntity::getId).collect(Collectors.toSet());

        for (Long requestedId : readingIds) {
            if (!foundIds.contains(requestedId)) {
                failedOperations.add(new BulkToggleActiveResponseDTO.FailedOperation(
                        requestedId, "Leitura não encontrada com ID: " + requestedId));
            }
        }

        for (ReadingEntity reading : readings) {
            reading.setActive(active);
            successfulIds.add(reading.getId());
        }

        if (!readings.isEmpty()) {
            readingRepository.saveAll(readings);
            log.info("Bulk toggle: {} readings atualizadas", successfulIds.size());
        }

        return buildBulkToggleResponse(readingIds.size(), successfulIds, failedOperations);
    }

    @Transactional
    public void delete(Long id) {
        validateEditPermission();

        ReadingEntity reading = readingRepository.findByIdWithAllRelations(id)
                .orElseThrow(() -> new NotFoundException("Leitura não encontrada com ID: " + id));

        readingRepository.delete(reading);
        log.info("Leitura excluída: ID {}", id);
    }

    @Transactional(readOnly = true)
    public PagedReadingResponseDTO<ReadingResponseDTO> findByInstrumentId(Long instrumentId, Pageable pageable) {
        pageable = ensureDefaultSort(pageable);

        Page<ReadingEntity> readings = readingRepository.findByInstrumentIdWithAllRelations(instrumentId, pageable);
        return createPagedResponse(readings.map(this::mapToResponseDTO));
    }

    @Transactional(readOnly = true)
    public PagedReadingResponseDTO<ReadingResponseDTO> findByMultipleInstruments(
            List<Long> instrumentIds, LocalDate startDate, LocalDate endDate,
            LimitStatusEnum limitStatus, Boolean active, Pageable pageable) {

        validateViewPermission();

        if (instrumentIds == null || instrumentIds.isEmpty()) {
            throw new InvalidInputException("É necessário fornecer pelo menos um ID de instrumento!");
        }

        pageable = ensureDefaultSort(pageable);
        Boolean activeFilter = active != null ? active : true;

        Page<ReadingEntity> readings = readingRepository.findByMultipleInstrumentsWithAllRelations(
                instrumentIds, startDate, endDate, limitStatus, activeFilter, pageable);

        return createPagedResponse(readings.map(this::mapToResponseDTO));
    }

    @Transactional(readOnly = true)
    public PagedReadingResponseDTO<ReadingResponseDTO> findGroupedReadingsFlatByMultipleInstruments(
            List<Long> instrumentIds, Pageable pageable) {

        validateViewPermission();

        if (instrumentIds == null || instrumentIds.isEmpty()) {
            throw new InvalidInputException("É necessário fornecer pelo menos um ID de instrumento");
        }

        Page<Object[]> dateHourPage = readingRepository.findDistinctDateHourByMultipleInstrumentIds(instrumentIds, pageable);

        if (dateHourPage.isEmpty()) {
            return createEmptyPagedResponse(dateHourPage);
        }

        List<LocalDate> dates = new ArrayList<>(dateHourPage.getContent().size());
        List<LocalTime> hours = new ArrayList<>(dateHourPage.getContent().size());

        for (Object[] dh : dateHourPage.getContent()) {
            dates.add((LocalDate) dh[0]);
            hours.add((LocalTime) dh[1]);
        }

        List<ReadingResponseDTO> dtos = readingRepository
                .findByMultipleInstrumentIdsAndDateHoursWithAllRelations(instrumentIds, dates, hours)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());

        return createPagedResponse(dtos, dateHourPage);
    }

    @Transactional(readOnly = true)
    public PagedReadingResponseDTO<ReadingResponseDTO> findByFilters(
            Long instrumentId, Long outputId, LocalDate startDate, LocalDate endDate,
            LimitStatusEnum limitStatus, Boolean active, Pageable pageable) {

        validateViewPermission();
        pageable = ensureDefaultSort(pageable);
        Boolean activeFilter = active != null ? active : true;

        Page<ReadingEntity> readings = readingRepository.findByFiltersWithAllRelations(
                instrumentId, outputId, startDate, endDate, limitStatus, activeFilter, pageable);

        return createPagedResponse(readings.map(this::mapToResponseDTO));
    }

    @Transactional(readOnly = true)
    public MultiInstrumentReadingsResponseDTO findLatestReadingsForMultipleInstruments(
            List<Long> instrumentIds, List<Long> outputIds,
            LocalDate startDate, LocalDate endDate, int pageSize) {

        Set<Long> uniqueInstrumentIds = new HashSet<>();

        if (instrumentIds != null && !instrumentIds.isEmpty()) {
            uniqueInstrumentIds.addAll(instrumentIds);
        }

        if (outputIds != null && !outputIds.isEmpty()) {
            uniqueInstrumentIds.addAll(readingRepository.findInstrumentIdsByOutputIds(outputIds));
        }

        if (uniqueInstrumentIds.isEmpty()) {
            return new MultiInstrumentReadingsResponseDTO(List.of(), pageSize, 0);
        }

        Map<Long, InstrumentEntity> instrumentsMap = instrumentRepository.findAllById(uniqueInstrumentIds)
                .stream()
                .collect(Collectors.toMap(InstrumentEntity::getId, Function.identity()));

        List<ReadingEntity> allReadings = readingRepository.findLatestReadingsByInstrumentIdsWithAllRelations(
                new ArrayList<>(uniqueInstrumentIds), startDate, endDate, pageSize);

        Map<Long, List<ReadingEntity>> readingsByInstrument = allReadings.stream()
                .collect(Collectors.groupingBy(r -> r.getInstrument().getId()));

        List<InstrumentReadingsDTO> result = new ArrayList<>(uniqueInstrumentIds.size());

        for (Long instId : uniqueInstrumentIds) {
            InstrumentEntity instrument = instrumentsMap.get(instId);
            if (instrument == null) {
                continue;
            }

            List<ReadingResponseDTO> readingDTOs = readingsByInstrument
                    .getOrDefault(instId, List.of())
                    .stream()
                    .map(this::mapToResponseDTO)
                    .collect(Collectors.toList());

            result.add(new InstrumentReadingsDTO(
                    instrument.getId(),
                    instrument.getName(),
                    instrument.getInstrumentType().getName(),
                    readingDTOs));
        }

        return new MultiInstrumentReadingsResponseDTO(result, pageSize, uniqueInstrumentIds.size());
    }

    private void validateViewPermission() {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity user = AuthenticatedUserUtil.getCurrentUser();
            if (!user.getInstrumentationPermission().getViewRead()) {
                throw new UnauthorizedException("Usuário não tem permissão para visualizar leituras!");
            }
        }
    }

    private void validateEditPermission() {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity user = AuthenticatedUserUtil.getCurrentUser();
            if (!user.getInstrumentationPermission().getEditRead()) {
                throw new UnauthorizedException("Usuário não autorizado a modificar leituras!");
            }
        }
    }

    private UserEntity resolveCurrentUser(boolean skipPermissionCheck) {
        if (skipPermissionCheck) {
            return userRepository.findByEmail("noreply@geometrisa-prod.com.br")
                    .orElseThrow(() -> new NotFoundException("Usuário do sistema não encontrado!"));
        }

        UserEntity currentUser = AuthenticatedUserUtil.getCurrentUser();
        if (!AuthenticatedUserUtil.isAdmin() && !currentUser.getInstrumentationPermission().getEditRead()) {
            throw new UnauthorizedException("Usuário não autorizado a criar leituras!");
        }
        return currentUser;
    }

    private void validateDateTimeChange(LocalDate newDate, LocalTime newHour, Long instrumentId, Long excludeId) {
        LocalDateTime newDateTime = LocalDateTime.of(newDate, newHour);
        if (newDateTime.truncatedTo(ChronoUnit.MINUTES).isAfter(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))) {
            throw new InvalidInputException("Não é possível atualizar leituras para data e hora futura.");
        }

        if (readingRepository.existsByInstrumentIdAndDateAndHourExcludingId(instrumentId, newDate, newHour, excludeId)) {
            throw new InvalidInputException("Já existe leitura registrada para este instrumento na mesma data e hora ("
                    + newDate + " " + newHour + ")");
        }
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

    private void updateInputValuesForGroup(Long instrumentId, LocalDate date, LocalTime hour,
            Map<String, Double> newInputValues, InstrumentEntity instrument) {

        List<ReadingEntity> groupReadings = readingRepository.findAllReadingsInGroupWithRelations(
                instrumentId, date, hour);

        if (groupReadings.isEmpty()) {
            throw new NotFoundException("Não foram encontradas leituras para este grupo");
        }

        Map<String, InputEntity> instrumentInputs = instrument.getInputs().stream()
                .collect(Collectors.toMap(InputEntity::getAcronym, Function.identity()));

        for (String inputAcronym : newInputValues.keySet()) {
            if (!instrumentInputs.containsKey(inputAcronym)) {
                throw new InvalidInputException("Input com acrônimo '" + inputAcronym + "' não encontrado no instrumento");
            }
        }

        Map<String, Double> calculationInputs = new HashMap<>();
        for (ReadingEntity reading : groupReadings) {
            for (ReadingInputValueEntity riv : reading.getInputValues()) {
                calculationInputs.putIfAbsent(riv.getInputAcronym(), riv.getValue());
            }
        }

        boolean inputsChanged = false;
        for (Map.Entry<String, Double> entry : newInputValues.entrySet()) {
            if (!Objects.equals(calculationInputs.get(entry.getKey()), entry.getValue())) {
                inputsChanged = true;
                calculationInputs.put(entry.getKey(), entry.getValue());
            }
        }

        if (inputsChanged) {
            for (ReadingEntity reading : groupReadings) {

                for (ReadingInputValueEntity riv : reading.getInputValues()) {
                    Double newValue = newInputValues.get(riv.getInputAcronym());
                    if (newValue != null) {
                        riv.setValue(newValue);
                    }
                }

                OutputEntity output = reading.getOutput();
                Double newCalculatedValue = outputCalculationService.calculateOutput(output, null, calculationInputs);
                reading.setCalculatedValue(newCalculatedValue);
                reading.setLimitStatus(determineLimitStatus(instrument, newCalculatedValue, output));
            }

            readingRepository.saveAll(groupReadings);
        }
    }

    private boolean updateGroupReadings(List<ReadingEntity> groupReadings, UpdateReadingRequestDTO request,
            LocalDate newDate, LocalTime newHour, UserEntity newUser, boolean isDateTimeChanged) {

        boolean hasChanges = false;
        boolean isUpdatingComment = request.getComment() != null;

        for (ReadingEntity groupReading : groupReadings) {
            if (isDateTimeChanged) {
                groupReading.setDate(newDate);
                groupReading.setHour(newHour);
                hasChanges = true;
            }
            if (newUser != null) {
                groupReading.setUser(newUser);
                hasChanges = true;
            }
            if (isUpdatingComment) {
                groupReading.setComment(request.getComment());
                hasChanges = true;
            }
        }

        return hasChanges;
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
        dto.setActive(reading.getActive());

        if (reading.getUser() != null) {
            dto.setCreatedBy(new UserInfoDTO(
                    reading.getUser().getId(),
                    reading.getUser().getName(),
                    reading.getUser().getEmail()));
        }

        dto.setInputValues(reading.getInputValues().stream()
                .map(this::mapToInputValueDTO)
                .collect(Collectors.toList()));

        return dto;
    }

    private ReadingInputValueDTO mapToInputValueDTO(ReadingInputValueEntity entity) {
        ReadingInputValueDTO dto = new ReadingInputValueDTO();
        dto.setInputAcronym(entity.getInputAcronym());
        dto.setInputName(entity.getInputName());
        dto.setValue(entity.getValue());
        return dto;
    }

    private InstrumentLimitStatusDTO buildInstrumentLimitStatusDTO(
            InstrumentEntity instrument, LimitStatusEnum status, String lastReadingDate) {

        InstrumentLimitStatusDTO dto = new InstrumentLimitStatusDTO();
        dto.setInstrumentId(instrument.getId());
        dto.setInstrumentName(instrument.getName());
        dto.setInstrumentType(instrument.getInstrumentType().getName());
        dto.setInstrumentTypeId(instrument.getInstrumentType().getId());
        dto.setDamId(instrument.getDam().getId());
        dto.setDamName(instrument.getDam().getName());
        dto.setClientId(instrument.getDam().getClient().getId());
        dto.setClientName(instrument.getDam().getClient().getName());
        dto.setLimitStatus(status);
        dto.setLastReadingDate(lastReadingDate);
        return dto;
    }

    private InstrumentLimitStatusDTO buildInstrumentLimitStatusDTOFromProjection(
            InstrumentLimitStatusProjection projection, LimitStatusEnum status, String lastReadingDate) {

        InstrumentLimitStatusDTO dto = new InstrumentLimitStatusDTO();
        dto.setInstrumentId(projection.getInstrumentId());
        dto.setInstrumentName(projection.getInstrumentName());
        dto.setInstrumentType(projection.getInstrumentTypeName());
        dto.setInstrumentTypeId(projection.getInstrumentTypeId());
        dto.setDamId(projection.getDamId());
        dto.setDamName(projection.getDamName());
        dto.setClientId(projection.getClientId());
        dto.setClientName(projection.getClientName());
        dto.setLimitStatus(status);
        dto.setLastReadingDate(lastReadingDate);
        return dto;
    }

    private Pageable ensureDefaultSort(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_SORT);
        }
        return pageable;
    }

    private <T> PagedReadingResponseDTO<T> createPagedResponse(Page<T> page) {
        return new PagedReadingResponseDTO<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                page.isFirst());
    }

    private <T> PagedReadingResponseDTO<T> createPagedResponse(List<T> content, Page<?> page) {
        return new PagedReadingResponseDTO<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                page.isFirst());
    }

    private <T> PagedReadingResponseDTO<T> createEmptyPagedResponse(Page<?> page) {
        return new PagedReadingResponseDTO<>(
                List.of(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                page.isFirst());
    }

    private BulkToggleActiveResponseDTO buildBulkToggleResponse(
            int totalProcessed, List<Long> successfulIds,
            List<BulkToggleActiveResponseDTO.FailedOperation> failedOperations) {

        BulkToggleActiveResponseDTO response = new BulkToggleActiveResponseDTO();
        response.setSuccessfulIds(successfulIds);
        response.setFailedOperations(failedOperations);
        response.setTotalProcessed(totalProcessed);
        response.setSuccessCount(successfulIds.size());
        response.setFailureCount(failedOperations.size());
        return response;
    }

    private Double formatToSpecificPrecision(Double value, Integer precision) {
        if (value == null || precision == null) {
            return value;
        }
        return BigDecimal.valueOf(value)
                .setScale(precision, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private record DateTimePair(LocalDate date, LocalTime hour) {

    }
}
