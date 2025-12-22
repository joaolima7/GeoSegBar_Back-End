package com.geosegbar.configs.metrics;

import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PerformanceInterceptor implements HandlerInterceptor {

    private final MeterRegistry meterRegistry;
    private static final String TIMER_ATTRIBUTE = "requestTimer";
    private static final String START_TIME_ATTRIBUTE = "startTime";

    public PerformanceInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {
        Timer.Sample sample = Timer.start(meterRegistry);
        request.setAttribute(TIMER_ATTRIBUTE, sample);
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex) {
        Timer.Sample sample = (Timer.Sample) request.getAttribute(TIMER_ATTRIBUTE);
        Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);

        if (sample != null) {
            String endpoint = normalizeEndpoint(request.getRequestURI());
            String method = request.getMethod();
            String status = String.valueOf(response.getStatus());

            sample.stop(Timer.builder("http.server.requests.custom")
                    .description("Tempo de resposta HTTP detalhado")
                    .tag("method", method)
                    .tag("endpoint", endpoint)
                    .tag("status", status)
                    .tag("success", isSuccessStatus(response.getStatus()) ? "true" : "false")
                    .register(meterRegistry));

            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                if (duration > 2000) {
                    log.warn("⚠️  Slow request detected: {} {} - {}ms",
                            method, endpoint, duration);
                }
            }
        }
    }

    private String normalizeEndpoint(String uri) {

        return uri.replaceAll("/\\d+", "/{id}")
                .replaceAll("/[0-9a-f-]{36}", "/{uuid}");
    }

    private boolean isSuccessStatus(int status) {
        return status >= 200 && status < 400;
    }
}
