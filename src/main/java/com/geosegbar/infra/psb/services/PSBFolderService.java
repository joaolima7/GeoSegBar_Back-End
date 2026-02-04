package com.geosegbar.infra.psb.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.PSBFileEntity;
import com.geosegbar.entities.PSBFolderEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.BusinessRuleException;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.file_storage.FileStorageService;
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

    private final PSBFolderRepository psbFolderRepository;
    private final DamRepository damRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<PSBFolderEntity> findAllByDamId(Long damId) {
        validateViewPermission();
        return psbFolderRepository.findByDamIdAndParentFolderIsNullOrderByFolderIndexAsc(damId);
    }

    @Transactional(readOnly = true)
    public List<PSBFolderEntity> findSubfolders(Long parentFolderId) {
        validateViewPermission();

        if (!psbFolderRepository.existsById(parentFolderId)) {
            throw new NotFoundException("Pasta pai não encontrada");
        }
        return psbFolderRepository.findByParentFolderIdOrderByFolderIndexAsc(parentFolderId);
    }

    @Transactional(readOnly = true)
    public List<PSBFolderEntity> findCompleteHierarchyByDamId(Long damId) {
        validateViewPermission();

        if (!damRepository.existsById(damId)) {
            throw new NotFoundException("Barragem não encontrada");
        }

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

    @Transactional(readOnly = true)
    public PSBFolderEntity findById(Long id) {
        validateViewPermission();
        PSBFolderEntity folder = psbFolderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pasta PSB não encontrada"));

        if (folder.getParentFolder() != null) {
            folder.getParentFolder().getName();
        }
        folder.getFiles().size();
        initializeSubfolders(folder);

        return folder;
    }

    @Transactional
    public PSBFolderEntity create(CreatePSBFolderRequest request) {
        validateEditPermission();

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

        validateDuplicates(dam.getId(), request.getName(), request.getFolderIndex(), parentFolder);

        String folderPath = createHierarchicalFolderPath(dam.getId(), parentFolder,
                request.getFolderIndex(), request.getName());

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
        validateEditPermission();

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

        if (nameChanged || indexChanged || parentChanged) {
            validateDuplicatesForUpdate(existingFolder, request.getName(), request.getFolderIndex(), newParentId);
        }

        if (nameChanged || indexChanged || parentChanged) {
            String newFolderPath = createHierarchicalFolderPath(
                    existingFolder.getDam().getId(), newParent,
                    request.getFolderIndex(), request.getName());

            existingFolder.setServerPath(newFolderPath);
            updateSubfoldersPath(existingFolder);
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
        validateEditPermission();

        PSBFolderEntity folderToDelete = psbFolderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pasta PSB não encontrada"));

        Long damId = folderToDelete.getDam().getId();
        Integer deletedFolderIndex = folderToDelete.getFolderIndex();
        PSBFolderEntity parentFolder = folderToDelete.getParentFolder();

        deleteS3ContentRecursively(folderToDelete);

        psbFolderRepository.delete(folderToDelete);

        reindexSiblingsAfterDelete(damId, parentFolder, deletedFolderIndex);
    }

    private void deleteS3ContentRecursively(PSBFolderEntity folder) {

        if (folder.getFiles() != null) {
            for (PSBFileEntity file : folder.getFiles()) {
                try {
                    fileStorageService.deleteFile(file.getDownloadUrl());
                } catch (Exception e) {
                    log.error("Erro ao deletar arquivo S3 {}: {}", file.getFilename(), e.getMessage());
                }
            }
        }

        if (folder.getSubfolders() != null) {
            for (PSBFolderEntity subfolder : folder.getSubfolders()) {
                deleteS3ContentRecursively(subfolder);
            }
        }
    }

    private void reindexSiblingsAfterDelete(Long damId, PSBFolderEntity parentFolder, Integer deletedFolderIndex) {
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
            Integer newIndex = folder.getFolderIndex() - 1;

            String newFolderPath = createHierarchicalFolderPath(
                    damId, folder.getParentFolder(), newIndex, folder.getName());

            folder.setFolderIndex(newIndex);
            folder.setServerPath(newFolderPath);
            folder.setUpdatedAt(LocalDateTime.now());

            updateSubfoldersPath(folder);
        }

        if (!foldersToReindex.isEmpty()) {
            psbFolderRepository.saveAll(foldersToReindex);
        }
    }

    @Transactional
    public List<PSBFolderEntity> createMultipleFolders(DamEntity dam, List<PSBFolderCreationDTO> folderRequests, Long createdById) {
        validateEditPermission();

        UserEntity creator = userRepository.findById(createdById)
                .orElseThrow(() -> new NotFoundException("Usuário criador não encontrado"));

        List<PSBFolderEntity> createdFolders = new ArrayList<>();

        for (PSBFolderCreationDTO folderDTO : folderRequests) {

            validateDuplicates(dam.getId(), folderDTO.getName(), folderDTO.getFolderIndex(), null);

            String folderPath = createHierarchicalFolderPath(dam.getId(), null,
                    folderDTO.getFolderIndex(), folderDTO.getName());

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

            String parentPath = parentFolder.getServerPath();
            if (!parentPath.endsWith("/")) {
                parentPath += "/";
            }
            return parentPath + folderDirName;
        } else {

            return "dam-" + damId + "/" + folderDirName;
        }
    }

    private void updateSubfoldersPath(PSBFolderEntity folder) {

        List<PSBFolderEntity> subfolders = psbFolderRepository
                .findByParentFolderIdOrderByFolderIndexAsc(folder.getId());

        for (PSBFolderEntity subfolder : subfolders) {
            String newSubfolderPath = createHierarchicalFolderPath(
                    folder.getDam().getId(), folder, subfolder.getFolderIndex(), subfolder.getName());

            subfolder.setServerPath(newSubfolderPath);
            psbFolderRepository.save(subfolder);

            updateSubfoldersPath(subfolder);
        }
    }

    @Transactional
    public void syncRootFolders(DamEntity dam, List<com.geosegbar.infra.psb.dtos.PSBFolderUpdateDTO> psbFolderDTOs,
            Long updatedById) {
        if (psbFolderDTOs == null || psbFolderDTOs.isEmpty()) {
            return;
        }

        UserEntity updater = userRepository.findById(updatedById)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        List<PSBFolderEntity> existingRootFolders = psbFolderRepository
                .findByDamIdAndParentFolderIsNullOrderByFolderIndexAsc(dam.getId());

        List<Long> sentFolderIds = psbFolderDTOs.stream()
                .map(com.geosegbar.infra.psb.dtos.PSBFolderUpdateDTO::getId)
                .filter(id -> id != null)
                .toList();

        for (PSBFolderEntity existingFolder : existingRootFolders) {
            if (!sentFolderIds.contains(existingFolder.getId())) {
                log.info("Deletando pasta raiz não enviada: {}", existingFolder.getName());
                delete(existingFolder.getId());
            }
        }

        psbFolderRepository.flush();

        for (com.geosegbar.infra.psb.dtos.PSBFolderUpdateDTO folderDTO : psbFolderDTOs) {
            if (folderDTO.getId() != null) {
                updateRootFolder(folderDTO, dam, updater);
            } else {
                createRootFolder(folderDTO, dam, updater);
            }
        }
    }

    private void validateViewPermission() {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity user = AuthenticatedUserUtil.getCurrentUser();
            if (user.getDocumentationPermission() == null || !Boolean.TRUE.equals(user.getDocumentationPermission().getViewPSB())) {
                throw new NotFoundException("Usuário não tem permissão para acessar as pastas PSB");
            }
        }
    }

    private void validateEditPermission() {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity user = AuthenticatedUserUtil.getCurrentUser();
            if (user.getDocumentationPermission() == null || !Boolean.TRUE.equals(user.getDocumentationPermission().getEditPSB())) {
                throw new NotFoundException("Usuário não tem permissão para editar pastas PSB!");
            }
        }
    }

    private void validateDuplicates(Long damId, String name, Integer index, PSBFolderEntity parent) {
        if (parent != null) {
            if (psbFolderRepository.existsByDamIdAndNameAndParentFolderId(damId, name, parent.getId())) {
                throw new DuplicateResourceException("Já existe uma pasta com este nome neste nível");
            }
            if (psbFolderRepository.existsByParentFolderIdAndFolderIndex(parent.getId(), index)) {
                throw new DuplicateResourceException("Já existe uma pasta com este índice neste nível");
            }
        } else {
            if (psbFolderRepository.existsByDamIdAndNameAndParentFolderIsNull(damId, name)) {
                throw new DuplicateResourceException("Já existe uma pasta raiz com este nome");
            }
            if (psbFolderRepository.existsByDamIdAndFolderIndexAndParentFolderIsNull(damId, index)) {
                throw new DuplicateResourceException("Já existe uma pasta raiz com este índice");
            }
        }
    }

    private void validateDuplicatesForUpdate(PSBFolderEntity currentFolder, String newName, Integer newIndex, Long newParentId) {

        List<PSBFolderEntity> siblings;

        if (newParentId != null) {
            siblings = psbFolderRepository.findByParentFolderIdOrderByFolderIndexAsc(newParentId);
        } else {
            siblings = psbFolderRepository.findByDamIdAndParentFolderIsNullOrderByFolderIndexAsc(currentFolder.getDam().getId());
        }

        for (PSBFolderEntity sibling : siblings) {
            if (!sibling.getId().equals(currentFolder.getId())) {
                if (sibling.getName().equals(newName)) {
                    throw new DuplicateResourceException("Já existe uma pasta com este nome neste nível");
                }
                if (sibling.getFolderIndex().equals(newIndex)) {
                    throw new DuplicateResourceException("Já existe uma pasta com este índice neste nível");
                }
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

    private void updateRootFolder(com.geosegbar.infra.psb.dtos.PSBFolderUpdateDTO folderDTO,
            DamEntity dam, UserEntity updater) {

        PSBFolderEntity folder = psbFolderRepository.findById(folderDTO.getId())
                .orElseThrow(() -> new NotFoundException("Pasta PSB não encontrada: " + folderDTO.getId()));

        if (folder.getParentFolder() != null || !folder.getDam().getId().equals(dam.getId())) {
            throw new BusinessRuleException("Conflito de hierarquia ou barragem na atualização de pasta raiz.");
        }

        if (!folder.getName().equals(folderDTO.getName()) || !folder.getFolderIndex().equals(folderDTO.getFolderIndex())) {
            validateDuplicatesForUpdate(folder, folderDTO.getName(), folderDTO.getFolderIndex(), null);
        }

        boolean pathChanged = !folder.getName().equals(folderDTO.getName()) || !folder.getFolderIndex().equals(folderDTO.getFolderIndex());

        folder.setName(folderDTO.getName());
        folder.setFolderIndex(folderDTO.getFolderIndex());
        folder.setDescription(folderDTO.getDescription());
        folder.setColor(folderDTO.getColor());
        folder.setUpdatedAt(LocalDateTime.now());

        if (pathChanged) {
            String newPath = createHierarchicalFolderPath(dam.getId(), null, folderDTO.getFolderIndex(), folderDTO.getName());
            folder.setServerPath(newPath);
            updateSubfoldersPath(folder);
        }

        psbFolderRepository.save(folder);
    }

    private void createRootFolder(com.geosegbar.infra.psb.dtos.PSBFolderUpdateDTO folderDTO,
            DamEntity dam, UserEntity creator) {
        validateDuplicates(dam.getId(), folderDTO.getName(), folderDTO.getFolderIndex(), null);

        String folderPath = createHierarchicalFolderPath(dam.getId(), null, folderDTO.getFolderIndex(), folderDTO.getName());

        PSBFolderEntity folder = new PSBFolderEntity();
        folder.setName(folderDTO.getName());
        folder.setFolderIndex(folderDTO.getFolderIndex());
        folder.setDescription(folderDTO.getDescription());
        folder.setDam(dam);
        folder.setParentFolder(null);
        folder.setServerPath(folderPath);
        folder.setCreatedBy(creator);
        folder.setColor(folderDTO.getColor());

        psbFolderRepository.save(folder);
    }
}
