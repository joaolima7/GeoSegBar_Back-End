package com.geosegbar.configs.metrics;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MetricsScheduler {

    private final CustomMetricsService metricsService;

    public MetricsScheduler(CustomMetricsService metricsService) {
        this.metricsService = metricsService;
    }

    /**
     * Reseta métricas diárias à meia-noite (00:00:00)
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "America/Sao_Paulo")
    public void resetDailyMetrics() {
        log.info("🔄 Executando reset de métricas diárias...");
        metricsService.resetDailyReadings();
        log.info("✅ Métricas diárias resetadas com sucesso");
    }

    /**
     * Atualiza gauge de instrumentos ativos a cada 5 minutos (você pode
     * implementar a lógica de contagem real aqui)
     */
    @Scheduled(fixedDelay = 300000)
    public void updateActiveInstrumentsGauge() {

    }
}
