package com.example.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.clients.repository.ClientRepository;
import com.example.clients.repository.jt400.Jt400ClientRepository;
import com.example.clients.repository.memory.InMemoryClientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

class ClientsApplicationTest {

    private static ConfigurableApplicationContext start(String... args) {
        return new SpringApplicationBuilder(ClientsApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Test
    void defaultsToInMemoryRepository() {
        try (ConfigurableApplicationContext ctx = start()) {
            assertThat(ctx.getBean(ClientRepository.class)).isInstanceOf(InMemoryClientRepository.class);
        }
    }

    @Test
    void jt400ProfileSelectsJt400RepositoryWithoutConnectingAtStartup() {
        try (ConfigurableApplicationContext ctx = start("--spring.profiles.active=jt400",
                "--ibmi.host=ibmi.invalid", "--ibmi.user=U", "--ibmi.password=P", "--ibmi.library=CLIELIB")) {
            assertThat(ctx.getBean(ClientRepository.class)).isInstanceOf(Jt400ClientRepository.class);
        }
    }

    @Test
    void jt400RepositoryPropertySelectsJt400Repository() {
        try (ConfigurableApplicationContext ctx = start("--clients.repository=jt400",
                "--ibmi.host=ibmi.invalid", "--ibmi.user=U", "--ibmi.password=P", "--ibmi.library=CLIELIB")) {
            assertThat(ctx.getBean(ClientRepository.class)).isInstanceOf(Jt400ClientRepository.class);
        }
    }

    @Test
    void jt400WithoutConnectionSettingsFailsFast() {
        assertThatThrownBy(() -> start("--clients.repository=jt400"))
                .rootCause()
                .hasMessageContaining("IBMI_HOST")
                .hasMessageContaining("IBMI_PASSWORD");
    }

    @Test
    void jpaHookFailsFastUntilImplemented() {
        assertThatThrownBy(() -> start("--clients.repository=jpa"))
                .rootCause()
                .hasMessageContaining("JpaClientRepository");
    }
}
