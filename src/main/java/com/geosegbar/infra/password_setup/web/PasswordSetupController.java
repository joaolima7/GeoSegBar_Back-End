package com.geosegbar.infra.password_setup.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.infra.password_setup.dtos.CompletePasswordSetupDTO;
import com.geosegbar.infra.password_setup.dtos.PasswordSetupInfoDTO;
import com.geosegbar.infra.password_setup.services.PasswordSetupService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Endpoints públicos (sem autenticação) do fluxo de primeiro acesso por link.
 * O usuário ainda não tem senha e portanto não tem como se autenticar aqui — o
 * próprio token de uso único é a credencial da operação.
 */
@RestController
@RequestMapping("/password-setup")
@RequiredArgsConstructor
public class PasswordSetupController {

    private final PasswordSetupService passwordSetupService;

    @GetMapping("/validate")
    public ResponseEntity<WebResponseEntity<PasswordSetupInfoDTO>> validate(@RequestParam String token) {
        PasswordSetupInfoDTO info = passwordSetupService.validateToken(token);
        return ResponseEntity.ok(WebResponseEntity.success(info, "Link válido!"));
    }

    @PostMapping("/complete")
    public ResponseEntity<WebResponseEntity<Void>> complete(@Valid @RequestBody CompletePasswordSetupDTO request) {
        passwordSetupService.completeSetup(request);
        return ResponseEntity.ok(WebResponseEntity.success(null,
                "Senha definida com sucesso! Você já pode entrar no sistema."));
    }
}
