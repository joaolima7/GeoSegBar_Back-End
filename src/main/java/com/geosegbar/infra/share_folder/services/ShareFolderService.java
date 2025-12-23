package com.geosegbar.infra.share_folder.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.email.EmailService;
import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.PSBFolderEntity;
import com.geosegbar.entities.ShareFolderEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.exceptions.ShareFolderException;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.psb.persistence.PSBFolderRepository;
import com.geosegbar.infra.share_folder.dtos.CreateShareFolderRequest;
import com.geosegbar.infra.share_folder.persistence.ShareFolderRepository;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShareFolderService {

    private final ShareFolderRepository shareFolderRepository;
    private final PSBFolderRepository psbFolderRepository;
    private final UserRepository userRepository;
    private final DamRepository damRepository;
    private final EmailService emailService;
    private final ZipService zipService;

    @Transactional(readOnly = true)
    public List<ShareFolderEntity> findAllByUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));
        return shareFolderRepository.findBySharedBy(user);
    }

    @Transactional(readOnly = true)
    public List<ShareFolderEntity> findAllByFolder(Long folderId) {
        PSBFolderEntity folder = psbFolderRepository.findById(folderId)
                .orElseThrow(() -> new NotFoundException("Pasta PSB não encontrada!"));
        return shareFolderRepository.findByPsbFolder(folder);
    }

    @Transactional(readOnly = true)
    public ShareFolderEntity findByToken(String token) {
        return shareFolderRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Link de compartilhamento não encontrado!"));
    }

    @Transactional
    public ShareFolderEntity create(CreateShareFolderRequest request) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            if (!AuthenticatedUserUtil.getCurrentUser().getDocumentationPermission().getSharePSB()) {
                throw new NotFoundException("Usuário não tem permissão para compartilhar pastas PSB!");
            }
        }

        PSBFolderEntity folder = psbFolderRepository.findById(request.getPsbFolderId())
                .orElseThrow(() -> new NotFoundException("Pasta PSB não encontrada!"));

        UserEntity sharedBy = userRepository.findById(request.getSharedById())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));

        List<ShareFolderEntity> validShares = shareFolderRepository.findValidSharesByFolderAndEmail(
                folder.getId(),
                request.getSharedWithEmail(),
                LocalDateTime.now()
        );

        if (!validShares.isEmpty()) {
            throw new ShareFolderException("Esta pasta já possui um compartilhamento válido com este email!");
        }

        ShareFolderEntity shareFolder = new ShareFolderEntity();
        shareFolder.setPsbFolder(folder);
        shareFolder.setSharedBy(sharedBy);
        shareFolder.setSharedWithEmail(request.getSharedWithEmail());
        shareFolder.setExpiresAt(request.getExpiresAt());

        ShareFolderEntity savedShare = shareFolderRepository.save(shareFolder);

        emailService.sendShareFolderEmail(
                request.getSharedWithEmail(),
                sharedBy.getName(),
                folder.getName(),
                savedShare.getToken(),
                request.getCustomMessage()
        );

        return savedShare;
    }

    @Transactional
    public PSBFolderEntity registerAccessAndGetFolder(String token) {
        ShareFolderEntity shareFolder = shareFolderRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Link de compartilhamento não encontrado!"));

        if (shareFolder.getExpiresAt() != null
                && LocalDateTime.now().isAfter(shareFolder.getExpiresAt())) {
            throw new ShareFolderException("Este link de compartilhamento expirou!");
        }

        shareFolder.incrementAccessCount();
        shareFolderRepository.save(shareFolder);

        PSBFolderEntity folder = psbFolderRepository.findById(shareFolder.getPsbFolder().getId())
                .orElseThrow(() -> new NotFoundException("Pasta PSB não encontrada!"));

        initializeFolderHierarchy(folder);

        return folder;
    }

    @Transactional
    public void deleteShare(Long shareId) {
        if (!AuthenticatedUserUtil.isAdmin()) {
            if (!AuthenticatedUserUtil.getCurrentUser().getDocumentationPermission().getSharePSB()) {
                throw new NotFoundException("Usuário não tem permissão para excluir compartilhamentos de pastas PSB!");
            }
        }

        ShareFolderEntity shareFolder = shareFolderRepository.findById(shareId)
                .orElseThrow(() -> new NotFoundException("Link de compartilhamento não encontrado!"));

        shareFolderRepository.delete(shareFolder);
    }

    @Transactional(readOnly = true)
    public List<ShareFolderEntity> findAllByDamId(Long damId) {
        if (!damRepository.existsById(damId)) {
            throw new NotFoundException("Barragem não encontrada!");
        }

        return shareFolderRepository.findByPsbFolderDamIdOrderByCreatedAtDesc(damId);
    }

    @Transactional(readOnly = true)
    public java.io.ByteArrayOutputStream downloadAllFiles(String token) {
        ShareFolderEntity shareFolder = shareFolderRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Link de compartilhamento não encontrado!"));

        if (shareFolder.getExpiresAt() != null
                && LocalDateTime.now().isAfter(shareFolder.getExpiresAt())) {
            throw new ShareFolderException("Este link de compartilhamento expirou!");
        }

        shareFolder.incrementAccessCount();
        shareFolderRepository.save(shareFolder);

        PSBFolderEntity folder = psbFolderRepository.findById(shareFolder.getPsbFolder().getId())
                .orElseThrow(() -> new NotFoundException("Pasta PSB não encontrada!"));

        initializeFolderHierarchy(folder);

        return zipService.createZipFromFolder(folder);
    }

    /**
     * Inicializa recursivamente todas as subpastas e arquivos (força
     * carregamento lazy)
     */
    private void initializeFolderHierarchy(PSBFolderEntity folder) {

        folder.getFiles().size();

        if (folder.getSubfolders() != null && !folder.getSubfolders().isEmpty()) {
            folder.getSubfolders().forEach(subfolder -> {
                subfolder.getFiles().size();
                initializeFolderHierarchy(subfolder);
            });
        }
    }
}
