package com.example.clients.clientsv;

/** Calls {@code CLIENTSV.clientsvExecute(in: out)} with raw DS bytes. */
@FunctionalInterface
public interface ServiceProgramInvoker {

    /** @param in {@code clientsv_in_t} bytes; @return {@code clientsv_out_t} bytes */
    byte[] execute(byte[] in);
}
