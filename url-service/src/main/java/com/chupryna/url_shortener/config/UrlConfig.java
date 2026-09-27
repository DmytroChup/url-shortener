package com.chupryna.url_shortener.config;

import com.chupryna.url_shortener.config.properties.UrlProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(UrlProperties.class)
public class UrlConfig {
}
