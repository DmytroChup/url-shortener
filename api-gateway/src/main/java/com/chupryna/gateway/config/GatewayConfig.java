package com.chupryna.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class GatewayConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.justOrEmpty(exchange.getRequest().getRemoteAddress())
                .map(InetSocketAddress::getHostString)
                .defaultIfEmpty("anonymous");
    }

    @Bean
    public RedisRateLimiter writeRateLimiter() {
        return new RedisRateLimiter(1, 10);
    }

    @Bean
    @Primary
    public RedisRateLimiter readRateLimiter() {
        return new RedisRateLimiter(10, 100);
    }
}
