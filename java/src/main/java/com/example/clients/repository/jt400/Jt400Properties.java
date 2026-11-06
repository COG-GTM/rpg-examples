package com.example.clients.repository.jt400;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * IBM i connection settings, bound from {@code IBMI_*} environment variables via application.yml.
 * Never commit real values.
 *
 * @param host        IBM i host name (IBMI_HOST)
 * @param user        user profile (IBMI_USER)
 * @param password    password (IBMI_PASSWORD)
 * @param library     library holding CLIECRUD / CLIESRV (IBMI_LIBRARY)
 * @param dataLibrary library holding TRCLIE (IBMI_DATA_LIBRARY, defaults to {@code library})
 * @param ccsid       CCSID of the character parameters (IBMI_CCSID, default 37)
 */
@ConfigurationProperties(prefix = "ibmi")
public record Jt400Properties(String host, String user, String password, String library, String dataLibrary,
        Integer ccsid) {

    public String effectiveDataLibrary() {
        return isBlank(dataLibrary) ? library : dataLibrary.trim();
    }

    public int effectiveCcsid() {
        return ccsid == null ? CliesrvCodec.DEFAULT_CCSID : ccsid;
    }

    /** Fails fast when the jt400 repository is selected without connection settings. */
    public void validate() {
        StringBuilder missing = new StringBuilder();
        if (isBlank(host)) {
            missing.append(" IBMI_HOST");
        }
        if (isBlank(user)) {
            missing.append(" IBMI_USER");
        }
        if (isBlank(password)) {
            missing.append(" IBMI_PASSWORD");
        }
        if (isBlank(library)) {
            missing.append(" IBMI_LIBRARY");
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("CLIENTS_REPOSITORY=jt400 requires:" + missing);
        }
    }

    @Override
    public String toString() {
        return "Jt400Properties[host=" + host + ", user=" + user + ", password=***, library=" + library
                + ", dataLibrary=" + effectiveDataLibrary() + ", ccsid=" + effectiveCcsid() + "]";
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
