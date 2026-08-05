package com.transitops.backend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Railway provides DATABASE_URL as postgres://user:pass@host/db.
 * Convert it to JDBC properties before Spring Boot DataSource auto-config runs,
 * and activate the prod profile when a real DB URL is present (unless an explicit
 * local profile like h2/local was requested via SPRING_PROFILES_ACTIVE).
 *
 * Also loads a local gitignored {@code .env} file for developer convenience
 * (does not override variables already present in the process environment).
 */
public class RailwayEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        loadDotEnv(environment);

        String profileEnv = firstNonBlank(
                System.getenv("SPRING_PROFILES_ACTIVE"),
                environment.getProperty("SPRING_PROFILES_ACTIVE")
        );
        if (profileEnv != null && (profileEnv.equalsIgnoreCase("h2") || profileEnv.equalsIgnoreCase("local"))) {
            // Explicit local profiles must not be overridden by Railway DATABASE_URL
            return;
        }

        String databaseUrl = firstNonBlank(
                System.getenv("DATABASE_URL"),
                environment.getProperty("DATABASE_URL")
        );
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }

        Map<String, Object> props = new HashMap<>();

        if (databaseUrl.startsWith("jdbc:")) {
            props.put("spring.datasource.url", databaseUrl);
        } else {
            try {
                URI uri = URI.create(databaseUrl);
                String userInfo = uri.getUserInfo();
                if (userInfo != null) {
                    String[] parts = userInfo.split(":", 2);
                    props.put("spring.datasource.username", decode(parts[0]));
                    if (parts.length > 1) {
                        props.put("spring.datasource.password", decode(parts[1]));
                    }
                }
                String query = uri.getQuery() != null ? uri.getQuery() : "sslmode=require";
                String host = uri.getHost();
                int port = uri.getPort();
                String path = uri.getPath() != null ? uri.getPath() : "/";
                String jdbc = "jdbc:postgresql://" + host
                        + (port > 0 ? ":" + port : "")
                        + path + "?" + query;
                props.put("spring.datasource.url", jdbc);
            } catch (Exception ex) {
                throw new IllegalStateException("Invalid DATABASE_URL for Railway/Postgres: " + ex.getMessage(), ex);
            }
        }

        props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
        props.put("spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");

        if (profileEnv == null || profileEnv.isBlank()) {
            props.put("spring.profiles.active", "prod");
            environment.setActiveProfiles("prod");
        }

        environment.getPropertySources().addFirst(new MapPropertySource("railwayDatabaseUrl", props));
    }

    /** Load `.env` from the working directory if present (local only). */
    private static void loadDotEnv(ConfigurableEnvironment environment) {
        Path envFile = Path.of(".env");
        if (!Files.isRegularFile(envFile)) {
            return;
        }
        Map<String, Object> props = new HashMap<>();
        try {
            for (String raw : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                // Prefer real OS env / already-set Spring properties
                if (System.getenv(key) != null) continue;
                if (environment.getProperty(key) != null) continue;
                props.put(key, value);
            }
        } catch (IOException ignored) {
            return;
        }
        if (!props.isEmpty()) {
            environment.getPropertySources().addLast(new MapPropertySource("dotenvFile", props));
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
