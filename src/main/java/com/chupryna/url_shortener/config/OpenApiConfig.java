package com.chupryna.url_shortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("URL Shortener API")
                        .version("1.0.0")
                        .description("High-performance URL shortening service built with Spring Boot 4," +
                                "PostgreSQL, and Redis cache.")
                        .contact(new Contact()
                                .name("Dmytro")
                                .url("https://github.com/DmytroChup")));
    }
}
