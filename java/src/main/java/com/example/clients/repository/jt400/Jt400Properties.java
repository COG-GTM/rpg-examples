package com.example.clients.repository.jt400;

import org.springframework.boot.context.properties.ConfigurationProperties;

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
    }
}
