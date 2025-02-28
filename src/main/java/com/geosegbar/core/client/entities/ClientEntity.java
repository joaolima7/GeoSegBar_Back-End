package com.geosegbar.core.client.entities;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.geosegbar.core.dam.entities.DamEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
@Entity
@Table(name = "client")
public class ClientEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome é obrigatório!")
    @Column(nullable = false, unique = true)
    private String name;

    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "O campo não pode conter números!")
    @Size(min = 3, max = 3, message = "A sigla deve ter 3 caracteres!")
    @Column(nullable = false, unique = true)
    private String acronym;

    @NotBlank(message = "Email é obrigatório!")
    @Email(message = "Email inválido!")
    @Column(nullable = false)
    private String email;

    @NotBlank(message = "O nome da rua é obrigatório!")
    @Column(nullable = false)
    private String street;

    @NotBlank(message = "O nome do bairro é obrigatório!")
    @Size(max = 100, message = "O nome do bairro deve ter no máximo 100 caracteres!")
    @Column(nullable = false, length = 100)
    private String neighborhood;

    @Pattern(regexp = "^[0-9]+$", message = "O número do endereço deve conter apenas números!")
    @Size(max = 10, message = "O número do endereço deve ter no máximo 10 caracteres!")
    @Column(length = 10)
    private String numberAddress;

    @NotBlank(message = "O nome da cidade é obrigatório!")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "Cidade não pode conter números!")
    @Size(max = 100, message = "O nome da cidade deve ter no máximo 100 caracteres!")
    @Column(nullable = false, length = 100)
    private String city;

    @NotBlank(message = "O nome do estado é obrigatório!")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "Estado não pode conter números!")
    @Size(max = 100, message = "O nome do estado deve ter no máximo 100 caracteres!")
    @Column(nullable = false, length = 100)
    private String state;

    @NotBlank(message = "CEP é obrigatório!")
    @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido!")
    @Column(nullable = false, length = 9)
    private String zipCode;

    @Size(max = 100, message = "O complemento deve ter no máximo 100 caracteres!")
    @Column(length = 100)
    private String complement;

    @NotBlank(message = "O telefone é obrigatório!")
    @Pattern(regexp = "^\\d{10,11}$", message = "O telefone deve conter 10 ou 11 dígitos numéricos!")
    @Column(nullable = false, length = 11)
    private String phone;

    @Pattern(regexp = "^\\d{10,11}$", message = "O WhatsApp deve conter 10 ou 11 dígitos numéricos!")
    @Size(max = 11, message = "O WhatsApp deve conter 10 ou 11 dígitos numéricos!")
    @Column(length = 11)
    private String whatsappPhone;

    @Email(message = "Email inválido!")
    @Size(max = 150, message = "O email de contato deve ter no máximo 150 caracteres!")
    @Column(length = 150)
    private String emailContact;

    @JsonIgnore
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<DamEntity> dams = new HashSet<>();
}
