package com.example.clients.repository.jt400;

import com.example.clients.domain.Client;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.AS400ZonedDecimal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

/**
 * Byte-level codec for the CLIECRUD parameters (contract A). Character data is EBCDIC
 * in the configured CCSID; DNI is zoned(8:0); timestamps are the 26-char *ISO form.
 *
 * <pre>
 * cliesrv_status_t (63)  success 0-1 | code 1-11 | message 11-63
 * cliesrv_client_t (146) dni 0-8 | name 8-38 | createdTs 38-64 | createdBy 64-74
 *                        updatedTs 74-100 | updatedBy 100-110 | deletedTs 110-136 | deletedBy 136-146
 * </pre>
 */
public class CliesrvCodec {

    public static final int STATUS_LENGTH = 63;
    public static final int CLIENT_LENGTH = 146;
    public static final int NAME_LENGTH = 30;

    static final int CODE_LENGTH = 10;
    static final int MESSAGE_LENGTH = 52;
    static final int DNI_DIGITS = 8;
    static final int TS_LENGTH = 26;
    static final int USER_LENGTH = 10;

    public static final String NULL_TIMESTAMP = "0001-01-01-00.00.00.000000";
    private static final DateTimeFormatter RPG_TS =
            DateTimeFormatter.ofPattern("uuuu-MM-dd-HH.mm.ss.SSSSSS").withResolverStyle(ResolverStyle.STRICT);

    private final AS400Text op;
    private final AS400Text indicator;
    private final AS400Text code;
    private final AS400Text message;
    private final AS400Text name;
    private final AS400Text timestamp;
    private final AS400Text user;
    private final AS400ZonedDecimal dni = new AS400ZonedDecimal(DNI_DIGITS, 0);

    public CliesrvCodec(int ccsid) {
        this.op = new AS400Text(1, ccsid);
        this.indicator = new AS400Text(1, ccsid);
        this.code = new AS400Text(CODE_LENGTH, ccsid);
        this.message = new AS400Text(MESSAGE_LENGTH, ccsid);
        this.name = new AS400Text(NAME_LENGTH, ccsid);
        this.timestamp = new AS400Text(TS_LENGTH, ccsid);
        this.user = new AS400Text(USER_LENGTH, ccsid);
    }

    // ---- input parameters -------------------------------------------------

    public byte[] encodeOp(char operation) {
        return op.toBytes(String.valueOf(operation));
    }

    public byte[] encodeDni(long value) {
        return dni.toBytes(BigDecimal.valueOf(value));
    }

    public byte[] encodeName(String value) {
        String text = fit(value == null ? "" : value, NAME_LENGTH, "name");
        byte[] bytes = name.toBytes(text);
        if (!((String) name.toObject(bytes)).stripTrailing().equals(text.stripTrailing())) {
            throw new IllegalArgumentException("name has characters not representable in the configured CCSID");
        }
        return bytes;
    }

    // ---- status DS --------------------------------------------------------

    public byte[] encodeStatus(CliesrvStatus status) {
        byte[] out = new byte[STATUS_LENGTH];
        put(out, 0, indicator.toBytes(status.success() ? "1" : "0"));
        put(out, 1, code.toBytes(fit(nullToEmpty(status.code()), CODE_LENGTH, "code")));
        put(out, 11, message.toBytes(fit(nullToEmpty(status.message()), MESSAGE_LENGTH, "message")));
        return out;
    }

    public CliesrvStatus decodeStatus(byte[] data) {
        requireLength(data, STATUS_LENGTH, "status");
        String success = (String) indicator.toObject(data, 0);
        return new CliesrvStatus(
                "1".equals(success),
                ((String) code.toObject(data, 1)).trim(),
                ((String) message.toObject(data, 11)).trim());
    }

    // ---- client DS --------------------------------------------------------

    public byte[] encodeClient(Client client) {
        byte[] out = new byte[CLIENT_LENGTH];
        put(out, 0, encodeDni(client.dni()));
        put(out, 8, encodeName(client.name()));
        put(out, 38, timestamp.toBytes(formatTimestamp(client.createdAt())));
        put(out, 64, user.toBytes(fit(nullToEmpty(client.createdBy()), USER_LENGTH, "createdBy")));
        put(out, 74, timestamp.toBytes(formatTimestamp(client.updatedAt())));
        put(out, 100, user.toBytes(fit(nullToEmpty(client.updatedBy()), USER_LENGTH, "updatedBy")));
        put(out, 110, timestamp.toBytes(formatTimestamp(client.deletedAt())));
        put(out, 136, user.toBytes(fit(nullToEmpty(client.deletedBy()), USER_LENGTH, "deletedBy")));
        return out;
    }

    public Client decodeClient(byte[] data) {
        requireLength(data, CLIENT_LENGTH, "client");
        long dniValue = ((BigDecimal) dni.toObject(data, 0)).longValueExact();
        return toClient(dniValue,
                (String) name.toObject(data, 8),
                (String) timestamp.toObject(data, 38), (String) user.toObject(data, 64),
                (String) timestamp.toObject(data, 74), (String) user.toObject(data, 100),
                (String) timestamp.toObject(data, 110), (String) user.toObject(data, 136));
    }

    /** Builds a {@link Client} from raw TRCLIE column values (program call or JDBC row). */
    public static Client toClient(long dni, String name,
                                  String createdTs, String createdBy,
                                  String updatedTs, String updatedBy,
                                  String deletedTs, String deletedBy) {
        return new Client(dni, name == null ? "" : name.stripTrailing(),
                parseTimestamp(createdTs), user(createdBy),
                parseTimestamp(updatedTs), user(updatedBy),
                parseTimestamp(deletedTs), user(deletedBy));
    }

    /** {@code 0001-01-01-00.00.00.000000} or blank means "not set". */
    public static LocalDateTime parseTimestamp(String value) {
        if (value == null || value.isBlank() || value.trim().equals(NULL_TIMESTAMP)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), RPG_TS);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid RPG timestamp: '" + value + "'", e);
        }
    }

    public static String formatTimestamp(LocalDateTime value) {
        return value == null ? NULL_TIMESTAMP : value.truncatedTo(ChronoUnit.MICROS).format(RPG_TS);
    }

    private static String user(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String fit(String value, int length, String field) {
        if (value.length() > length) {
            throw new IllegalArgumentException(field + " exceeds " + length + " characters");
        }
        return value;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static void put(byte[] target, int offset, byte[] source) {
        System.arraycopy(source, 0, target, offset, source.length);
    }

    private static void requireLength(byte[] data, int length, String what) {
        if (data == null || data.length < length) {
            throw new IllegalArgumentException(what + " buffer must be " + length + " bytes, got "
                    + (data == null ? "null" : data.length));
        }
    }

    /** A blank (EBCDIC 0x40) buffer, used to initialise output parameters. */
    public static byte[] blanks(int length) {
        byte[] out = new byte[length];
        Arrays.fill(out, (byte) 0x40);
        return out;
    }
}
