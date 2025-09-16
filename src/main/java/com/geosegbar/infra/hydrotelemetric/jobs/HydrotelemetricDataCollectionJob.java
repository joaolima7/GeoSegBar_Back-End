package com.geosegbar.infra.hydrotelemetric.jobs;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.enums.ReadingTypeEnum;
import com.geosegbar.common.response.AnaTelemetryResponse.TelemetryItem;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.HydrotelemetricReadingEntity;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.hydrotelemetric.persistence.jpa.HydrotelemetricReadingRepository;
import com.geosegbar.infra.hydrotelemetric.services.AnaApiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class HydrotelemetricDataCollectionJob {

    private final AnaApiService anaApiService;
    private final DamRepository damRepository;
    private final HydrotelemetricReadingRepository hydrotelemetricReadingRepository;

    @Scheduled(cron = "0 30 0 * * ?")
    public void collectHydrotelemetricData() {
        log.info("Iniciando coleta de dados hidrotelemétricos");

        try {
            String authToken = anaApiService.getAuthToken();

            List<DamEntity> dams = damRepository.findAll();

            LocalDate today = LocalDate.now();

            for (DamEntity dam : dams) {
                try {
                    collectDamData(dam, authToken, today);
                } catch (Exception e) {
                    log.error("Erro ao coletar dados para a barragem {}: {}", dam.getName(), e.getMessage(), e);
                }
            }

            log.info("Coleta de dados hidrotelemétricos finalizada com sucesso");
        } catch (Exception e) {
            log.error("Erro durante a coleta de dados hidrotelemétricos: {}", e.getMessage(), e);
        }
    }

    @Transactional(timeout = 60)
    private void collectDamData(DamEntity dam, String authToken, LocalDate date) {

        if (dam.getUpstreamId() == null && dam.getDownstreamId() == null) {
            log.warn("Barragem {} não possui IDs de montante nem jusante. Ignorando.", dam.getName());
            return;
        }

        boolean readingExists = hydrotelemetricReadingRepository.existsByDamIdAndDate(dam.getId(), date);
        if (readingExists) {
            log.info("Já existe leitura para a barragem {} na data {}. Ignorando.", dam.getName(), date);
            return;
        }

        try {
            Double upstreamAverageM = null;
            Double downstreamAverageM = null;

            if (dam.getUpstreamId() != null) {
                String upstreamId = String.valueOf(dam.getUpstreamId());
                List<TelemetryItem> upstreamData = anaApiService.getTelemetryData(upstreamId, authToken);
                Double upstreamAverageMm = anaApiService.calculateAverageLevel(upstreamData, date);
                upstreamAverageM = upstreamAverageMm != null ? upstreamAverageMm / 1000.0 : null;
            }

            if (dam.getDownstreamId() != null) {
                String downstreamId = String.valueOf(dam.getDownstreamId());
                List<TelemetryItem> downstreamData = anaApiService.getTelemetryData(downstreamId, authToken);
                Double downstreamAverageMm = anaApiService.calculateAverageLevel(downstreamData, date);
                downstreamAverageM = downstreamAverageMm != null ? downstreamAverageMm / 1000.0 : null;
            }

            HydrotelemetricReadingEntity reading = new HydrotelemetricReadingEntity();
            reading.setDam(dam);
            reading.setDate(date);
            reading.setUpstreamAverage(upstreamAverageM);
            reading.setDownstreamAverage(downstreamAverageM);
            reading.setReadingType(ReadingTypeEnum.ANA);

            hydrotelemetricReadingRepository.save(reading);

            if (upstreamAverageM == null && downstreamAverageM == null) {
                log.warn("Leitura hidrotelemétrica salva com valores nulos para barragem: {}", dam.getName());
            } else {
                log.info("Leitura hidrotelemétrica salva com sucesso para barragem: {}", dam.getName());
            }

        } catch (Exception e) {
            log.error("Erro ao coletar dados para barragem {}: {}", dam.getName(), e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void collectDataManually() {
        collectHydrotelemetricData();
    }
}
