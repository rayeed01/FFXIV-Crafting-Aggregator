package com.crafting.ffxivcraftingaggregator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Cross-origin rules for browser clients.
 *
 * <p>The frontend is served from its own origin in development, so without this every request
 * from it would be blocked by the browser.
 */
@Configuration
public class CorsConfig {

    /**
     * Allows the local development frontends to call the API.
     *
     * <p>Origins are listed explicitly rather than wildcarded: requests carry an Authorization
     * header, and a wildcard origin with credentials is both rejected by browsers and a bad idea.
     *
     * <p>A deployed frontend needs its origin added here.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}