package com.geosegbar.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.geosegbar.common.enums.AuthErrorCodeEnum;

public class WebResponseEntity<T> {

    private boolean success;
    private String message;
    private T data;

    /**
     * Presente apenas em respostas 401 e 403. Omitido do JSON quando nulo, de
     * modo que nenhuma resposta existente muda de formato.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorCode;

    public WebResponseEntity(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public WebResponseEntity(boolean success, String message, T data, String errorCode) {
        this(success, message, data);
        this.errorCode = errorCode;
    }

    public static <T> WebResponseEntity<T> success(T data, String message) {
        return new WebResponseEntity<>(true, message, data);
    }

    public static <T> WebResponseEntity<T> error(String message) {
        return new WebResponseEntity<>(false, message, null);
    }

    /**
     * Erro de autenticação (401) ou de permissão (403), com o código que diz ao
     * front se ele deve deslogar o usuário ou apenas exibir o aviso.
     */
    public static <T> WebResponseEntity<T> error(String message, AuthErrorCodeEnum errorCode) {
        return new WebResponseEntity<>(false, message, null, errorCode.name());
    }

    public static <T> WebResponseEntity<T> errorValidation(String message, T data) {
        return new WebResponseEntity<>(false, message, data);
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setData(T data) {
        this.data = data;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
