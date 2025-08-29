package com.geosegbar.entities;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "dam_permissions",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"user_id", "dam_id", "client_id"})
        },
        indexes = {
            @Index(name = "idx_dam_perm_user_id", columnList = "user_id"),
            @Index(name = "idx_dam_perm_dam_id", columnList = "dam_id"),
            @Index(name = "idx_dam_perm_client_id", columnList = "client_id"),
            @Index(name = "idx_dam_perm_access", columnList = "has_access"),
            @Index(name = "idx_dam_perm_created_at", columnList = "created_at"),
            @Index(name = "idx_dam_perm_created_by", columnList = "created_by"),
            @Index(name = "idx_dam_perm_updated_by", columnList = "updated_by"),
            @Index(name = "idx_dam_perm_user_access", columnList = "user_id, has_access"),
            @Index(name = "idx_dam_perm_client_access", columnList = "client_id, has_access"),
            @Index(name = "idx_dam_perm_dam_access", columnList = "dam_id, has_access"),
            @Index(name = "idx_dam_perm_user_client", columnList = "user_id, client_id"),
            @Index(name = "idx_dam_perm_user_dam", columnList = "user_id, dam_id")
        }
)
public class DamPermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "dam_id", nullable = false)
    private DamEntity dam;

    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    @Column(name = "has_access", nullable = false)
    private Boolean hasAccess = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private UserEntity createdBy;

    @ManyToOne
    @JoinColumn(name = "updated_by")
    private UserEntity updatedBy;
}
