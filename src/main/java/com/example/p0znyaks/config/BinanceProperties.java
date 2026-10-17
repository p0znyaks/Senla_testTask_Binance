package com.example.p0znyaks.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "binance")
public class BinanceProperties {
    private String baseUrl;
    private String symbol;
    private Duration pollInterval;
    private Duration connectTimeout;
    private Duration readTimeout;
}
