package com.skillgap.analyzer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Centralized CORS configuration for local development (Phase 8B).
 *
 * <p>Allows the Vite dev server (http://localhost:5173) to call this API
 * (http://localhost:8080). Only that exact origin is allowed - the wildcard
 * "*" is deliberately not used. Spring Security is not part of this phase,
 * so no authentication is introduced here.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** The single allowed development frontend origin. */
    private static final String FRONTEND_ORIGIN = "http://localhost:5173";

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(FRONTEND_ORIGIN)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
