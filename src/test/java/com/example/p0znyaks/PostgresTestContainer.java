package com.example.p0znyaks;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

/**
 * Shared PostgreSQL container so the whole test suite starts a single database.
 * <p>
 * The container is started lazily on first access and stopped by a JVM shutdown hook.
 * <p>
 * The image, database name and credentials are read from the {@code .env} file in the project
 * root, so {@code .env} stays the single source of truth for the whole project.
 */
public final class PostgresTestContainer {

    private static final Properties ENV = loadEnv();

    private static final PostgreSQLContainer<?> INSTANCE = new PostgreSQLContainer<>(
            DockerImageName.parse(ENV.getProperty("POSTGRES_IMAGE", "postgres:17-alpine")))
            .withDatabaseName(ENV.getProperty("POSTGRES_DB", "binance"))
            .withUsername(ENV.getProperty("POSTGRES_USER", "binance"))
            .withPassword(ENV.getProperty("POSTGRES_PASSWORD", "binance"))
            .withStartupTimeout(Duration.ofMinutes(3));

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(INSTANCE::stop, "postgres-testcontainer-stop"));
    }

    private PostgresTestContainer() {
    }

    /**
     * Reads the {@code .env} file in the project root. Missing file or missing key
     * simply means the built-in fallback is used.
     */
    private static Properties loadEnv() {
        Properties properties = new Properties();
        Path envFile = Path.of(".env");
        if (!Files.exists(envFile)) {
            return properties;
        }
        try (var lines = Files.lines(envFile)) {
            lines.filter(line -> !line.isBlank() && !line.stripLeading().startsWith("#"))
                    .map(line -> line.split("=", 2))
                    .filter(parts -> parts.length == 2)
                    .forEach(parts -> properties.setProperty(parts[0].strip(), parts[1].strip()));
        } catch (IOException e) {
            // fall back to the built-in defaults
        }
        return properties;
    }

    /**
     * @return the running shared container, or {@code null} when Docker is unavailable.
     */
    public static PostgreSQLContainer<?> getInstance() {
        if (!DockerClientFactory.instance().isDockerAvailable()) {
            return null;
        }
        if (!INSTANCE.isRunning()) {
            INSTANCE.start();
        }
        return INSTANCE;
    }

    /**
     * JUnit condition that skips a test class when no Docker daemon is reachable.
     */
    public static final class DockerAvailableCondition implements ExecutionCondition {

        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            if (DockerClientFactory.instance().isDockerAvailable()) {
                return ConditionEvaluationResult.enabled("Docker is available");
            }
            return ConditionEvaluationResult.disabled("Docker is not available");
        }
    }
}
