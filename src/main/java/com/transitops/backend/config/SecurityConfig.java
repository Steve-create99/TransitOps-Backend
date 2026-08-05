package com.transitops.backend.config;

import com.transitops.backend.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Value("${transitops.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/refresh").permitAll()
                        .requestMatchers("/health", "/api/health").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Exact origins (credentials-safe). Strip paths from env values like .../dashboard
        Set<String> origins = new LinkedHashSet<>();
        origins.add("https://transitops-frontend.pages.dev");
        origins.add("http://localhost:5173");
        origins.add("http://127.0.0.1:5173");
        origins.add("http://localhost:3000");
        if (allowedOrigins != null) {
            for (String raw : allowedOrigins.split(",")) {
                String origin = normalizeOrigin(raw);
                if (!origin.isEmpty()) {
                    origins.add(origin);
                }
            }
        }
        config.setAllowedOrigins(List.copyOf(origins));

        // Patterns for preview / tunnel hosts
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://*.pages.dev",
                "https://*.vercel.app",
                "https://*.netlify.app",
                "https://*.exp.direct",
                "https://*.up.railway.app"
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** Accept bare origins only — browsers reject origins that include a path. */
    private static String normalizeOrigin(String raw) {
        if (raw == null) return "";
        String value = raw.trim();
        if (value.isEmpty()) return "";
        try {
            java.net.URI uri = java.net.URI.create(value);
            if (uri.getScheme() == null || uri.getHost() == null) {
                // Already a bare origin or invalid; strip trailing slash/path heuristically
                int schemeIdx = value.indexOf("://");
                if (schemeIdx < 0) return value.replaceAll("/+$", "");
                int pathIdx = value.indexOf('/', schemeIdx + 3);
                return pathIdx > 0 ? value.substring(0, pathIdx) : value.replaceAll("/+$", "");
            }
            StringBuilder sb = new StringBuilder();
            sb.append(uri.getScheme()).append("://").append(uri.getHost());
            if (uri.getPort() > 0) {
                sb.append(':').append(uri.getPort());
            }
            return sb.toString();
        } catch (Exception ex) {
            return value.replaceAll("/+$", "");
        }
    }
}
