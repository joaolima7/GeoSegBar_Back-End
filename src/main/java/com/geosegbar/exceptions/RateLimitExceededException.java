package com.geosegbar.exceptions;

/**
 * Exceção lançada quando o limite de requisições (rate limit) é excedido. Esta
 * exceção contém informações sobre o limite, tokens restantes e tempo para
 * renovação.
 */
public class RateLimitExceededException extends RuntimeException {

    private final long remainingTokens;
    private final long secondsUntilRefill;
    private final long capacity;

    public RateLimitExceededException(String message, long remainingTokens, long secondsUntilRefill, long capacity) {
        super(message);
        this.remainingTokens = remainingTokens;
        this.secondsUntilRefill = secondsUntilRefill;
        this.capacity = capacity;
    }

    public long getRemainingTokens() {
        return remainingTokens;
    }

    public long getSecondsUntilRefill() {
        return secondsUntilRefill;
    }

    public long getCapacity() {
        return capacity;
    }
}
