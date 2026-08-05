package com.example.rpgexamples.basics;

import java.io.PrintStream;

/**
 * Java equivalent of {@code rpg ile/basics/free_hello_world.rpgle}.
 */
public final class HelloWorld {

    private HelloWorld() {
    }

    public static void main(String[] args) {
        run(System.out);
    }

    static void run(PrintStream output) {
        output.println("Hello world!");
    }
}
