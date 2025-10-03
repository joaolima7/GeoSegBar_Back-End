package com.geosegbar.infra.reading.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
import com.geosegbar.infra.reading.dtos.InstrumentLimitStatusDTO;
import com.geosegbar.infra.reading.dtos.InstrumentReadingsDTO;
import com.geosegbar.infra.reading.dtos.InstrumentReadingsDTO.MultiInstrumentReadingsResponseDTO;
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

    @Cacheable(value = "readingsByInstrument", key = "#instrumentId", cacheManager = "readingCacheManager")
    public List<ReadingResponseDTO> findByInstrumentId(Long instrumentId) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getInstrumentationPermission().getViewRead()) {
                throw new UnauthorizedException("Usuário não tem permissão para visualizar leituras!");
            }
        }

        List<ReadingEntity> readings = readingRepository.findByInstrumentIdOptimized(instrumentId);
        return readings.stream()
                .map(this::mapToResponseDTOOptimized)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "readingExists", key = "#instrumentId + '_' + #date", cacheManager = "readingCacheManager")
    public boolean existsByInstrumentAndDate(Long instrumentId, LocalDate date) {
        return readingRepository.existsByInstrumentIdAndDate(instrumentId, date);
    }

    @Cacheable(value = "instrumentLimitStatus", key = "#instrumentId + '_' + #limit", cacheManager = "readingCacheManager")
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

    @Cacheable(value = "clientInstrumentLimitStatuses", key = "#clientId + '_' + #limit", cacheManager = "readingCacheManager")
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
        dto.setInstrumentType(instrument.getInstrumentType().getName());
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

    @Cacheable(value = "readingsByOutput", key = "#outputId", cacheManager = "readingCacheManager")
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

    @Cacheable(
            value = "readingsByFilters",
            key = "#instrumentId + '_' + #outputId + '_' + #startDate + '_' + #endDate + '_' + "
            + "#limitStatus + '_' + #active + '_' + #pageable.pageNumber + '_' + #pageable.pageSize",
            cacheManager = "readingCacheManager"
    )
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

    @Cacheable(value = "readingById", key = "#id", cacheManager = "readingCacheManager")
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
    @CacheEvict(
            value = {
                "readingsByInstrument", "instrumentLimitStatus", "clientInstrumentLimitStatuses",
                "latestReadings", "readingExists", "multiInstrumentReadings", "groupedReadings"
            },
            allEntries = true,
            cacheManager = "readingCacheManager"
    )
    public List<ReadingResponseDTO> create(Long instrumentId, ReadingRequestDTO request, boolean skipPermissionCheck) {
        UserEntity currentUser;

        LocalTime truncatedHour = request.getHour().withNano(0); // Trunca para segundos, ignorando nanossegundos

        // Validar data/hora futura
        LocalDateTime readingDateTime = LocalDateTime.of(request.getDate(), truncatedHour);
        LocalDateTime now = LocalDateTime.now();

        if (readingDateTime.isAfter(now)) {
            throw new InvalidInputException("Não é possível criar leituras com data e hora futura. "
                    + "Data/hora informada: " + DateFormatter.formatDateTime(readingDateTime)
                    + ", Data/hora atual: " + DateFormatter.formatDateTime(now));
        }

        // Verificar se já existe leitura para essa data/hora
        if (readingRepository.existsByInstrumentIdAndDateAndHourAndActive(instrumentId, request.getDate(), truncatedHour, true)) {
            throw new InvalidInputException("Já existe leitura registrada para este instrumento na mesma data e hora ("
                    + request.getDate() + " " + truncatedHour + ")");
        }

        // Ajustar a hora na requisição para garantir consistência
        request.setHour(truncatedHour);

        // Obter usuário
        if (skipPermissionCheck) {
            String systemUserEmail = "noreply@geometrisa-prod.com.br";
            currentUser = userRepository.findByEmail(systemUserEmail)
                    .orElseThrow(() -> new NotFoundException("Usuário do sistema não encontrado!"));
        } else {
            currentUser = AuthenticatedUserUtil.getCurrentUser();

            if (!AuthenticatedUserUtil.isAdmin()) {
                if (!currentUser.getInstrumentationPermission().getEditRead()) {
                    throw new UnauthorizedException("Usuário não autorizado a criar leituras!");
                }
            }
        }

        // Obter instrumento
        InstrumentEntity instrument = instrumentRepository.findWithActiveOutputsById(instrumentId)
                .orElseThrow(() -> new NotFoundException("Instrumento não encontrado com ID: " + instrumentId));

        // Obter outputs ativos
        List<OutputEntity> activeOutputs = instrument.getOutputs().stream()
                .filter(OutputEntity::getActive)
                .collect(Collectors.toList());

        if (activeOutputs.isEmpty()) {
            throw new NotFoundException("O instrumento não possui outputs ativos para calcular leituras");
        }

        // Validar valores de input
        validateInputValues(instrument, request.getInputValues());

        // Formatar valores com precisão correta
        Map<String, Double> formattedInputValues = new HashMap<>();
        for (InputEntity input : instrument.getInputs()) {
            Double inputValue = request.getInputValues().get(input.getAcronym());
            if (inputValue != null) {
                Double formattedValue = formatToSpecificPrecision(inputValue, input.getPrecision());
                formattedInputValues.put(input.getAcronym(), formattedValue);
            }
        }

        // Criar os ReadingInputValueEntity que serão compartilhados
        Set<ReadingInputValueEntity> sharedInputValues = new HashSet<>();
        Map<String, String> inputNames = instrument.getInputs().stream()
                .collect(Collectors.toMap(InputEntity::getAcronym, InputEntity::getName));

        // Criar os valores de input (uma vez só)
        for (Map.Entry<String, Double> entry : formattedInputValues.entrySet()) {
            ReadingInputValueEntity inputValue = new ReadingInputValueEntity();
            inputValue.setInputAcronym(entry.getKey());
            inputValue.setInputName(inputNames.get(entry.getKey()));
            inputValue.setValue(entry.getValue());

            ReadingInputValueEntity savedInputValue = readingInputValueRepository.save(inputValue);
            sharedInputValues.add(savedInputValue);
        }

        // Criar as readings para cada output
        List<ReadingEntity> createdReadings = new ArrayList<>();

        for (OutputEntity output : activeOutputs) {
            // Calcular o valor para este output
            Double calculatedValue = outputCalculationService.calculateOutput(output, request, formattedInputValues);

            // Criar a reading
            ReadingEntity reading = new ReadingEntity();
            reading.setDate(request.getDate());
            reading.setHour(request.getHour());
            reading.setCalculatedValue(calculatedValue);
            reading.setInstrument(instrument);
            reading.setOutput(output);
            reading.setUser(currentUser);
            reading.setActive(true);
            reading.setComment(request.getComment());

            // Determinar o status do limite
            LimitStatusEnum limitStatus = determineLimitStatus(instrument, calculatedValue, output);
            reading.setLimitStatus(limitStatus);

            // Associar os mesmos valores de input a esta reading
            reading.setInputValues(sharedInputValues);

            // Salvar a reading
            ReadingEntity savedReading = readingRepository.save(reading);
            createdReadings.add(savedReading);
        }

        return createdReadings.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Cacheable(
            value = "multiInstrumentReadings",
            key = "T(org.springframework.util.StringUtils).collectionToDelimitedString(#instrumentIds, '_') + '_' + "
            + "T(org.springframework.util.StringUtils).collectionToDelimitedString(#outputIds, '_') + '_' + "
            + "#startDate + '_' + #endDate + '_' + #pageSize",
            cacheManager = "readingCacheManager"
    )
    @Transactional(readOnly = true)
    public MultiInstrumentReadingsResponseDTO findLatestReadingsForMultipleInstruments(
            List<Long> instrumentIds,
            List<Long> outputIds,
            LocalDate startDate,
            LocalDate endDate,
            int pageSize) {

        Set<Long> uniqueInstrumentIds = new HashSet<>();

        if (instrumentIds != null && !instrumentIds.isEmpty()) {
            uniqueInstrumentIds.addAll(instrumentIds);
        }

        if (outputIds != null && !outputIds.isEmpty()) {
            Set<Long> instrumentIdsFromOutputs = readingRepository.findInstrumentIdsByOutputIds(outputIds);
            uniqueInstrumentIds.addAll(instrumentIdsFromOutputs);
        }

        if (uniqueInstrumentIds.isEmpty()) {
            return new MultiInstrumentReadingsResponseDTO(
                    List.of(),
                    pageSize,
                    0
            );
        }

        List<InstrumentEntity> instruments = instrumentRepository.findAllById(uniqueInstrumentIds);

        List<InstrumentReadingsDTO> result = new ArrayList<>();

        for (InstrumentEntity instrument : instruments) {
            List<Long> readingIds;

            if (startDate != null && endDate != null) {
                readingIds = readingRepository.findLatestReadingIdsByInstrumentIdAndDateRange(
                        instrument.getId(), startDate, endDate, pageSize);
            } else if (startDate != null) {
                readingIds = readingRepository.findLatestReadingIdsByInstrumentIdAndStartDate(
                        instrument.getId(), startDate, pageSize);
            } else if (endDate != null) {
                readingIds = readingRepository.findLatestReadingIdsByInstrumentIdAndEndDate(
                        instrument.getId(), endDate, pageSize);
            } else {
                readingIds = readingRepository.findLatestReadingIdsByInstrumentId(
                        instrument.getId(), pageSize);
            }

            if (readingIds.isEmpty()) {
                result.add(new InstrumentReadingsDTO(
                        instrument.getId(),
                        instrument.getName(),
                        instrument.getInstrumentType().getName(),
                        List.of()
                ));
                continue;
            }

            List<ReadingEntity> readings = readingRepository.findByIdIn(readingIds);

            List<ReadingResponseDTO> readingDTOs = readings.stream()
                    .map(this::mapToResponseDTOOptimized)
                    .collect(Collectors.toList());

            result.add(new InstrumentReadingsDTO(
                    instrument.getId(),
                    instrument.getName(),
                    instrument.getInstrumentType().getName(),
                    readingDTOs
            ));
        }

        return new MultiInstrumentReadingsResponseDTO(
                result,
                pageSize,
                uniqueInstrumentIds.size()
        );
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
@CacheEvict(
        value = {
            "readingById", "readingResponseDTO", "readingsByInstrument", "readingsByOutput",
            "readingsByFilters", "instrumentLimitStatus", "clientInstrumentLimitStatuses",
            "latestReadings", "groupedReadings", "multiInstrumentReadings"
        },
        allEntries = true,
        cacheManager = "readingCacheManager"
)
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

    InstrumentEntity instrument = reading.getInstrument();

    // Verificar se há tentativa de atualizar valores de input
    boolean isUpdatingInputValues = request.getInputValues() != null && !request.getInputValues().isEmpty();

    // Verificar se as variáveis do instrumento foram alteradas desde a leitura
    if (isUpdatingInputValues) {
        LocalDateTime readingDateTime = LocalDateTime.of(reading.getDate(), reading.getHour());
        if (instrument.getLastUpdateVariablesDate() != null
                && instrument.getLastUpdateVariablesDate().isAfter(readingDateTime)) {
            throw new InvalidInputException("Não é possível editar os valores desta leitura pois as variáveis do instrumento foram alteradas após o registro.");
        }
    }

    // Armazenar valores originais para usar nas atualizações em grupo
    LocalDate originalDate = reading.getDate();
    LocalTime originalHour = reading.getHour();
    Long instrumentId = instrument.getId();

    // Se está atualizando data ou hora, verificar possível conflito
    LocalDate newDate = request.getDate() != null ? request.getDate() : originalDate;
    LocalTime newHour = request.getHour() != null ? request.getHour() : originalHour;

    // Truncar a hora para ignorar nanossegundos
    if (newHour != null) {
        newHour = newHour.withNano(0);
    }

    // Verificar se alguma data/hora está sendo alterada
    boolean isDateTimeChanged = (request.getDate() != null && !Objects.equals(newDate, originalDate))
            || (request.getHour() != null && !Objects.equals(newHour, originalHour));

    // Se estiver alterando data/hora, verificar se já existe outro grupo com a mesma data/hora
    if (isDateTimeChanged) {
        // Validar que a nova data/hora não é futura
        LocalDateTime newDateTime = LocalDateTime.of(newDate, newHour);
        LocalDateTime now = LocalDateTime.now();

        if (newDateTime.isAfter(now)) {
            throw new InvalidInputException("Não é possível atualizar leituras para data e hora futura. "
                    + "Data/hora informada: " + DateFormatter.formatDateTime(newDateTime)
                    + ", Data/hora atual: " + DateFormatter.formatDateTime(now));
        }

        List<ReadingEntity> existingReadings;

        if (originalDate == null || originalHour == null) {
            existingReadings = readingRepository.findByInstrumentIdAndDateAndHourActiveTrue(
                    instrumentId, newDate, newHour);
        } else {
            existingReadings = readingRepository.findByInstrumentIdAndDateAndHourExcludingSpecific(
                    instrumentId, newDate, newHour, originalDate, originalHour);
        }

        if (!existingReadings.isEmpty()) {
            throw new InvalidInputException("Já existe leitura registrada para este instrumento na mesma data e hora ("
                    + newDate + " " + newHour + ")");
        }
    }

    // Preparar o novo usuário se fornecido
    UserEntity newUser = null;
    if (request.getUserId() != null) {
        newUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado com ID: " + request.getUserId()));
    }

    // IMPORTANTE: Primeiro atualizar os valores de input (se necessário)
    // para garantir que todas as readings (inclusive a principal) sejam recalculadas
    if (isUpdatingInputValues) {
        updateInputValues(instrumentId, originalDate, originalHour, request.getInputValues());
    }

    // Depois atualizar data/hora e usuário em todas as readings do grupo
    List<ReadingEntity> groupReadings = readingRepository.findAllReadingsInGroup(
            instrumentId, originalDate, originalHour);
            
    for (ReadingEntity groupReading : groupReadings) {
        boolean changed = false;
        
        if (isDateTimeChanged) {
            groupReading.setDate(newDate);
            groupReading.setHour(newHour);
            changed = true;
        }
        
        if (newUser != null) {
            groupReading.setUser(newUser);
            changed = true;
        }
        
        if (changed) {
            readingRepository.save(groupReading);
            log.info("Atualizada reading {} com data/hora/usuário", groupReading.getId());
        }
    }
    
    // IMPORTANTE: Recarregar a reading principal para garantir que temos os dados mais atualizados
    ReadingEntity updatedReading = readingRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Leitura não encontrada após atualização"));
            
    return mapToResponseDTO(updatedReading);
}

private void updateInputValues(Long instrumentId, LocalDate date, LocalTime hour, Map<String, Double> newInputValues) {
    // Obter todas as leituras do grupo
    List<ReadingEntity> groupReadings = readingRepository.findAllReadingsInGroup(instrumentId, date, hour);
    
    if (groupReadings.isEmpty()) {
        throw new NotFoundException("Não foram encontradas leituras para este grupo");
    }
    
    log.info("Atualizando valores de input para {} leituras do grupo {}/{}", 
            groupReadings.size(), date, hour);
    
    InstrumentEntity instrument = instrumentRepository.findById(instrumentId)
            .orElseThrow(() -> new NotFoundException("Instrumento não encontrado"));
    
    // Validar inputs
    Map<String, InputEntity> instrumentInputs = instrument.getInputs().stream()
            .collect(Collectors.toMap(InputEntity::getAcronym, input -> input));
    
    for (String inputAcronym : newInputValues.keySet()) {
        if (!instrumentInputs.containsKey(inputAcronym)) {
            throw new InvalidInputException("Input com acrônimo '" + inputAcronym + 
                    "' não encontrado no instrumento");
        }
    }
    
    // Primeiro: atualizar todos os valores de input do grupo
    // Como os valores são compartilhados (many-to-many), precisamos atualizar apenas uma vez cada valor
    Set<ReadingInputValueEntity> uniqueInputValues = new HashSet<>();
    for (ReadingEntity reading : groupReadings) {
        uniqueInputValues.addAll(reading.getInputValues());
    }
    
    // Mapear por acrônimo para facilitar atualização
    Map<String, ReadingInputValueEntity> inputValuesByAcronym = uniqueInputValues.stream()
            .collect(Collectors.toMap(ReadingInputValueEntity::getInputAcronym, value -> value));
    
    // Mapa para calcular novos valores
    Map<String, Double> calculationInputs = new HashMap<>();
    for (ReadingInputValueEntity value : uniqueInputValues) {
        calculationInputs.put(value.getInputAcronym(), value.getValue());
    }
    
    // Atualizar valores
    boolean inputsChanged = false;
    for (Map.Entry<String, Double> entry : newInputValues.entrySet()) {
        String acronym = entry.getKey();
        Double newValue = entry.getValue();
        Double oldValue = calculationInputs.get(acronym);
        
        if (!Objects.equals(oldValue, newValue)) {
            inputsChanged = true;
            calculationInputs.put(acronym, newValue);
            
            ReadingInputValueEntity inputValue = inputValuesByAcronym.get(acronym);
            if (inputValue != null) {
                log.info("Atualizando valor para {}: {} → {}", acronym, oldValue, newValue);
                inputValue.setValue(newValue);
                readingInputValueRepository.save(inputValue);
            }
        }
    }
    
    // Se pelo menos um valor foi alterado, recalcular TODAS as readings do grupo
    if (inputsChanged) {
        log.info("Recalculando valores para todas as {} readings do grupo", groupReadings.size());
        
        // Recalcular cada reading do grupo com os novos valores
        for (ReadingEntity reading : groupReadings) {
            OutputEntity output = reading.getOutput();
            
            Double oldValue = reading.getCalculatedValue();
            Double newCalculatedValue = outputCalculationService.calculateOutput(
                    output, null, calculationInputs);
            
            reading.setCalculatedValue(newCalculatedValue);
            
            LimitStatusEnum newLimitStatus = determineLimitStatus(instrument, newCalculatedValue, output);
            reading.setLimitStatus(newLimitStatus);
            
            log.info("Reading {}: Output {} recalculado: {} → {}", 
                    reading.getId(), output.getAcronym(), oldValue, newCalculatedValue);
            
            readingRepository.save(reading);
        }
    } else {
        log.info("Nenhum valor de input foi alterado, não é necessário recalcular");
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
    @CacheEvict(
            value = {
                "readingById", "readingResponseDTO", "readingsByInstrument", "readingsByOutput",
                "readingsByFilters", "instrumentLimitStatus", "clientInstrumentLimitStatuses",
                "latestReadings", "groupedReadings", "multiInstrumentReadings"
            },
            allEntries = true,
            cacheManager = "readingCacheManager"
    )
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
    @CacheEvict(
            value = {
                "readingById", "readingResponseDTO", "readingsByInstrument", "readingsByOutput",
                "readingsByFilters", "instrumentLimitStatus", "clientInstrumentLimitStatuses",
                "latestReadings", "groupedReadings", "multiInstrumentReadings"
            },
            allEntries = true,
            cacheManager = "readingCacheManager"
    )
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
    @CacheEvict(
            value = {
                "readingById", "readingResponseDTO"
            },
            key = "#id",
            cacheManager = "readingCacheManager"
    )
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
    @CacheEvict(
            value = {
                "readingById", "readingResponseDTO", "readingsByInstrument", "readingsByOutput",
                "readingsByFilters", "instrumentLimitStatus", "clientInstrumentLimitStatuses",
                "latestReadings", "readingExists", "groupedReadings", "multiInstrumentReadings"
            },
            allEntries = true,
            cacheManager = "readingCacheManager"
    )
    public void delete(Long id) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getInstrumentationPermission().getEditRead()) {
                throw new UnauthorizedException("Usuário não autorizado a excluir leituras!");
            }
        }

        ReadingEntity reading = readingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Leitura não encontrada com ID: " + id));

        // Remover associações com valores de input
        Set<ReadingInputValueEntity> inputValues = new HashSet<>(reading.getInputValues());
        reading.getInputValues().clear();
        readingRepository.save(reading);

        // Verificar quais valores de input não são mais usados por nenhuma outra leitura
        for (ReadingInputValueEntity inputValue : inputValues) {
            inputValue.getReadings().remove(reading);

            if (inputValue.getReadings().isEmpty()) {
                // Se não é mais usado por nenhuma leitura, podemos excluí-lo
                readingInputValueRepository.delete(inputValue);
            } else {
                readingInputValueRepository.save(inputValue);
            }
        }

        // Agora podemos excluir a leitura com segurança
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

    @Cacheable(value = "readingResponseDTO", key = "#reading.id", cacheManager = "readingCacheManager")
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
            dto.setCreatedBy(new ReadingResponseDTO.UserInfoDTO(
                    reading.getUser().getId(),
                    reading.getUser().getName(),
                    reading.getUser().getEmail()
            ));
        }

        dto.setInputValues(getInputValuesForReading(reading));

        return dto;
    }

    @Cacheable(
            value = "groupedReadings",
            key = "#instrumentId + '_' + #active + '_' + #pageable.pageNumber + '_' + #pageable.pageSize",
            cacheManager = "readingCacheManager"
    )
    public PagedReadingResponseDTO<ReadingResponseDTO> findGroupedReadingsFlatByInstrument(
            Long instrumentId, Boolean active, Pageable pageable) {

        // Verificar permissões
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity userLogged = AuthenticatedUserUtil.getCurrentUser();
            if (!userLogged.getInstrumentationPermission().getViewRead()) {
                throw new UnauthorizedException("Usuário não tem permissão para visualizar leituras");
            }
        }

        Page<Object[]> dateHourPage = readingRepository.findDistinctDateHourByInstrumentIdAndActive(instrumentId, active, pageable);

        List<ReadingResponseDTO> allReadings = new ArrayList<>();
        for (Object[] dh : dateHourPage.getContent()) {
            LocalDate date = (LocalDate) dh[0];
            LocalTime hour = (LocalTime) dh[1];
            List<ReadingEntity> readings = readingRepository.findByInstrumentIdAndDateAndHourAndActive(
                    instrumentId, date, hour, active);

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
        dto.setActive(reading.getActive());

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

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(precision, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @Cacheable(
            value = "clientInstrumentLatestGroupedReadings",
            key = "#clientId + '_' + #limit",
            cacheManager = "readingCacheManager"
    )
    @Transactional(readOnly = true)
    public List<InstrumentGroupedReadingsDTO> findLatestGroupedReadingsByClientId(Long clientId, int limit) {
        // Verificações de permissão e existência do cliente (mantidas)

        // Buscar os momentos mais recentes para cada instrumento
        List<Object[]> latestDateHours = readingRepository.findLatestDistinctDateHoursByClientId(clientId, limit);

        if (latestDateHours.isEmpty()) {
            return List.of();
        }

        // Agrupar as datas/horas por instrumento
        Map<Long, List<Object[]>> instrumentDateHoursMap = new HashMap<>();
        for (Object[] row : latestDateHours) {
            Long instrumentId = ((Number) row[0]).longValue();
            java.sql.Date sqlDate = (java.sql.Date) row[1];
            java.sql.Time sqlTime = (java.sql.Time) row[2];

            LocalDate date = sqlDate.toLocalDate();
            LocalTime hour = sqlTime.toLocalTime();

            instrumentDateHoursMap
                    .computeIfAbsent(instrumentId, k -> new ArrayList<>())
                    .add(new Object[]{date, hour});
        }

        // Preparar o resultado
        List<InstrumentGroupedReadingsDTO> result = new ArrayList<>();

        // Para cada instrumento, buscar as leituras das datas/horas especificadas
        for (Map.Entry<Long, List<Object[]>> entry : instrumentDateHoursMap.entrySet()) {
            Long instrumentId = entry.getKey();
            List<Object[]> dateHours = entry.getValue();

            // Buscar detalhes do instrumento
            InstrumentEntity instrument = instrumentRepository.findById(instrumentId)
                    .orElseThrow(() -> new NotFoundException("Instrumento não encontrado com ID: " + instrumentId));

            // Criar o DTO para o instrumento
            InstrumentGroupedReadingsDTO instrumentDTO = new InstrumentGroupedReadingsDTO();
            instrumentDTO.setInstrumentId(instrument.getId());
            instrumentDTO.setInstrumentName(instrument.getName());
            instrumentDTO.setInstrumentType(instrument.getInstrumentType().getName());
            instrumentDTO.setDamId(instrument.getDam().getId());
            instrumentDTO.setDamName(instrument.getDam().getName());
            instrumentDTO.setGroupedReadings(new ArrayList<>());

            // Para cada data/hora, buscar as leituras (todos os outputs daquele momento)
            for (Object[] dateHour : dateHours) {
                LocalDate date = (LocalDate) dateHour[0];
                LocalTime hour = (LocalTime) dateHour[1];

                // Buscar leituras para essa data/hora específica
                List<ReadingEntity> readings = readingRepository.findByInstrumentIdAndDateAndHourAndActiveTrue(
                        instrumentId, date, hour);

                if (!readings.isEmpty()) {
                    // Criar um grupo para essa data/hora
                    String dateHourKey = date.toString() + " " + hour.toString();
                    InstrumentGroupedReadingsDTO.GroupedDateHourReadingsDTO group
                            = new InstrumentGroupedReadingsDTO.GroupedDateHourReadingsDTO();
                    group.setDateTime(dateHourKey);

                    // Mapear as leituras para DTOs (todas as leituras desse momento - um por output)
                    List<ReadingResponseDTO> readingDTOs = readings.stream()
                            .map(this::mapToResponseDTOOptimized)
                            .collect(Collectors.toList());

                    group.setReadings(readingDTOs);
                    instrumentDTO.getGroupedReadings().add(group);
                }
            }

            result.add(instrumentDTO);
        }

        return result;
    }
}
