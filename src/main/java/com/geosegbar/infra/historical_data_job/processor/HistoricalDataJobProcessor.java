package com.geosegbar.infra.historical_data_job.processor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.enums.AuditSource;
import com.geosegbar.common.enums.AuditStatus;
import com.geosegbar.common.response.AnaTelemetryResponse.TelemetryItem;
import com.geosegbar.entities.HistoricalDataJobEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.exceptions.ExternalApiException;
import com.geosegbar.infra.audit.services.AuditContext;
import com.geosegbar.infra.audit.services.AuditService;
import com.geosegbar.infra.historical_data_job.service.HistoricalDataJobService;
import com.geosegbar.infra.hydrotelemetric.services.AnaApiService;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
import com.geosegbar.infra.reading.dtos.ReadingRequestDTO;
import com.geosegbar.infra.reading.services.ReadingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Processa jobs de coleta de dados históricos de forma assíncrona
 *
 * Workflow: 1. Busca job e instrumento 2. Obtém token ANA 3. Itera sobre
 * período (startDate até endDate) 4. Coleta dados mensais da API (30 dias por
 * vez) 5. Calcula médias diárias 6. Faz batch insert (30-50 readings) 7.
 * Atualiza checkpoint a cada batch 8. Marca como COMPLETED ou FAILED
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HistoricalDataJobProcessor {

    private static final int BATCH_SIZE = 40;
    private static final int DAYS_PER_REQUEST = 30;
    private static final String INPUT_ACRONYM = "LEI";

    private static final String ACTION = "JOB_HISTORICAL_DATA";
    private static final String ACTION_LABEL = "Coleta de dados históricos";
    private static final String ENTITY_TYPE = "HistoricalDataJob";

    private final HistoricalDataJobService jobService;
    private final InstrumentRepository instrumentRepository;
    private final AnaApiService anaApiService;
    private final ReadingService readingService;
    private final AuditService auditService;

    /**
     * Processa um job completo de coleta histórica
     *
     * Executa em thread separada do pool historicalDataExecutor. Pode levar
     * várias horas para completar (10 anos de dados).
     *
     * @param jobId ID do job a processar
     * @return CompletableFuture para monitoramento assíncrono
     */
    @Async("historicalDataExecutor")
    public CompletableFuture<Void> processJob(Long jobId) {
        log.info("🚀 Iniciando processamento do job {}", jobId);

        try {

            HistoricalDataJobEntity job = jobService.findById(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Job não encontrado: " + jobId));

            jobService.markAsProcessing(jobId);

            InstrumentEntity instrument = instrumentRepository.findById(job.getInstrumentId())
                    .orElseThrow(() -> new IllegalArgumentException(
                    "Instrumento não encontrado: " + job.getInstrumentId()));

            if (instrument.getLinimetricRulerCode() == null) {
                throw new IllegalStateException(
                        "Instrumento não possui código de régua linimétrica: " + instrument.getName());
            }

            String authToken = anaApiService.getAuthToken();
            log.info("Token ANA obtido para job {}", jobId);

            processHistoricalPeriod(job, instrument, authToken);

            jobService.markAsCompleted(jobId);
            log.info("✅ Job {} COMPLETADO com sucesso", jobId);

            auditService.record(AuditContext.builder()
                    .action(ACTION).actionLabel(ACTION_LABEL).source(AuditSource.JOB)
                    .status(AuditStatus.SUCCESS)
                    .message("Coleta histórica concluída para o instrumento " + instrument.getName() + ".")
                    .entityType(ENTITY_TYPE).entityId(jobId)
                    .build());

            return CompletableFuture.completedFuture(null);

        } catch (ExternalApiException e) {
            handleApiError(jobId, e);
            return CompletableFuture.failedFuture(e);
        } catch (Exception e) {
            handleGenericError(jobId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Processa todo o período histórico (10 anos)
     *
     * Itera mês a mês, coletando dados em blocos de 30 dias. Faz checkpoint a
     * cada batch para permitir resume.
     */
    private void processHistoricalPeriod(HistoricalDataJobEntity job, InstrumentEntity instrument, String authToken) {
        LocalDate currentDate = job.getCheckpointDate();
        LocalDate endDate = job.getEndDate();
        String stationCode = String.valueOf(instrument.getLinimetricRulerCode());

        List<ReadingRequestDTO> readingBatch = new ArrayList<>();
        int totalCreated = 0;
        int totalSkipped = 0;
        String currentToken = authToken;

        log.info("Processando período: {} até {} para instrumento {}",
                currentDate, endDate, instrument.getName());

        while (!currentDate.isAfter(endDate)) {
            try {

                LocalDate periodEnd = currentDate.plusDays(DAYS_PER_REQUEST - 1);
                if (periodEnd.isAfter(endDate)) {
                    periodEnd = endDate;
                }

                log.debug("Coletando dados: {} a {} (job {})", currentDate, periodEnd, job.getId());

                List<TelemetryItem> telemetryData = anaApiService.getTelemetryDataForHistoricalPeriod(
                        stationCode,
                        currentDate,
                        currentToken
                );

                log.debug("API retornou {} itens para período {} a {} (job {})",
                        telemetryData.size(), currentDate, periodEnd, job.getId());

                if (telemetryData.isEmpty()) {
                    log.warn("⚠️ Sem dados disponíveis para período {} a {} (estação: {})",
                            currentDate, periodEnd, stationCode);
                    totalSkipped += (int) ChronoUnit.DAYS.between(currentDate, periodEnd) + 1;
                    currentDate = periodEnd.plusDays(1);
                    continue;
                }

                Map<LocalDate, List<TelemetryItem>> itemsByDate = new HashMap<>();
                for (TelemetryItem item : telemetryData) {
                    if (item.getDataHoraMedicao() != null) {
                        String dateStr = item.getDataHoraMedicao().substring(0, 10);
                        LocalDate itemDate = LocalDate.parse(dateStr);
                        itemsByDate.computeIfAbsent(itemDate, k -> new ArrayList<>()).add(item);
                    }
                }

                log.info("📅 Dados agrupados em {} dias distintos. Período: {} a {}",
                        itemsByDate.size(), currentDate, periodEnd);
                log.info("📅 Dias com dados na API: {}", itemsByDate.keySet().stream().sorted().toList());

                for (Map.Entry<LocalDate, List<TelemetryItem>> entry : itemsByDate.entrySet()) {
                    LocalDate date = entry.getKey();
                    List<TelemetryItem> dayItems = entry.getValue();

                    boolean exists = readingService.existsByInstrumentAndDate(instrument.getId(), date);
                    if (exists) {
                        log.debug("Pulando {}: leitura já existe", date);
                        totalSkipped++;
                        continue;
                    }

                    if (dayItems == null || dayItems.isEmpty()) {
                        log.debug("Pulando {}: sem dados da API para este dia", date);
                        totalSkipped++;
                        continue;
                    }

                    log.debug("Processando {}: {} leituras encontradas", date, dayItems.size());

                    Double averageMm = dayItems.stream()
                            .filter(item -> item.getCotaAdotada() != null && !item.getCotaAdotada().isEmpty())
                            .filter(item -> !item.getCotaAdotada().equals("0.00"))
                            .mapToDouble(item -> {
                                try {
                                    return Double.parseDouble(item.getCotaAdotada());
                                } catch (NumberFormatException e) {
                                    log.warn("Valor de cota inválido: {}", item.getCotaAdotada());
                                    return 0.0;
                                }
                            })
                            .filter(value -> value > 0.0)
                            .average()
                            .orElse(0.0);

                    if (averageMm == null || averageMm == 0.0) {
                        log.warn("⚠️ Pulando {}: média calculada é 0 ou nula", date);
                        totalSkipped++;
                        continue;
                    }

                    log.info("✅ Criando reading para {}: {} mm (job {})", date, averageMm, job.getId());

                    ReadingRequestDTO reading = createReading(date, averageMm);
                    readingBatch.add(reading);
                    totalCreated++;

                    if (readingBatch.size() >= BATCH_SIZE) {
                        batchInsertReadings(instrument.getId(), readingBatch);
                        readingBatch.clear();

                        jobService.updateProgress(job.getId(), date, BATCH_SIZE, totalSkipped);
                        totalSkipped = 0;
                    }
                }

                currentDate = periodEnd.plusDays(1);

                Thread.sleep(500);

            } catch (ExternalApiException e) {
                log.error("Erro de API no job {}: {}", job.getId(), e.getMessage());
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Job interrompido", e);
            } catch (Exception e) {
                log.error("Erro ao processar período {} no job {}: {}",
                        currentDate, job.getId(), e.getMessage(), e);
                currentDate = currentDate.plusDays(DAYS_PER_REQUEST);
            }
        }

        if (!readingBatch.isEmpty()) {
            batchInsertReadings(instrument.getId(), readingBatch);
            jobService.updateProgress(job.getId(), endDate, readingBatch.size(), totalSkipped);
        }

        log.info("Período processado: {} readings criados, {} dias pulados (job {})",
                totalCreated, totalSkipped, job.getId());
    }

    /**
     * Cria objeto ReadingRequestDTO com dados coletados
     */
    private ReadingRequestDTO createReading(LocalDate date, Double valueMm) {
        ReadingRequestDTO reading = new ReadingRequestDTO();
        reading.setDate(date);
        reading.setHour(LocalTime.of(0, 30));

        Map<String, Double> inputValues = new HashMap<>();
        inputValues.put(INPUT_ACRONYM, valueMm);
        reading.setInputValues(inputValues);

        reading.setComment("Coleta histórica automática ANA");
        return reading;
    }

    /**
     * Faz batch insert de readings
     *
     * Usa skipPermissionCheck=true para coleta automática. Transação isolada
     * para não comprometer job inteiro em caso de erro.
     */
    @Transactional
    private void batchInsertReadings(Long instrumentId, List<ReadingRequestDTO> readings) {
        try {
            for (ReadingRequestDTO reading : readings) {
                readingService.create(instrumentId, reading, true);
            }
            log.debug("Batch de {} readings inserido", readings.size());
        } catch (Exception e) {
            log.error("Erro ao inserir batch de readings: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Trata erros da API externa (ANA)
     *
     * Token expirado ou timeout: PAUSED + retry Outros erros: incrementa retry
     * ou FAILED
     */
    private void handleApiError(Long jobId, ExternalApiException e) {
        log.error("❌ Erro de API externa no job {}: {}", jobId, e.getMessage());

        try {
            boolean canRetry = jobService.incrementRetry(jobId);

            if (canRetry) {
                jobService.markAsPaused(jobId, "API Error: " + e.getMessage());
                log.info("Job {} pausado para retry ({})", jobId, jobId);
            } else {
                jobService.markAsFailed(jobId, "Falhou após 3 tentativas: " + e.getMessage());
                log.error("Job {} FALHOU definitivamente após retries", jobId);
                auditService.record(AuditContext.builder()
                        .action(ACTION).actionLabel(ACTION_LABEL).source(AuditSource.JOB)
                        .status(AuditStatus.ERROR)
                        .message("Coleta histórica falhou após 3 tentativas (erro de API externa).")
                        .error(e)
                        .entityType(ENTITY_TYPE).entityId(jobId)
                        .build());
            }
        } catch (Exception ex) {
            log.error("Erro ao marcar job como pausado/falho: {}", ex.getMessage());
        }
    }

    /**
     * Trata erros genéricos (bugs, validação, etc)
     *
     * Erros não recuperáveis: marca como FAILED imediatamente
     */
    private void handleGenericError(Long jobId, Exception e) {
        log.error("❌ Erro genérico no job {}: {}", jobId, e.getMessage(), e);

        try {
            String errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
            if (errorMessage.length() > 2000) {
                errorMessage = errorMessage.substring(0, 1997) + "...";
            }
            jobService.markAsFailed(jobId, errorMessage);
            auditService.record(AuditContext.builder()
                    .action(ACTION).actionLabel(ACTION_LABEL).source(AuditSource.JOB)
                    .status(AuditStatus.ERROR)
                    .message("Coleta histórica falhou (erro não recuperável).")
                    .error(e)
                    .entityType(ENTITY_TYPE).entityId(jobId)
                    .build());
        } catch (Exception ex) {
            log.error("Erro ao marcar job como falho: {}", ex.getMessage());
        }
    }
}
