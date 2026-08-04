package com.transitops.backend.config;

/**
 * Railway DATABASE_URL conversion is handled by {@link RailwayEnvironmentPostProcessor}
 * so Spring Boot's DataSource auto-configuration receives a valid JDBC URL.
 * This class is retained as a marker/documentation hook.
 */
public final class DatabaseUrlConfig {
    private DatabaseUrlConfig() {}
}
