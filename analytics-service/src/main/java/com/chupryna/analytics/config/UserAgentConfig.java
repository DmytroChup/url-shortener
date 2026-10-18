package com.chupryna.analytics.config;

import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserAgentConfig {

    @Bean
    public UserAgentAnalyzer userAgentAnalyzer() {
        return UserAgentAnalyzer.newBuilder()
                .hideMatcherLoadStats()
                .withField(UserAgent.DEVICE_CLASS)
                .withField(UserAgent.OPERATING_SYSTEM_NAME)
                .withField(UserAgent.AGENT_NAME)
                .withCache(10_000)
                .build();
    }
}
