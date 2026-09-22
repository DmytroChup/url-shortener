package com.chupryna.url_shortener.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private static final String WRITE_KEY_PREFIX = "rate-limit:write:";
    private static final String READ_KEY_PREFIX = "rate-limit:read:";

    private final ProxyManager<String> proxyManager;

    private static BucketConfiguration writeConfiguration()  {
        Bandwidth limit = Bandwidth.builder()
                .capacity(10L)
                .refillGreedy(10L, Duration.ofMinutes(1L))
                .build();

        return BucketConfiguration.builder()
                .addLimit(limit)
                .build();
    }

    private static BucketConfiguration readConfiguration()  {
        Bandwidth limit = Bandwidth.builder()
                .capacity(100L)
                .refillGreedy(100L, Duration.ofMinutes(1L))
                .build();

        return BucketConfiguration.builder()
                .addLimit(limit)
                .build();
    }

    public boolean tryConsumeWrite(String ip) {
        Bucket bucket = proxyManager.builder().build(WRITE_KEY_PREFIX + ip,RateLimitingService::writeConfiguration);
        return bucket.tryConsume(1);
    }

    public boolean tryConsumeRead(String ip) {
        Bucket bucket = proxyManager.builder().build(READ_KEY_PREFIX + ip, RateLimitingService::readConfiguration);
        return bucket.tryConsume(1);
    }
}
