package com.geosegbar.configs.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RateLimitInfo {

    private final boolean allowed;

    private final long remainingTokens;

    private final long secondsUntilRefill;

    private final long capacity;
}
