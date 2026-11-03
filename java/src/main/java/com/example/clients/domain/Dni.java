package com.example.clients.domain;

/** DNI rule from {@code dniIsValid}: {@code dni > 999999 and dni < 100000000}. */
public final class Dni {

    public static final long MIN = 1_000_000L;
    public static final long MAX = 99_999_999L;

    private Dni() {
    }

    public static boolean isValid(long dni) {
        return dni > 999_999L && dni < 100_000_000L;
    }
}
