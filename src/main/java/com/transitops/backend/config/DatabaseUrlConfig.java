package com.transitops.backend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseUrlConfig {

    /**
     * When Railway's DATABASE_URL (postgres://...) is set, build a JDBC DataSource.
     * Otherwise Spring Boot uses spring.datasource.* from application properties / local profile.
     */
    @Bean
    @Primary
    @ConditionalOnExpression("T(System).getenv('DATABASE_URL') != null && !T(System).getenv('DATABASE_URL').isBlank()")
    public DataSource railwayDataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        HikariDataSource ds = new HikariDataSource();
        if (databaseUrl.startsWith("jdbc:")) {
            ds.setJdbcUrl(databaseUrl);
            if (System.getenv("DATABASE_USERNAME") != null) ds.setUsername(System.getenv("DATABASE_USERNAME"));
            if (System.getenv("DATABASE_PASSWORD") != null) ds.setPassword(System.getenv("DATABASE_PASSWORD"));
        } else {
            URI uri = URI.create(databaseUrl);
            String userInfo = uri.getUserInfo();
            if (userInfo != null) {
                String[] parts = userInfo.split(":", 2);
                ds.setUsername(parts[0]);
                if (parts.length > 1) ds.setPassword(parts[1]);
            }
            String query = uri.getQuery() != null ? uri.getQuery() : "sslmode=require";
            ds.setJdbcUrl("jdbc:postgresql://" + uri.getHost()
                    + (uri.getPort() > 0 ? ":" + uri.getPort() : "")
                    + uri.getPath() + "?" + query);
        }
        ds.setDriverClassName("org.postgresql.Driver");
        return ds;
    }
}
