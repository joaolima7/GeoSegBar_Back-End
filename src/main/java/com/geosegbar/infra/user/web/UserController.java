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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.infra.user.dto.LoginRequestDTO;
import com.geosegbar.infra.user.dto.LoginResponseDTO;
import com.geosegbar.infra.user.dto.UserClientAssociationDTO;
import com.geosegbar.infra.user.dto.UserCreateDTO;
import com.geosegbar.infra.user.dto.UserPasswordUpdateDTO;
import com.geosegbar.infra.user.dto.UserUpdateDTO;
import com.geosegbar.infra.user.service.UserService;
import com.geosegbar.infra.verification_code.dto.ForgotPasswordRequestDTO;
import com.geosegbar.infra.verification_code.dto.ResetPasswordRequestDTO;
import com.geosegbar.infra.verification_code.dto.VerifyCodeRequestDTO;

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

    @GetMapping("/filter")
    public ResponseEntity<WebResponseEntity<List<UserEntity>>> getUsersByRoleAndClient(
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Long clientId) {
        
        List<UserEntity> users = userService.findByRoleAndClient(roleId, clientId);
        WebResponseEntity<List<UserEntity>> response = WebResponseEntity.success(
            users, 
            "Usuários filtrados obtidos com sucesso!"
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<WebResponseEntity<UserEntity>> createUser(@Valid @RequestBody UserCreateDTO userDTO) {
        UserEntity createdUser = userService.save(userDTO);
        WebResponseEntity<UserEntity> response = WebResponseEntity.success(createdUser, "Usuário criado com sucesso!");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login/initiate")
    public ResponseEntity<WebResponseEntity<Void>> initiateLogin(@Valid @RequestBody LoginRequestDTO userDTO) {
        userService.initiateLogin(userDTO);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, 
            "Código de verificação enviado para seu email, verifique também a caixa de spam!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login/verify")
    public ResponseEntity<WebResponseEntity<LoginResponseDTO>> verifyAndLogin(@Valid @RequestBody VerifyCodeRequestDTO verifyRequest) {
        LoginResponseDTO loggedUser = userService.verifyCodeAndLogin(verifyRequest);
        WebResponseEntity<LoginResponseDTO> response = WebResponseEntity.success(loggedUser, 
            "Usuário autenticado com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<WebResponseEntity<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO requestDTO) {
        userService.initiatePasswordReset(requestDTO);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, 
            "Código de redefinição de senha enviado para seu email, verifique também a caixa de spam!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<WebResponseEntity<Boolean>> verifyResetCode(@Valid @RequestBody VerifyCodeRequestDTO verifyRequest) {
        boolean valid = userService.verifyResetCode(verifyRequest);
        WebResponseEntity<Boolean> response = WebResponseEntity.success(valid, 
            "Código verificado com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<WebResponseEntity<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO requestDTO) {
        userService.resetPassword(requestDTO);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, 
            "Senha redefinida com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebResponseEntity<UserEntity>> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO user) {
        UserEntity updatedUser = userService.update(id, user);
        WebResponseEntity<UserEntity> response = WebResponseEntity.success(updatedUser, "Usuário atualizado com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<WebResponseEntity<UserEntity>> updatePassword(@PathVariable Long id, @Valid @RequestBody UserPasswordUpdateDTO passwordDTO) {
        UserEntity updatedUser = userService.updatePassword(id, passwordDTO);
        WebResponseEntity<UserEntity> response = WebResponseEntity.success(updatedUser, "Senha atualizada com sucesso!");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/clients")
    public ResponseEntity<WebResponseEntity<UserEntity>> updateUserClients(
        @PathVariable Long id, 
        @RequestBody UserClientAssociationDTO clientAssociationDTO) {
        
        UserEntity updatedUser = userService.updateUserClients(id, clientAssociationDTO);
        WebResponseEntity<UserEntity> response = 
            WebResponseEntity.success(updatedUser, "Clientes do usuário atualizados com sucesso!");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<WebResponseEntity<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        WebResponseEntity<Void> response = WebResponseEntity.success(null, "Usuário deletado com sucesso!");
        return ResponseEntity.ok(response);
    }
    
}
