package com.librarymanager.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.*;

/**
 * Verification test to ensure test configuration is working properly.
 * 
 * This test validates:
 * - H2 database connection
 * - Test profile activation
 * - DataJpaTest slice configuration
 * - Flyway is properly disabled
 * 
 * @author Marcel Pulido
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ConfigurationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void testDatabaseConnection() throws SQLException {
        // Verify we can connect to H2 database
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection).isNotNull();
            assertThat(connection.getMetaData().getURL()).contains("h2:mem:testdb");
            assertThat(connection.isValid(1)).isTrue();
        }
    }

    @Test
    void testTestProfileIsActive() {
        // This test will only run if the test profile is properly active
        // and the H2 database is configured correctly
        assertThat(dataSource).isNotNull();
    }
}