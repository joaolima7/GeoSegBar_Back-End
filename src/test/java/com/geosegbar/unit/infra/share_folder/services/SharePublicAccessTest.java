package com.geosegbar.unit.infra.share_folder.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.geosegbar.common.email.EmailService;
import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.PSBFileEntity;
import com.geosegbar.entities.PSBFolderEntity;
import com.geosegbar.entities.ShareFolderEntity;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.exceptions.ShareFolderException;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.psb.persistence.PSBFolderRepository;
import com.geosegbar.infra.psb.services.PSBFileService;
import com.geosegbar.infra.psb.services.PSBFolderService;
import com.geosegbar.infra.share_folder.persistence.ShareFolderRepository;
import com.geosegbar.infra.share_folder.services.ShareFolderService;
import com.geosegbar.infra.share_folder.services.ZipService;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

/**
 * Fluxo público de compartilhamento de PSB: o destinatário não tem login, e a
 * autorização é o token do link.
 *
 * O bug do chamado era que o acesso caía em validateViewPermission ->
 * getCurrentUser, que lança para requisição anônima — ou seja, o link público
 * exigia autenticação. Estes testes garantem que o caminho público nunca mais
 * passe por checagem de sessão, e que o token não vire chave-mestra.
 */
@Tag("unit")
@DisplayName("Unit Tests - Acesso público ao PSB compartilhado")
class SharePublicAccessTest extends BaseUnitTest {

    private static final String TOKEN = "tok-abc-123";
    private static final Long PASTA_COMPARTILHADA = 10L;
    private static final Long OUTRA_PASTA = 99L;

    @Mock
    private ShareFolderRepository shareFolderRepository;
    @Mock
    private PSBFolderRepository psbFolderRepository;
    @Mock
    private PSBFolderService psbFolderService;
    @Mock
    private PSBFileService psbFileService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DamRepository damRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private ZipService zipService;

    @InjectMocks
    private ShareFolderService shareFolderService;

    private PSBFolderEntity pastaCompartilhada;
    private ShareFolderEntity share;

    @BeforeEach
    void setUp() {
        pastaCompartilhada = folder(PASTA_COMPARTILHADA, null);

        share = new ShareFolderEntity();
        share.setId(1L);
        share.setToken(TOKEN);
        share.setPsbFolder(pastaCompartilhada);
        share.setAccessCount(0);
        share.setExpiresAt(null);

        lenient().when(shareFolderRepository.findByToken(TOKEN)).thenReturn(Optional.of(share));
    }

    // ---------------------------------------------------------------- acesso
    @Test
    @DisplayName("Abrir o link não passa por checagem de sessão")
    void accessDoesNotRequireAuthentication() {
        when(psbFolderService.findByIdForSharedAccess(PASTA_COMPARTILHADA)).thenReturn(pastaCompartilhada);

        assertThatCode(() -> shareFolderService.registerAccessAndGetFolder(TOKEN))
                .doesNotThrowAnyException();

        // findById é a versão que exige usuário autenticado — jamais no fluxo público.
        verify(psbFolderService, never()).findById(PASTA_COMPARTILHADA);
        verify(psbFolderService).findByIdForSharedAccess(PASTA_COMPARTILHADA);
    }

    @Test
    @DisplayName("Abrir o link registra o acesso")
    void accessIncrementsCounter() {
        when(psbFolderService.findByIdForSharedAccess(PASTA_COMPARTILHADA)).thenReturn(pastaCompartilhada);

        shareFolderService.registerAccessAndGetFolder(TOKEN);

        assertThat(share.getAccessCount()).isEqualTo(1);
        assertThat(share.getLastAccessedAt()).isNotNull();
        verify(shareFolderRepository).save(share);
    }

    @Test
    @DisplayName("Link expirado é recusado")
    void expiredLinkIsRejected() {
        share.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        assertThatThrownBy(() -> shareFolderService.registerAccessAndGetFolder(TOKEN))
                .isInstanceOf(ShareFolderException.class)
                .hasMessageContaining("expirou");
    }

    @Test
    @DisplayName("Link sem data de expiração continua válido")
    void linkWithoutExpiryStaysValid() {
        share.setExpiresAt(null);
        when(psbFolderService.findByIdForSharedAccess(PASTA_COMPARTILHADA)).thenReturn(pastaCompartilhada);

        assertThatCode(() -> shareFolderService.registerAccessAndGetFolder(TOKEN))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Token inexistente resulta em não encontrado")
    void unknownTokenIsNotFound() {
        when(shareFolderRepository.findByToken("nao-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shareFolderService.registerAccessAndGetFolder("nao-existe"))
                .isInstanceOf(NotFoundException.class);
    }

    // ------------------------------------------------------- download de arquivo
    @Test
    @DisplayName("Baixa arquivo que está na pasta compartilhada")
    void downloadsFileInSharedFolder() {
        PSBFileEntity arquivo = file(500L, pastaCompartilhada);
        when(psbFileService.findByIdForSharedAccess(500L)).thenReturn(arquivo);

        PSBFileEntity resolvido = shareFolderService.resolveSharedFile(TOKEN, 500L);

        assertThat(resolvido).isSameAs(arquivo);
    }

    @Test
    @DisplayName("Baixa arquivo de subpasta — o compartilhamento cobre a subárvore")
    void downloadsFileInSubfolder() {
        PSBFolderEntity subpasta = folder(11L, pastaCompartilhada);
        PSBFolderEntity subsubpasta = folder(12L, subpasta);
        PSBFileEntity arquivo = file(501L, subsubpasta);
        when(psbFileService.findByIdForSharedAccess(501L)).thenReturn(arquivo);

        assertThat(shareFolderService.resolveSharedFile(TOKEN, 501L)).isSameAs(arquivo);
    }

    @Test
    @DisplayName("Recusa arquivo de outra pasta — token não é chave-mestra")
    void refusesFileOutsideSharedFolder() {
        PSBFolderEntity outra = folder(OUTRA_PASTA, null);
        PSBFileEntity arquivoAlheio = file(900L, outra);
        when(psbFileService.findByIdForSharedAccess(900L)).thenReturn(arquivoAlheio);

        assertThatThrownBy(() -> shareFolderService.resolveSharedFile(TOKEN, 900L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("não encontrado nesta pasta compartilhada");
    }

    @Test
    @DisplayName("Recusa arquivo de pasta ACIMA da compartilhada")
    void refusesFileInParentFolder() {
        PSBFolderEntity pai = folder(5L, null);
        pastaCompartilhada.setParentFolder(pai);
        PSBFileEntity arquivoDoPai = file(901L, pai);
        when(psbFileService.findByIdForSharedAccess(901L)).thenReturn(arquivoDoPai);

        assertThatThrownBy(() -> shareFolderService.resolveSharedFile(TOKEN, 901L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("Download por link expirado é recusado antes de tocar no arquivo")
    void refusesFileWhenLinkExpired() {
        share.setExpiresAt(LocalDateTime.now().minusDays(1));

        assertThatThrownBy(() -> shareFolderService.resolveSharedFile(TOKEN, 500L))
                .isInstanceOf(ShareFolderException.class)
                .hasMessageContaining("expirou");

        verify(psbFileService, never()).findByIdForSharedAccess(500L);
    }

    @Test
    @DisplayName("Hierarquia cíclica não trava a thread")
    void cyclicHierarchyTerminates() {
        PSBFolderEntity a = folder(60L, null);
        PSBFolderEntity b = folder(61L, a);
        a.setParentFolder(b); // ciclo
        PSBFileEntity arquivo = file(902L, a);
        when(psbFileService.findByIdForSharedAccess(902L)).thenReturn(arquivo);

        assertThatThrownBy(() -> shareFolderService.resolveSharedFile(TOKEN, 902L))
                .isInstanceOf(NotFoundException.class);
    }

    // ------------------------------------------------------------------ zip
    @Test
    @DisplayName("Baixar tudo também não passa por checagem de sessão")
    void zipDownloadDoesNotRequireAuthentication() {
        when(psbFolderService.findByIdForSharedAccess(PASTA_COMPARTILHADA)).thenReturn(pastaCompartilhada);
        when(zipService.createZipFromFolder(pastaCompartilhada))
                .thenReturn(new java.io.ByteArrayOutputStream());

        assertThatCode(() -> shareFolderService.downloadAllFiles(TOKEN)).doesNotThrowAnyException();

        verify(psbFolderService, never()).findById(PASTA_COMPARTILHADA);
    }

    // ------------------------------------------------------------- fixtures
    private PSBFolderEntity folder(Long id, PSBFolderEntity parent) {
        PSBFolderEntity f = new PSBFolderEntity();
        f.setId(id);
        f.setName("Pasta " + id);
        f.setParentFolder(parent);
        return f;
    }

    private PSBFileEntity file(Long id, PSBFolderEntity folder) {
        PSBFileEntity f = new PSBFileEntity();
        f.setId(id);
        f.setOriginalFilename("arquivo-" + id + ".pdf");
        f.setContentType("application/pdf");
        f.setPsbFolder(folder);
        return f;
    }
}
