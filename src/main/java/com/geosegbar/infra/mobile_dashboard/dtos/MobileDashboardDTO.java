package com.geosegbar.infra.mobile_dashboard.dtos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * O painel do aplicativo, em uma resposta só.
 *
 * Recorte: as barragens que o usuário do token pode acessar. Nenhum parâmetro
 * de barragem é aceito, de propósito — o app é offline-first e trabalha com
 * permissão cacheada, então uma lista de ids vinda do aparelho está sempre a um
 * passo de estar defasada. As rotas de /dashboard, que a web usa, exigem
 * damIds e devolvem 403 se UMA barragem da lista não for permitida; aqui isso é
 * impossível, porque o recorte nasce no servidor.
 *
 * Tudo aqui é agregado no banco. São nove consultas de GROUP BY sobre colunas
 * indexadas; nenhuma linha de leitura, resposta ou anomalia trafega.
 *
 * Datas seguem a convenção da API: ISO-8601 sem fuso, hora local do servidor.
 */
public record MobileDashboardDTO(
        LocalDateTime generatedAt,
        LocalDate periodStart,
        LocalDate periodEnd,
        Scope scope,
        Kpis kpis,
        List<ActivityPoint> activityByMonth,
        List<DamInspection> inspectionsByDam,
        List<CategoryCount> instrumentStatus,
        List<TypeCount> instrumentsByType,
        List<TypeCount> myReadingsByType,
        List<CategoryCount> anomaliesByDangerLevel,
        List<CriticalInstrument> criticalInstruments) {

    /**
     * O tamanho do mundo do usuário. Serve para o app dizer "5 barragens · 40
     * instrumentos" sem uma segunda chamada.
     */
    public record Scope(
            long damsTotal,
            long instrumentsTotal,
            List<Long> damIds) {

    }

    /**
     * Os números do topo. "my*" é o trabalho do próprio usuário; os demais são
     * o estado das barragens dele, independente de quem registrou.
     */
    public record Kpis(
            long myChecklistsThisMonth,
            long myReadingsThisMonth,
            long myChecklistsInPeriod,
            long myReadingsInPeriod,
            long damsInspectedThisMonth,
            long damsTotal,
            long instrumentsTotal,
            long instrumentsWithReading,
            long instrumentsWithoutReading,
            long instrumentsAttention,
            long instrumentsCritical,
            long anomaliesInPeriod) {

    }

    /**
     * Um ponto da série "minha atividade". O mês vem pronto como "YYYY-MM"
     * para o app não ter que reconstruir bucket nenhum; monthStart existe para
     * quem quiser formatar o rótulo no idioma do aparelho.
     *
     * Meses sem trabalho vêm com zero — a série é densa, nunca esburacada, de
     * forma que o gráfico de linha não invente interpolação.
     */
    public record ActivityPoint(
            String month,
            LocalDate monthStart,
            long checklists,
            long readings) {

    }

    /**
     * Uma barragem acessível. lastInspectionAt nulo = nunca inspecionada.
     */
    public record DamInspection(
            Long damId,
            String damName,
            long responsesInPeriod,
            LocalDateTime lastInspectionAt,
            boolean inspectedThisMonth) {

    }

    public record CategoryCount(
            String name,
            long count,
            double percentage) {

    }

    public record TypeCount(
            Long typeId,
            String typeName,
            long total) {

    }

    public record CriticalInstrument(
            Long instrumentId,
            String instrumentName,
            Long damId,
            String damName,
            String limitStatus,
            LocalDate lastReadingDate) {

    }

    /**
     * A resposta de quem não tem nenhuma barragem liberada. 200 com tudo
     * zerado, nunca 403 — o app precisa distinguir "sem acesso a nada" de
     * "erro", e um 403 aqui derrubaria a tela inteira.
     */
    public static MobileDashboardDTO empty(
            LocalDate periodStart, LocalDate periodEnd, List<ActivityPoint> emptySeries) {
        return new MobileDashboardDTO(
                LocalDateTime.now(),
                periodStart,
                periodEnd,
                new Scope(0L, 0L, List.of()),
                new Kpis(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                emptySeries,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of());
    }
}
