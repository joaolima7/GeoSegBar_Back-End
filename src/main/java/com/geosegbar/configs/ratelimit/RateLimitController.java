package com.geosegbar.configs.ratelimit;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rate-limit")
@RequiredArgsConstructor
public class RateLimitController {

    private final RateLimitService rateLimitService;

    @GetMapping("/status")
    public ResponseEntity<WebResponseEntity<Map<String, Object>>> getStatus() {
        Map<String, Object> stats = rateLimitService.getStatistics();
        return ResponseEntity.ok(WebResponseEntity.success(stats, "Status do rate limiting"));
    }
}
