package com.geosegbar.infra.client.dtos;

import java.util.Set;

import com.geosegbar.entities.StatusEntity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientDTO {

    private Long id;

    @NotBlank(message = "Nome é obrigatório!")
    private String name;

    @NotBlank(message = "Email é obrigatório!")
    @Email(message = "Email inválido!")
    private String email;

    @NotBlank(message = "O nome da rua é obrigatório!")
    private String street;

    @NotBlank(message = "O nome do bairro é obrigatório!")
    @Size(max = 100, message = "O nome do bairro deve ter no máximo 100 caracteres!")
    private String neighborhood;

    @Size(max = 10, message = "O número do endereço deve ter no máximo 10 caracteres!")
    private String numberAddress;

    @NotBlank(message = "O nome da cidade é obrigatório!")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "Cidade não pode conter números!")
    @Size(max = 100, message = "O nome da cidade deve ter no máximo 100 caracteres!")
    private String city;

    @NotBlank(message = "O nome do estado é obrigatório!")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "Estado não pode conter números!")
    @Size(max = 100, message = "O nome do estado deve ter no máximo 100 caracteres!")
    private String state;

    @NotBlank(message = "CEP é obrigatório!")
    @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido!")
    private String zipCode;

    @Size(max = 100, message = "O complemento deve ter no máximo 100 caracteres!")
    private String complement;

    @NotBlank(message = "O telefone é obrigatório!")
    @Pattern(regexp = "^\\d{10,11}$", message = "O telefone deve conter 10 ou 11 dígitos numéricos!")
    private String phone;

    @Pattern(regexp = "^\\d{10,11}$", message = "O WhatsApp deve conter 10 ou 11 dígitos numéricos!")
    @Size(max = 11, message = "O WhatsApp deve conter 10 ou 11 dígitos numéricos!")
    private String whatsappPhone;

    @Email(message = "Email inválido!")
    @Size(max = 150, message = "O email de contato deve ter no máximo 150 caracteres!")
    private String emailContact;

    private String logoBase64;

    private StatusEntity status;

    private Set<Long> userIds;
}
