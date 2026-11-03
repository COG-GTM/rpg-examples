package com.example.clients.config;

import com.example.clients.repository.ClientRepository;
import com.example.clients.repository.jt400.CliesrvCodec;
import com.example.clients.repository.jt400.Jt400ClientRepository;
import com.example.clients.repository.jt400.Jt400CliecrudGateway;
import com.example.clients.repository.jt400.Jt400Properties;
import com.example.clients.repository.memory.InMemoryClientRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Selects the {@link ClientRepository} implementation from {@code clients.repository}
 * (env {@code CLIENTS_REPOSITORY}). Add a {@code jpa} branch here for a future
 * {@code JpaClientRepository} mapped onto TRCLIE.
 */
@Configuration
public class RepositoryConfig {

    @Bean
    @ConditionalOnProperty(name = "clients.repository", havingValue = "memory", matchIfMissing = true)
    ClientRepository inMemoryClientRepository(ClientsProperties properties) {
        return new InMemoryClientRepository(properties.auditUser(), Clock.systemDefaultZone());
    }

    @Bean
    @ConditionalOnProperty(name = "clients.repository", havingValue = "jt400")
    ClientRepository jt400ClientRepository(Jt400Properties properties) {
        CliesrvCodec codec = new CliesrvCodec(properties.ccsid());
        return new Jt400ClientRepository(new Jt400CliecrudGateway(properties, codec), codec);
    }
}
