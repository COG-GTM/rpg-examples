package com.example.clients.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ClientControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void fullLifecycle() throws Exception {
        mvc.perform(post("/clients/12345678").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Ana\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dni").value(12345678))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdBy").value("WEBAPI"));
        mvc.perform(post("/clients/12345678").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Ana\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPDNI"))
                .andExpect(jsonPath("$.detail").value("DNI already exists"));
        mvc.perform(put("/clients/12345678").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Ana Maria\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Maria"))
                .andExpect(jsonPath("$.updatedBy").value("WEBAPI"));
        mvc.perform(get("/clients")).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(delete("/clients/12345678")).andExpect(status().isNoContent());
        mvc.perform(get("/clients/12345678"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("DELETED"));
        mvc.perform(get("/clients")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void dniOutOfRangeIsRejectedBeforeReachingTheRepository() throws Exception {
        mvc.perform(get("/clients/999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVDNI"))
                .andExpect(jsonPath("$.detail").value("DNI not valid"));
        mvc.perform(post("/clients/100000000").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownClientIsNotFound() throws Exception {
        mvc.perform(get("/clients/12345678"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTFND"));
    }

    @Test
    void nameLongerThanClnameIsRejected() throws Exception {
        mvc.perform(post("/clients/12345678").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + "x".repeat(31) + "\"}"))
                .andExpect(status().isBadRequest());
    }
}
