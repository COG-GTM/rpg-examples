package com.example.clients.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param repository     {@code memory} (default), {@code jt400}, or {@code jpa} (reserved)
 * @param auditUser      audit user stamped by the in-memory repository (CLIENTS_AUDIT_USER)
 * @param allowedOrigins CORS origins allowed to call the API (CLIENTS_CORS_ORIGINS)
 */
@ConfigurationProperties(prefix = "clients")
public record ClientsProperties(
        @DefaultValue("memory") String repository,
        @DefaultValue("WEBAPP") String auditUser,
        @DefaultValue("http://localhost:4200") List<String> allowedOrigins) {
}
