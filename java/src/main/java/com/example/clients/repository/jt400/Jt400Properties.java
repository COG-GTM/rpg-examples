package com.example.clients.repository.jt400;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * {@code clients.jt400.*}, normally supplied through IBMI_* environment variables.
 *
 * @param host        IBM i host name
 * @param user        user profile (also what CLIESRV stamps as CLCRUS/CLUPUS/CLDEUS)
 * @param password    password, never committed
 * @param library     library holding the CLIECRUD program and CLIESRV service program
 * @param dataLibrary library holding TRCLIE (defaults to {@code library})
 * @param ccsid       EBCDIC CCSID of the character fields
 */
@ConfigurationProperties("clients.jt400")
public record Jt400Properties(
        String host,
        String user,
        String password,
        String library,
        String dataLibrary,
        @DefaultValue("37") int ccsid) {

    private static final Pattern OBJECT_NAME = Pattern.compile("[A-Z$#@][A-Z0-9$#@_.]{0,9}");

    public String programLibrary() {
        return objectName(library, "clients.jt400.library (IBMI_LIBRARY)");
    }

    public String fileLibrary() {
        return dataLibrary == null || dataLibrary.isBlank()
                ? programLibrary()
                : objectName(dataLibrary, "clients.jt400.data-library (IBMI_DATA_LIBRARY)");
    }

    /** Fails fast when the jt400 repository is selected without a usable connection config. */
    public void validate() {
        require(host, "clients.jt400.host (IBMI_HOST)");
        require(user, "clients.jt400.user (IBMI_USER)");
        require(password, "clients.jt400.password (IBMI_PASSWORD)");
        programLibrary();
        fileLibrary();
    }

    private static String objectName(String value, String property) {
        require(value, property);
        String upper = value.trim().toUpperCase(Locale.ROOT);
        if (!OBJECT_NAME.matcher(upper).matches()) {
            throw new IllegalStateException(property + " is not a valid IBM i object name: " + value);
        }
        return upper;
    }

    private static void require(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(property + " must be set when clients.repository=jt400");
        }
    }

    @Override
    public String toString() {
        return "Jt400Properties[host=" + host + ", user=" + user + ", password=****, library=" + library
                + ", dataLibrary=" + dataLibrary + ", ccsid=" + ccsid + "]";
    }
}
