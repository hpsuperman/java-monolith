package com.hpsuperman.monolith.common.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret;

    private Duration accessTokenTtl = Duration.ofMinutes(30);

    private Duration refreshTokenTtl = Duration.ofDays(7);

    private String issuer = "java-monolith";
}
