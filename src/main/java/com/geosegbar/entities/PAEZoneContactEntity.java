package com.geosegbar.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.geosegbar.common.enums.PAEZoneTypeEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pae_zone_contact", indexes = {
    @Index(name = "idx_pae_zone_contact_pae_id", columnList = "pae_id"),
    @Index(name = "idx_pae_zone_contact_zone", columnList = "zone"),
    @Index(name = "idx_pae_zone_contact_pae_zone", columnList = "pae_id, zone")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PAEZoneContactEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pae_id", nullable = false)
    @JsonIgnoreProperties("contacts")
    private PAEEntity pae;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone", nullable = false, length = 3)
    private PAEZoneTypeEnum zone;

    @Column(name = "name")
    private String name;

    @Column(name = "role")
    private String role;

    @Column(name = "city")
    private String city;

    @Column(name = "state", length = 2)
    private String state;

    @Column(name = "phone")
    private String phone;

    @Email(message = "Email do contato inválido!")
    @Column(name = "email")
    private String email;
}
