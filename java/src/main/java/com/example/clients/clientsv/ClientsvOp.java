package com.example.clients.clientsv;

/** CLIENTSV / CLIECRUD operation codes. */
public enum ClientsvOp {
    CREATE('C'), READ('R'), UPDATE('U'), DELETE('D');

    private final char code;

    ClientsvOp(char code) {
        this.code = code;
    }

    public char code() {
        return code;
    }
}
