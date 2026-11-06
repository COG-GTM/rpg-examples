package com.example.clients.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param backend        {@code memory} (default, CLIENTSV emulation) or {@code jt400} (IBM i)
 * @param auditUser      user stamped by the memory backend (CLIENTSV uses the IBM i job user)
 * @param seed           load demo clients into the memory backend
 * @param allowedOrigins CORS origins for the Angular app
 */
@ConfigurationProperties(prefix = "clients")
public record ClientsProperties(String backend, String auditUser, boolean seed, List<String> allowedOrigins) {
}
