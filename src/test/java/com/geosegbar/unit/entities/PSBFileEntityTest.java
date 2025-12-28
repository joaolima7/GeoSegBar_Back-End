package com.geosegbar.unit.entities;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.PSBFileEntity;
import com.geosegbar.entities.PSBFolderEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.fixtures.TestDataBuilder;

@DisplayName("Unit Tests - PSBFileEntity")
class PSBFileEntityTest extends BaseUnitTest {

    private PSBFolderEntity psbFolder;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        TestDataBuilder.resetIdGenerator();

        psbFolder = new PSBFolderEntity();
        psbFolder.setId(1L);
        psbFolder.setName("Pasta PSB");

        user = new UserEntity();
        user.setId(1L);
        user.setName("User Test");
    }

    @Test
    @DisplayName("Should create PSB file with all required fields")
    void shouldCreatePSBFileWithAllRequiredFields() {
        // Given
        PSBFileEntity file = new PSBFileEntity();
        file.setId(1L);
        file.setFilename("documento.pdf");
        file.setFilePath("/psb/files/documento.pdf");
        file.setPsbFolder(psbFolder);

        // Then
        assertThat(file).satisfies(f -> {
            assertThat(f.getId()).isEqualTo(1L);
            assertThat(f.getFilename()).isEqualTo("documento.pdf");
            assertThat(f.getFilePath()).isEqualTo("/psb/files/documento.pdf");
            assertThat(f.getPsbFolder()).isEqualTo(psbFolder);
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        PSBFileEntity file = new PSBFileEntity(
                1L,
                "documento.pdf",
                "/psb/files/documento.pdf",
                "Documento Original.pdf",
                "application/pdf",
                1024L,
                "https://example.com/download/documento.pdf",
                psbFolder,
                user,
                now
        );

        // Then
        assertThat(file.getId()).isEqualTo(1L);
        assertThat(file.getFilename()).isEqualTo("documento.pdf");
        assertThat(file.getFilePath()).isEqualTo("/psb/files/documento.pdf");
        assertThat(file.getOriginalFilename()).isEqualTo("Documento Original.pdf");
        assertThat(file.getContentType()).isEqualTo("application/pdf");
        assertThat(file.getSize()).isEqualTo(1024L);
        assertThat(file.getDownloadUrl()).isEqualTo("https://example.com/download/documento.pdf");
        assertThat(file.getPsbFolder()).isEqualTo(psbFolder);
        assertThat(file.getUploadedBy()).isEqualTo(user);
        assertThat(file.getUploadedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should set uploadedAt timestamp")
    void shouldSetUploadedAtTimestamp() {
        // Given
        PSBFileEntity file = new PSBFileEntity();
        file.setFilename("documento.pdf");
        file.setFilePath("/psb/files/documento.pdf");

        LocalDateTime timestamp = LocalDateTime.now();

        // When
        file.setUploadedAt(timestamp);

        // Then
        assertThat(file.getUploadedAt())
                .isNotNull()
                .isCloseTo(timestamp, within(1, ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with PSBFolder")
    void shouldMaintainManyToOneRelationshipWithPSBFolder() {
        // Given
        PSBFileEntity file = new PSBFileEntity();
        file.setPsbFolder(psbFolder);

        // Then
        assertThat(file.getPsbFolder())
                .isNotNull()
                .isEqualTo(psbFolder);
    }

    @Test
    @DisplayName("Should maintain ManyToOne relationship with User")
    void shouldMaintainManyToOneRelationshipWithUser() {
        // Given
        PSBFileEntity file = new PSBFileEntity();
        file.setUploadedBy(user);

        // Then
        assertThat(file.getUploadedBy())
                .isNotNull()
                .isEqualTo(user);
    }

    @Test
    @DisplayName("Should allow null uploadedBy")
    void shouldAllowNullUploadedBy() {
        // Given
        PSBFileEntity file = new PSBFileEntity();
        file.setFilename("documento.pdf");
        file.setUploadedBy(null);

        // Then
        assertThat(file.getUploadedBy()).isNull();
    }

    @Test
    @DisplayName("Should support PDF file type")
    void shouldSupportPDFFileType() {
        // Given
        PSBFileEntity file = new PSBFileEntity();
        file.setFilename("documento.pdf");
        file.setContentType("application/pdf");

        // Then
        assertThat(file.getContentType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("Should support different content types")
    void shouldSupportDifferentContentTypes() {
        // Given
        PSBFileEntity pdf = new PSBFileEntity();
        pdf.setContentType("application/pdf");

        PSBFileEntity docx = new PSBFileEntity();
        docx.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        PSBFileEntity image = new PSBFileEntity();
        image.setContentType("image/jpeg");

        // Then
        assertThat(pdf.getContentType()).contains("pdf");
        assertThat(docx.getContentType()).contains("word");
        assertThat(image.getContentType()).contains("image");
    }

    @Test
    @DisplayName("Should allow null contentType")
    void shouldAllowNullContentType() {
        // Given
        PSBFileEntity file = new PSBFileEntity();
        file.setFilename("documento.pdf");
        file.setContentType(null);

        // Then
        assertThat(file.getContentType()).isNull();
    }

    @Test
    @DisplayName("Should support file size in bytes")
    void shouldSupportFileSizeInBytes() {
        // Given
        PSBFileEntity file = new PSBFileEntity();
        file.setSize(2048L);

        // Then
        assertThat(file.getSize()).isEqualTo(2048L);
    }

    @Test
    @DisplayName("Should allow null size")
    void shouldAllowNullSize() {
        // Given
        PSBFileEntity file = new PSBFileEntity();
        file.setFilename("documento.pdf");
        file.setSize(null);

        // Then
        assertThat(file.getSize()).isNull();
    }

    @Test
    @DisplayName("Should support download URL")
    void shouldSupportDownloadURL() {
        // Given
        PSBFileEntity file = new PSBFileEntity();
        file.setDownloadUrl("https://example.com/download/file");

        // Then
        assertThat(file.getDownloadUrl()).isEqualTo("https://example.com/download/file");
    }

    @Test
    @DisplayName("Should allow null downloadUrl")
    void shouldAllowNullDownloadUrl() {
        // Given
        PSBFileEntity file = new PSBFileEntity();
        file.setFilename("documento.pdf");
        file.setDownloadUrl(null);

        // Then
        assertThat(file.getDownloadUrl()).isNull();
    }

    @Test
    @DisplayName("Should preserve original filename")
    void shouldPreserveOriginalFilename() {
        // Given
        PSBFileEntity file = new PSBFileEntity();
        file.setFilename("abc123.pdf");
        file.setOriginalFilename("Relatório de Segurança 2024.pdf");

        // Then
        assertThat(file.getOriginalFilename()).isEqualTo("Relatório de Segurança 2024.pdf");
        assertThat(file.getFilename()).isNotEqualTo(file.getOriginalFilename());
    }

    @Test
    @DisplayName("Should allow null originalFilename")
    void shouldAllowNullOriginalFilename() {
        // Given
        PSBFileEntity file = new PSBFileEntity();
        file.setFilename("documento.pdf");
        file.setOriginalFilename(null);

        // Then
        assertThat(file.getOriginalFilename()).isNull();
    }

    @Test
    @DisplayName("Should support Portuguese characters in filename")
    void shouldSupportPortugueseCharactersInFilename() {
        // Given
        PSBFileEntity file = new PSBFileEntity();
        file.setFilename("relatório-técnico.pdf");

        // Then
        assertThat(file.getFilename()).contains("ó", "é");
    }

    @Test
    @DisplayName("Should support multiple files per folder")
    void shouldSupportMultipleFilesPerFolder() {
        // Given
        PSBFileEntity file1 = new PSBFileEntity();
        file1.setId(1L);
        file1.setFilename("doc1.pdf");
        file1.setPsbFolder(psbFolder);

        PSBFileEntity file2 = new PSBFileEntity();
        file2.setId(2L);
        file2.setFilename("doc2.pdf");
        file2.setPsbFolder(psbFolder);

        // Then
        assertThat(file1.getPsbFolder()).isEqualTo(file2.getPsbFolder());
        assertThat(file1.getId()).isNotEqualTo(file2.getId());
    }

    @Test
    @DisplayName("Should support multiple files uploaded by same user")
    void shouldSupportMultipleFilesUploadedBySameUser() {
        // Given
        PSBFileEntity file1 = new PSBFileEntity();
        file1.setId(1L);
        file1.setFilename("doc1.pdf");
        file1.setUploadedBy(user);

        PSBFileEntity file2 = new PSBFileEntity();
        file2.setId(2L);
        file2.setFilename("doc2.pdf");
        file2.setUploadedBy(user);

        // Then
        assertThat(file1.getUploadedBy()).isEqualTo(file2.getUploadedBy());
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {
        // Given
        PSBFileEntity file = new PSBFileEntity();
        file.setId(1L);
        file.setFilename("doc1.pdf");

        Long originalId = file.getId();

        // When
        file.setFilename("doc2.pdf");

        // Then
        assertThat(file.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support file path with directories")
    void shouldSupportFilePathWithDirectories() {
        // Given
        PSBFileEntity file = new PSBFileEntity();
        file.setFilePath("/psb/folder1/subfolder2/documento.pdf");

        // Then
        assertThat(file.getFilePath()).contains("/", "psb", "folder1", "subfolder2");
    }

    @Test
    @DisplayName("Should support different file extensions")
    void shouldSupportDifferentFileExtensions() {
        // Given
        PSBFileEntity pdf = new PSBFileEntity();
        pdf.setFilename("doc.pdf");

        PSBFileEntity docx = new PSBFileEntity();
        docx.setFilename("doc.docx");

        PSBFileEntity xlsx = new PSBFileEntity();
        xlsx.setFilename("planilha.xlsx");

        // Then
        assertThat(pdf.getFilename()).endsWith(".pdf");
        assertThat(docx.getFilename()).endsWith(".docx");
        assertThat(xlsx.getFilename()).endsWith(".xlsx");
    }

    @Test
    @DisplayName("Should support timestamp tracking")
    void shouldSupportTimestampTracking() {
        // Given
        PSBFileEntity file = new PSBFileEntity();
        LocalDateTime timestamp = LocalDateTime.of(2024, 12, 28, 10, 30);
        file.setUploadedAt(timestamp);

        // Then
        assertThat(file.getUploadedAt()).isEqualTo(timestamp);
    }
}
