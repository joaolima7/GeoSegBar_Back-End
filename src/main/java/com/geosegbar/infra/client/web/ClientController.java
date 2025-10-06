package com.geosegbar.infra.client.web;

import java.util.List;

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

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.infra.client.dtos.ClientDTO;
import com.geosegbar.infra.client.service.ClientService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/client")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<ClientEntity>>> getAllClients() {
        List<ClientEntity> clients = clientService.findAll();
        WebResponseEntity<List<ClientEntity>> response = WebResponseEntity.success(clients, "Clientes obtidos com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<ClientEntity>> getClientById(@PathVariable Long id) {
        ClientEntity client = clientService.findById(id);
        WebResponseEntity<ClientEntity> response = WebResponseEntity.success(client, "Cliente obtido com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/filter")
    public ResponseEntity<WebResponseEntity<List<ClientEntity>>> getClientsByStatus(
            @RequestParam(required = false) Long statusId) {

        List<ClientEntity> clients = clientService.findByStatus(statusId);
        WebResponseEntity<List<ClientEntity>> response = WebResponseEntity.success(
                clients,
                "Clientes filtrados obtidos com sucesso!"
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<WebResponseEntity<ClientEntity>> createClient(@Valid @RequestBody ClientDTO clientDTO) {
        ClientEntity createdClient = clientService.save(clientDTO);
        WebResponseEntity<ClientEntity> response = WebResponseEntity.success(createdClient, "Cliente criado com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<ClientEntity>> updateClient(@PathVariable Long id, @Valid @RequestBody ClientDTO clientDTO) {
        clientDTO.setId(id);
        ClientEntity updatedClient = clientService.update(clientDTO);
        WebResponseEntity<ClientEntity> response = WebResponseEntity.success(updatedClient, "Cliente atualizado com sucesso!");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteClient(@PathVariable Long id) {
        clientService.deleteById(id);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, "Cliente deletado com sucesso!");
        return ResponseEntity.ok(response);
    }
}
