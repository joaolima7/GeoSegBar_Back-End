package com.geosegbar.infra.psb.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.PSBFolderEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.BusinessRuleException;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.psb.dtos.CreatePSBFolderRequest;
import com.geosegbar.infra.psb.dtos.PSBFolderCreationDTO;
import com.geosegbar.infra.psb.persistence.PSBFolderRepository;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PSBFolderService {

    @Value("${file.psb-dir:${file.upload-dir}/psb}")
    private String psbBaseDir;

    private final PSBFolderRepository psbFolderRepository;
    private final DamRepository damRepository;
    private final UserRepository userRepository;

    public List<PSBFolderEntity> findAllByDamId(Long damId) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            if (!AuthenticatedUserUtil.getCurrentUser().getDocumentationPermission().getViewPSB()) {
                throw new NotFoundException("Usuário não tem permissão para acessar as pastas PSB");
            }
        }
        return psbFolderRepository.findByDamIdAndParentFolderIsNullOrderByFolderIndexAsc(damId);
    }

    public List<PSBFolderEntity> findSubfolders(Long parentFolderId) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            if (!AuthenticatedUserUtil.getCurrentUser().getDocumentationPermission().getViewPSB()) {
                throw new NotFoundException("Usuário não tem permissão para acessar as pastas PSB");
            }
        }
        psbFolderRepository.findById(parentFolderId)
                .orElseThrow(() -> new NotFoundException("Pasta pai não encontrada"));
        return psbFolderRepository.findByParentFolderIdOrderByFolderIndexAsc(parentFolderId);
    }

    @Transactional(readOnly = true)
    public List<PSBFolderEntity> findCompleteHierarchyByDamId(Long damId) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            if (!AuthenticatedUserUtil.getCurrentUser().getDocumentationPermission().getViewPSB()) {
                throw new NotFoundException("Usuário não tem permissão para acessar as pastas PSB");
            }
        }

        damRepository.findById(damId)
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada"));

        List<PSBFolderEntity> rootFolders = psbFolderRepository.findCompleteHierarchyByDamId(damId);

        rootFolders.forEach(this::initializeSubfolders);

        return rootFolders;
    }

    private void initializeSubfolders(PSBFolderEntity folder) {

        folder.getFiles().size();

        folder.getSubfolders().forEach(subfolder -> {
            subfolder.getFiles().size();
            initializeSubfolders(subfolder);
        });
    }

    public PSBFolderEntity findById(Long id) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            if (!AuthenticatedUserUtil.getCurrentUser().getDocumentationPermission().getViewPSB()) {
                throw new NotFoundException("Usuário não tem permissão para acessar as pastas PSB");
            }
        }
        return psbFolderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pasta PSB não encontrada"));
    }

    @Transactional
    public PSBFolderEntity create(CreatePSBFolderRequest request) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            if (!AuthenticatedUserUtil.getCurrentUser().getDocumentationPermission().getEditPSB()) {
                throw new NotFoundException("Usuário não tem permissão para criar pastas PSB!");
            }
        }
        DamEntity dam = damRepository.findById(request.getDamId())
                .orElseThrow(() -> new NotFoundException("Barragem não encontrada"));

        UserEntity currentUser = userRepository.findById(request.getCreatedById())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        PSBFolderEntity parentFolder = null;

        if (request.getParentFolderId() != null) {
            parentFolder = psbFolderRepository.findById(request.getParentFolderId())
                    .orElseThrow(() -> new NotFoundException("Pasta pai não encontrada"));

            if (!parentFolder.getDam().getId().equals(dam.getId())) {
                throw new BusinessRuleException("A pasta pai deve pertencer à mesma barragem");
            }
        }

        if (parentFolder != null) {
            if (psbFolderRepository.existsByDamIdAndNameAndParentFolderId(
                    dam.getId(), request.getName(), parentFolder.getId())) {
                throw new DuplicateResourceException("Já existe uma pasta com este nome neste nível");
            }
        } else {
            if (psbFolderRepository.existsByDamIdAndNameAndParentFolderIsNull(
                    dam.getId(), request.getName())) {
                throw new DuplicateResourceException("Já existe uma pasta raiz com este nome");
            }
        }

        if (parentFolder != null) {
            if (psbFolderRepository.existsByParentFolderIdAndFolderIndex(
                    parentFolder.getId(), request.getFolderIndex())) {
                throw new DuplicateResourceException("Já existe uma pasta com este índice neste nível");
            }
        } else {
            if (psbFolderRepository.existsByDamIdAndFolderIndexAndParentFolderIsNull(
                    dam.getId(), request.getFolderIndex())) {
                throw new DuplicateResourceException("Já existe uma pasta raiz com este índice");
            }
        }

        String folderPath = createHierarchicalFolderPath(dam.getId(), parentFolder,
                request.getFolderIndex(), request.getName());
        ensureDirectoryExists(folderPath);

        PSBFolderEntity folder = new PSBFolderEntity();
        folder.setName(request.getName());
        folder.setFolderIndex(request.getFolderIndex());
        folder.setDescription(request.getDescription());
        folder.setDam(dam);
        folder.setParentFolder(parentFolder);
        folder.setServerPath(folderPath);
        folder.setCreatedBy(currentUser);
        folder.setColor(request.getColor());

        return psbFolderRepository.save(folder);
    }

    @Transactional
    public PSBFolderEntity update(Long id, CreatePSBFolderRequest request) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            if (!AuthenticatedUserUtil.getCurrentUser().getDocumentationPermission().getEditPSB()) {
                throw new NotFoundException("Usuário não tem permissão para editar pastas do PSB!");
            }
        }

        PSBFolderEntity existingFolder = psbFolderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pasta PSB não encontrada"));

        PSBFolderEntity newParent = null;
        if (request.getParentFolderId() != null) {
            newParent = psbFolderRepository.findById(request.getParentFolderId())
                    .orElseThrow(() -> new NotFoundException("Pasta pai não encontrada"));

            if (isDescendant(existingFolder, newParent)) {
                throw new BusinessRuleException(
                        "Não é possível mover uma pasta para dentro de si mesma ou de suas subpastas");
            }

            if (!newParent.getDam().getId().equals(existingFolder.getDam().getId())) {
                throw new BusinessRuleException("A pasta pai deve pertencer à mesma barragem");
            }
        }

        PSBFolderEntity currentParent = existingFolder.getParentFolder();
        Long currentParentId = currentParent != null ? currentParent.getId() : null;
        Long newParentId = request.getParentFolderId();

        boolean nameChanged = !existingFolder.getName().equals(request.getName());
        boolean indexChanged = !existingFolder.getFolderIndex().equals(request.getFolderIndex());
        boolean parentChanged = (currentParentId == null && newParentId != null)
                || (currentParentId != null && !currentParentId.equals(newParentId))
                || (currentParentId != null && newParentId == null);

        if (nameChanged || parentChanged) {
            if (newParentId != null) {
                List<PSBFolderEntity> siblings = psbFolderRepository
                        .findByParentFolderIdOrderByFolderIndexAsc(newParentId);
                for (PSBFolderEntity sibling : siblings) {
                    if (!sibling.getId().equals(id) && sibling.getName().equals(request.getName())) {
                        throw new DuplicateResourceException("Já existe uma pasta com este nome neste nível");
                    }
                }
            } else {
                List<PSBFolderEntity> siblings = psbFolderRepository
                        .findByDamIdAndParentFolderIsNullOrderByFolderIndexAsc(existingFolder.getDam().getId());
                for (PSBFolderEntity sibling : siblings) {
                    if (!sibling.getId().equals(id) && sibling.getName().equals(request.getName())) {
                        throw new DuplicateResourceException("Já existe uma pasta raiz com este nome");
                    }
                }
            }
        }

        if (indexChanged || parentChanged) {
            if (newParentId != null) {
                List<PSBFolderEntity> siblings = psbFolderRepository
                        .findByParentFolderIdOrderByFolderIndexAsc(newParentId);
                for (PSBFolderEntity sibling : siblings) {
                    if (!sibling.getId().equals(id)
                            && sibling.getFolderIndex().equals(request.getFolderIndex())) {
                        throw new DuplicateResourceException("Já existe uma pasta com este índice neste nível");
                    }
                }
            } else {
                List<PSBFolderEntity> siblings = psbFolderRepository
                        .findByDamIdAndParentFolderIsNullOrderByFolderIndexAsc(existingFolder.getDam().getId());
                for (PSBFolderEntity sibling : siblings) {
                    if (!sibling.getId().equals(id)
                            && sibling.getFolderIndex().equals(request.getFolderIndex())) {
                        throw new DuplicateResourceException("Já existe uma pasta raiz com este índice");
                    }
                }
            }
        }

        if (nameChanged || indexChanged || parentChanged) {
            String newFolderPath = createHierarchicalFolderPath(
                    existingFolder.getDam().getId(), newParent,
                    request.getFolderIndex(), request.getName());

            try {
                Path oldPath = Paths.get(existingFolder.getServerPath());
                Path newPath = Paths.get(newFolderPath);

                if (Files.exists(oldPath)) {
                    Files.createDirectories(newPath.getParent());
                    Files.move(oldPath, newPath);
                    log.info("Pasta movida de {} para {}", oldPath, newPath);
                } else {
                    ensureDirectoryExists(newFolderPath);
                }

                existingFolder.setServerPath(newFolderPath);

                updateSubfoldersPath(existingFolder);

            } catch (IOException e) {
                log.error("Erro ao mover pasta: {}", e.getMessage());
                throw new BusinessRuleException("Erro ao mover pasta no sistema de arquivos");
            }
        }

        existingFolder.setName(request.getName());
        existingFolder.setFolderIndex(request.getFolderIndex());
        existingFolder.setDescription(request.getDescription());
        existingFolder.setUpdatedAt(LocalDateTime.now());
        existingFolder.setColor(request.getColor());

        if (parentChanged) {
            existingFolder.setParentFolder(newParent);
        }

        return psbFolderRepository.save(existingFolder);
    }

    @Transactional
    public void delete(Long id) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            if (!AuthenticatedUserUtil.getCurrentUser().getDocumentationPermission().getEditPSB()) {
                throw new NotFoundException("Usuário não tem permissão para deletar pastas do PSB!");
            }
        }

        PSBFolderEntity folderToDelete = psbFolderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pasta PSB não encontrada"));

        Long damId = folderToDelete.getDam().getId();
        Integer deletedFolderIndex = folderToDelete.getFolderIndex();
        PSBFolderEntity parentFolder = folderToDelete.getParentFolder();

        try {
            Path folderPath = Paths.get(folderToDelete.getServerPath());
            if (Files.exists(folderPath)) {
                Files.walk(folderPath)
                        .sorted((a, b) -> b.toString().length() - a.toString().length())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (Exception e) {
                                log.error("Erro ao deletar: " + path + ": " + e.getMessage());
                            }
                        });
                log.info("Pasta e subpastas excluídas fisicamente: {}", folderPath);
            }
        } catch (Exception e) {
            log.error("Erro ao deletar diretório: {}", e.getMessage());
        }

        psbFolderRepository.delete(folderToDelete);

        List<PSBFolderEntity> foldersToReindex;
        if (parentFolder != null) {
            foldersToReindex = psbFolderRepository
                    .findByParentFolderIdAndFolderIndexGreaterThanOrderByFolderIndexAsc(
                            parentFolder.getId(), deletedFolderIndex);
        } else {
            foldersToReindex = psbFolderRepository
                    .findByDamIdAndParentFolderIsNullAndFolderIndexGreaterThanOrderByFolderIndexAsc(
                            damId, deletedFolderIndex);
        }

        for (PSBFolderEntity folder : foldersToReindex) {
            Integer oldIndex = folder.getFolderIndex();
            Integer newIndex = oldIndex - 1;

            String oldFolderPath = folder.getServerPath();
            String newFolderPath = createHierarchicalFolderPath(
                    damId, folder.getParentFolder(), newIndex, folder.getName());

            try {
                Path sourcePath = Paths.get(oldFolderPath);
                Path targetPath = Paths.get(newFolderPath);

                if (Files.exists(sourcePath) && !sourcePath.equals(targetPath)) {
                    Files.move(sourcePath, targetPath);
                    log.info("Pasta reindexada de {} para {}", sourcePath, targetPath);
                }

                folder.setFolderIndex(newIndex);
                folder.setServerPath(newFolderPath);
                folder.setUpdatedAt(LocalDateTime.now());

                updateSubfoldersPath(folder);

            } catch (IOException e) {
                log.error("Erro ao reindexar pasta {}: {}", folder.getName(), e.getMessage());
            }
        }

        psbFolderRepository.saveAll(foldersToReindex);
    }

    private String createFolderPath(Long damId, Integer folderIndex, String folderName) {
        return createHierarchicalFolderPath(damId, null, folderIndex, folderName);
    }

    private String createHierarchicalFolderPath(Long damId, PSBFolderEntity parentFolder,
            Integer folderIndex, String folderName) {
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

        String folderDirName = String.format("%03d", folderIndex) + "-" + normalizedName;

        if (parentFolder != null) {

            return Paths.get(parentFolder.getServerPath(), folderDirName).toString();
        } else {

            return Paths.get(psbBaseDir, "dam-" + damId, folderDirName).toString();
        }
    }

    @Transactional
    public List<PSBFolderEntity> createMultipleFolders(DamEntity dam, List<PSBFolderCreationDTO> folderRequests, Long createdById) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            if (!AuthenticatedUserUtil.getCurrentUser().getDocumentationPermission().getEditPSB()) {
                throw new NotFoundException("Usuário não tem permissão para criar pastas PSB!");
            }
        }
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
            folder.setColor(folderDTO.getColor());

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

    private void updateSubfoldersPath(PSBFolderEntity folder) {
        List<PSBFolderEntity> subfolders = psbFolderRepository
                .findByParentFolderIdOrderByFolderIndexAsc(folder.getId());

        for (PSBFolderEntity subfolder : subfolders) {
            String newSubfolderPath = createHierarchicalFolderPath(
                    folder.getDam().getId(), folder, subfolder.getFolderIndex(), subfolder.getName());

            try {
                Path oldPath = Paths.get(subfolder.getServerPath());
                Path newPath = Paths.get(newSubfolderPath);

                if (Files.exists(oldPath) && !oldPath.equals(newPath)) {
                    Files.move(oldPath, newPath);
                    log.info("Subpasta movida de {} para {}", oldPath, newPath);
                }

                subfolder.setServerPath(newSubfolderPath);
                psbFolderRepository.save(subfolder);

                updateSubfoldersPath(subfolder);

            } catch (IOException e) {
                log.error("Erro ao atualizar caminho da subpasta {}: {}",
                        subfolder.getName(), e.getMessage());
            }
        }
    }

    private boolean isDescendant(PSBFolderEntity ancestor, PSBFolderEntity potentialDescendant) {
        if (ancestor.getId().equals(potentialDescendant.getId())) {
            return true;
        }

        PSBFolderEntity current = potentialDescendant.getParentFolder();
        while (current != null) {
            if (current.getId().equals(ancestor.getId())) {
                return true;
            }
            current = current.getParentFolder();
        }

        return false;
    }
}
