package com.example.clients.clientsv;

import com.example.clients.domain.Client;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.AS400ZonedDecimal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Encodes/decodes the CLIENTSV op-in/op-out data structures (see CLIENTSV_H) with IBM Toolbox
 * converters. The converters work without an IBM i connection, so layouts are unit-testable.
 *
 * <pre>
 * clientsv_in_t   (39): op char(1) | dni zoned(8:0) | name char(30)
 * clientsv_out_t (209): success ind(1) | code char(10) | message char(52) | client_t(146)
 * client_t       (146): cldni zoned(8:0) | clname char(30)
 *                       | clcrts ts(26) | clcrus char(10)
 *                       | clupts ts(26) | clupus char(10)
 *                       | cldets ts(26) | cldeus char(10)
 * </pre>
 */
public class ClientsvCodec {

    public static final int DNI_DIGITS = 8;
    public static final int NAME_LENGTH = 30;
    public static final int CODE_LENGTH = 10;
    public static final int MESSAGE_LENGTH = 52;
    public static final int TS_LENGTH = 26;
    public static final int USER_LENGTH = 10;
    public static final int IN_LENGTH = 1 + DNI_DIGITS + NAME_LENGTH;
    public static final int CLIENT_LENGTH = DNI_DIGITS + NAME_LENGTH + 3 * (TS_LENGTH + USER_LENGTH);
    public static final int OUT_LENGTH = 1 + CODE_LENGTH + MESSAGE_LENGTH + CLIENT_LENGTH;
    public static final int DEFAULT_CCSID = 37;

    public static final String TS_NONE = "0001-01-01-00.00.00.000000";
    static final DateTimeFormatter ISO_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS");
    static final DateTimeFormatter SQL_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    private final AS400Text char1;
    private final AS400Text code;
    private final AS400Text message;
    private final AS400Text name;
    private final AS400Text ts;
    private final AS400Text user;
    private final AS400ZonedDecimal dni = new AS400ZonedDecimal(DNI_DIGITS, 0);

    public ClientsvCodec() {
        this(DEFAULT_CCSID);
    }

    public ClientsvCodec(int ccsid) {
        char1 = new AS400Text(1, ccsid);
        code = new AS400Text(CODE_LENGTH, ccsid);
        message = new AS400Text(MESSAGE_LENGTH, ccsid);
        name = new AS400Text(NAME_LENGTH, ccsid);
        ts = new AS400Text(TS_LENGTH, ccsid);
        user = new AS400Text(USER_LENGTH, ccsid);
    }

    // ---- clientsv_in_t ----

    public byte[] encodeRequest(ClientsvRequest request) {
        byte[] out = new byte[IN_LENGTH];
        char1.toBytes(String.valueOf(request.op()), out, 0);
        dni.toBytes(BigDecimal.valueOf(request.dni()), out, 1);
        name.toBytes(fit(request.name(), NAME_LENGTH), out, 1 + DNI_DIGITS);
        return out;
    }

    public ClientsvRequest decodeRequest(byte[] bytes) {
        requireLength(bytes, IN_LENGTH, "clientsv_in_t");
        String op = (String) char1.toObject(bytes, 0);
        long d = ((BigDecimal) dni.toObject(bytes, 1)).longValueExact();
        String n = stripTrailing((String) name.toObject(bytes, 1 + DNI_DIGITS));
        return new ClientsvRequest(op.isEmpty() ? ' ' : op.charAt(0), d, n);
    }

    // ---- clientsv_out_t ----

    public ClientsvResponse decodeResponse(byte[] bytes) {
        requireLength(bytes, OUT_LENGTH, "clientsv_out_t");
        boolean success = "1".equals(char1.toObject(bytes, 0));
        String c = ((String) code.toObject(bytes, 1)).trim();
        String m = ((String) message.toObject(bytes, 1 + CODE_LENGTH)).trim();
        Client client = success ? decodeClient(bytes, 1 + CODE_LENGTH + MESSAGE_LENGTH) : null;
        return new ClientsvResponse(success, c, m, client);
    }

    public byte[] encodeResponse(ClientsvResponse response) {
        byte[] out = new byte[OUT_LENGTH];
        char1.toBytes(response.success() ? "1" : "0", out, 0);
        code.toBytes(fit(response.code(), CODE_LENGTH), out, 1);
        message.toBytes(fit(response.message(), MESSAGE_LENGTH), out, 1 + CODE_LENGTH);
        encodeClient(response.client(), out, 1 + CODE_LENGTH + MESSAGE_LENGTH);
        return out;
    }

    // ---- client_t ----

    Client decodeClient(byte[] bytes, int offset) {
        int pos = offset;
        long d = ((BigDecimal) dni.toObject(bytes, pos)).longValueExact();
        pos += DNI_DIGITS;
        String n = stripTrailing((String) name.toObject(bytes, pos));
        pos += NAME_LENGTH;
        LocalDateTime[] stamps = new LocalDateTime[3];
        String[] users = new String[3];
        for (int i = 0; i < 3; i++) {
            stamps[i] = timestamp((String) ts.toObject(bytes, pos));
            pos += TS_LENGTH;
            users[i] = user((String) user.toObject(bytes, pos));
            pos += USER_LENGTH;
        }
        return new Client(d, n, stamps[0], users[0], stamps[1], users[1], stamps[2], users[2]);
    }

    void encodeClient(Client client, byte[] out, int offset) {
        Client c = client != null ? client : new Client(0, "", null, null, null, null, null, null);
        int pos = offset;
        dni.toBytes(BigDecimal.valueOf(c.dni()), out, pos);
        pos += DNI_DIGITS;
        name.toBytes(fit(c.name(), NAME_LENGTH), out, pos);
        pos += NAME_LENGTH;
        LocalDateTime[] stamps = {c.createdAt(), c.updatedAt(), c.deletedAt()};
        String[] users = {c.createdBy(), c.updatedBy(), c.deletedBy()};
        for (int i = 0; i < 3; i++) {
            ts.toBytes(formatTimestamp(stamps[i]), out, pos);
            pos += TS_LENGTH;
            user.toBytes(fit(users[i], USER_LENGTH), out, pos);
            pos += USER_LENGTH;
        }
    }

    // ---- helpers ----

    /** Parses a DB2 *ISO (or SQL) timestamp; blank or the 0001-01-01 "not set" value become null. */
    public static LocalDateTime timestamp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        if (v.startsWith("0001-01-01")) {
            return null;
        }
        try {
            return LocalDateTime.parse(v, v.length() > 10 && v.charAt(10) == ' ' ? SQL_TS : ISO_TS);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Unparseable TRCLIE timestamp: '" + v + "'", e);
        }
    }

    public static String formatTimestamp(LocalDateTime value) {
        return value == null ? TS_NONE : ISO_TS.format(value);
    }

    public static String user(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }

    private static String stripTrailing(String value) {
        return value == null ? "" : value.stripTrailing();
    }

    private static String fit(String value, int length) {
        String v = value == null ? "" : value;
        return v.length() > length ? v.substring(0, length) : v;
    }

    private static void requireLength(byte[] bytes, int length, String what) {
        if (bytes == null || bytes.length < length) {
            throw new IllegalArgumentException(what + " must be " + length + " bytes, got "
                    + (bytes == null ? "null" : bytes.length));
        }
    }
}
