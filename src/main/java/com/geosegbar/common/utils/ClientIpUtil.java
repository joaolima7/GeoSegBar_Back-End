package com.geosegbar.common.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utilitário para extrair o endereço IP real do cliente, considerando proxies e
 * load balancers.
 * <p>
 * Verifica os headers na seguinte ordem: 1. X-Forwarded-For (padrão de proxies)
 * 2. X-Real-IP (Nginx) 3. RemoteAddr (conexão direta).
 */
public final class ClientIpUtil {

    private ClientIpUtil() {
    }

    public static String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String ip = request.getHeader("X-Forwarded-For");

        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For pode conter múltiplos IPs separados por vírgula.
            // O primeiro é o IP real do cliente.
            int commaIndex = ip.indexOf(',');
            if (commaIndex > 0) {
                ip = ip.substring(0, commaIndex).trim();
            }
            return ip;
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        // Fallback para RemoteAddr
        ip = request.getRemoteAddr();

        // Normaliza localhost IPv6 para IPv4
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip;
    }
}
