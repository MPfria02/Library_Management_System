package com.librarymanager.backend.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(Environment environment) {
        return flyway -> {
            boolean isIntegrationProfile = environment.matchesProfiles("integration");
            boolean isTestProfile = environment.matchesProfiles("test");

            if (isIntegrationProfile) {
                // ✅ Run only schema migrations for intgration profile
                Flyway.configure()
                        .dataSource(flyway.getConfiguration().getDataSource())
                        .locations("classpath:db/migration/schema")
                        .load()
                        .migrate();
            
            } else if (isTestProfile) {
                
                // Skip Flyway entirely for lightweight tests
                log.info("[FlywayConfig] Skipping Flyway migration for 'test' profile.");
                return;

            } else {
                // ✅ Run schema + seed migrations for all other profiles
                Flyway.configure()
                        .dataSource(flyway.getConfiguration().getDataSource())
                        .locations("classpath:db/migration/schema", "classpath:db/migration/seed")
                        .load()
                        .migrate();
            }
        };
    }
}
