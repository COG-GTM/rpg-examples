package com.example.clients.api;

import com.example.clients.domain.Client;
import com.example.clients.domain.ValidDni;
import com.example.clients.repository.ClientRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientRepository repository;

    public ClientController(ClientRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Client> list() {
        return repository.findAllActive();
    }

    @GetMapping("/{dni}")
    public Client read(@PathVariable @ValidDni long dni) {
        return repository.read(dni);
    }

    @PostMapping("/{dni}")
    @ResponseStatus(HttpStatus.CREATED)
    public Client create(@PathVariable @ValidDni long dni, @Valid @RequestBody ClientRequest body) {
        repository.create(dni, body.name());
        return repository.read(dni);
    }

    @PutMapping("/{dni}")
    public Client update(@PathVariable @ValidDni long dni, @Valid @RequestBody ClientRequest body) {
        repository.update(dni, body.name());
        return repository.read(dni);
    }

    @DeleteMapping("/{dni}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @ValidDni long dni) {
        repository.delete(dni);
    }
}
