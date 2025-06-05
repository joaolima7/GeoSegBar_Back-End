package com.geosegbar.common.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnaTelemetryResponse {

    private String status;
    private int code;
    private String message;
    private List<TelemetryItem> items;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TelemetryItem {

        @JsonProperty("Chuva_Adotada")
        private String chuvaAdotada;

        @JsonProperty("Chuva_Adotada_Status")
        private String chuvaAdotadaStatus;

        @JsonProperty("Cota_Adotada")
        private String cotaAdotada;

        @JsonProperty("Cota_Adotada_Status")
        private String cotaAdotadaStatus;

        @JsonProperty("Data_Atualizacao")
        private String dataAtualizacao;

        @JsonProperty("Data_Hora_Medicao")
        private String dataHoraMedicao;

        @JsonProperty("Vazao_Adotada")
        private String vazaoAdotada;

        @JsonProperty("Vazao_Adotada_Status")
        private String vazaoAdotadaStatus;

        @JsonProperty("codigoestacao")
        private String codigoEstacao;
    }
}
