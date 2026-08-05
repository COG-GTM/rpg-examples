package com.example.rpgexamples.support;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Captures standard output for assertions against RPG {@code dsply}-equivalent output.
 */
public final class ConsoleCapture implements AutoCloseable {

    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    public ConsoleCapture() {
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
    }

    public String output() {
        return output.toString(StandardCharsets.UTF_8);
    }

    @Override
    public void close() {
        System.setOut(originalOut);
    }
}
