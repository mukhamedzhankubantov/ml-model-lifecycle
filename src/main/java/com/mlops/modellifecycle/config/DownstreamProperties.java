package com.mlops.modellifecycle.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.downstream")
public record DownstreamProperties(String baseUrl, Duration timeout) {
}