package com.geosegbar.infra.address.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.WebResponseEntity;
import com.geosegbar.core.address.entities.AddressEntity;
import com.geosegbar.infra.address.service.AddressService;

@RestController
@RequestMapping("/address")
public class AddressController {
    
     private final AddressService addressService;
    
    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }
    
    @GetMapping
    public ResponseEntity<WebResponseEntity<List<AddressEntity>>> getAllAddresses() {
        List<AddressEntity> addresses = addressService.findAll();
        WebResponseEntity<List<AddressEntity>> response = WebResponseEntity.success(addresses, "Endereços obtidos com sucesso");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<AddressEntity>> getAddressById(@PathVariable Long id) {
        AddressEntity address = addressService.findById(id);
        WebResponseEntity<AddressEntity> response = WebResponseEntity.success(address, "Endereço obtido com sucesso");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    public ResponseEntity<WebResponseEntity<AddressEntity>> createAddress(@RequestBody AddressEntity address) {
        AddressEntity createdAddress = addressService.create(address);
        WebResponseEntity<AddressEntity> response = WebResponseEntity.success(createdAddress, "Endereço criado com sucesso");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<AddressEntity>> updateAddress(@PathVariable Long id, @RequestBody AddressEntity address) {
        address.setId(id);
        AddressEntity updatedAddress = addressService.update(address);
        WebResponseEntity<AddressEntity> response = WebResponseEntity.success(updatedAddress, "Endereço atualizado com sucesso");
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteAddress(@PathVariable Long id) {
        addressService.delete(id);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, "Endereço excluído com sucesso");
        return ResponseEntity.ok(response);
    }
}
