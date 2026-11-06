package com.example.clients.config;

import com.example.clients.repository.ClientRepository;
import com.example.clients.repository.jt400.CliesrvCodec;
import com.example.clients.repository.jt400.Jt400CliecrudGateway;
import com.example.clients.repository.jt400.Jt400ClientRepository;
import com.example.clients.repository.jt400.Jt400Properties;
import com.example.clients.repository.memory.InMemoryClientRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Picks the {@link ClientRepository} from {@code clients.repository} (env CLIENTS_REPOSITORY). */
@Configuration(proxyBeanMethods = false)
public class RepositoryConfig {

    @Bean
    @ConditionalOnProperty(name = "clients.repository", havingValue = "memory", matchIfMissing = true)
    ClientRepository inMemoryClientRepository(ClientsProperties props) {
        return new InMemoryClientRepository(props.auditUser());
    }

    @Bean
    @ConditionalOnProperty(name = "clients.repository", havingValue = "jt400")
    ClientRepository jt400ClientRepository(Jt400Properties props) {
        props.validate();
        CliesrvCodec codec = new CliesrvCodec(props.effectiveCcsid());
        return new Jt400ClientRepository(new Jt400CliecrudGateway(props, codec), codec);
    }

    /** Hook for a future JpaClientRepository mapped onto TRCLIE; see {@code repository.jpa}. */
    @Bean
    @ConditionalOnProperty(name = "clients.repository", havingValue = "jpa")
    ClientRepository jpaClientRepository() {
        throw new IllegalStateException(
                "CLIENTS_REPOSITORY=jpa is reserved for a future JpaClientRepository and is not implemented yet");
    }
}
