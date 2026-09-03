package com.geosegbar.infra.mobile_dashboard.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.infra.mobile_dashboard.dtos.MobileDashboardDTO;
import com.geosegbar.infra.mobile_dashboard.services.MobileDashboardService;

import lombok.RequiredArgsConstructor;

/**
 * Rotas do aplicativo de campo. Namespace separado de propósito: aqui o
 * recorte de barragem vem sempre do token, nunca do cliente, e nenhuma rota
 * daqui é consumida pela web — o que torna qualquer mudança neste arquivo
 * incapaz de quebrar o painel web.
 */
@RestController
@RequestMapping("/mobile")
@RequiredArgsConstructor
public class MobileDashboardController {

    private static final int DEFAULT_MONTHS = 6;
    private static final int MIN_MONTHS = 1;
    private static final int MAX_MONTHS = 12;

    private static final int DEFAULT_CRITICAL_LIMIT = 5;
    private static final int MIN_CRITICAL_LIMIT = 1;
    private static final int MAX_CRITICAL_LIMIT = 25;

    private final MobileDashboardService mobileDashboardService;

    /**
     * O painel inteiro em uma chamada.
     *
     * Sem damIds: o recorte deriva do token. Usuário sem barragem liberada
     * recebe 200 com tudo zerado — nunca 403, porque um 403 aqui apagaria a
     * tela inicial de quem só está esperando o cadastro sair.
     *
     * Os dois parâmetros são presos entre limites no servidor em vez de
     * confiar no cliente: months manda no tamanho da série e criticalLimit no
     * tamanho da lista, e são as duas únicas coisas que poderiam fazer a
     * resposta crescer.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<WebResponseEntity<MobileDashboardDTO>> getDashboard(
            @RequestParam(required = false) Integer months,
            @RequestParam(required = false) Integer criticalLimit) {

        int resolvedMonths = clamp(months, DEFAULT_MONTHS, MIN_MONTHS, MAX_MONTHS);
        int resolvedCriticalLimit
                = clamp(criticalLimit, DEFAULT_CRITICAL_LIMIT, MIN_CRITICAL_LIMIT, MAX_CRITICAL_LIMIT);

        MobileDashboardDTO dashboard
                = mobileDashboardService.build(resolvedMonths, resolvedCriticalLimit);

        return ResponseEntity.ok(
                WebResponseEntity.success(dashboard, "Painel do aplicativo obtido com sucesso!"));
    }

    private int clamp(Integer value, int fallback, int min, int max) {
        if (value == null) {
            return fallback;
        }
        return Math.min(Math.max(value, min), max);
    }
}
