package com.geosegbar.entities;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.geosegbar.common.enums.FolderColorEnum;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "psb_folders", indexes = {
    @Index(name = "idx_psb_folder_dam_id", columnList = "dam_id"),
    @Index(name = "idx_psb_folder_created_by", columnList = "created_by"),
    @Index(name = "idx_psb_folder_dam_idx", columnList = "dam_id, folder_index"),
    @Index(name = "idx_psb_folder_dam_name", columnList = "dam_id, name"),
    @Index(name = "idx_psb_folder_updated_at", columnList = "updated_at")
})
public class PSBFolderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome da pasta é obrigatório")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "O índice da pasta é obrigatório")
    @Column(name = "folder_index", nullable = false)
    private Integer folderIndex;

    @Column(nullable = true, length = 1000)
    private String description;

    @Column(name = "server_path", nullable = false)
    private String serverPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "color", nullable = true)
    private FolderColorEnum color = FolderColorEnum.BLUE;

    @ManyToOne
    @JoinColumn(name = "dam_id", nullable = false)
    @JsonIgnoreProperties({"psbFolders", "reservoirs", "regulatoryDam", "documentationDam",
        "checklists", "checklistResponses", "damPermissions"})
    private DamEntity dam;

    @OneToMany(mappedBy = "psbFolder", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"psbFolder"})
    private Set<PSBFileEntity> files = new HashSet<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "created_by")
    @JsonIgnoreProperties({"psbFoldersCreated", "psbFilesUploaded"})
    private UserEntity createdBy;

    @OneToMany(mappedBy = "psbFolder", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"psbFolder"})
    private Set<ShareFolderEntity> shareLinks = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
