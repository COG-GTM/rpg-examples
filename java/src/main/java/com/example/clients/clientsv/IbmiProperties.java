package com.example.clients.clientsv;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * IBM i connection for {@code clients.backend=jt400}. Supply through environment variables only.
 *
 * @param library      library holding service program CLIENTSV
 * @param dataLibrary  library holding TRCLIE (defaults to {@code library})
 */
@ConfigurationProperties(prefix = "ibmi")
public record IbmiProperties(String host, String user, String password, String library, String dataLibrary,
        int ccsid) {

    public String effectiveDataLibrary() {
        return dataLibrary == null || dataLibrary.isBlank() ? library : dataLibrary;
    }

    public void validate() {
        StringBuilder missing = new StringBuilder();
        if (host == null || host.isBlank()) missing.append(" IBMI_HOST");
        if (user == null || user.isBlank()) missing.append(" IBMI_USER");
        if (password == null || password.isBlank()) missing.append(" IBMI_PASSWORD");
        if (library == null || library.isBlank()) missing.append(" IBMI_LIBRARY");
        if (!missing.isEmpty()) {
            throw new IllegalStateException("clients.backend=jt400 requires:" + missing);
        }
    }
}
