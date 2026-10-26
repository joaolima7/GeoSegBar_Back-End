package com.geosegbar.unit.configs.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.configs.filters.RequestCorrelationFilter;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;

@Tag("unit")
@DisplayName("Unit Tests - RequestCorrelationFilter")
class RequestCorrelationFilterTest extends BaseUnitTest {

    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<Tracer> provider(Tracer tracer) {
        ObjectProvider<Tracer> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(tracer);
        return provider;
    }

    private static Tracer tracerComTrace(String traceId) {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext context = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(context);
        when(context.traceId()).thenReturn(traceId);
        return tracer;
    }

    @Test
    @DisplayName("Reaproveita o X-Request-Id do nginx, no MDC e na resposta")
    void reaproveitaRequestIdValido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dams");
        request.addHeader("X-Request-Id", "3f2a9c1e5b7d4e8fa1b2c3d4e5f60718");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDuranteACadeia = new AtomicReference<>();
        FilterChain chain = (req, res) -> mdcDuranteACadeia.set(MDC.get("requestId"));

        new RequestCorrelationFilter(provider(null), true).doFilter(request, response, chain);

        assertThat(mdcDuranteACadeia.get()).isEqualTo("3f2a9c1e5b7d4e8fa1b2c3d4e5f60718");
        assertThat(response.getHeader("X-Request-Id")).isEqualTo("3f2a9c1e5b7d4e8fa1b2c3d4e5f60718");
    }

    @Test
    @DisplayName("Header com quebra de linha ou texto livre não entra no log: gera outro id")
    void recusaRequestIdInvalido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dams");
        request.addHeader("X-Request-Id", "abc\n[ERROR] linha forjada");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDuranteACadeia = new AtomicReference<>();
        FilterChain chain = (req, res) -> mdcDuranteACadeia.set(MDC.get("requestId"));

        new RequestCorrelationFilter(provider(null), true).doFilter(request, response, chain);

        assertThat(mdcDuranteACadeia.get()).matches("^[0-9a-f]{32}$");
        assertThat(response.getHeader("X-Request-Id")).isEqualTo(mdcDuranteACadeia.get());
    }

    @Test
    @DisplayName("Limpa requestId e userId do MDC no fim, mesmo com exceção")
    void limpaMdcNoFim() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dams");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            MDC.put("userId", "42");
            throw new IllegalStateException("falha no controller");
        };

        try {
            new RequestCorrelationFilter(provider(null), true).doFilter(request, response, chain);
        } catch (Exception esperado) {
            // a exceção sobe intacta; o que importa é o MDC limpo
        }

        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    @DisplayName("Devolve X-Trace-Id quando há span ativo")
    void devolveTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dams");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RequestCorrelationFilter(provider(tracerComTrace("4bf92f3577b34da6a3ce929d0e0e4736")), true)
                .doFilter(request, response, (req, res) -> {
                });

        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
    }

    @Test
    @DisplayName("Com a exportação de traces desligada não há X-Trace-Id, mesmo com span ativo")
    void semTraceIdQuandoExportacaoDesligada() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dams");
        MockHttpServletResponse response = new MockHttpServletResponse();

        @SuppressWarnings("unchecked")
        ObjectProvider<Tracer> provider = mock(ObjectProvider.class);

        new RequestCorrelationFilter(provider, false).doFilter(request, response, (req, res) -> {
        });

        assertThat(response.getHeader("X-Trace-Id")).isNull();
        // Nem consulta o tracer: o id nunca existiria no Tempo.
        verifyNoInteractions(provider);
    }

    @Test
    @DisplayName("Sem tracing (Tracer NOOP devolve id vazio) não há X-Trace-Id")
    void semTraceIdQuandoTracingDesligado() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dams");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RequestCorrelationFilter(provider(tracerComTrace("")), true).doFilter(request, response, (req, res) -> {
        });

        assertThat(response.getHeader("X-Trace-Id")).isNull();
    }
}
