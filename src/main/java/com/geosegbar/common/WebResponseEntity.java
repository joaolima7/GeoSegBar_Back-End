package com.geosegbar.common;

public class WebResponseEntity<T> {
    private boolean success;
    private String message;
    private T data;


    public WebResponseEntity(boolean success, String message, T data) {
        this.success = success;       
        this.message = message;
        this.data = data;
    }

    public static <T> WebResponseEntity<T> success(T data, String message) {
        return new WebResponseEntity<>(true, message, data);
    }

    public static <T> WebResponseEntity<T> error(String message) {
        return new WebResponseEntity<>(false, message, null);
    }

    public boolean isSuccess() { return success; }
    public T getData() { return data; }
    public String getMessage() { return message; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setData(T data) { this.data = data; }
    public void setMessage(String message) { this.message = message; }
}

