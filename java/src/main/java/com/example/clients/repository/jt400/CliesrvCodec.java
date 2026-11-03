package com.example.clients.repository.jt400;

import com.example.clients.domain.Client;
import com.example.clients.domain.ClientStatus;
import com.ibm.as400.access.AS400DataType;
import com.ibm.as400.access.AS400Structure;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.AS400ZonedDecimal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Byte layouts of the CLIECRUD parameters, kept in sync with cliesrv_p.rpgle.
 */
public final class CliesrvCodec {

    /** {@code cliesrv_status_t}: success ind(1) + code char(10) + message char(52). */
    public static final int STATUS_LENGTH = 63;
    /** {@code cliesrv_client_t}: zoned(8:0) + char(30) + 3 x (timestamp(26) + char(10)). */
    public static final int CLIENT_LENGTH = 146;

    static final String LOW_TIMESTAMP = "0001-01-01-00.00.00.000000";
    static final DateTimeFormatter RPG_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS");

    private final AS400Text op;
    private final AS400ZonedDecimal dni = new AS400ZonedDecimal(8, 0);
    private final AS400Text name;
    private final AS400Structure status;
    private final AS400Structure client;

    public CliesrvCodec(int ccsid) {
        op = new AS400Text(1, ccsid);
        name = new AS400Text(30, ccsid);
        status = new AS400Structure(new AS400DataType[] {
            new AS400Text(1, ccsid), new AS400Text(10, ccsid), new AS400Text(52, ccsid)});
        client = new AS400Structure(new AS400DataType[] {
            new AS400ZonedDecimal(8, 0), new AS400Text(30, ccsid),
            new AS400Text(26, ccsid), new AS400Text(10, ccsid),
            new AS400Text(26, ccsid), new AS400Text(10, ccsid),
            new AS400Text(26, ccsid), new AS400Text(10, ccsid)});
    }

    public byte[] encodeOp(char operation) {
        return op.toBytes(String.valueOf(operation));
    }

    public byte[] encodeDni(long value) {
        return dni.toBytes(BigDecimal.valueOf(value));
    }

    public byte[] encodeName(String value) {
        return name.toBytes(value == null ? "" : value);
    }

    public Status decodeStatus(byte[] bytes) {
        Object[] f = (Object[]) status.toObject(bytes);
        return new Status("1".equals(f[0]), ClientStatus.fromRpgCode((String) f[1]), ((String) f[2]).trim());
    }

    public Client decodeClient(byte[] bytes) {
        Object[] f = (Object[]) client.toObject(bytes);
        return new Client(
                ((BigDecimal) f[0]).longValueExact(),
                ((String) f[1]).stripTrailing(),
                parseTimestamp((String) f[2]), ((String) f[3]).trim(),
                parseTimestamp((String) f[4]), ((String) f[5]).trim(),
                parseTimestamp((String) f[6]), ((String) f[7]).trim());
    }

    /** Only used by tests to fabricate what CLIECRUD would return. */
    byte[] encodeStatus(boolean success, String code, String message) {
        return status.toBytes(new Object[] {success ? "1" : "0", code, message});
    }

    /** Only used by tests to fabricate what CLIECRUD would return. */
    byte[] encodeClient(Client c) {
        return client.toBytes(new Object[] {
            BigDecimal.valueOf(c.dni()), c.name(),
            formatTimestamp(c.createdAt()), c.createdBy(),
            formatTimestamp(c.updatedAt()), c.updatedBy(),
            formatTimestamp(c.deletedAt()), c.deletedBy()});
    }

    static LocalDateTime parseTimestamp(String value) {
        if (value == null || value.isBlank() || LOW_TIMESTAMP.equals(value.trim())) {
            return null;
        }
        return LocalDateTime.parse(value.trim(), RPG_TIMESTAMP);
    }

    static String formatTimestamp(LocalDateTime value) {
        return value == null ? LOW_TIMESTAMP : RPG_TIMESTAMP.format(value);
    }

    /** Decoded {@code cliesrv_status_t}. */
    public record Status(boolean success, ClientStatus code, String message) {
    }
}
