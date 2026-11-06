package com.example.clients.api;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.clients.domain.ClientErrorCode;
import com.example.clients.domain.ClientException;
import com.example.clients.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Error mapping for every repository code, with the repository mocked. */
@WebMvcTest(ClientController.class)
class ClientControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    ClientRepository repository;

    private static ResultActions expectProblem(ResultActions actions, int status, String code, String detail)
            throws Exception {
        return actions.andExpect(status().is(status))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.title").isNotEmpty());
    }

    @ParameterizedTest
    @CsvSource({
        "INVDNI,400,DNI not valid",
        "NOTFND,404,DNI not exists",
        "DUPDNI,409,DNI already exists",
        "DELETED,410,Client marked as deleted",
        "IOERR,500,'I/O error on TRCLIE, status 1218'",
        "INVOP,500,OP. Error",
    })
    void repositoryCodesMapToProblemJson(ClientErrorCode code, int status, String message) throws Exception {
        ClientException ex = new ClientException(code, message);
        when(repository.read(anyLong())).thenThrow(ex);
        when(repository.create(anyLong(), anyString())).thenThrow(ex);
        when(repository.update(anyLong(), anyString())).thenThrow(ex);
        doThrow(ex).when(repository).delete(anyLong());
        when(repository.findAllActive()).thenThrow(ex);

        expectProblem(mvc.perform(get("/clients/12345678")), status, code.name(), message)
                .andExpect(jsonPath("$.instance").value("/clients/12345678"));
        expectProblem(mvc.perform(post("/clients/12345678").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ANA\"}")), status, code.name(), message);
        expectProblem(mvc.perform(put("/clients/12345678").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ANA\"}")), status, code.name(), message);
        expectProblem(mvc.perform(delete("/clients/12345678")), status, code.name(), message);
        expectProblem(mvc.perform(get("/clients")), status, code.name(), message);
    }

    @Test
    void unexpectedExceptionIsIoerr() throws Exception {
        when(repository.findAllActive()).thenThrow(new IllegalStateException("boom"));
        expectProblem(mvc.perform(get("/clients")), 500, "IOERR", "I/O error on TRCLIE");
    }

    @ParameterizedTest
    @CsvSource({"abc", "12.5", "99999999999999999999999", "-", "0x1000000"})
    void nonNumericOrOverflowingDniIsInvdni(String dni) throws Exception {
        expectProblem(mvc.perform(get("/clients/" + dni)), 400, "INVDNI", "DNI not valid");
        expectProblem(mvc.perform(delete("/clients/" + dni)), 400, "INVDNI", "DNI not valid");
        expectProblem(mvc.perform(post("/clients/" + dni).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ANA\"}")), 400, "INVDNI", "DNI not valid");
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @CsvSource({"999999", "100000000", "0", "-12345678"})
    void outOfRangeDniIsInvdniBeforeReachingRepository(long dni) throws Exception {
        expectProblem(mvc.perform(get("/clients/" + dni)), 400, "INVDNI", "DNI not valid");
        expectProblem(mvc.perform(put("/clients/" + dni).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ANA\"}")), 400, "INVDNI", "DNI not valid");
        expectProblem(mvc.perform(delete("/clients/" + dni)), 400, "INVDNI", "DNI not valid");
        verifyNoInteractions(repository);
    }

    @Test
    void invdniTakesPrecedenceOverBodyValidation() throws Exception {
        expectProblem(mvc.perform(post("/clients/999999").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}")), 400, "INVDNI", "DNI not valid");
        expectProblem(mvc.perform(post("/clients/abc").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}")), 400, "INVDNI", "DNI not valid");
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "{\"name\":\"\"}|name: must not be blank",
        "{\"name\":\"   \"}|name: must not be blank",
        "{}|name: must not be blank",
        "{\"name\":\"1234567890123456789012345678901\"}|name: size must be between 0 and 30",
    })
    void invalidNameIsValidation(String body, String detail) throws Exception {
        expectProblem(mvc.perform(post("/clients/12345678").contentType(MediaType.APPLICATION_JSON).content(body)),
                400, "VALIDATION", detail);
        expectProblem(mvc.perform(put("/clients/12345678").contentType(MediaType.APPLICATION_JSON).content(body)),
                400, "VALIDATION", detail);
        verifyNoInteractions(repository);
    }

    @Test
    void missingOrMalformedBodyIsValidation() throws Exception {
        mvc.perform(post("/clients/12345678").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION"));
        mvc.perform(put("/clients/12345678").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.detail").value(containsString("name")));
        verifyNoInteractions(repository);
    }
}
