package com.example.clients;

import com.example.clients.repository.ClientRepository;
import com.example.clients.repository.memory.InMemoryClientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full context with the default (in-memory) repository: one lifecycle end to end. */
@SpringBootTest
@AutoConfigureMockMvc
class ClientsApplicationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ClientRepository repository;

    @Test
    void defaultsToInMemoryRepositoryAndRunsLifecycle() throws Exception {
        assertThat(repository).isInstanceOf(InMemoryClientRepository.class);

        mvc.perform(post("/clients/23456789").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"JUAN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdBy").value("WEBAPP"));
        mvc.perform(post("/clients/23456789").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"JUAN\"}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/clients/23456789").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"JUAN B\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedBy").value("WEBAPP"));
        mvc.perform(delete("/clients/23456789")).andExpect(status().isNoContent());
        mvc.perform(get("/clients/23456789")).andExpect(status().isGone());
        mvc.perform(get("/clients")).andExpect(jsonPath("$.length()").value(0));
    }
}
