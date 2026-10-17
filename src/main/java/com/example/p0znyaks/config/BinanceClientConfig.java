package com.example.p0znyaks.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(BinanceProperties.class)
public class BinanceClientConfig {
    @Bean
    RestClient binanceRestClient(BinanceProperties properties) {
        var settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(properties.getConnectTimeout())
                .withReadTimeout(properties.getReadTimeout());
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
                .build();
    }
}
