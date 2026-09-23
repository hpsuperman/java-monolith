package com.hpsuperman.monolith.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {
    private List<String> allowedOrigins = List.of("*");

    private List<String> allowedMethods = List.of("*");

    private List<String> allowedHeaders = List.of("*");

    private List<String> exposedHeaders = List.of();

    private boolean allowCredentials = false;

    private Duration maxAge = Duration.ofHours(1);
}
