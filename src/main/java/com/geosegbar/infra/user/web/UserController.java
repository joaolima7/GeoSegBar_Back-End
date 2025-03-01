package com.geosegbar.infra.user.web;

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
import com.geosegbar.core.user.entities.UserEntity;
import com.geosegbar.infra.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<UserEntity>>> getAllUsers() {
        List<UserEntity> users = userService.findAll();
        WebResponseEntity<List<UserEntity>> response = WebResponseEntity.success(users, "Usuários obtidos com sucesso!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<UserEntity>> getUserById(@PathVariable Long id) {
        UserEntity user = userService.findById(id);
        WebResponseEntity<UserEntity> response = WebResponseEntity.success(user, "Usuário obtido com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<WebResponseEntity<UserEntity>> createUser(@Valid @RequestBody UserEntity user) {
        UserEntity createdUser = userService.save(user);
        WebResponseEntity<UserEntity> response = WebResponseEntity.success(createdUser, "Usuário criado com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<UserEntity>> updateUser(@PathVariable Long id, @Valid @RequestBody UserEntity user) {
        user.setId(id);
        UserEntity updatedUser = userService.update(user);
        WebResponseEntity<UserEntity> response = WebResponseEntity.success(updatedUser, "Usuário atualizado com sucesso!");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, "Usuário deletado com sucesso!");
        return ResponseEntity.ok(response);
    }
    
}
