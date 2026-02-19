package com.geosegbar.infra.psb.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PSBFolderService {

    @PersistenceContext
    private EntityManager entityManager;

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
        PSBFolderEntity folder = psbFolderRepository.findById(id).orElseThrow(() -> new NotFoundException("Pasta PSB não encontrada"));
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
        DamEntity dam = damRepository.findById(request.getDamId()).orElseThrow(() -> new NotFoundException("Barragem não encontrada"));
        UserEntity currentUser = userRepository.findById(request.getCreatedById()).orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        PSBFolderEntity parentFolder = null;
        if (request.getParentFolderId() != null) {
            parentFolder = psbFolderRepository.findById(request.getParentFolderId()).orElseThrow(() -> new NotFoundException("Pasta pai não encontrada"));
            if (!parentFolder.getDam().getId().equals(dam.getId())) {
                throw new BusinessRuleException("A pasta pai deve pertencer à mesma barragem");
            }
        }
        validateDuplicates(dam.getId(), request.getName(), request.getFolderIndex(), parentFolder);
        String folderPath = createHierarchicalFolderPath(dam.getId(), parentFolder, request.getFolderIndex(), request.getName());
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
        PSBFolderEntity existingFolder = psbFolderRepository.findById(id).orElseThrow(() -> new NotFoundException("Pasta PSB não encontrada"));
        PSBFolderEntity newParent = null;
        if (request.getParentFolderId() != null) {
            newParent = psbFolderRepository.findById(request.getParentFolderId()).orElseThrow(() -> new NotFoundException("Pasta pai não encontrada"));
            if (isDescendant(existingFolder, newParent)) {
                throw new BusinessRuleException("Ciclo detectado");
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
        boolean parentChanged = (currentParentId == null && newParentId != null) || (currentParentId != null && !currentParentId.equals(newParentId)) || (currentParentId != null && newParentId == null);

        if (nameChanged || indexChanged || parentChanged) {
            validateDuplicatesForUpdate(existingFolder, request.getName(), request.getFolderIndex(), newParentId);
        }
        if (nameChanged || indexChanged || parentChanged) {
            String newFolderPath = createHierarchicalFolderPath(existingFolder.getDam().getId(), newParent, request.getFolderIndex(), request.getName());
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

        log.info("[PSB-DELETE] Iniciando exclusão da pasta ID: {}", id);

        PSBFolderEntity folderToDelete = psbFolderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pasta PSB não encontrada"));

        Long damId = folderToDelete.getDam().getId();
        Integer deletedFolderIndex = folderToDelete.getFolderIndex();
        Long parentFolderId = folderToDelete.getParentFolder() != null ? folderToDelete.getParentFolder().getId() : null;

        log.info("[PSB-DELETE] Pasta: '{}', damId={}, index={}, parentId={}",
                folderToDelete.getName(), damId, deletedFolderIndex, parentFolderId);

        log.info("[PSB-DELETE] Iniciando limpeza S3 recursiva...");
        deleteS3ContentRecursively(folderToDelete);
        log.info("[PSB-DELETE] S3 limpo com sucesso.");

        entityManager.clear();
        log.info("[PSB-DELETE] Contexto JPA limpo após S3.");

        log.info("[PSB-DELETE] Iniciando exclusão recursiva no banco (JPQL bulk delete)...");
        deleteFolderRecursivelyFromDB(id);
        log.info("[PSB-DELETE] Exclusão no banco concluída.");

        entityManager.clear();
        log.info("[PSB-DELETE] Contexto JPA limpo. Iniciando reindexação de irmãos...");

        reindexSiblingsAfterDelete(damId, parentFolderId, deletedFolderIndex);
        log.info("[PSB-DELETE] Exclusão e reindexação concluídas com sucesso para pasta ID: {}", id);
    }

    /**
     * Deleta recursivamente uma pasta e todo seu conteúdo do banco de dados
     * usando JPQL bulk DELETE (bottom-up). Esse método NUNCA aciona o cascade
     * do JPA, o que evita TransientObjectException em estruturas bidirecionais.
     */
    private void deleteFolderRecursivelyFromDB(Long folderId) {
        log.debug("[PSB-DELETE-DB] Processando pasta ID: {}", folderId);

        List<Long> subfolderIds = entityManager
                .createQuery("SELECT f.id FROM PSBFolderEntity f WHERE f.parentFolder.id = :pid", Long.class)
                .setParameter("pid", folderId)
                .getResultList();

        log.debug("[PSB-DELETE-DB] Pasta {} tem {} subpasta(s): {}", folderId, subfolderIds.size(), subfolderIds);

        for (Long subId : subfolderIds) {
            deleteFolderRecursivelyFromDB(subId);
        }

        int filesDeleted = entityManager
                .createQuery("DELETE FROM PSBFileEntity f WHERE f.psbFolder.id = :folderId")
                .setParameter("folderId", folderId)
                .executeUpdate();
        log.debug("[PSB-DELETE-DB] {} arquivo(s) deletado(s) da pasta {}", filesDeleted, folderId);

        int linksDeleted = entityManager
                .createQuery("DELETE FROM ShareFolderEntity sl WHERE sl.psbFolder.id = :folderId")
                .setParameter("folderId", folderId)
                .executeUpdate();
        log.debug("[PSB-DELETE-DB] {} share link(s) deletado(s) da pasta {}", linksDeleted, folderId);

        int psbLinkNulled = entityManager
                .createQuery("UPDATE DamEntity d SET d.psbLinkFolder = null WHERE d.psbLinkFolder.id = :folderId")
                .setParameter("folderId", folderId)
                .executeUpdate();
        int legLinkNulled = entityManager
                .createQuery("UPDATE DamEntity d SET d.legislationLinkFolder = null WHERE d.legislationLinkFolder.id = :folderId")
                .setParameter("folderId", folderId)
                .executeUpdate();
        if (psbLinkNulled > 0 || legLinkNulled > 0) {
            log.debug("[PSB-DELETE-DB] Referências FK em dam nullificadas para pasta {}: psbLink={}, legLink={}",
                    folderId, psbLinkNulled, legLinkNulled);
        }

        int deleted = entityManager
                .createQuery("DELETE FROM PSBFolderEntity f WHERE f.id = :id")
                .setParameter("id", folderId)
                .executeUpdate();
        log.debug("[PSB-DELETE-DB] Pasta {} deletada: {}", folderId, deleted > 0);
    }

    /**
     * 🔥 LÓGICA DE SYNC CORRIGIDA 1. Valida payload em memória. 2. Deleta sem
     * reindexar (para não bagunçar índices). 3. Cria/Atualiza sem validar DB
     * (para evitar conflitos temporários).
     */
    @Transactional
    public void syncRootFolders(DamEntity dam, List<com.geosegbar.infra.psb.dtos.PSBFolderUpdateDTO> psbFolderDTOs,
            Long updatedById) {
        if (psbFolderDTOs == null || psbFolderDTOs.isEmpty()) {
            return;
        }

        UserEntity updater = userRepository.findById(updatedById)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        validatePayloadDuplicates(psbFolderDTOs);

        List<PSBFolderEntity> existingRootFolders = psbFolderRepository
                .findByDamIdAndParentFolderIsNullOrderByFolderIndexAsc(dam.getId());

        List<Long> sentFolderIds = psbFolderDTOs.stream()
                .map(com.geosegbar.infra.psb.dtos.PSBFolderUpdateDTO::getId)
                .filter(id -> id != null)
                .toList();

        for (PSBFolderEntity existingFolder : existingRootFolders) {
            if (!sentFolderIds.contains(existingFolder.getId())) {
                log.info("Deletando pasta raiz não enviada: {}", existingFolder.getName());

                deleteS3ContentRecursively(existingFolder);

                psbFolderRepository.delete(existingFolder);
            }
        }

        psbFolderRepository.flush();

        for (com.geosegbar.infra.psb.dtos.PSBFolderUpdateDTO folderDTO : psbFolderDTOs) {
            if (folderDTO.getId() != null) {

                updateRootFolderInternal(folderDTO, dam, updater, true);
            } else {

                createRootFolderInternal(folderDTO, dam, updater, true);
            }
        }
    }

    private void validatePayloadDuplicates(List<com.geosegbar.infra.psb.dtos.PSBFolderUpdateDTO> dtos) {
        Set<Integer> indexes = new HashSet<>();
        Set<String> names = new HashSet<>();

        for (var dto : dtos) {
            if (!indexes.add(dto.getFolderIndex())) {
                throw new DuplicateResourceException("O payload contém índices duplicados: " + dto.getFolderIndex());
            }
            if (!names.add(dto.getName())) {
                throw new DuplicateResourceException("O payload contém nomes duplicados: " + dto.getName());
            }
        }
    }

    private void updateRootFolderInternal(com.geosegbar.infra.psb.dtos.PSBFolderUpdateDTO folderDTO,
            DamEntity dam, UserEntity updater, boolean skipDbValidation) {

        PSBFolderEntity folder = psbFolderRepository.findById(folderDTO.getId())
                .orElseThrow(() -> new NotFoundException("Pasta PSB não encontrada: " + folderDTO.getId()));

        if (folder.getParentFolder() != null || !folder.getDam().getId().equals(dam.getId())) {
            throw new BusinessRuleException("Conflito de hierarquia ou barragem na atualização de pasta raiz.");
        }

        if (!skipDbValidation) {
            if (!folder.getName().equals(folderDTO.getName()) || !folder.getFolderIndex().equals(folderDTO.getFolderIndex())) {
                validateDuplicatesForUpdate(folder, folderDTO.getName(), folderDTO.getFolderIndex(), null);
            }
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

    private void createRootFolderInternal(com.geosegbar.infra.psb.dtos.PSBFolderUpdateDTO folderDTO,
            DamEntity dam, UserEntity creator, boolean skipDbValidation) {

        if (!skipDbValidation) {
            validateDuplicates(dam.getId(), folderDTO.getName(), folderDTO.getFolderIndex(), null);
        }

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

    @Transactional
    public List<PSBFolderEntity> createMultipleFolders(DamEntity dam, List<PSBFolderCreationDTO> folderRequests, Long createdById) {
        validateEditPermission();
        UserEntity creator = userRepository.findById(createdById).orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        List<PSBFolderEntity> createdFolders = new ArrayList<>();

        for (PSBFolderCreationDTO folderDTO : folderRequests) {

            validateDuplicates(dam.getId(), folderDTO.getName(), folderDTO.getFolderIndex(), null);

            String folderPath = createHierarchicalFolderPath(dam.getId(), null, folderDTO.getFolderIndex(), folderDTO.getName());
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

    private void deleteS3ContentRecursively(PSBFolderEntity folder) {
        if (folder.getFiles() != null) {
            for (PSBFileEntity file : folder.getFiles()) {
                try {
                    fileStorageService.deleteFile(file.getDownloadUrl());
                } catch (Exception e) {
                    log.error("Erro ao deletar: {}", e.getMessage());
                }
            }
        }
        if (folder.getSubfolders() != null) {
            for (PSBFolderEntity sub : folder.getSubfolders()) {
                deleteS3ContentRecursively(sub);
            }
        }
    }

    private void reindexSiblingsAfterDelete(Long damId, Long parentFolderId, Integer deletedFolderIndex) {
        List<PSBFolderEntity> foldersToReindex;
        if (parentFolderId != null) {
            foldersToReindex = psbFolderRepository.findByParentFolderIdAndFolderIndexGreaterThanOrderByFolderIndexAsc(parentFolderId, deletedFolderIndex);
        } else {
            foldersToReindex = psbFolderRepository.findByDamIdAndParentFolderIsNullAndFolderIndexGreaterThanOrderByFolderIndexAsc(damId, deletedFolderIndex);
        }

        for (PSBFolderEntity folder : foldersToReindex) {
            Integer newIndex = folder.getFolderIndex() - 1;
            String newFolderPath = createHierarchicalFolderPath(damId, folder.getParentFolder(), newIndex, folder.getName());
            folder.setFolderIndex(newIndex);
            folder.setServerPath(newFolderPath);
            folder.setUpdatedAt(LocalDateTime.now());
            updateSubfoldersPath(folder);
        }
        if (!foldersToReindex.isEmpty()) {
            psbFolderRepository.saveAll(foldersToReindex);
        }
    }

    private String createHierarchicalFolderPath(Long damId, PSBFolderEntity parentFolder, Integer folderIndex, String folderName) {
        String normalizedName = folderName.trim().toLowerCase().replaceAll("\\s+", "_").replaceAll("[^a-z0-9_]", "");
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
        List<PSBFolderEntity> subfolders = psbFolderRepository.findByParentFolderIdOrderByFolderIndexAsc(folder.getId());
        for (PSBFolderEntity subfolder : subfolders) {
            String newSubfolderPath = createHierarchicalFolderPath(folder.getDam().getId(), folder, subfolder.getFolderIndex(), subfolder.getName());
            subfolder.setServerPath(newSubfolderPath);
            psbFolderRepository.save(subfolder);
            updateSubfoldersPath(subfolder);
        }
    }

    private void validateViewPermission() {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity user = AuthenticatedUserUtil.getCurrentUser();
            if (user.getDocumentationPermission() == null || !Boolean.TRUE.equals(user.getDocumentationPermission().getViewPSB())) {
                throw new NotFoundException("Sem permissão de visualização");
            }
        }
    }

    private void validateEditPermission() {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity user = AuthenticatedUserUtil.getCurrentUser();
            if (user.getDocumentationPermission() == null || !Boolean.TRUE.equals(user.getDocumentationPermission().getEditPSB())) {
                throw new NotFoundException("Sem permissão de edição");
            }
        }
    }

    private void validateDuplicates(Long damId, String name, Integer index, PSBFolderEntity parent) {
        if (parent != null) {
            if (psbFolderRepository.existsByDamIdAndNameAndParentFolderId(damId, name, parent.getId())) {
                throw new DuplicateResourceException("Nome duplicado");
            }
            if (psbFolderRepository.existsByParentFolderIdAndFolderIndex(parent.getId(), index)) {
                throw new DuplicateResourceException("Índice duplicado");
            }
        } else {
            if (psbFolderRepository.existsByDamIdAndNameAndParentFolderIsNull(damId, name)) {
                throw new DuplicateResourceException("Nome duplicado");
            }
            if (psbFolderRepository.existsByDamIdAndFolderIndexAndParentFolderIsNull(damId, index)) {
                throw new DuplicateResourceException("Índice duplicado");
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
                    throw new DuplicateResourceException("Nome duplicado");
                }
                if (sibling.getFolderIndex().equals(newIndex)) {
                    throw new DuplicateResourceException("Índice duplicado");
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
}
