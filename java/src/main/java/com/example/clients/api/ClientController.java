package com.example.clients.api;

import com.example.clients.domain.Client;
import com.example.clients.domain.ClientException;
import com.example.clients.domain.ClientsvCode;
import com.example.clients.domain.Dni;
import com.example.clients.service.ClientService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST facade over CLIENTSV. POST/GET/PUT/DELETE /clients/{dni} map to op C/R/U/D.
 * The DNI is checked before the body, so an invalid DNI is always INVDNI (RPG guard order).
 */
@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientService service;
    private final Validator validator;

    public ClientController(ClientService service, Validator validator) {
        this.service = service;
        this.validator = validator;
    }

    @GetMapping
    public List<Client> list(@RequestParam(defaultValue = "false") boolean includeDeleted) {
        return service.list(includeDeleted);
    }

    @PostMapping("/{dni}")
    public ResponseEntity<Client> create(@PathVariable("dni") String dni,
            @RequestBody(required = false) ClientNameRequest body) {
        long d = Dni.parse(dni);
        Client created = service.create(d, validName(body));
        return ResponseEntity.created(URI.create("/clients/" + d)).body(created);
    }

    @GetMapping("/{dni}")
    public Client read(@PathVariable("dni") String dni) {
        return service.read(Dni.parse(dni));
    }

    @PutMapping("/{dni}")
    public Client update(@PathVariable("dni") String dni, @RequestBody(required = false) ClientNameRequest body) {
        long d = Dni.parse(dni);
        return service.update(d, validName(body));
    }

    /** Soft delete: returns the record with deletedAt/deletedBy stamped. */
    @DeleteMapping("/{dni}")
    public Client delete(@PathVariable("dni") String dni) {
        return service.delete(Dni.parse(dni));
    }

    private String validName(ClientNameRequest body) {
        ClientNameRequest b = body != null ? body : new ClientNameRequest(null);
        var violations = validator.validate(b);
        if (!violations.isEmpty()) {
            String detail = violations.stream().map(ConstraintViolation::getMessage).sorted()
                    .collect(Collectors.joining("; "));
            throw new ClientException(ClientsvCode.VALIDATION, detail);
        }
        return b.name().strip();
    }
}
