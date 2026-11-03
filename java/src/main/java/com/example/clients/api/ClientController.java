package com.example.clients.api;

import com.example.clients.domain.Client;
import com.example.clients.repository.ClientRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
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
    public Client read(@PathVariable @ValidDni Long dni) {
        return repository.findActive(dni);
    }

    @PostMapping("/{dni}")
    public ResponseEntity<Client> create(@PathVariable @ValidDni Long dni,
                                         @RequestBody @Valid ClientNameRequest request) {
        Client created = repository.create(dni, request.name());
        return ResponseEntity.created(URI.create("/clients/" + dni)).body(created);
    }

    @PutMapping("/{dni}")
    public Client update(@PathVariable @ValidDni Long dni,
                         @RequestBody @Valid ClientNameRequest request) {
        return repository.update(dni, request.name());
    }

    @DeleteMapping("/{dni}")
    public ResponseEntity<Void> delete(@PathVariable @ValidDni Long dni) {
        repository.softDelete(dni);
        return ResponseEntity.noContent().build();
    }
}
