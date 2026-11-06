package com.example.clients.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = {"clients.backend=memory", "clients.seed=true"})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ClientApiTest {

    @Autowired
    MockMvc mvc;

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder b, String body) {
        return b.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void listShowsActiveClientsByDefault() throws Exception {
        mvc.perform(get("/clients")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/clients?includeDeleted=true")).andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[2].active").value(false));
    }

    @Test
    void fullCrudLifecycle() throws Exception {
        mvc.perform(json(post("/clients/12345678"), "{\"name\":\"  ANA  \"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/clients/12345678"))
                .andExpect(jsonPath("$.dni").value(12345678))
                .andExpect(jsonPath("$.name").value("ANA"))
                .andExpect(jsonPath("$.createdBy").value("WEBAPP"))
                .andExpect(jsonPath("$.active").value(true));
        mvc.perform(get("/clients/12345678")).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("ANA"));
        mvc.perform(json(put("/clients/12345678"), "{\"name\":\"ANA MARIA\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.updatedBy").value("WEBAPP"));
        mvc.perform(delete("/clients/12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedBy").value("WEBAPP"))
                .andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/clients/12345678"))
                .andExpect(status().isGone()).andExpect(jsonPath("$.code").value("DELETED"))
                .andExpect(jsonPath("$.detail").value("Client marked as deleted"));
        mvc.perform(json(post("/clients/12345678"), "{\"name\":\"AGAIN\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DUPDNI"));
    }

    @Test
    void validationFailuresAre4xxProblems() throws Exception {
        mvc.perform(get("/clients/999999")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVDNI")).andExpect(jsonPath("$.detail").value("DNI not valid"));
        mvc.perform(get("/clients/abc")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVDNI"));
        mvc.perform(get("/clients/123456789")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVDNI"));
        mvc.perform(get("/clients/7654321")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOTFND"));
        mvc.perform(json(post("/clients/7654321"), "{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION"));
        mvc.perform(json(post("/clients/7654321"), "{\"name\":\"" + "X".repeat(31) + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION"));
        // DNI is checked before the body (RPG guard order)
        mvc.perform(json(put("/clients/12"), "{\"name\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVDNI"));
        mvc.perform(json(post("/clients/7654321"), "not json"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION"));
        mvc.perform(json(post("/clients/12"), "not json"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVDNI"));
        mvc.perform(json(put("/clients/abc"), "not json"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVDNI"));
        mvc.perform(json(post(URI.create("/clients/%37%36%35%34%33%32%31")), "not json"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION"));
        mvc.perform(json(put("/clients/7654321"), "{\"name\":\"X\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOTFND"));
        mvc.perform(delete("/clients/31222333")).andExpect(status().isGone());
    }
}
