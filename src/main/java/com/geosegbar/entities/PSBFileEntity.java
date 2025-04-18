package com.geosegbar.entities;

import java.time.LocalDateTime;

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
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "psb_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PSBFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "O nome do arquivo é obrigatório")
    @Column(nullable = false)
    private String filename;
    
    @NotBlank(message = "O caminho do arquivo é obrigatório")
    @Column(name = "file_path", nullable = false)
    private String filePath;
    
    @Column(name = "original_filename")
    private String originalFilename;
    
    @Column(name = "content_type")
    private String contentType;
    
    @Column
    private Long size;
    
    @Column(name = "download_url")
    private String downloadUrl;
    
    @ManyToOne
    @JoinColumn(name = "psb_folder_id", nullable = false)
    @JsonIgnoreProperties({"files", "dam"})
    private PSBFolderEntity psbFolder;
    
    @ManyToOne
    @JoinColumn(name = "uploaded_by_id")
    @JsonIgnoreProperties({"psbFoldersCreated", "psbFilesUploaded"})
    private UserEntity uploadedBy;
    
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
    
    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }
}