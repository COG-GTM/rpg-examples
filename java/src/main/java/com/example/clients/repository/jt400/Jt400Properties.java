package com.example.clients.repository.jt400;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Connection settings for the IBM i hosting TRCLIE, CLIESRV and CLIECRUD.
 *
 * @param system   IBM i host name
 * @param user     user profile (becomes the audit user stamped by CLIESRV via the PSDS)
 * @param password password (inject from the environment, never commit)
 * @param library  library holding TRCLIE and the CLIECRUD program
 * @param program  driver program called with ProgramCall
 * @param ccsid    EBCDIC CCSID used to encode character parameters
 */
@ConfigurationProperties(prefix = "clients.jt400")
public record Jt400Properties(
        String system,
        String user,
        String password,
        String library,
        String program,
        Integer ccsid) {

    public Jt400Properties {
        program = program == null || program.isBlank() ? "CLIECRUD" : program;
        ccsid = ccsid == null ? 37 : ccsid;
        library = library == null || library.isBlank() ? library : objectName(library, "library");
        program = objectName(program, "program");
    }

    private static final Pattern OBJECT_NAME = Pattern.compile("[A-Z$#@][A-Z0-9$#@_.]{0,9}");

    /** IBM i system object name (max 10 chars); rejects anything that could alter SQL or CL text. */
    static String objectName(String value, String what) {
        String name = value.trim().toUpperCase(Locale.ROOT);
        if (!OBJECT_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid IBM i " + what + " name: " + value);
        }
        return name;
    }
}
