package com.example.clients.api;

import com.example.clients.config.ClientsProperties;
import com.example.clients.config.WebConfig;
import com.example.clients.domain.Client;
import com.example.clients.domain.ClientErrorCode;
import com.example.clients.domain.ClientException;
import com.example.clients.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientController.class)
@Import(WebConfig.class)
@EnableConfigurationProperties(ClientsProperties.class)
class ClientControllerTest {

    private static final Client ANA = new Client(12345678, "ANA PEREZ",
            LocalDateTime.of(2026, 11, 3, 10, 15, 30, 123_456_000), "QUSER", null, null, null, null);

    @Autowired
    MockMvc mvc;

    @MockBean
    ClientRepository repository;

    @Test
    void list200() throws Exception {
        when(repository.findAllActive()).thenReturn(List.of(ANA));
        mvc.perform(get("/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dni").value(12345678))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void read200MatchesContractJson() throws Exception {
        when(repository.findActive(12345678)).thenReturn(ANA);
        mvc.perform(get("/clients/12345678"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        { "dni": 12345678, "name": "ANA PEREZ",
                          "createdAt": "2026-11-03T10:15:30.123456", "createdBy": "QUSER",
                          "updatedAt": null, "updatedBy": null, "deletedAt": null, "deletedBy": null,
                          "active": true }
                        """, true));
    }

    @Test
    void create201WithLocation() throws Exception {
        when(repository.create(12345678, "ANA PEREZ")).thenReturn(ANA);
        mvc.perform(post("/clients/12345678").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ANA PEREZ\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/clients/12345678"))
                .andExpect(jsonPath("$.name").value("ANA PEREZ"));
    }

    @Test
    void update200() throws Exception {
        when(repository.update(12345678, "ANA MARIA")).thenReturn(ANA);
        mvc.perform(put("/clients/12345678").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ANA MARIA\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void delete204() throws Exception {
        when(repository.softDelete(12345678)).thenReturn(ANA);
        mvc.perform(delete("/clients/12345678"))
                .andExpect(status().isNoContent());
    }

    @Test
    void dniBoundariesOnPathVariable() throws Exception {
        when(repository.findActive(anyLong())).thenReturn(ANA);
        problem(mvc.perform(get("/clients/999999")), 400, "INVDNI", "DNI not valid");
        mvc.perform(get("/clients/1000000")).andExpect(status().isOk());
        mvc.perform(get("/clients/99999999")).andExpect(status().isOk());
        problem(mvc.perform(get("/clients/100000000")), 400, "INVDNI", "DNI not valid");
    }

    @Test
    void invalidDniOnEveryVerbIs400Invdni() throws Exception {
        problem(mvc.perform(post("/clients/999999").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ANA\"}")), 400, "INVDNI", "DNI not valid");
        problem(mvc.perform(put("/clients/100000000").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ANA\"}")), 400, "INVDNI", "DNI not valid");
        problem(mvc.perform(delete("/clients/5")), 400, "INVDNI", "DNI not valid");
        problem(mvc.perform(get("/clients/abc")), 400, "INVDNI", "DNI not valid");
        problem(mvc.perform(get("/clients/99999999999999999999")), 400, "INVDNI", "DNI not valid");
        verifyNoInteractions(repository);
    }

    @Test
    void invalidDniTakesPrecedenceOverInvalidName() throws Exception {
        problem(mvc.perform(post("/clients/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}")), 400, "INVDNI", "DNI not valid");
    }

    @Test
    void badNameIs400Validation() throws Exception {
        problem(mvc.perform(post("/clients/12345678").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"   \"}")), 400, "VALIDATION", "name is required");
        problem(mvc.perform(post("/clients/12345678").contentType(MediaType.APPLICATION_JSON)
                .content("{}")), 400, "VALIDATION", "name is required");
        problem(mvc.perform(put("/clients/12345678").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + "X".repeat(31) + "\"}")), 400, "VALIDATION",
                "name must be at most 30 characters");
        problem(mvc.perform(post("/clients/12345678").contentType(MediaType.APPLICATION_JSON)
                .content("not json")), 400, "VALIDATION", "Request body is missing or malformed");
        problem(mvc.perform(post("/clients/12345678").contentType(MediaType.APPLICATION_JSON)),
                400, "VALIDATION", "Request body is missing or malformed");
        verifyNoInteractions(repository);
    }

    @Test
    void thirtyCharNameIsAccepted() throws Exception {
        when(repository.create(12345678, "X".repeat(30))).thenReturn(ANA);
        mvc.perform(post("/clients/12345678").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + "X".repeat(30) + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void repositoryCodesMapToStatuses() throws Exception {
        when(repository.findActive(10000001)).thenThrow(new ClientException(ClientErrorCode.NOTFND));
        when(repository.create(10000002, "ANA")).thenThrow(new ClientException(ClientErrorCode.DUPDNI));
        when(repository.update(10000003, "ANA")).thenThrow(new ClientException(ClientErrorCode.DELETED));
        when(repository.softDelete(10000004)).thenThrow(new ClientException(ClientErrorCode.IOERR, "IBM i request failed"));
        when(repository.findActive(10000005)).thenThrow(new ClientException(ClientErrorCode.INVDNI));
        when(repository.findAllActive()).thenThrow(new IllegalStateException("boom"));

        problem(mvc.perform(get("/clients/10000001")), 404, "NOTFND", "DNI not exists");
        problem(mvc.perform(post("/clients/10000002").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ANA\"}")), 409, "DUPDNI", "DNI already exists");
        problem(mvc.perform(put("/clients/10000003").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ANA\"}")), 410, "DELETED", "Client marked as deleted");
        problem(mvc.perform(delete("/clients/10000004")), 500, "IOERR", "IBM i request failed");
        problem(mvc.perform(get("/clients/10000005")), 400, "INVDNI", "DNI not valid");
        problem(mvc.perform(get("/clients")), 500, "IOERR", "Unexpected server error");
    }

    @Test
    void corsAllowsAngularDevServer() throws Exception {
        mvc.perform(options("/clients/12345678")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
        mvc.perform(options("/clients")
                        .header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    private static void problem(ResultActions result, int status, String code, String detail) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.title").exists());
    }
}
