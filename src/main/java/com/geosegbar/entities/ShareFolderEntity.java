package com.geosegbar.entities;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "share_folders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ShareFolderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "A pasta PSB é obrigatória")
    @ManyToOne
    @JoinColumn(name = "psb_folder_id", nullable = false)
    @JsonIgnoreProperties({"shareLinks", "files", "dam", "createdBy"})
    private PSBFolderEntity psbFolder;
    
    @NotNull(message = "O usuário que compartilha é obrigatório")
    @ManyToOne
    @JoinColumn(name = "shared_by_id", nullable = false)
    @JsonIgnoreProperties({"psbFoldersCreated", "psbFilesUploaded", "sharedFolders"})
    private UserEntity sharedBy;
    
    @NotBlank(message = "O email do destinatário é obrigatório")
    @Email(message = "Email inválido")
    @Column(name = "shared_with_email", nullable = false)
    private String sharedWithEmail;
    
    @Column(name = "access_count", nullable = false)
    private Integer accessCount = 0;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;
    
    @Column(name = "expires_at", nullable = true)
    private LocalDateTime expiresAt;
    
    @Column(name = "token", nullable = false, unique = true, length = 36)
    private String token;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.token == null) {
            this.token = UUID.randomUUID().toString();
        }
    }
    
    public void incrementAccessCount() {
        this.accessCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }
}