package com.geosegbar.infra.psb.services;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.PSBFolderEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.psb.dtos.CreatePSBFolderRequest;
import com.geosegbar.infra.psb.dtos.PSBFolderCreationDTO;
import com.geosegbar.infra.psb.persistence.PSBFolderRepository;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PSBFolderService {

    @Value("${file.psb-dir:${file.upload-dir}/psb}")
    private String psbBaseDir;
    
    @Value("${file.base-url}")
    private String baseUrl;
    
    private final PSBFolderRepository psbFolderRepository;
    private final DamRepository damRepository;
    private final UserRepository userRepository;
    
    public List<PSBFolderEntity> findAllByDamId(Long damId) {
        return psbFolderRepository.findByDamIdOrderByFolderIndexAsc(damId);
    }
    
    public PSBFolderEntity findById(Long id) {
        return psbFolderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pasta PSB não encontrada"));
    }
    
    @Transactional
    public PSBFolderEntity create(CreatePSBFolderRequest request) {
        DamEntity dam = damRepository.findById(request.getDamId())
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada"));
        
        UserEntity currentUser = userRepository.findById(request.getCreatedById())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
                
        if (psbFolderRepository.existsByDamIdAndName(dam.getId(), request.getName())) {
            throw new DuplicateResourceException("Já existe uma pasta com este nome nesta barragem");
        }
        
        if (psbFolderRepository.existsByDamIdAndFolderIndex(dam.getId(), request.getFolderIndex())) {
            throw new DuplicateResourceException("Já existe uma pasta com este índice nesta barragem");
        }
        
        String folderPath = createFolderPath(dam.getId(), request.getFolderIndex(), request.getName());
        ensureDirectoryExists(folderPath);
        
        PSBFolderEntity folder = new PSBFolderEntity();
        folder.setName(request.getName());
        folder.setFolderIndex(request.getFolderIndex());
        folder.setDescription(request.getDescription());
        folder.setDam(dam);
        folder.setServerPath(folderPath);
        folder.setCreatedBy(currentUser);
        
        return psbFolderRepository.save(folder);
    }
    
    @Transactional
    public PSBFolderEntity update(Long id, CreatePSBFolderRequest request) {
        PSBFolderEntity existingFolder = psbFolderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pasta PSB não encontrada"));
        
        if (!existingFolder.getName().equals(request.getName()) && 
            psbFolderRepository.existsByDamIdAndName(existingFolder.getDam().getId(), request.getName())) {
            throw new DuplicateResourceException("Já existe uma pasta com este nome nesta barragem");
        }
        
        if (!existingFolder.getFolderIndex().equals(request.getFolderIndex()) &&
            psbFolderRepository.existsByDamIdAndFolderIndex(existingFolder.getDam().getId(), request.getFolderIndex())) {
            throw new DuplicateResourceException("Já existe uma pasta com este índice nesta barragem");
        }
        
        if (!existingFolder.getName().equals(request.getName()) || 
            !existingFolder.getFolderIndex().equals(request.getFolderIndex())) {
            
            String newFolderPath = createFolderPath(
                existingFolder.getDam().getId(), request.getFolderIndex(), request.getName());
            
            Path oldPath = Paths.get(existingFolder.getServerPath());
            Path newPath = Paths.get(newFolderPath);
            
            try {
                ensureDirectoryExists(newFolderPath);
                
                if (Files.exists(oldPath) && Files.isDirectory(oldPath)) {
                    Files.list(oldPath).forEach(file -> {
                        try {
                            Files.move(file, newPath.resolve(file.getFileName()));
                        } catch (Exception e) {
                            throw new RuntimeException("Falha ao mover arquivos: " + e.getMessage());
                        }
                    });
                    
                    Files.deleteIfExists(oldPath);
                }
                
                existingFolder.setServerPath(newFolderPath);
            } catch (Exception e) {
                throw new RuntimeException("Falha ao atualizar diretório: " + e.getMessage());
            }
        }
        
        existingFolder.setName(request.getName());
        existingFolder.setFolderIndex(request.getFolderIndex());
        existingFolder.setDescription(request.getDescription());
        existingFolder.setUpdatedAt(LocalDateTime.now());
        
        return psbFolderRepository.save(existingFolder);
    }
    
    @Transactional
    public void delete(Long id) {
        PSBFolderEntity folder = psbFolderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pasta PSB não encontrada"));
        
        try {
            Path folderPath = Paths.get(folder.getServerPath());
            if (Files.exists(folderPath)) {
                Files.walk(folderPath)
                    .sorted((a, b) -> b.toString().length() - a.toString().length()) 
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (Exception e) {
                            System.err.println("Erro ao deletar: " + path + ": " + e.getMessage());
                        }
                    });
            }
        } catch (Exception e) {
            System.err.println("Erro ao deletar diretório: " + e.getMessage());
        }
        
        psbFolderRepository.delete(folder);
    }
    
    private String createFolderPath(Long damId, Integer folderIndex, String folderName) {
        String normalizedName = folderName.trim()
        .toLowerCase()
        .replace('ç', 'c')
        .replace('á', 'a').replace('à', 'a').replace('ã', 'a').replace('â', 'a')
        .replace('é', 'e').replace('ê', 'e')
        .replace('í', 'i')
        .replace('ó', 'o').replace('ô', 'o').replace('õ', 'o')
        .replace('ú', 'u')
        .replaceAll("\\s+", "_")
        .replaceAll("[^a-z0-9_]", "");
        
    return Paths.get(psbBaseDir, 
            "dam-" + damId, 
            String.format("%03d", folderIndex) + "-" + normalizedName)
        .toString();
    }

    @Transactional
    public List<PSBFolderEntity> createMultipleFolders(DamEntity dam, List<PSBFolderCreationDTO> folderRequests, Long createdById) {
        UserEntity creator = userRepository.findById(createdById)
            .orElseThrow(() -> new NotFoundException("Usuário criador não encontrado"));

        List<PSBFolderEntity> createdFolders = new ArrayList<>();
        
        for (PSBFolderCreationDTO folderDTO : folderRequests) {
            if (psbFolderRepository.existsByDamIdAndName(dam.getId(), folderDTO.getName())) {
                throw new DuplicateResourceException("Já existe uma pasta com este nome nesta barragem: " + folderDTO.getName());
            }
            
            if (psbFolderRepository.existsByDamIdAndFolderIndex(dam.getId(), folderDTO.getFolderIndex())) {
                throw new DuplicateResourceException("Já existe uma pasta com este índice nesta barragem: " + folderDTO.getFolderIndex());
            }
            
            String folderPath = createFolderPath(dam.getId(), folderDTO.getFolderIndex(), folderDTO.getName());
            ensureDirectoryExists(folderPath);
            
            PSBFolderEntity folder = new PSBFolderEntity();
            folder.setName(folderDTO.getName());
            folder.setFolderIndex(folderDTO.getFolderIndex());
            folder.setDescription(folderDTO.getDescription());
            folder.setDam(dam);
            folder.setServerPath(folderPath);
            folder.setCreatedBy(creator);
            
            createdFolders.add(psbFolderRepository.save(folder));
        }
        
        return createdFolders;
    }
    
    private void ensureDirectoryExists(String dirPath) {
        File directory = new File(dirPath);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new RuntimeException("Não foi possível criar o diretório: " + dirPath);
            }
        }
    }
}