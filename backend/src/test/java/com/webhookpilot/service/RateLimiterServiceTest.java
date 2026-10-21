package com.webhookpilot.service;

import com.webhookpilot.config.RateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private RateLimiterService rateLimiterService;
    private RateLimitProperties properties;
    private UUID appId;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setMaxEventsPerMinute(100);
        rateLimiterService = new RateLimiterService(redisTemplate, properties);
        appId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("allowEvent returns true when under limit")
    void allowEvent_underLimit() {
        when(valueOps.increment(anyString())).thenReturn(50L);

        assertTrue(rateLimiterService.allowEvent(appId));
    }

    @Test
    @DisplayName("allowEvent returns true at exactly the limit")
    void allowEvent_atLimit() {
        when(valueOps.increment(anyString())).thenReturn(100L);

        assertTrue(rateLimiterService.allowEvent(appId));
    }

    @Test
    @DisplayName("allowEvent returns false when over limit")
    void allowEvent_overLimit() {
        when(valueOps.increment(anyString())).thenReturn(101L);

        assertFalse(rateLimiterService.allowEvent(appId));
    }

    @Test
    @DisplayName("allowEvent sets TTL on first request (count == 1)")
    void allowEvent_setsTtlOnFirst() {
        when(valueOps.increment(anyString())).thenReturn(1L);

        rateLimiterService.allowEvent(appId);

        verify(redisTemplate).expire(anyString(), any());
    }

    @Test
    @DisplayName("allowEvent does not set TTL on subsequent requests")
    void allowEvent_noTtlOnSubsequent() {
        when(valueOps.increment(anyString())).thenReturn(50L);

        rateLimiterService.allowEvent(appId);

        verify(redisTemplate, never()).expire(anyString(), any());
    }
}
