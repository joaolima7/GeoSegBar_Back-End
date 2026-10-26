package com.geosegbar.configs.filters;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.filter.OncePerRequestFilter;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Dá a cada requisição um código de correlação e o deixa visível no log e na
 * resposta.
 *
 * <ul>
 * <li>{@code requestId}: vem do header {@code X-Request-Id} que o nginx gera
 * para toda requisição ({@code $request_id}). A mesma chave aparece no access
 * log do nginx e no log JSON da API, então uma busca no Loki acha as duas
 * pontas. Sem header válido (chamada direta à API, testes), um novo é gerado
 * aqui.</li>
 * <li>{@code userId}: colocado no MDC pelo {@code SecurityFilter} depois da
 * autenticação; removido aqui no fim da requisição.</li>
 * <li>{@code X-Trace-Id}: o trace do Tempo desta requisição. O suporte pede esse
 * código ao usuário que reclamou de erro e abre o trace exato no Grafana. Só
 * vai na resposta quando a exportação de traces está ligada
 * (management.tracing.enabled): com ela desligada o Spring ainda cria spans e
 * ids, mas eles nunca chegam ao Tempo, e um código que não abre nada só
 * confunde o suporte.</li>
 * </ul>
 *
 * Registrado por {@code ObservabilityConfig} depois do filtro de observação
 * HTTP do Spring (é ele que abre o span) e antes da cadeia de segurança.
 */
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_USER_ID = "userId";

    /**
     * O header vem de fora (nginx, mas também de quem chamar a API direto). Só
     * entra no log se tiver cara de identificador: nada de quebra de linha nem
     * texto arbitrário injetado no log.
     */
    private static final Pattern VALID_REQUEST_ID = Pattern.compile("^[A-Za-z0-9-]{8,64}$");

    private final ObjectProvider<Tracer> tracerProvider;
    private final boolean traceExportEnabled;

    public RequestCorrelationFilter(ObjectProvider<Tracer> tracerProvider, boolean traceExportEnabled) {
        this.tracerProvider = tracerProvider;
        this.traceExportEnabled = traceExportEnabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER));
        MDC.put(MDC_REQUEST_ID, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        String traceId = traceExportEnabled ? currentTraceId() : null;
        if (traceId != null) {
            response.setHeader(TRACE_ID_HEADER, traceId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_USER_ID);
        }
    }

    static String resolveRequestId(String header) {
        if (header != null && VALID_REQUEST_ID.matcher(header).matches()) {
            return header;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String currentTraceId() {
        Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer == null) {
            return null;
        }
        Span span = tracer.currentSpan();
        if (span == null) {
            return null;
        }
        String traceId = span.context().traceId();
        // Tracer NOOP (sem a ponte do OpenTelemetry no classpath) devolve id vazio.
        return traceId == null || traceId.isBlank() ? null : traceId;
    }
}
