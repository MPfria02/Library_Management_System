package com.librarymanager.backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for integration tests using Testcontainers.
 *
 * Features:
 * - Boots the full Spring context (with HTTP layer).
 * - Spins up a real PostgreSQL Testcontainer before tests.
 * - Automatically injects container DB credentials into Spring Boot.
 * - Uses "test" profile for consistency with application-test.properties.
 *
 * Extend this class in your integration test classes to ensure consistent setup.
 *
 * NOTE:
 * - withReuse(true) requires ~/.testcontainers.properties with:
 *      testcontainers.reuse.enable=true
 *   Otherwise, containers will be disposable per run.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integration")
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_library_manager")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    /**
     * Simple smoke test to confirm container is running
     * and wired into Spring Boot context.
     */
    @Test
    void contextLoads() {
        assertThat(postgres.isRunning()).isTrue();
    }

    protected static PostgreSQLContainer<?> getPostgresContainer() {
        return postgres;
    }

    protected static String getDatabaseUrl() {
        return postgres.getJdbcUrl();
    }

    protected static String getDatabaseUsername() {
        return postgres.getUsername();
    }

    protected static String getDatabasePassword() {
        return postgres.getPassword();
    }
}
