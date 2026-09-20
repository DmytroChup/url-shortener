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

    private static final String KEY_PREFIX = "rate-limit:";

    private final ProxyManager<String> proxyManager;

    private static BucketConfiguration createBucketConfiguration()  {
        Bandwidth limit = Bandwidth.builder()
                .capacity(10L)
                .refillGreedy(10L, Duration.ofMinutes(1L))
                .build();

        return BucketConfiguration.builder()
                .addLimit(limit)
                .build();
    }

    public boolean tryConsume(String ip) {
        Bucket bucket = proxyManager.builder().build(KEY_PREFIX + ip, RateLimitingService::createBucketConfiguration);
        return bucket.tryConsume(1);
    }
}
