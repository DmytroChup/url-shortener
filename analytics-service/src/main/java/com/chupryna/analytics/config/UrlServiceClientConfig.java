package com.chupryna.analytics.config;

import com.chupryna.analytics.config.properties.UrlServiceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableCaching
@EnableConfigurationProperties(UrlServiceProperties.class)
public class UrlServiceClientConfig {

    @Bean
    public RestClient urlServiceRestClient(UrlServiceProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
            .baseUrl(properties.url())
            .requestFactory(factory)
            .build();
    }
}
