/**
 * WebMvcConfig
 *
 * PURPOSE: Global MVC configuration — currently handles CORS so the React
 *          dev server (localhost:5173) can call the Spring Boot API (localhost:8080)
 *          without the browser blocking the requests.
 *
 * DEV vs PROD:
 *   In development the React app runs on its own Vite dev server (port 5173) while
 *   Spring Boot runs on port 8080 — different origins, so CORS headers are required.
 *   In production the React build would typically be served as static files from
 *   within the WAR (same origin), so the CORS rule becomes a no-op.
 *
 * ALSO REGISTERS: SpringDataWebAutoConfiguration's PageableHandlerMethodArgumentResolver
 *   is auto-configured by Spring Boot when spring-data-web is on the classpath, which
 *   lets controllers accept Pageable as a method argument and resolve
 *   ?page=0&size=50&sort=transactionTimestamp,desc from the query string automatically.
 */
package com.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:5173",   // Vite dev server
                        "http://localhost:4173"    // Vite preview
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
