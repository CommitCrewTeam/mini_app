package com.miniecommerce.auth.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.refresh-token")
public record RefreshTokenProperties(Duration ttl) {}
