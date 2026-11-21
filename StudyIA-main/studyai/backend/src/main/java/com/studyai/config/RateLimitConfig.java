package com.studyai.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration et stockage en mémoire des buckets Bucket4j pour le rate limiting.
 */
@Component
public class RateLimitConfig {

    @Value("${app.rate-limit.login-capacity}")
    private int loginCapacity;

    @Value("${app.rate-limit.login-refill-minutes}")
    private int loginRefillMinutes;

    /** Buckets par IP pour le rate limiting du login */
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();

    public Bucket getLoginBucket(String ip) {
        return loginBuckets.computeIfAbsent(ip, k -> createLoginBucket());
    }

    private Bucket createLoginBucket() {
        Refill refill = Refill.intervally(loginCapacity, Duration.ofMinutes(loginRefillMinutes));
        Bandwidth limit = Bandwidth.classic(loginCapacity, refill);
        return Bucket.builder().addLimit(limit).build();
    }
}
