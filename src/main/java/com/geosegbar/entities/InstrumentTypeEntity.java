package com.geosegbar.entities;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tipo de instrumento — catálogo por cliente.
 *
 * Cada cliente tem o seu próprio conjunto de tipos, e uma barragem só pode usar
 * tipos do cliente dono dela. Duas consequências que valem registrar:
 *
 * - Renomear um tipo reflete em todas as barragens <em>daquele cliente</em>,
 *   porque o catálogo é compartilhado dentro do cliente. Nunca atravessa
 *   clientes.
 * - Nomes iguais em clientes diferentes são permitidos, e por isso a
 *   unicidade é por (client_id, name), não por name.
 */
@Entity
@Table(name = "instrument_type",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_instrument_type_client_name", columnNames = {"client_id", "name"})
        },
        indexes = {
            @Index(name = "idx_instrument_type_name_search", columnList = "name"),
            @Index(name = "idx_instrument_type_client_id", columnList = "client_id"),
            @Index(name = "idx_instrument_type_client_name", columnList = "client_id, name")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InstrumentTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome do tipo de instrumento é obrigatório")
    @Column(nullable = false)
    private String name;

    /**
     * Cliente dono do tipo. A coluna é nullable no banco só para permitir que a
     * migração aconteça em duas etapas — subir o código e depois atrelar os tipos
     * já existentes a um cliente. A obrigatoriedade é imposta no serviço: nenhum
     * tipo novo é criado sem cliente, e tipo sem cliente (legado) não pode ser
     * editado nem excluído, justamente para não replicar alteração entre clientes.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", foreignKey = @ForeignKey(name = "fk_instrument_type_client"))
    @JsonIgnoreProperties({"dams", "users", "damPermissions", "questions", "hibernateLazyInitializer", "handler"})
    private ClientEntity client;

    @JsonIgnore
    @OneToMany(mappedBy = "instrumentType")
    private Set<InstrumentEntity> instruments = new HashSet<>();
}
