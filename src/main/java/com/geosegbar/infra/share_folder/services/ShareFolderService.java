package com.geosegbar.infra.share_folder.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.email.EmailService;
import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.PSBFileEntity;
import com.geosegbar.entities.PSBFolderEntity;
import com.geosegbar.entities.ShareFolderEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.exceptions.ShareFolderException;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.psb.persistence.PSBFolderRepository;
import com.geosegbar.infra.psb.services.PSBFileService;
import com.geosegbar.infra.psb.services.PSBFolderService;
import com.geosegbar.infra.share_folder.dtos.CreateShareFolderRequest;
import com.geosegbar.infra.share_folder.persistence.ShareFolderRepository;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShareFolderService {

    private final ShareFolderRepository shareFolderRepository;
    private final PSBFolderRepository psbFolderRepository;
    private final PSBFolderService psbFolderService;
    private final PSBFileService psbFileService;
    private final UserRepository userRepository;
    private final DamRepository damRepository;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public List<ShareFolderEntity> findAllByUser(Long userId) {
        validateViewPermission();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado!"));
        return shareFolderRepository.findBySharedBy(user);
    }

    @Transactional(readOnly = true)
    public List<ShareFolderEntity> findAllByFolder(Long folderId) {
        validateViewPermission();

        if (!psbFolderRepository.existsById(folderId)) {
            throw new NotFoundException("Pasta PSB não encontrada!");
        }

        PSBFolderEntity folder = psbFolderRepository.findById(folderId).orElseThrow();
        return shareFolderRepository.findByPsbFolder(folder);
    }

    @Transactional(readOnly = true)
    public ShareFolderEntity findByToken(String token) {
        return shareFolderRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Link de compartilhamento não encontrado!"));
    }

    @Transactional
    public ShareFolderEntity create(CreateShareFolderRequest request) {
        validateSharePermission();

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

    /**
     * Localiza o compartilhamento pelo token e recusa se estiver vencido. É o
     * único ponto de autorização do fluxo público — nenhuma sessão é exigida
     * daqui para frente, então tudo que for liberado precisa passar por aqui.
     */
    private ShareFolderEntity requireValidShare(String token) {
        ShareFolderEntity shareFolder = shareFolderRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Link de compartilhamento não encontrado!"));

        if (shareFolder.getExpiresAt() != null
                && LocalDateTime.now().isAfter(shareFolder.getExpiresAt())) {
            throw new ShareFolderException("Este link de compartilhamento expirou!");
        }

        return shareFolder;
    }

    /**
     * Baixa um arquivo específico pelo link compartilhado, sem exigir login.
     *
     * A autorização é o token — por isso é obrigatório conferir que o arquivo
     * pertence mesmo à pasta compartilhada (ou a uma subpasta dela). Sem essa
     * conferência, qualquer link válido viraria uma chave para baixar qualquer
     * arquivo PSB do sistema, bastando adivinhar o id.
     */
    @Transactional
    public PSBFileEntity resolveSharedFile(String token, Long fileId) {
        ShareFolderEntity shareFolder = requireValidShare(token);

        PSBFileEntity file = psbFileService.findByIdForSharedAccess(fileId);

        if (!belongsToSharedFolder(file, shareFolder.getPsbFolder().getId())) {
            throw new NotFoundException("Arquivo não encontrado nesta pasta compartilhada!");
        }

        return file;
    }

    /**
     * Sobe a hierarquia a partir da pasta do arquivo até encontrar a pasta
     * compartilhada. O compartilhamento cobre a subárvore inteira — é o mesmo
     * conteúdo que o ZIP de "baixar tudo" entrega.
     */
    private boolean belongsToSharedFolder(PSBFileEntity file, Long sharedFolderId) {
        PSBFolderEntity current = file.getPsbFolder();

        // Guarda contra ciclo em dados corrompidos: a árvore de PSB é rasa, mas
        // um laço aqui travaria a thread.
        int maxDepth = 50;
        while (current != null && maxDepth-- > 0) {
            if (sharedFolderId.equals(current.getId())) {
                return true;
            }
            current = current.getParentFolder();
        }
        return false;
    }

    @Transactional
    public PSBFolderEntity registerAccessAndGetFolder(String token) {
        ShareFolderEntity shareFolder = requireValidShare(token);

        shareFolder.incrementAccessCount();
        shareFolderRepository.save(shareFolder);

        return psbFolderService.findByIdForSharedAccess(shareFolder.getPsbFolder().getId());
    }

    @Transactional
    public void deleteShare(Long shareId) {
        validateSharePermission();

        if (!shareFolderRepository.existsById(shareId)) {
            throw new NotFoundException("Link de compartilhamento não encontrado!");
        }
        shareFolderRepository.deleteById(shareId);
    }

    @Transactional(readOnly = true)
    public List<ShareFolderEntity> findAllByDamId(Long damId) {
        validateViewPermission();
        if (!damRepository.existsById(damId)) {
            throw new NotFoundException("Barragem não encontrada!");
        }
        return shareFolderRepository.findByPsbFolderDamIdOrderByCreatedAtDesc(damId);
    }

    /**
     * Valida o link e devolve a pasta pronta para ser transmitida como ZIP.
     *
     * Não monta o ZIP: quem escreve é o controller, direto na resposta HTTP.
     * Antes este método devolvia um ByteArrayOutputStream com o ZIP inteiro na
     * memória, e o controller ainda fazia toByteArray() por cima — duas cópias
     * completas, mais um byte[] por arquivo. Duas requisições bastaram para
     * derrubar a aplicação com OutOfMemoryError em 24/08/2026, e o processo
     * ficou 18 horas vivo sem aceitar conexões.
     */
    @Transactional
    public PSBFolderEntity prepareFolderDownload(String token) {
        ShareFolderEntity shareFolder = requireValidShare(token);

        shareFolder.incrementAccessCount();
        shareFolderRepository.save(shareFolder);

        return psbFolderService.findByIdForSharedAccess(shareFolder.getPsbFolder().getId());
    }

    /**
     * Valida se o usuário atual tem permissão de visualização de PSB.
     * Administradores têm permissão automática.
     */
    private void validateViewPermission() {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity user = AuthenticatedUserUtil.getCurrentUser();
            if (user.getDocumentationPermission() == null || !Boolean.TRUE.equals(user.getDocumentationPermission().getViewPSB())) {
                throw new NotFoundException("Usuário não tem permissão para visualizar informações de PSB!");
            }
        }
    }

    /**
     * Valida se o usuário atual tem permissão de compartilhamento de PSB.
     * Administradores têm permissão automática.
     */
    private void validateSharePermission() {
        if (!AuthenticatedUserUtil.isAdmin()) {
            UserEntity user = AuthenticatedUserUtil.getCurrentUser();
            if (user.getDocumentationPermission() == null || !Boolean.TRUE.equals(user.getDocumentationPermission().getSharePSB())) {
                throw new NotFoundException("Usuário não tem permissão para compartilhar pastas PSB!");
            }
        }
    }
}
