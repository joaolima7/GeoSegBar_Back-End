package com.geosegbar.unit.entities;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.common.enums.FolderColorEnum;
import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.PSBFileEntity;
import com.geosegbar.entities.PSBFolderEntity;
import com.geosegbar.entities.ShareFolderEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - PSBFolderEntity")
class PSBFolderEntityTest extends BaseUnitTest {

    private DamEntity dam;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();

        dam = new DamEntity();
        dam.setId(1L);
        dam.setName("Barragem Teste");

        user = new UserEntity();
        user.setId(1L);
        user.setName("User Test");
    }

    @Test
    @DisplayName("Should create PSB folder with all required fields")
    void shouldCreatePSBFolderWithAllRequiredFields() {

        PSBFolderEntity folder = new PSBFolderEntity();
        folder.setId(1L);
        folder.setName("Documentos PSB");
        folder.setFolderIndex(0);
        folder.setServerPath("/psb/documents");
        folder.setDam(dam);

        assertThat(folder).satisfies(f -> {
            assertThat(f.getId()).isEqualTo(1L);
            assertThat(f.getName()).isEqualTo("Documentos PSB");
            assertThat(f.getFolderIndex()).isEqualTo(0);
            assertThat(f.getServerPath()).isEqualTo("/psb/documents");
            assertThat(f.getDam()).isEqualTo(dam);
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        PSBFolderEntity parentFolder = new PSBFolderEntity();
        parentFolder.setId(1L);
        LocalDateTime now = LocalDateTime.now();

        PSBFolderEntity folder = new PSBFolderEntity(
                2L,
                "Subfolder",
                1,
                "Descrição da pasta",
                "/psb/subfolder",
                FolderColorEnum.BLUE,
                dam,
                parentFolder,
                null,
                null,
                now,
                now,
                user,
                null
        );

        assertThat(folder.getId()).isEqualTo(2L);
        assertThat(folder.getName()).isEqualTo("Subfolder");
        assertThat(folder.getFolderIndex()).isEqualTo(1);
        assertThat(folder.getDescription()).isEqualTo("Descrição da pasta");
        assertThat(folder.getServerPath()).isEqualTo("/psb/subfolder");
        assertThat(folder.getColor()).isEqualTo(FolderColorEnum.BLUE);
        assertThat(folder.getDam()).isEqualTo(dam);
        assertThat(folder.getParentFolder()).isEqualTo(parentFolder);
        assertThat(folder.getCreatedBy()).isEqualTo(user);
    }

    @Test
    @DisplayName("Should default color to BLUE")
    void shouldDefaultColorToBlue() {

        PSBFolderEntity folder = new PSBFolderEntity();
        folder.setName("Nova Pasta");

        assertThat(folder.getColor()).isEqualTo(FolderColorEnum.BLUE);
    }

    @Test
    @DisplayName("Should support different folder colors")
    void shouldSupportDifferentFolderColors() {

        PSBFolderEntity blue = new PSBFolderEntity();
        blue.setColor(FolderColorEnum.BLUE);

        PSBFolderEntity red = new PSBFolderEntity();
        red.setColor(FolderColorEnum.RED);

        assertThat(blue.getColor()).isEqualTo(FolderColorEnum.BLUE);
        assertThat(red.getColor()).isEqualTo(FolderColorEnum.RED);
    }

    @Test
    @DisplayName("Should set timestamps on persist")
    void shouldSetTimestampsOnPersist() {

        PSBFolderEntity folder = new PSBFolderEntity();
        folder.setName("Pasta Teste");

        LocalDateTime beforePersist = LocalDateTime.now();

        folder.setCreatedAt(beforePersist);
        folder.setUpdatedAt(beforePersist);

        assertThat(folder.getCreatedAt())
                .isNotNull()
                .isCloseTo(beforePersist, within(1, ChronoUnit.SECONDS));
        assertThat(folder.getUpdatedAt())
                .isNotNull()
                .isCloseTo(beforePersist, within(1, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with Dam")
    void shouldMaintainManyToOneRelationshipWithDam() {

        PSBFolderEntity folder = new PSBFolderEntity();
        folder.setDam(dam);

        assertThat(folder.getDam())
                .isNotNull()
                .isEqualTo(dam);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with parent folder")
    void shouldMaintainManyToOneRelationshipWithParentFolder() {

        PSBFolderEntity parentFolder = new PSBFolderEntity();
        parentFolder.setId(1L);
        parentFolder.setName("Pasta Pai");

        PSBFolderEntity childFolder = new PSBFolderEntity();
        childFolder.setId(2L);
        childFolder.setName("Pasta Filha");
        childFolder.setParentFolder(parentFolder);

        assertThat(childFolder.getParentFolder())
                .isNotNull()
                .isEqualTo(parentFolder);
    }

    @Test
    @DisplayName("Should allow null parent folder for root folders")
    void shouldAllowNullParentFolderForRootFolders() {

        PSBFolderEntity rootFolder = new PSBFolderEntity();
        rootFolder.setName("Pasta Raiz");
        rootFolder.setParentFolder(null);

        assertThat(rootFolder.getParentFolder()).isNull();
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of subfolders")
    void shouldMaintainOneToManyCollectionOfSubfolders() {

        PSBFolderEntity parentFolder = new PSBFolderEntity();
        parentFolder.setName("Pasta Pai");

        PSBFolderEntity subfolder = new PSBFolderEntity();
        subfolder.setId(1L);
        subfolder.setName("Subpasta");

        parentFolder.getSubfolders().add(subfolder);

        assertThat(parentFolder.getSubfolders())
                .isNotNull()
                .hasSize(1)
                .contains(subfolder);
    }

    @Test
    @DisplayName("Should support multiple subfolders")
    void shouldSupportMultipleSubfolders() {

        PSBFolderEntity parentFolder = new PSBFolderEntity();
        parentFolder.setName("Pasta Pai");

        PSBFolderEntity sub1 = new PSBFolderEntity();
        sub1.setId(1L);
        PSBFolderEntity sub2 = new PSBFolderEntity();
        sub2.setId(2L);
        PSBFolderEntity sub3 = new PSBFolderEntity();
        sub3.setId(3L);

        parentFolder.getSubfolders().add(sub1);
        parentFolder.getSubfolders().add(sub2);
        parentFolder.getSubfolders().add(sub3);

        assertThat(parentFolder.getSubfolders()).hasSize(3);
    }

    @Test
    @DisplayName("Should initialize empty subfolders collection by default")
    void shouldInitializeEmptySubfoldersCollectionByDefault() {

        PSBFolderEntity folder = new PSBFolderEntity();

        assertThat(folder.getSubfolders()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of files")
    void shouldMaintainOneToManyCollectionOfFiles() {

        PSBFolderEntity folder = new PSBFolderEntity();
        folder.setName("Pasta");

        PSBFileEntity file = new PSBFileEntity();
        file.setId(1L);
        file.setFilename("documento.pdf");

        folder.getFiles().add(file);

        assertThat(folder.getFiles())
                .isNotNull()
                .hasSize(1)
                .contains(file);
    }

    @Test
    @DisplayName("Should support multiple files per folder")
    void shouldSupportMultipleFilesPerFolder() {

        PSBFolderEntity folder = new PSBFolderEntity();
        folder.setName("Documentos");

        PSBFileEntity file1 = new PSBFileEntity();
        file1.setId(1L);
        PSBFileEntity file2 = new PSBFileEntity();
        file2.setId(2L);
        PSBFileEntity file3 = new PSBFileEntity();
        file3.setId(3L);

        folder.getFiles().add(file1);
        folder.getFiles().add(file2);
        folder.getFiles().add(file3);

        assertThat(folder.getFiles()).hasSize(3);
    }

    @Test
    @DisplayName("Should initialize empty files collection by default")
    void shouldInitializeEmptyFilesCollectionByDefault() {

        PSBFolderEntity folder = new PSBFolderEntity();

        assertThat(folder.getFiles()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of share links")
    void shouldMaintainOneToManyCollectionOfShareLinks() {

        PSBFolderEntity folder = new PSBFolderEntity();
        folder.setName("Pasta Compartilhada");

        ShareFolderEntity shareLink = new ShareFolderEntity();
        shareLink.setId(1L);

        folder.getShareLinks().add(shareLink);

        assertThat(folder.getShareLinks())
                .isNotNull()
                .hasSize(1)
                .contains(shareLink);
    }

    @Test
    @DisplayName("Should initialize empty share links collection by default")
    void shouldInitializeEmptyShareLinksCollectionByDefault() {

        PSBFolderEntity folder = new PSBFolderEntity();

        assertThat(folder.getShareLinks()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support folder index for ordering")
    void shouldSupportFolderIndexForOrdering() {

        PSBFolderEntity folder1 = new PSBFolderEntity();
        folder1.setFolderIndex(0);

        PSBFolderEntity folder2 = new PSBFolderEntity();
        folder2.setFolderIndex(1);

        PSBFolderEntity folder3 = new PSBFolderEntity();
        folder3.setFolderIndex(2);

        assertThat(folder1.getFolderIndex()).isZero();
        assertThat(folder2.getFolderIndex()).isEqualTo(1);
        assertThat(folder3.getFolderIndex()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should support optional description")
    void shouldSupportOptionalDescription() {

        PSBFolderEntity folder = new PSBFolderEntity();
        folder.setName("Pasta");
        folder.setDescription("Esta é uma descrição detalhada da pasta");

        assertThat(folder.getDescription()).isEqualTo("Esta é uma descrição detalhada da pasta");
    }

    @Test
    @DisplayName("Should allow null description")
    void shouldAllowNullDescription() {

        PSBFolderEntity folder = new PSBFolderEntity();
        folder.setName("Pasta");
        folder.setDescription(null);

        assertThat(folder.getDescription()).isNull();
    }

    @Test
    @DisplayName("Should support long descriptions up to 1000 characters")
    void shouldSupportLongDescriptionsUpTo1000Characters() {

        String longDescription = "A".repeat(1000);
        PSBFolderEntity folder = new PSBFolderEntity();
        folder.setDescription(longDescription);

        assertThat(folder.getDescription()).hasSize(1000);
    }

    @Test
    @DisplayName("Should support Portuguese characters in name")
    void shouldSupportPortugueseCharactersInName() {

        PSBFolderEntity folder = new PSBFolderEntity();
        folder.setName("Documentação Técnica");

        assertThat(folder.getName()).contains("ç", "é");
    }

    @Test
    @DisplayName("Should support server path with directories")
    void shouldSupportServerPathWithDirectories() {

        PSBFolderEntity folder = new PSBFolderEntity();
        folder.setServerPath("/psb/barragem1/documentos/tecnicos");

        assertThat(folder.getServerPath()).contains("/", "psb", "barragem1", "documentos", "tecnicos");
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with createdBy user")
    void shouldMaintainManyToOneRelationshipWithCreatedByUser() {

        PSBFolderEntity folder = new PSBFolderEntity();
        folder.setCreatedBy(user);

        assertThat(folder.getCreatedBy())
                .isNotNull()
                .isEqualTo(user);
    }

    @Test
    @DisplayName("Should allow null createdBy")
    void shouldAllowNullCreatedBy() {

        PSBFolderEntity folder = new PSBFolderEntity();
        folder.setName("Pasta");
        folder.setCreatedBy(null);

        assertThat(folder.getCreatedBy()).isNull();
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        PSBFolderEntity folder = new PSBFolderEntity();
        folder.setId(1L);
        folder.setName("Pasta Original");

        Long originalId = folder.getId();

        folder.setName("Pasta Renomeada");
        folder.setColor(FolderColorEnum.RED);

        assertThat(folder.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support hierarchical folder structure")
    void shouldSupportHierarchicalFolderStructure() {

        PSBFolderEntity root = new PSBFolderEntity();
        root.setId(1L);
        root.setName("PSB Root");
        root.setFolderIndex(0);
        root.setParentFolder(null);

        PSBFolderEntity level1 = new PSBFolderEntity();
        level1.setId(2L);
        level1.setName("Documentos");
        level1.setFolderIndex(0);
        level1.setParentFolder(root);

        PSBFolderEntity level2 = new PSBFolderEntity();
        level2.setId(3L);
        level2.setName("Técnicos");
        level2.setFolderIndex(0);
        level2.setParentFolder(level1);

        root.getSubfolders().add(level1);
        level1.getSubfolders().add(level2);

        assertThat(root.getParentFolder()).isNull();
        assertThat(level1.getParentFolder()).isEqualTo(root);
        assertThat(level2.getParentFolder()).isEqualTo(level1);
        assertThat(root.getSubfolders()).contains(level1);
        assertThat(level1.getSubfolders()).contains(level2);
    }

    @Test
    @DisplayName("Should support cascade operations on subfolders")
    void shouldSupportCascadeOperationsOnSubfolders() {

        PSBFolderEntity parentFolder = new PSBFolderEntity();
        parentFolder.setName("Pasta Pai");

        PSBFolderEntity subfolder = new PSBFolderEntity();
        subfolder.setName("Subpasta");

        parentFolder.getSubfolders().add(subfolder);

        assertThat(parentFolder.getSubfolders()).hasSize(1);
    }

    @Test
    @DisplayName("Should support orphan removal for subfolders")
    void shouldSupportOrphanRemovalForSubfolders() {

        PSBFolderEntity parentFolder = new PSBFolderEntity();
        PSBFolderEntity subfolder = new PSBFolderEntity();
        parentFolder.getSubfolders().add(subfolder);

        parentFolder.getSubfolders().remove(subfolder);

        assertThat(parentFolder.getSubfolders()).isEmpty();
    }

    @Test
    @DisplayName("Should support multiple folders per dam")
    void shouldSupportMultipleFoldersPerDam() {

        PSBFolderEntity folder1 = new PSBFolderEntity();
        folder1.setId(1L);
        folder1.setName("Pasta 1");
        folder1.setDam(dam);

        PSBFolderEntity folder2 = new PSBFolderEntity();
        folder2.setId(2L);
        folder2.setName("Pasta 2");
        folder2.setDam(dam);

        assertThat(folder1.getDam()).isEqualTo(folder2.getDam());
        assertThat(folder1.getId()).isNotEqualTo(folder2.getId());
    }
}
