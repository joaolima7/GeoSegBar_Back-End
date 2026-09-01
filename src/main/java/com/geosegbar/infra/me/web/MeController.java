package com.geosegbar.infra.me.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.infra.me.dtos.MePermissionsDTO;
import com.geosegbar.infra.me.services.MeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

    private final MeService meService;

    /**
     * Sem parâmetro: o usuário vem do token. Uma chamada após o login basta
     * para o app saber tudo sobre acesso e guardar para uso offline.
     */
    @GetMapping("/permissions")
    public ResponseEntity<WebResponseEntity<MePermissionsDTO>> getMyPermissions() {
        return ResponseEntity.ok(WebResponseEntity.success(
                meService.currentUserPermissions(),
                "Permissões do usuário obtidas com sucesso!"));
    }
}
