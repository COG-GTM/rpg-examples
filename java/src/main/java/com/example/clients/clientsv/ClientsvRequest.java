package com.example.clients.clientsv;

/** {@code clientsv_in_t}: op char(1) | dni zoned(8:0) | name char(30). */
public record ClientsvRequest(char op, long dni, String name) {

    public static ClientsvRequest of(ClientsvOp op, long dni, String name) {
        return new ClientsvRequest(op.code(), dni, name == null ? "" : name);
    }
}
