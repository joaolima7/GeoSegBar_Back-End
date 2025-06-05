package com.geosegbar.common.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnaAuthResponse {

    private String status;
    private int code;
    private String message;
    private AuthItems items;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthItems {

        private boolean sucesso;
        private String token;
        private String validade;
        private String tokenautenticacao;
        private String respostaautenticacao;
    }
}
