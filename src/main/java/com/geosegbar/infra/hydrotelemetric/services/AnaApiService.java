package com.geosegbar.infra.hydrotelemetric.services;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geosegbar.common.response.AnaAuthResponse;
import com.geosegbar.common.response.AnaTelemetryResponse;
import com.geosegbar.common.response.AnaTelemetryResponse.TelemetryItem;
import com.geosegbar.exceptions.ExternalApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnaApiService {

    @Value("${ana.api.identifier}")
    private String apiIdentifier;

    @Value("${ana.api.password}")
    private String apiPassword;

    @Value("${ana.api.auth-url}")
    private String authUrl;

    @Value("${ana.api.telemetry-url}")
    private String telemetryUrl;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public String getAuthToken() {
        try {

            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    AnaAuthResponse response = webClient.get()
                            .uri(authUrl)
                            .header("Identificador", apiIdentifier)
                            .header("Senha", apiPassword)
                            .accept(MediaType.APPLICATION_JSON)
                            .retrieve()
                            .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                                    clientResponse -> {
                                        return clientResponse.bodyToMono(String.class)
                                                .flatMap(errorBody -> Mono.error(new ExternalApiException(
                                                "Erro ao obter token da API da ANA: " + errorBody)));
                                    })
                            .bodyToMono(AnaAuthResponse.class)
                            .block();

                    if (response == null || response.getItems() == null
                            || response.getItems().getTokenautenticacao() == null) {
                        throw new ExternalApiException("Resposta da API da ANA inválida ou sem token");
                    }

                    log.info("Token de autenticação obtido com sucesso");
                    return response.getItems().getTokenautenticacao();
                } catch (Exception e) {
                    if (attempt == 3) {
                        throw e;
                    }
                    log.warn("Tentativa {} falhou, tentando novamente em {} segundos",
                            attempt, attempt * 2);
                    Thread.sleep(attempt * 2000);
                }
            }
            throw new ExternalApiException("Todas as tentativas de obter token falharam");
        } catch (Exception e) {
            log.error("Erro ao obter token de autenticação", e);
            throw new ExternalApiException("Falha ao obter token de autenticação: " + e.getMessage(), e);
        }
    }

    public List<TelemetryItem> getTelemetryData(String stationCode, String authToken) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = telemetryUrl
                    + "?C%C3%B3digo%20da%20Esta%C3%A7%C3%A3o=" + stationCode
                    + "&Tipo%20Filtro%20Data=DATA_LEITURA"
                    + "&Range%20Intervalo%20de%20busca=DIAS_2";

            HttpGet request = new HttpGet(url);
            request.setHeader("Authorization", "Bearer " + authToken);
            request.setHeader("Accept", "*/*");
            request.setHeader("User-Agent", "Mozilla/5.0");
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Host", "www.ana.gov.br");

            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();

            if (response.getStatusLine().getStatusCode() != 200) {
                String errorBody = EntityUtils.toString(entity);
                log.error("Erro na resposta da API: {} - {}", response.getStatusLine().getStatusCode(), errorBody);
                throw new ExternalApiException("Erro ao obter dados de telemetria: " + errorBody);
            }

            String responseBody = EntityUtils.toString(entity);

            AnaTelemetryResponse telemetryResponse = objectMapper.readValue(responseBody, AnaTelemetryResponse.class);

            if (telemetryResponse == null || telemetryResponse.getItems() == null) {
                throw new ExternalApiException("Resposta da API da ANA inválida ou sem itens");
            }

            return telemetryResponse.getItems();
        } catch (IOException e) {
            log.error("Erro ao obter dados de telemetria para estação: {}", stationCode, e);
            throw new ExternalApiException("Falha ao obter dados de telemetria: " + e.getMessage(), e);
        }
    }

    public Double calculateAverageLevel(List<TelemetryItem> items, LocalDate date) {
        if (items == null || items.isEmpty()) {
            return null;
        }

        String dateStr = date.toString();

        return items.stream()
                .filter(item -> item.getDataHoraMedicao() != null && item.getDataHoraMedicao().startsWith(dateStr))
                .filter(item -> item.getCotaAdotada() != null && !item.getCotaAdotada().isEmpty())
                .mapToDouble(item -> {
                    try {
                        return Double.parseDouble(item.getCotaAdotada());
                    } catch (NumberFormatException e) {
                        log.warn("Valor de cota inválido: {}", item.getCotaAdotada());
                        return 0.0;
                    }
                })
                .average()
                .orElse(0.0);
    }
}
