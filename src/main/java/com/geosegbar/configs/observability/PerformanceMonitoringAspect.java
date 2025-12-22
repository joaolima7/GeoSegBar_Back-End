package com.geosegbar.configs.observability;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class PerformanceMonitoringAspect {

    private final MeterRegistry meterRegistry;

    private final Map<String, Timer> timerCache = new ConcurrentHashMap<>();

    /**
     * ⭐ Monitora TODOS os métodos de Service
     */
    @Around("execution(* com.geosegbar..services..*(..))")
    public Object monitorServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return trackPerformance(joinPoint, "service");
    }

    /**
     * ⭐ Monitora TODOS os métodos de Repository
     */
    @Around("execution(* com.geosegbar..persistence.jpa..*(..))")
    public Object monitorRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return trackPerformance(joinPoint, "repository");
    }

    /**
     * ⭐ Monitora TODOS os Controllers
     */
    @Around("execution(* com.geosegbar..web..*(..))")
    public Object monitorControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return trackPerformance(joinPoint, "controller");
    }

    /**
     * ⭐ Monitora métodos anotados com @Timed customizado
     */
    @Around("@annotation(timed)")
    public Object monitorTimedMethods(ProceedingJoinPoint joinPoint, Timed timed) throws Throwable {
        return trackPerformance(joinPoint, timed.value());
    }

    private Object trackPerformance(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String metricName = String.format("%s.%s.%s", layer, className, methodName);

        Timer timer = timerCache.computeIfAbsent(metricName, name
                -> Timer.builder(name)
                        .description("Execution time for " + name)
                        .tag("layer", layer)
                        .tag("class", className)
                        .tag("method", methodName)
                        .publishPercentiles(0.5, 0.95, 0.99)
                        .publishPercentileHistogram()
                        .register(meterRegistry)
        );

        long startTime = System.nanoTime();
        Object result = null;
        Throwable exception = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            exception = t;
            throw t;
        } finally {
            long duration = System.nanoTime() - startTime;
            long durationMs = TimeUnit.NANOSECONDS.toMillis(duration);

            timer.record(duration, TimeUnit.NANOSECONDS);

            String status = exception == null ? "SUCCESS" : "ERROR";
            String logLevel = determineLogLevel(durationMs);

            if ("ERROR".equals(logLevel) || "WARN".equals(logLevel)) {
                log.warn("⚠️ SLOW {} | {}.{} | {}ms | status={} | args={}",
                        layer.toUpperCase(),
                        className,
                        methodName,
                        durationMs,
                        status,
                        formatArgs(joinPoint.getArgs()));
            } else if (log.isDebugEnabled()) {
                log.debug("✅ {} | {}.{} | {}ms | status={}",
                        layer.toUpperCase(),
                        className,
                        methodName,
                        durationMs,
                        status);
            }

            if (durationMs > 500) {
                meterRegistry.counter("slow.methods",
                        "layer", layer,
                        "class", className,
                        "method", methodName).increment();
            }
        }
    }

    private String determineLogLevel(long durationMs) {
        if (durationMs > 2000) {
            return "ERROR";
        }
        if (durationMs > 500) {
            return "WARN";
        }
        return "INFO";
    }

    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length && i < 5; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            Object arg = args[i];
            if (arg == null) {
                sb.append("null");
            } else {
                String argStr = arg.toString();
                sb.append(argStr.length() > 50 ? argStr.substring(0, 47) + "..." : argStr);
            }
        }
        if (args.length > 5) {
            sb.append(", ... +").append(args.length - 5).append(" more");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * ⭐ Anotação customizada para marcar métodos específicos
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Timed {

        String value() default "custom";
    }
}
