package com.example.p0znyaks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(properties = "binance.scheduler.enabled=false")
@ExtendWith(PostgresTestContainer.DockerAvailableCondition.class)
class SenlaTestTaskBinanceApplicationTests {

    static PostgreSQLContainer<?> postgres;

    @DynamicPropertySource
    static void springProperties(DynamicPropertyRegistry registry) {
        postgres = PostgresTestContainer.getInstance();
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
        registry.add("spring.datasource.username", () -> postgres.getUsername());
        registry.add("spring.datasource.password", () -> postgres.getPassword());
    }

    @Test
    void contextLoads() {
    }

}
