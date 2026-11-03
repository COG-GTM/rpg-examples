package com.example.clients.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code clients.*} settings.
 *
 * @param repository   {@code memory} (default) or {@code jt400}
 * @param auditUser    user stamped in CLCRUS/CLUPUS/CLDEUS by the in-memory repository
 * @param corsOrigins  origins allowed to call the API from a browser
 */
@ConfigurationProperties("clients")
public record ClientsProperties(
        @DefaultValue("memory") String repository,
        @DefaultValue("WEBAPP") String auditUser,
        @DefaultValue("http://localhost:4200") String[] corsOrigins) {
}
