package com.example.clients.domain;

/**
 * Business invariants carried over unchanged from CLIESRV (clients_crud.rpgle).
 */
public final class ClientRules {

    /** Smallest valid DNI ({@code > 999999}). */
    public static final long DNI_MIN = 1_000_000L;
    /** Largest valid DNI ({@code < 100000000}). */
    public static final long DNI_MAX = 99_999_999L;
    /** TRCLIE.CLNAME length. */
    public static final int NAME_MAX_LENGTH = 30;
    /** TRCLIE audit user columns (CLCRUS/CLUPUS/CLDEUS) length. */
    public static final int USER_MAX_LENGTH = 10;

    private ClientRules() {
    }

    public static boolean dniIsValid(long dni) {
        return dni > 999_999L && dni < 100_000_000L;
    }

    /** A client is active while CLDEUS (deleted-by user) is blank. */
    public static boolean isActive(String deletedBy) {
        return deletedBy == null || deletedBy.isBlank();
    }
}
