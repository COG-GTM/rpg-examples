package com.example.clients.domain;

import java.util.regex.Pattern;

/** DNI rules shared with CLIENTSV {@code dniIsValid}: {@code dni > 999999 and dni < 100000000}. */
public final class Dni {

    public static final long MIN_EXCLUSIVE = 999_999L;
    public static final long MAX_EXCLUSIVE = 100_000_000L;
    private static final Pattern DIGITS = Pattern.compile("\\d{1,8}");

    private Dni() {
    }

    public static boolean isValid(long dni) {
        return dni > MIN_EXCLUSIVE && dni < MAX_EXCLUSIVE;
    }

    /** Parses a path DNI; anything that is not a valid zoned(8:0) DNI is INVDNI. */
    public static long parse(String raw) {
        if (raw == null || !DIGITS.matcher(raw).matches()) {
            throw new ClientException(ClientsvCode.INVDNI, ClientsvCode.INVDNI.legacyMessage());
        }
        long dni = Long.parseLong(raw);
        if (!isValid(dni)) {
            throw new ClientException(ClientsvCode.INVDNI, ClientsvCode.INVDNI.legacyMessage());
        }
        return dni;
    }
}
