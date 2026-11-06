package com.example.clients.api;

import com.example.clients.domain.Client;
import com.example.clients.domain.Dni;
import com.example.clients.repository.ClientRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.beans.PropertyEditorSupport;
import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Contract B: REST surface over CLIECRUD operations C/R/U/D plus the active-client list. */
@RestController
@RequestMapping("/clients")
public class ClientController {

    static final String DNI = "dni";
    static final Pattern DECIMAL = Pattern.compile("-?\\d+");

    private final ClientRepository repository;

    public ClientController(ClientRepository repository) {
        this.repository = repository;
    }

    /** Plain decimal digits only (Spring would otherwise also decode "0x.." and "#.."); bad input is INVDNI. */
    @InitBinder
    void decimalDniOnly(WebDataBinder binder) {
        binder.registerCustomEditor(long.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || !DECIMAL.matcher(text).matches()) {
                    throw new IllegalArgumentException("DNI must be decimal digits");
                }
                setValue(Long.parseLong(text));
            }
        });
    }

    @GetMapping
    public List<Client> list() {
        return repository.findAllActive();
    }

    @GetMapping("/{dni}")
    public Client read(@PathVariable(DNI) @Min(Dni.MIN) @Max(Dni.MAX) long dni) {
        return repository.read(dni);
    }

    @PostMapping("/{dni}")
    public ResponseEntity<Client> create(@PathVariable(DNI) @Min(Dni.MIN) @Max(Dni.MAX) long dni,
            @Valid @RequestBody ClientNameRequest body) {
        Client created = repository.create(dni, body.name());
        return ResponseEntity.created(URI.create("/clients/" + dni)).body(created);
    }

    @PutMapping("/{dni}")
    public Client update(@PathVariable(DNI) @Min(Dni.MIN) @Max(Dni.MAX) long dni,
            @Valid @RequestBody ClientNameRequest body) {
        return repository.update(dni, body.name());
    }

    @DeleteMapping("/{dni}")
    public ResponseEntity<Void> delete(@PathVariable(DNI) @Min(Dni.MIN) @Max(Dni.MAX) long dni) {
        repository.delete(dni);
        return ResponseEntity.noContent().build();
    }
}
