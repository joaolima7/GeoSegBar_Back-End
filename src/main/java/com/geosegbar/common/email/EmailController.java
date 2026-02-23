package com.geosegbar.common.email;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.email.dtos.SupportEmailRequestDTO;
import com.geosegbar.common.response.WebResponseEntity;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @PostMapping(value = "/support", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WebResponseEntity<Void>> sendSupportRequest(
            @Valid @ModelAttribute SupportEmailRequestDTO request) {
        emailService.sendSupportRequest(request);
        WebResponseEntity<Void> response = WebResponseEntity.success(null,
                "Solicitação de suporte recebida com sucesso! Em breve entraremos em contato.");
        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }
}
