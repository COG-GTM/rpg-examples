package com.example.rpgexamples.basics;

import com.example.rpgexamples.support.ConsoleCapture;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HelloWorldTest {

    @Test
    void printsHelloWorld() {
        try (ConsoleCapture console = new ConsoleCapture()) {
            HelloWorld.main(new String[0]);

            assertEquals("Hello world!" + System.lineSeparator(), console.output());
        }
    }
}
