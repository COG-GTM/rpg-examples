package com.example.clients.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Contract B end to end over the default in-memory repository. */
@SpringBootTest(properties = "clients.audit-user=TESTER")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ClientApiIntegrationTest {

    private static final String TS = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}";

    @Autowired
    MockMvc mvc;

    private ResultActions create(long dni, String name) throws Exception {
        return mvc.perform(post("/clients/" + dni).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\"}"));
    }

    @Test
    void fullLifecycle() throws Exception {
        create(12345678, "ANA GARCIA")
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/clients/12345678"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.dni").value(12345678))
                .andExpect(jsonPath("$.name").value("ANA GARCIA"))
                .andExpect(jsonPath("$.createdAt").value(matchesPattern(TS)))
                .andExpect(jsonPath("$.createdBy").value("TESTER"))
                .andExpect(jsonPath("$.updatedAt").value(nullValue()))
                .andExpect(jsonPath("$.updatedBy").value(nullValue()))
                .andExpect(jsonPath("$.deletedAt").value(nullValue()))
                .andExpect(jsonPath("$.deletedBy").value(nullValue()))
                .andExpect(jsonPath("$.active").value(true));

        mvc.perform(get("/clients/12345678")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ANA GARCIA"));

        mvc.perform(put("/clients/12345678").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ANA MARIA GARCIA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ANA MARIA GARCIA"))
                .andExpect(jsonPath("$.updatedAt").value(matchesPattern(TS)))
                .andExpect(jsonPath("$.updatedBy").value("TESTER"));

        mvc.perform(delete("/clients/12345678")).andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mvc.perform(get("/clients/12345678")).andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("DELETED"));
        mvc.perform(put("/clients/12345678").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}"))
                .andExpect(status().isGone()).andExpect(jsonPath("$.code").value("DELETED"));
        mvc.perform(delete("/clients/12345678")).andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("DELETED"));
        create(12345678, "AGAIN").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPDNI"))
                .andExpect(jsonPath("$.detail").value("DNI already exists"));
    }

    @Test
    void dniBoundaries() throws Exception {
        create(999999, "LOW").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVDNI"));
        create(1000000, "MIN").andExpect(status().isCreated());
        create(99999999, "MAX").andExpect(status().isCreated());
        create(100000000, "HIGH").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVDNI"));
        mvc.perform(get("/clients/1000000")).andExpect(status().isOk());
        mvc.perform(get("/clients/99999999")).andExpect(status().isOk());
    }

    @Test
    void listIsActiveOnlyOrderedByDni() throws Exception {
        create(30000000, "C").andExpect(status().isCreated());
        create(10000000, "A").andExpect(status().isCreated());
        create(20000000, "B").andExpect(status().isCreated());
        mvc.perform(delete("/clients/20000000")).andExpect(status().isNoContent());
        mvc.perform(get("/clients")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].dni").value(10000000))
                .andExpect(jsonPath("$[1].dni").value(30000000));
    }

    @Test
    void notFound() throws Exception {
        mvc.perform(get("/clients/12345678")).andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOTFND"))
                .andExpect(jsonPath("$.detail").value("DNI not exists"));
    }

    @Test
    void corsAllowsAngularDevServerAndExposesLocation() throws Exception {
        mvc.perform(options("/clients/12345678")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
        create(12345678, "ANA");
        mvc.perform(post("/clients/23456789").header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"BOB\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Location"));
        mvc.perform(options("/clients").header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
