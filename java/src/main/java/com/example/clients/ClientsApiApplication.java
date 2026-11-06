package com.example.clients;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ClientsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientsApiApplication.class, args);
    }
}
