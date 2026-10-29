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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.enums.LimitStatusEnum;
import com.geosegbar.common.metrics.BusinessMetrics;
import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.DeterministicLimitEntity;
import com.geosegbar.entities.InputEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.OutputEntity;
import com.geosegbar.entities.ReadingEntity;
import com.geosegbar.entities.ReadingInputValueEntity;
import com.geosegbar.entities.StatisticalLimitEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.ForbiddenException;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.dam.services.DamAccessService;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
import com.geosegbar.infra.reading.dtos.BulkToggleActiveResponseDTO;
import com.geosegbar.infra.reading.dtos.InstrumentGroupedReadingsDTO;
import com.geosegbar.infra.reading.dtos.InstrumentGroupedReadingsDTO.GroupedDateHourReadingsDTO;
import com.geosegbar.infra.reading.dtos.InstrumentLimitStatusDTO;
import com.geosegbar.infra.reading.dtos.InstrumentReadingsDTO;
import com.geosegbar.infra.reading.dtos.InstrumentReadingsDTO.MultiInstrumentReadingsResponseDTO;
import com.geosegbar.infra.reading.dtos.PagedReadingResponseDTO;
import com.geosegbar.infra.reading.dtos.ReadingGroupDTO;
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
    private final DamAccessService damAccessService;
    private final UserRepository userRepository;
    private final BusinessMetrics businessMetrics;

    @Value("${application.system-user-email}")
    private String systemUserEmail;

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

    @Transactional(readOnly = true)
    public List<InstrumentLimitStatusDTO> getAllInstrumentLimitStatusesByClientId(Long clientId, int limit) {
        if (!clientRepository.existsById(clientId)) {
            throw new NotFoundException("Cliente não encontrado com ID: " + clientId);
        }

        // Só as barragens que o usuário acessa; ADMIN vê o cliente inteiro.
        Optional<Set<Long>> restriction = damAccessService.listingRestriction();
        if (restriction.isPresent() && restriction.get().isEmpty()) {
            return List.of();
        }

        List<InstrumentLimitStatusProjection> projections = readingRepository.findLatestLimitStatusByClientId(clientId, limit);
        List<InstrumentEntity> allActiveInstruments = instrumentRepository.findByFiltersOptimized(null, null, null, true, clientId);

        if (restriction.isPresent()) {
            Set<Long> allowed = restriction.get();
            projections = projections.stream()
                    .filter(p -> allowed.contains(p.getDamId()))
                    .toList();
            allActiveInstruments = allActiveInstruments.stream()
                    .filter(i -> i.getDam() != null && allowed.contains(i.getDam().getId()))
                    .toList();
        }

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
    public PagedReadingResponseDTO<ReadingGroupDTO> findGroupedReadingsByInstrument(
            Long instrumentId, Boolean active, Pageable pageable) {

        validateViewPermission();

        Page<Object[]> dateHourPage = readingRepository.findDistinctDateHourByInstrumentIdAndActive(
                instrumentId, active, pageable);

        if (dateHourPage.isEmpty()) {
            return createEmptyPagedResponse(dateHourPage);
        }

        List<LocalDate> dates = new ArrayList<>(dateHourPage.getContent().size());
        List<LocalTime> hours = new ArrayList<>(dateHourPage.getContent().size());

        for (Object[] dh : dateHourPage.getContent()) {
            dates.add((LocalDate) dh[0]);
            hours.add((LocalTime) dh[1]);
        }

        List<ReadingEntity> rawReadings = readingRepository
                .findByInstrumentIdAndDatesOptimized(instrumentId, dates, hours, active);

        Map<String, List<ReadingResponseDTO>> readingsMap = new HashMap<>();

        for (ReadingEntity reading : rawReadings) {
            String key = reading.getDate().toString() + "_" + reading.getHour().toString();
            readingsMap.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(mapToResponseDTO(reading));
        }

        List<ReadingGroupDTO> groupedResult = new ArrayList<>(dateHourPage.getContent().size());

        for (Object[] dh : dateHourPage.getContent()) {
            LocalDate targetDate = (LocalDate) dh[0];
            LocalTime targetHour = (LocalTime) dh[1];
            String key = targetDate.toString() + "_" + targetHour.toString();

            List<ReadingResponseDTO> groupReadings = readingsMap.get(key);

            if (groupReadings != null && !groupReadings.isEmpty()) {
                groupedResult.add(new ReadingGroupDTO(targetDate, targetHour, groupReadings));
            }
        }

        return createPagedResponse(groupedResult, dateHourPage);
    }

    /**
     * Últimas {@code limit} datas/horas de leitura de cada instrumento ativo do
     * cliente, agrupadas por instrumento e por data/hora.
     *
     * Uma consulta só (findLatestGroupedReadingRowsByClientId), que traz direto
     * do banco as leituras dessas datas com saída, autor, valores de entrada,
     * instrumento e barragem — nenhuma entidade é carregada.
     * Antes eram duas consultas, e a segunda carregava TODAS as leituras
     * ativas dos instrumentos como entidades para filtrar em memória: no
     * cliente 16, ~39 mil leituras e ~250 mil queries por chamada, 25 s
     * (medido em produção em 26/10/2026). A consulta nova leva ~14 ms e
     * devolve o mesmo conjunto de leituras (conferido para todos os clientes
     * com limite 1, 3 e 10).
     */
    @Transactional(readOnly = true)
    public List<InstrumentGroupedReadingsDTO> findLatestGroupedReadingsByClientId(Long clientId, int limit) {
        List<Object[]> rows = readingRepository.findLatestGroupedReadingRowsByClientId(clientId, limit);

        if (rows.isEmpty()) {
            return List.of();
        }

        // Só as barragens que o usuário acessa (ADMIN vê todas). As linhas de
        // barragens sem acesso são descartadas abaixo, antes de virar resposta.
        Set<Long> allowedDams = damAccessService.listingRestriction().orElse(null);

        // instrumento -> "data hora" -> leituras. A consulta já vem ordenada por
        // instrumento e da data mais recente para a mais antiga; os
        // LinkedHashMap mantêm essa ordem. Cada leitura aparece em uma linha por
        // valor de entrada.
        Map<Long, InstrumentGroupedReadingsDTO> instruments = new LinkedHashMap<>();
        Map<Long, Map<String, List<ReadingResponseDTO>>> readingsByInstrument = new LinkedHashMap<>();
        Map<Long, ReadingResponseDTO> readingsById = new HashMap<>();

        for (Object[] row : rows) {
            Long damId = ((Number) row[19]).longValue();
            if (allowedDams != null && !allowedDams.contains(damId)) {
                continue;
            }

            Long instrumentId = ((Number) row[1]).longValue();
            if (!instruments.containsKey(instrumentId)) {
                InstrumentGroupedReadingsDTO instrumentDTO = new InstrumentGroupedReadingsDTO();
                instrumentDTO.setInstrumentId(instrumentId);
                instrumentDTO.setInstrumentName((String) row[17]);
                instrumentDTO.setInstrumentType((String) row[18]);
                instrumentDTO.setDamId(damId);
                instrumentDTO.setDamName((String) row[20]);
                instruments.put(instrumentId, instrumentDTO);
            }

            Long readingId = ((Number) row[0]).longValue();
            ReadingResponseDTO reading = readingsById.get(readingId);
            if (reading == null) {
                reading = mapLatestGroupedRow(row);
                readingsById.put(readingId, reading);
                readingsByInstrument
                        .computeIfAbsent(instrumentId, k -> new LinkedHashMap<>())
                        .computeIfAbsent(reading.getDate() + " " + reading.getHour(), k -> new ArrayList<>())
                        .add(reading);
            }

            if (row[14] != null) {
                ReadingInputValueDTO input = new ReadingInputValueDTO();
                input.setInputAcronym((String) row[14]);
                input.setInputName((String) row[15]);
                input.setValue(row[16] != null ? ((Number) row[16]).doubleValue() : null);
                reading.getInputValues().add(input);
            }
        }

        List<InstrumentGroupedReadingsDTO> result = new ArrayList<>(instruments.size());

        for (InstrumentGroupedReadingsDTO instrumentDTO : instruments.values()) {
            Map<String, List<ReadingResponseDTO>> dateHours = readingsByInstrument.get(instrumentDTO.getInstrumentId());

            List<GroupedDateHourReadingsDTO> groupedReadings = new ArrayList<>(dateHours.size());
            for (Map.Entry<String, List<ReadingResponseDTO>> dateHour : dateHours.entrySet()) {
                GroupedDateHourReadingsDTO group = new GroupedDateHourReadingsDTO();
                group.setDateTime(dateHour.getKey());
                group.setReadings(dateHour.getValue());
                groupedReadings.add(group);
            }

            instrumentDTO.setGroupedReadings(groupedReadings);
            result.add(instrumentDTO);
        }

        return result;
    }

    /**
     * Uma linha de findLatestGroupedReadingRowsByClientId vira a leitura da
     * resposta, igual ao que mapToResponseDTO monta a partir da entidade. Os
     * valores de entrada são acrescentados linha a linha pelo chamador.
     */
    private ReadingResponseDTO mapLatestGroupedRow(Object[] row) {
        ReadingResponseDTO dto = new ReadingResponseDTO();
        dto.setId(((Number) row[0]).longValue());
        dto.setDate(toLocalDate(row[2]));
        dto.setHour(toLocalTime(row[3]));
        dto.setCalculatedValue(toBigDecimal(row[4]));
        dto.setLimitStatus(row[5] != null ? LimitStatusEnum.valueOf((String) row[5]) : null);
        dto.setComment((String) row[6]);
        dto.setActive((Boolean) row[7]);
        dto.setInstrumentId(((Number) row[1]).longValue());
        dto.setInstrumentName((String) row[17]);
        if (row[8] != null) {
            dto.setOutputId(((Number) row[8]).longValue());
            dto.setOutputName((String) row[9]);
            dto.setOutputAcronym((String) row[10]);
        }
        if (row[11] != null) {
            dto.setCreatedBy(new UserInfoDTO(((Number) row[11]).longValue(), (String) row[12], (String) row[13]));
        }
        dto.setInputValues(new ArrayList<>());
        return dto;
    }

    private static LocalDate toLocalDate(Object value) {
        return value instanceof java.sql.Date date ? date.toLocalDate() : (LocalDate) value;
    }

    private static LocalTime toLocalTime(Object value) {
        return value instanceof java.sql.Time time ? time.toLocalTime() : (LocalTime) value;
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null || value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(value.toString());
    }

    @Transactional
    public List<ReadingResponseDTO> create(Long instrumentId, ReadingRequestDTO request, boolean skipPermissionCheck) {
        LocalTime truncatedHour = request.getHour().withNano(0);
        LocalDateTime readingDateTime = LocalDateTime.of(request.getDate(), truncatedHour);
        LocalDateTime now = LocalDateTime.now();

        if (readingDateTime.truncatedTo(ChronoUnit.MINUTES).isAfter(now.truncatedTo(ChronoUnit.MINUTES))) {
            throw new InvalidInputException("Não é possível criar leituras com data e hora futura.");
        }

        if (readingRepository.existsByInstrumentIdAndDateAndHourAndActive(
                instrumentId, request.getDate(), truncatedHour, true)) {
            throw new InvalidInputException("Já existe leitura registrada para este instrumento na mesma data e hora.");
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

        Map<String, BigDecimal> formattedInputValues = new HashMap<>();
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
            BigDecimal calculatedValue = outputCalculationService.calculateOutput(output, request, formattedInputValues);

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
            for (Map.Entry<String, BigDecimal> entry : formattedInputValues.entrySet()) {
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
        // Todo caminho de criação (tela, importação Excel, coleta ANA, histórico) passa por aqui
        businessMetrics.readingsCreated(createdReadings.size());

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
        // Corrigir leitura exige acesso à barragem do instrumento. A criação não
        // ganhou a mesma trava: o app da main lista instrumentos do cliente
        // inteiro e quebraria em campo para quem lê sem ter a barragem liberada.
        damAccessService.requireAccess(instrument.getDam() != null ? instrument.getDam().getId() : null);
        Long instrumentId = instrument.getId();
        LocalDate originalDate = reading.getDate();
        LocalTime originalHour = reading.getHour();

        boolean isUpdatingInputValues = request.getInputValues() != null && !request.getInputValues().isEmpty();
        if (isUpdatingInputValues) {
            request.getInputValues().forEach(ReadingService::requireInputValue);
        }

        if (isUpdatingInputValues && variablesChangedAfterReading(instrument, originalDate, originalHour)) {
            throw new InvalidInputException(
                    "Não é possível editar os valores desta leitura pois as variáveis do instrumento foram alteradas após o registro.");
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

        List<Long> foundIds = readingRepository.findAllById(readingIds)
                .stream().map(ReadingEntity::getId).toList();

        List<BulkToggleActiveResponseDTO.FailedOperation> failedOperations = new ArrayList<>();

        for (Long requestedId : readingIds) {
            if (!foundIds.contains(requestedId)) {
                failedOperations.add(new BulkToggleActiveResponseDTO.FailedOperation(
                        requestedId, "Leitura não encontrada com ID: " + requestedId));
            }
        }

        if (!foundIds.isEmpty()) {
            readingRepository.updateActiveStatusByIds(active, foundIds);
            log.info("Bulk toggle: {} readings atualizadas via Direct SQL", foundIds.size());
        }

        return buildBulkToggleResponse(readingIds.size(), foundIds, failedOperations);
    }

    @Transactional
    public void delete(Long id) {
        validateEditPermission();
        if (!readingRepository.existsById(id)) {
            throw new NotFoundException("Leitura não encontrada com ID: " + id);
        }
        readingRepository.deleteById(id);
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
            if (user == null || user.getInstrumentationPermission() == null || !user.getInstrumentationPermission().getViewRead()) {
                throw new ForbiddenException("Usuário não tem permissão para visualizar leituras!");
            }
        }
    }

    private void validateEditPermission() {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity user = AuthenticatedUserUtil.getCurrentUser();
            if (user == null || user.getInstrumentationPermission() == null || !user.getInstrumentationPermission().getEditRead()) {
                throw new ForbiddenException("Usuário não autorizado a modificar leituras!");
            }
        }
    }

    private UserEntity resolveCurrentUser(boolean skipPermissionCheck) {
        if (skipPermissionCheck) {
            return userRepository.findByEmail(systemUserEmail)
                    .orElseThrow(() -> new NotFoundException("Usuário do sistema não encontrado!"));
        }
        UserEntity currentUser = AuthenticatedUserUtil.getCurrentUser();
        if (!AuthenticatedUserUtil.isAdmin() && (currentUser.getInstrumentationPermission() == null || !currentUser.getInstrumentationPermission().getEditRead())) {
            throw new ForbiddenException("Usuário não autorizado a criar leituras!");
        }
        return currentUser;
    }

    private void validateDateTimeChange(LocalDate newDate, LocalTime newHour, Long instrumentId, Long excludeId) {
        LocalDateTime newDateTime = LocalDateTime.of(newDate, newHour);
        if (newDateTime.truncatedTo(ChronoUnit.MINUTES).isAfter(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))) {
            throw new InvalidInputException("Não é possível atualizar leituras para data e hora futura.");
        }
        if (readingRepository.existsByInstrumentIdAndDateAndHourExcludingId(instrumentId, newDate, newHour, excludeId)) {
            throw new InvalidInputException("Já existe leitura registrada para este instrumento na mesma data e hora.");
        }
    }

    private void validateInputValues(InstrumentEntity instrument, Map<String, Double> inputValues) {
        if (inputValues == null || inputValues.isEmpty()) {
            throw new InvalidInputException("É necessário fornecer valores para os inputs");
        }
        Set<String> requiredInputs = instrument.getInputs().stream().map(InputEntity::getAcronym).collect(Collectors.toSet());
        for (String inputAcronym : requiredInputs) {
            if (!inputValues.containsKey(inputAcronym)) {
                throw new InvalidInputException("Valor não fornecido para o input '" + inputAcronym + "'");
            }
            requireInputValue(inputAcronym, inputValues.get(inputAcronym));
        }
        for (String providedInput : inputValues.keySet()) {
            if (!requiredInputs.contains(providedInput)) {
                throw new InvalidInputException("Input '" + providedInput + "' não existe neste instrumento");
            }
        }
    }

    /**
     * Input enviado com valor nulo não é "leitura ausente": a equação do output
     * só funcionaria por acaso (quando o ramo que usa o input não é avaliado) e a
     * leitura seria gravada sem aquele valor. Quem não leu um input informa 0.
     */
    private static void requireInputValue(String inputAcronym, Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            throw new InvalidInputException("Valor não fornecido para o input '" + inputAcronym
                    + "'. Se não houve leitura, informe 0.");
        }
    }

    /**
     * Diz se as variáveis do instrumento foram realmente alteradas depois que a
     * leitura foi registrada — única situação em que recalcular os valores usaria
     * uma configuração diferente da vigente no momento do registro.
     *
     * Dois cuidados evitam o falso positivo que bloqueava a primeira leitura de um
     * instrumento recém-criado:
     *
     * 1. O cadastro não conta como alteração. Na criação, createdAt e
     *    lastUpdateVariablesDate recebem o mesmo instante; enquanto forem iguais
     *    nenhuma variável foi mexida e não há o que proteger.
     * 2. A comparação é feita no minuto. A leitura guarda date + hour com precisão
     *    de minuto (14:30), enquanto lastUpdateVariablesDate é um LocalDateTime com
     *    segundos (14:30:47). Sem truncar, qualquer alteração feita no mesmo minuto
     *    do registro parecia posterior a ele.
     */
    private boolean variablesChangedAfterReading(InstrumentEntity instrument, LocalDate readingDate, LocalTime readingHour) {
        LocalDateTime lastUpdate = instrument.getLastUpdateVariablesDate();
        if (lastUpdate == null) {
            return false;
        }

        LocalDateTime createdAt = instrument.getCreatedAt();
        if (createdAt != null && !lastUpdate.isAfter(createdAt)) {
            return false;
        }

        LocalDateTime readingDateTime = LocalDateTime.of(readingDate, readingHour).truncatedTo(ChronoUnit.MINUTES);
        return lastUpdate.truncatedTo(ChronoUnit.MINUTES).isAfter(readingDateTime);
    }

    private void updateInputValuesForGroup(Long instrumentId, LocalDate date, LocalTime hour, Map<String, Double> newInputValues, InstrumentEntity instrument) {
        List<ReadingEntity> groupReadings = readingRepository.findAllReadingsInGroupWithRelations(instrumentId, date, hour);
        if (groupReadings.isEmpty()) {
            throw new NotFoundException("Não foram encontradas leituras para este grupo");
        }

        Map<String, InputEntity> instrumentInputs = instrument.getInputs().stream().collect(Collectors.toMap(InputEntity::getAcronym, Function.identity()));
        for (String inputAcronym : newInputValues.keySet()) {
            if (!instrumentInputs.containsKey(inputAcronym)) {
                throw new InvalidInputException("Input com acrônimo '" + inputAcronym + "' não encontrado");
            }
        }

        Map<String, BigDecimal> calculationInputs = new HashMap<>();
        for (ReadingEntity reading : groupReadings) {
            for (ReadingInputValueEntity riv : reading.getInputValues()) {
                calculationInputs.putIfAbsent(riv.getInputAcronym(), riv.getValue());
            }
        }

        boolean inputsChanged = false;
        for (Map.Entry<String, Double> entry : newInputValues.entrySet()) {
            BigDecimal currentValue = calculationInputs.get(entry.getKey());
            if (!Objects.equals(currentValue != null ? currentValue.doubleValue() : null, entry.getValue())) {
                inputsChanged = true;
                calculationInputs.put(entry.getKey(), BigDecimal.valueOf(entry.getValue()));
            }
        }

        if (inputsChanged) {
            for (ReadingEntity reading : groupReadings) {
                for (ReadingInputValueEntity riv : reading.getInputValues()) {
                    Double newValue = newInputValues.get(riv.getInputAcronym());
                    if (newValue != null) {
                        riv.setValue(BigDecimal.valueOf(newValue));
                    }
                }
                OutputEntity output = reading.getOutput();
                BigDecimal newCalculatedValue = outputCalculationService.calculateOutput(output, null, calculationInputs);
                reading.setCalculatedValue(newCalculatedValue);
                reading.setLimitStatus(determineLimitStatus(instrument, newCalculatedValue, output));
            }
            readingRepository.saveAll(groupReadings);
        }
    }

    private boolean updateGroupReadings(List<ReadingEntity> groupReadings, UpdateReadingRequestDTO request, LocalDate newDate, LocalTime newHour, UserEntity newUser, boolean isDateTimeChanged) {
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

    private LimitStatusEnum determineLimitStatus(InstrumentEntity instrument, BigDecimal value, OutputEntity output) {
        if (Boolean.TRUE.equals(instrument.getNoLimit()) || value == null) {
            return LimitStatusEnum.NORMAL;
        }
        StatisticalLimitEntity statisticalLimit = output.getStatisticalLimit();
        if (statisticalLimit != null) {
            if (statisticalLimit.getLowerValue() != null && value.compareTo(statisticalLimit.getLowerValue()) < 0) {
                return LimitStatusEnum.INFERIOR;
            }
            if (statisticalLimit.getUpperValue() != null && value.compareTo(statisticalLimit.getUpperValue()) > 0) {
                return LimitStatusEnum.SUPERIOR;
            }
            return LimitStatusEnum.NORMAL;
        }
        DeterministicLimitEntity deterministicLimit = output.getDeterministicLimit();
        if (deterministicLimit != null) {
            if (deterministicLimit.getEmergencyValue() != null && value.compareTo(deterministicLimit.getEmergencyValue()) >= 0) {
                return LimitStatusEnum.EMERGENCIA;
            }
            if (deterministicLimit.getAlertValue() != null && value.compareTo(deterministicLimit.getAlertValue()) >= 0) {
                return LimitStatusEnum.ALERTA;
            }
            if (deterministicLimit.getAttentionValue() != null && value.compareTo(deterministicLimit.getAttentionValue()) >= 0) {
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
            dto.setCreatedBy(new UserInfoDTO(reading.getUser().getId(), reading.getUser().getName(), reading.getUser().getEmail()));
        }
        dto.setInputValues(reading.getInputValues().stream().map(this::mapToInputValueDTO).collect(Collectors.toList()));
        return dto;
    }

    private ReadingInputValueDTO mapToInputValueDTO(ReadingInputValueEntity entity) {
        ReadingInputValueDTO dto = new ReadingInputValueDTO();
        dto.setInputAcronym(entity.getInputAcronym());
        dto.setInputName(entity.getInputName());
        dto.setValue(entity.getValue() != null ? entity.getValue().doubleValue() : null);
        return dto;
    }

    private InstrumentLimitStatusDTO buildInstrumentLimitStatusDTO(InstrumentEntity instrument, LimitStatusEnum status, String lastReadingDate) {
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

    private InstrumentLimitStatusDTO buildInstrumentLimitStatusDTOFromProjection(InstrumentLimitStatusProjection projection, LimitStatusEnum status, String lastReadingDate) {
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
        return new PagedReadingResponseDTO<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast(), page.isFirst());
    }

    private <T> PagedReadingResponseDTO<T> createPagedResponse(List<T> content, Page<?> page) {
        return new PagedReadingResponseDTO<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast(), page.isFirst());
    }

    private <T> PagedReadingResponseDTO<T> createEmptyPagedResponse(Page<?> page) {
        return new PagedReadingResponseDTO<>(List.of(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast(), page.isFirst());
    }

    private BulkToggleActiveResponseDTO buildBulkToggleResponse(int totalProcessed, List<Long> successfulIds, List<BulkToggleActiveResponseDTO.FailedOperation> failedOperations) {
        BulkToggleActiveResponseDTO response = new BulkToggleActiveResponseDTO();
        response.setSuccessfulIds(successfulIds);
        response.setFailedOperations(failedOperations);
        response.setTotalProcessed(totalProcessed);
        response.setSuccessCount(successfulIds.size());
        response.setFailureCount(failedOperations.size());
        return response;
    }

    private BigDecimal formatToSpecificPrecision(Double value, Integer precision) {
        if (value == null || precision == null) {
            return value != null ? BigDecimal.valueOf(value) : null;
        }
        return BigDecimal.valueOf(value).setScale(precision, RoundingMode.HALF_UP);
    }

}
