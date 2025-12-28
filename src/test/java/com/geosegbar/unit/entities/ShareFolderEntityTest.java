package com.geosegbar.unit.entities;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.PSBFolderEntity;
import com.geosegbar.entities.ShareFolderEntity;
import com.geosegbar.entities.UserEntity;

@Tag("unit")
class ShareFolderEntityTest extends BaseUnitTest {

    private PSBFolderEntity psbFolder;
    private UserEntity sharedBy;

    @BeforeEach
    void setUp() {
        psbFolder = new PSBFolderEntity();
        psbFolder.setId(1L);
        psbFolder.setName("Pasta Principal");

        sharedBy = new UserEntity();
        sharedBy.setId(1L);
        sharedBy.setName("João Silva");
    }

    @Test
    @DisplayName("Should create share folder with all required fields")
    void shouldCreateShareFolderWithAllRequiredFields() {

        ShareFolderEntity shareFolder = new ShareFolderEntity();
        shareFolder.setId(1L);
        shareFolder.setPsbFolder(psbFolder);
        shareFolder.setSharedBy(sharedBy);
        shareFolder.setSharedWithEmail("destinatario@exemplo.com");
        shareFolder.setAccessCount(0);

        assertThat(shareFolder).satisfies(sf -> {
            assertThat(sf.getId()).isEqualTo(1L);
            assertThat(sf.getPsbFolder()).isEqualTo(psbFolder);
            assertThat(sf.getSharedBy()).isEqualTo(sharedBy);
            assertThat(sf.getSharedWithEmail()).isEqualTo("destinatario@exemplo.com");
            assertThat(sf.getAccessCount()).isEqualTo(0);
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expires = now.plusDays(7);
        
        ShareFolderEntity shareFolder = new ShareFolderEntity(
                1L,
                psbFolder,
                sharedBy,
                "destinatario@exemplo.com",
                0,
                now,
                null,
                expires,
                "test-token-123"
        );

        
        assertThat(shareFolder.getId()).isEqualTo(1L);
        assertThat(shareFolder.getPsbFolder()).isEqualTo(psbFolder);
        assertThat(shareFolder.getSharedBy()).isEqualTo(sharedBy);
        assertThat(shareFolder.getSharedWithEmail()).isEqualTo("destinatario@exemplo.com");
        assertThat(shareFolder.getAccessCount()).isEqualTo(0);
        assertThat(shareFolder.getCreatedAt()).isEqualTo(now);
        assertThat(shareFolder.getLastAccessedAt()).isNull();
        assertThat(shareFolder.getExpiresAt()).isEqualTo(expires);
        assertThat(shareFolder.getToken()).isEqualTo("test-token-123");
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with PSBFolder")
    void shouldMaintainManyToOneRelationshipWithPSBFolder() {
        
        ShareFolderEntity shareFolder = new ShareFolderEntity();
        shareFolder.setPsbFolder(psbFolder);

        
        assertThat(shareFolder.getPsbFolder())
                .isNotNull()
                .isEqualTo(psbFolder);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with User")
    void shouldMaintainManyToOneRelationshipWithUser() {
        
        ShareFolderEntity shareFolder = new ShareFolderEntity();
        shareFolder.setSharedBy(sharedBy);

        
        assertThat(shareFolder.getSharedBy())
                .isNotNull()
                .isEqualTo(sharedBy);
    }

    @Test
    @DisplayName("Should support valid email format")
    void shouldSupportValidEmailFormat() {
        
        ShareFolderEntity shareFolder = new ShareFolderEntity();
        shareFolder.setSharedWithEmail("usuario@dominio.com.br");

        
        assertThat(shareFolder.getSharedWithEmail()).isEqualTo("usuario@dominio.com.br");
    }

    @Test
    @DisplayName("Should default access count to zero")
    void shouldDefaultAccessCountToZero() {
        
        ShareFolderEntity shareFolder = new ShareFolderEntity();
        shareFolder.setAccessCount(0);

        
        assertThat(shareFolder.getAccessCount()).isZero();
    }

    @Test
    @DisplayName("Should support optional last accessed at")
    void shouldSupportOptionalLastAccessedAt() {
        
        ShareFolderEntity shareFolder = new ShareFolderEntity();
        LocalDateTime lastAccessed = LocalDateTime.now();
        shareFolder.setLastAccessedAt(lastAccessed);

        
        assertThat(shareFolder.getLastAccessedAt()).isEqualTo(lastAccessed);
    }

    @Test
    @DisplayName("Should allow null last accessed at")
    void shouldAllowNullLastAccessedAt() {
        
        ShareFolderEntity shareFolder = new ShareFolderEntity();
        shareFolder.setLastAccessedAt(null);

        
        assertThat(shareFolder.getLastAccessedAt()).isNull();
    }

    @Test
    @DisplayName("Should support optional expires at")
    void shouldSupportOptionalExpiresAt() {
        
        ShareFolderEntity shareFolder = new ShareFolderEntity();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);
        shareFolder.setExpiresAt(expiresAt);

        
        assertThat(shareFolder.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("Should allow null expires at")
    void shouldAllowNullExpiresAt() {
        
        ShareFolderEntity shareFolder = new ShareFolderEntity();
        shareFolder.setExpiresAt(null);

        
        assertThat(shareFolder.getExpiresAt()).isNull();
    }

    @Test
    @DisplayName("Should support token for share link")
    void shouldSupportTokenForShareLink() {
        
        ShareFolderEntity shareFolder = new ShareFolderEntity();
        shareFolder.setToken("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

        
        assertThat(shareFolder.getToken()).isEqualTo("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    }

    @Test
    @DisplayName("Should increment access count")
    void shouldIncrementAccessCount() {
        
        ShareFolderEntity shareFolder = new ShareFolderEntity();
        shareFolder.setAccessCount(0);

        
        shareFolder.incrementAccessCount();

        
        assertThat(shareFolder.getAccessCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should update last accessed at when incrementing access count")
    void shouldUpdateLastAccessedAtWhenIncrementingAccessCount() {
        
        ShareFolderEntity shareFolder = new ShareFolderEntity();
        shareFolder.setAccessCount(0);
        LocalDateTime before = LocalDateTime.now();

        
        shareFolder.incrementAccessCount();

        
        assertThat(shareFolder.getLastAccessedAt()).isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("Should support multiple increments of access count")
    void shouldSupportMultipleIncrementsOfAccessCount() {
        
        ShareFolderEntity shareFolder = new ShareFolderEntity();
        shareFolder.setAccessCount(0);

        
        shareFolder.incrementAccessCount();
        shareFolder.incrementAccessCount();
        shareFolder.incrementAccessCount();

        
        assertThat(shareFolder.getAccessCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should support multiple shares per folder")
    void shouldSupportMultipleSharesPerFolder() {
        
        ShareFolderEntity share1 = new ShareFolderEntity();
        share1.setPsbFolder(psbFolder);
        share1.setSharedWithEmail("usuario1@exemplo.com");

        ShareFolderEntity share2 = new ShareFolderEntity();
        share2.setPsbFolder(psbFolder);
        share2.setSharedWithEmail("usuario2@exemplo.com");

        
        assertThat(share1.getPsbFolder()).isEqualTo(share2.getPsbFolder());
        assertThat(share1.getSharedWithEmail()).isNotEqualTo(share2.getSharedWithEmail());
    }

    @Test
    @DisplayName("Should support multiple shares by user")
    void shouldSupportMultipleSharesByUser() {
        
        PSBFolderEntity folder1 = new PSBFolderEntity();
        folder1.setId(1L);

        PSBFolderEntity folder2 = new PSBFolderEntity();
        folder2.setId(2L);

        ShareFolderEntity share1 = new ShareFolderEntity();
        share1.setSharedBy(sharedBy);
        share1.setPsbFolder(folder1);

        ShareFolderEntity share2 = new ShareFolderEntity();
        share2.setSharedBy(sharedBy);
        share2.setPsbFolder(folder2);

        
        assertThat(share1.getSharedBy()).isEqualTo(share2.getSharedBy());
        assertThat(share1.getPsbFolder()).isNotEqualTo(share2.getPsbFolder());
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {
        
        ShareFolderEntity shareFolder = new ShareFolderEntity();
        shareFolder.setId(1L);
        shareFolder.setAccessCount(0);

        Long originalId = shareFolder.getId();

        
        shareFolder.incrementAccessCount();
        shareFolder.setSharedWithEmail("novo@exemplo.com");

        
        assertThat(shareFolder.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support expiration timestamp")
    void shouldSupportExpirationTimestamp() {
        
        ShareFolderEntity shareFolder = new ShareFolderEntity();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(7);

        shareFolder.setCreatedAt(now);
        shareFolder.setExpiresAt(expiresAt);

        
        assertThat(shareFolder.getExpiresAt()).isAfter(shareFolder.getCreatedAt());
    }

    @Test
    @DisplayName("Should support permanent share without expiration")
    void shouldSupportPermanentShareWithoutExpiration() {
        
        ShareFolderEntity shareFolder = new ShareFolderEntity();
        shareFolder.setCreatedAt(LocalDateTime.now());
        shareFolder.setExpiresAt(null);

        
        assertThat(shareFolder.getExpiresAt()).isNull();
    }

    @Test
    @DisplayName("Should support Portuguese characters in email")
    void shouldSupportPortugueseCharactersInEmail() {
        
        ShareFolderEntity shareFolder = new ShareFolderEntity();
        shareFolder.setSharedWithEmail("joão.silva@domínio.com.br");

        
        assertThat(shareFolder.getSharedWithEmail()).contains("ã", "í");
    }

    @Test
    @DisplayName("Should support 36 character UUID token")
    void shouldSupport36CharacterUuidToken() {
        
        ShareFolderEntity shareFolder = new ShareFolderEntity();
        String uuid = "550e8400-e29b-41d4-a716-446655440000";
        shareFolder.setToken(uuid);

        
        assertThat(shareFolder.getToken()).hasSize(36);
    }
}
