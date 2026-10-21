package com.webhookpilot.service;

import com.webhookpilot.config.RateLimitProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis-backed sliding window rate limiter for event ingestion.
 *
 * Each application gets a fixed number of events per minute (default: 100).
 * Uses a simple Redis counter with a 60-second TTL.
 *
 * Why rate limiting on ingestion?
 * - Prevents a single application from flooding the system
 * - Protects downstream Kafka and delivery workers from bursts
 * - Standard practice in any webhook/API platform (Stripe, Twilio, GitHub all do this)
 *
 * Redis key: ratelimit:{applicationId}
 * TTL: 60 seconds (resets every minute)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;

    private static final String RATE_LIMIT_KEY_PREFIX = "ratelimit:%s";

    /**
     * Checks if the application is within its rate limit.
     * Returns true if the event is allowed, false if rate limited.
     * Automatically increments the counter on allow.
     */
    public boolean allowEvent(UUID applicationId) {
        String key = String.format(RATE_LIMIT_KEY_PREFIX, applicationId);

        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) return true;

        // Set TTL only on the first increment (when count == 1)
        if (count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(60));
        }

        if (count > properties.getMaxEventsPerMinute()) {
            log.warn("Rate limit exceeded for application {}. Count: {}/{}",
                    applicationId, count, properties.getMaxEventsPerMinute());
            return false;
        }

        return true;
    }

    /**
     * Returns the current event count for an application in the current window.
     */
    public long getCurrentCount(UUID applicationId) {
        String key = String.format(RATE_LIMIT_KEY_PREFIX, applicationId);
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : 0;
    }
}
