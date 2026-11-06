package com.example.clients.repository.jt400;

import com.example.clients.domain.Client;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.AS400ZonedDecimal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Encodes/decodes the CLIECRUD parameters of Contract A with IBM Toolbox converters. All of them
 * work offline (no AS400 connection), so the byte layouts are unit-testable.
 *
 * <pre>
 * status (63): success ind(1) | code char(10) | message char(52)
 * client (146): dni zoned(8:0) | name char(30)
 *               | createdTs char(26) | createdBy char(10)
 *               | updatedTs char(26) | updatedBy char(10)
 *               | deletedTs char(26) | deletedBy char(10)
 * </pre>
 */
public class CliesrvCodec {

    public static final int OP_LENGTH = 1;
    public static final int DNI_DIGITS = 8;
    public static final int NAME_LENGTH = 30;
    public static final int CODE_LENGTH = 10;
    public static final int MESSAGE_LENGTH = 52;
    public static final int TS_LENGTH = 26;
    public static final int USER_LENGTH = 10;
    public static final int STATUS_LENGTH = 1 + CODE_LENGTH + MESSAGE_LENGTH;
    public static final int CLIENT_LENGTH = DNI_DIGITS + NAME_LENGTH + 3 * (TS_LENGTH + USER_LENGTH);
    public static final int DEFAULT_CCSID = 37;

    static final String SENTINEL_PREFIX = "0001-01-01";
    static final DateTimeFormatter ISO_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS");
    static final DateTimeFormatter SQL_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    private final AS400Text op;
    private final AS400Text ind;
    private final AS400Text code;
    private final AS400Text message;
    private final AS400Text name;
    private final AS400Text ts;
    private final AS400Text user;
    private final AS400ZonedDecimal dni = new AS400ZonedDecimal(DNI_DIGITS, 0);

    public CliesrvCodec() {
        this(DEFAULT_CCSID);
    }

    public CliesrvCodec(int ccsid) {
        op = new AS400Text(OP_LENGTH, ccsid);
        ind = new AS400Text(1, ccsid);
        code = new AS400Text(CODE_LENGTH, ccsid);
        message = new AS400Text(MESSAGE_LENGTH, ccsid);
        name = new AS400Text(NAME_LENGTH, ccsid);
        ts = new AS400Text(TS_LENGTH, ccsid);
        user = new AS400Text(USER_LENGTH, ccsid);
    }

    // ---- input parameters ----

    public byte[] encodeOp(char operation) {
        return op.toBytes(String.valueOf(operation));
    }

    public byte[] encodeDni(long value) {
        return dni.toBytes(BigDecimal.valueOf(value));
    }

    public byte[] encodeName(String value) {
        return name.toBytes(fit(value, NAME_LENGTH));
    }

    // ---- status ----

    public CliesrvStatus decodeStatus(byte[] bytes) {
        requireLength(bytes, STATUS_LENGTH, "status");
        String success = (String) ind.toObject(bytes, 0);
        String c = ((String) code.toObject(bytes, 1)).trim();
        String m = ((String) message.toObject(bytes, 1 + CODE_LENGTH)).trim();
        return new CliesrvStatus("1".equals(success), c, m);
    }

    public byte[] encodeStatus(CliesrvStatus status) {
        byte[] out = new byte[STATUS_LENGTH];
        ind.toBytes(status.success() ? "1" : "0", out, 0);
        code.toBytes(fit(status.code(), CODE_LENGTH), out, 1);
        message.toBytes(fit(status.message(), MESSAGE_LENGTH), out, 1 + CODE_LENGTH);
        return out;
    }

    // ---- client image ----

    public Client decodeClient(byte[] bytes) {
        requireLength(bytes, CLIENT_LENGTH, "client");
        int pos = 0;
        long d = ((BigDecimal) dni.toObject(bytes, pos)).longValueExact();
        pos += DNI_DIGITS;
        String n = (String) name.toObject(bytes, pos);
        pos += NAME_LENGTH;
        String[] audit = new String[6];
        for (int i = 0; i < 3; i++) {
            audit[i * 2] = (String) ts.toObject(bytes, pos);
            pos += TS_LENGTH;
            audit[i * 2 + 1] = (String) user.toObject(bytes, pos);
            pos += USER_LENGTH;
        }
        return toClient(new TrclieRow(d, n, audit[0], audit[1], audit[2], audit[3], audit[4], audit[5]));
    }

    public byte[] encodeClient(TrclieRow row) {
        byte[] out = new byte[CLIENT_LENGTH];
        int pos = 0;
        dni.toBytes(BigDecimal.valueOf(row.dni()), out, pos);
        pos += DNI_DIGITS;
        name.toBytes(fit(row.name(), NAME_LENGTH), out, pos);
        pos += NAME_LENGTH;
        String[] audit = {row.createdTs(), row.createdBy(), row.updatedTs(), row.updatedBy(),
                row.deletedTs(), row.deletedBy()};
        for (int i = 0; i < 3; i++) {
            ts.toBytes(fit(audit[i * 2], TS_LENGTH), out, pos);
            pos += TS_LENGTH;
            user.toBytes(fit(audit[i * 2 + 1], USER_LENGTH), out, pos);
            pos += USER_LENGTH;
        }
        return out;
    }

    // ---- shared mapping (program image and JDBC rows) ----

    public static Client toClient(TrclieRow row) {
        String deletedBy = user(row.deletedBy());
        return new Client(row.dni(), row.name() == null ? "" : row.name().stripTrailing(),
                timestamp(row.createdTs()), user(row.createdBy()),
                timestamp(row.updatedTs()), user(row.updatedBy()),
                timestamp(row.deletedTs()), deletedBy,
                deletedBy == null);
    }

    /** Parses a DB2 *ISO timestamp; blank or the {@code 0001-01-01} sentinel become null. */
    public static LocalDateTime timestamp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        if (v.startsWith(SENTINEL_PREFIX)) {
            return null;
        }
        try {
            return LocalDateTime.parse(v, v.charAt(10) == ' ' ? SQL_TS : ISO_TS);
        } catch (DateTimeParseException | StringIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Unparseable TRCLIE timestamp: '" + v + "'", e);
        }
    }

    public static String formatTimestamp(LocalDateTime value) {
        return value == null ? "0001-01-01-00.00.00.000000" : ISO_TS.format(value);
    }

    static String user(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }

    private static String fit(String value, int length) {
        String v = value == null ? "" : value;
        return v.length() > length ? v.substring(0, length) : v;
    }

    private static void requireLength(byte[] bytes, int length, String what) {
        if (bytes == null || bytes.length < length) {
            throw new IllegalArgumentException(what + " buffer must be " + length + " bytes, got "
                    + (bytes == null ? "null" : bytes.length));
        }
    }
}
