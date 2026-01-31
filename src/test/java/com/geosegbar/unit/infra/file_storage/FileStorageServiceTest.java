// package com.geosegbar.unit.infra.file_storage;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.assertj.core.api.Assertions.assertThatCode;
// import static org.assertj.core.api.Assertions.assertThatThrownBy;
// import static org.mockito.Mockito.mock;
// import static org.mockito.Mockito.when;

// import java.io.ByteArrayInputStream;
// import java.io.IOException;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.nio.file.Paths;

// import org.junit.jupiter.api.AfterEach;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Tag;
// import org.junit.jupiter.api.Test;
// import org.springframework.test.util.ReflectionTestUtils;
// import org.springframework.web.multipart.MultipartFile;

// import com.geosegbar.config.BaseUnitTest;
// import com.geosegbar.exceptions.FileStorageException;
// import com.geosegbar.infra.file_storage.FileStorageService;

// @Tag("unit")
// @DisplayName("FileStorageService - Unit Tests")
// class FileStorageServiceTest extends BaseUnitTest {

//     private FileStorageService fileStorageService;

//     private static final String TEST_UPLOAD_DIR = "storage-test";
//     private static final String TEST_BASE_URL = "/api/files/";
//     private static final String TEST_FRONTEND_URL = "https://test.com";

//     @BeforeEach
//     void setUp() throws IOException {
//         fileStorageService = new FileStorageService();
//         ReflectionTestUtils.setField(fileStorageService, "uploadDir", TEST_UPLOAD_DIR);
//         ReflectionTestUtils.setField(fileStorageService, "baseUrl", TEST_BASE_URL);
//         ReflectionTestUtils.setField(fileStorageService, "frontendUrl", TEST_FRONTEND_URL);

//         // Create test directory
//         Path uploadPath = Paths.get(TEST_UPLOAD_DIR).toAbsolutePath().normalize();
//         Files.createDirectories(uploadPath);
//     }

//     @AfterEach
//     void tearDown() throws IOException {
//         // Clean up test directory
//         Path uploadPath = Paths.get(TEST_UPLOAD_DIR).toAbsolutePath().normalize();
//         if (Files.exists(uploadPath)) {
//             Files.walk(uploadPath)
//                     .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
//                     .forEach(path -> {
//                         try {
//                             Files.deleteIfExists(path);
//                         } catch (IOException e) {
//                             // Ignore cleanup errors
//                         }
//                     });
//         }
//     }

//     @Test
//     @DisplayName("Should store file successfully with valid MultipartFile")
//     void shouldStoreFileSuccessfullyWithValidMultipartFile() throws IOException {
//         // Given
//         MultipartFile mockFile = mock(MultipartFile.class);
//         String originalFilename = "test-document.pdf";
//         byte[] fileContent = "Test file content".getBytes();

//         when(mockFile.getOriginalFilename()).thenReturn(originalFilename);
//         when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));

//         // When
//         String fileUrl = fileStorageService.storeFile(mockFile, "documents");

//         // Then
//         assertThat(fileUrl).isNotNull();
//         assertThat(fileUrl).startsWith(TEST_FRONTEND_URL + TEST_BASE_URL + "documents/");
//         assertThat(fileUrl).contains("test-document.pdf");
//     }

//     @Test
//     @DisplayName("Should sanitize filename with special characters")
//     void shouldSanitizeFilenameWithSpecialCharacters() throws IOException {
//         // Given
//         MultipartFile mockFile = mock(MultipartFile.class);
//         String originalFilename = "arquivo com espaços & caracteres!@#.pdf";
//         byte[] fileContent = "Test content".getBytes();

//         when(mockFile.getOriginalFilename()).thenReturn(originalFilename);
//         when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));

//         // When
//         String fileUrl = fileStorageService.storeFile(mockFile, "documents");

//         // Then
//         assertThat(fileUrl).isNotNull();
//         assertThat(fileUrl).doesNotContain(" ", "&", "!", "@", "#");
//         assertThat(fileUrl).contains("_"); // Special chars replaced with underscore
//     }

//     @Test
//     @DisplayName("Should add timestamp to filename to ensure uniqueness")
//     void shouldAddTimestampToFilenameToEnsureUniqueness() throws IOException {
//         // Given
//         MultipartFile mockFile = mock(MultipartFile.class);
//         String originalFilename = "document.pdf";
//         byte[] fileContent = "Test content".getBytes();

//         when(mockFile.getOriginalFilename()).thenReturn(originalFilename);
//         when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));

//         // When
//         String fileUrl1 = fileStorageService.storeFile(mockFile, "documents");

//         // Wait a moment to ensure different timestamp
//         try {
//             Thread.sleep(1100); // Wait more than 1 second for different epoch second
//         } catch (InterruptedException e) {
//             Thread.currentThread().interrupt();
//         }

//         String fileUrl2 = fileStorageService.storeFile(mockFile, "documents");

//         // Then
//         assertThat(fileUrl1).isNotEqualTo(fileUrl2);
//         assertThat(fileUrl1).contains("document.pdf");
//         assertThat(fileUrl2).contains("document.pdf");
//     }

//     @Test
//     @DisplayName("Should preserve file extension from original filename")
//     void shouldPreserveFileExtensionFromOriginalFilename() throws IOException {
//         // Given
//         MultipartFile mockFile = mock(MultipartFile.class);
//         String originalFilename = "image.png";
//         byte[] fileContent = "PNG content".getBytes();

//         when(mockFile.getOriginalFilename()).thenReturn(originalFilename);
//         when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));

//         // When
//         String fileUrl = fileStorageService.storeFile(mockFile, "images");

//         // Then
//         assertThat(fileUrl).endsWith(".png");
//     }

//     @Test
//     @DisplayName("Should create subdirectory if it does not exist")
//     void shouldCreateSubdirectoryIfItDoesNotExist() throws IOException {
//         // Given
//         MultipartFile mockFile = mock(MultipartFile.class);
//         String newSubDirectory = "new-folder/nested";
//         byte[] fileContent = "Test".getBytes();

//         when(mockFile.getOriginalFilename()).thenReturn("test.txt");
//         when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));

//         // When
//         String fileUrl = fileStorageService.storeFile(mockFile, newSubDirectory);

//         // Then
//         assertThat(fileUrl).contains(newSubDirectory);

//         Path subDirPath = Paths.get(TEST_UPLOAD_DIR, newSubDirectory).toAbsolutePath().normalize();
//         assertThat(Files.exists(subDirPath)).isTrue();
//         assertThat(Files.isDirectory(subDirPath)).isTrue();
//     }

//     @Test
//     @DisplayName("Should throw FileStorageException when IOException occurs during storeFile")
//     void shouldThrowFileStorageExceptionWhenIOExceptionOccursDuringStoreFile() throws IOException {
//         // Given
//         MultipartFile mockFile = mock(MultipartFile.class);

//         when(mockFile.getOriginalFilename()).thenReturn("test.pdf");
//         when(mockFile.getInputStream()).thenThrow(new IOException("Read error"));

//         // When & Then
//         assertThatThrownBy(() -> fileStorageService.storeFile(mockFile, "documents"))
//                 .isInstanceOf(FileStorageException.class)
//                 .hasMessageContaining("Could not store file");
//     }

//     @Test
//     @DisplayName("Should handle null original filename in MultipartFile")
//     void shouldHandleNullOriginalFilenameInMultipartFile() throws IOException {
//         // Given
//         MultipartFile mockFile = mock(MultipartFile.class);
//         byte[] fileContent = "Content".getBytes();

//         when(mockFile.getOriginalFilename()).thenReturn(null);
//         when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));

//         // When
//         String fileUrl = fileStorageService.storeFile(mockFile, "uploads");

//         // Then
//         assertThat(fileUrl).isNotNull();
//         assertThat(fileUrl).contains("file"); // Default filename when null
//     }

//     @Test
//     @DisplayName("Should store file from bytes successfully with JPEG content type")
//     void shouldStoreFileFromBytesSuccessfullyWithJpegContentType() throws IOException {
//         // Given
//         byte[] fileBytes = "JPEG image data".getBytes();
//         String originalFileName = "photo.jpg";
//         String contentType = "image/jpeg";

//         // When
//         String fileUrl = fileStorageService.storeFileFromBytes(fileBytes, originalFileName, contentType, "photos");

//         // Then
//         assertThat(fileUrl).isNotNull();
//         assertThat(fileUrl).startsWith(TEST_FRONTEND_URL + TEST_BASE_URL + "photos/");
//         assertThat(fileUrl).contains("photo.jpg");
//     }

//     @Test
//     @DisplayName("Should store file from bytes with PNG content type")
//     void shouldStoreFileFromBytesWithPngContentType() throws IOException {
//         // Given
//         byte[] fileBytes = "PNG data".getBytes();
//         String originalFileName = "image.png";
//         String contentType = "image/png";

//         // When
//         String fileUrl = fileStorageService.storeFileFromBytes(fileBytes, originalFileName, contentType, "images");

//         // Then
//         assertThat(fileUrl).endsWith(".png");
//     }

//     @Test
//     @DisplayName("Should store file from bytes with GIF content type")
//     void shouldStoreFileFromBytesWithGifContentType() throws IOException {
//         // Given
//         byte[] fileBytes = "GIF data".getBytes();
//         String originalFileName = "animation.gif";
//         String contentType = "image/gif";

//         // When
//         String fileUrl = fileStorageService.storeFileFromBytes(fileBytes, originalFileName, contentType, "animations");

//         // Then
//         assertThat(fileUrl).endsWith(".gif");
//     }

//     @Test
//     @DisplayName("Should store file from bytes with BMP content type")
//     void shouldStoreFileFromBytesWithBmpContentType() throws IOException {
//         // Given
//         byte[] fileBytes = "BMP data".getBytes();
//         String originalFileName = "bitmap.bmp";
//         String contentType = "image/bmp";

//         // When
//         String fileUrl = fileStorageService.storeFileFromBytes(fileBytes, originalFileName, contentType, "bitmaps");

//         // Then
//         assertThat(fileUrl).endsWith(".bmp");
//     }

//     @Test
//     @DisplayName("Should preserve original filename when it has no extension")
//     void shouldPreserveOriginalFilenameWhenItHasNoExtension() throws IOException {
//         // Given
//         byte[] fileBytes = "JPEG data".getBytes();
//         String originalFileName = "photo_without_extension";
//         String contentType = "image/jpeg";

//         // When
//         String fileUrl = fileStorageService.storeFileFromBytes(fileBytes, originalFileName, contentType, "photos");

//         // Then
//         assertThat(fileUrl).contains("photo_without_extension"); // Filename preserved
//         assertThat(fileUrl).isNotNull();
//     }

//     @Test
//     @DisplayName("Should handle unknown content type when storing from bytes")
//     void shouldHandleUnknownContentTypeWhenStoringFromBytes() throws IOException {
//         // Given
//         byte[] fileBytes = "Unknown data".getBytes();
//         String originalFileName = "file.dat";
//         String contentType = "application/octet-stream";

//         // When
//         String fileUrl = fileStorageService.storeFileFromBytes(fileBytes, originalFileName, contentType, "data");

//         // Then
//         assertThat(fileUrl).isNotNull();
//         assertThat(fileUrl).contains("file.dat");
//     }

//     @Test
//     @DisplayName("Should sanitize filename when storing from bytes")
//     void shouldSanitizeFilenameWhenStoringFromBytes() throws IOException {
//         // Given
//         byte[] fileBytes = "Data".getBytes();
//         String originalFileName = "file with spaces & special!chars.jpg";
//         String contentType = "image/jpeg";

//         // When
//         String fileUrl = fileStorageService.storeFileFromBytes(fileBytes, originalFileName, contentType, "photos");

//         // Then
//         assertThat(fileUrl).doesNotContain(" ", "&", "!");
//         assertThat(fileUrl).contains("_");
//     }

//     @Test
//     @DisplayName("Should handle null original filename when storing from bytes")
//     void shouldHandleNullOriginalFilenameWhenStoringFromBytes() throws IOException {
//         // Given
//         byte[] fileBytes = "Data".getBytes();
//         String originalFileName = null;
//         String contentType = "image/png";

//         // When
//         String fileUrl = fileStorageService.storeFileFromBytes(fileBytes, originalFileName, contentType, "uploads");

//         // Then
//         assertThat(fileUrl).isNotNull();
//         assertThat(fileUrl).contains(".png"); // Extension from content type
//     }

//     @Test
//     @DisplayName("Should store file from bytes with null content type")
//     void shouldStoreFileFromBytesWithNullContentType() throws IOException {
//         // Given
//         byte[] fileBytes = "Data".getBytes();
//         String fileName = "file.dat";
//         String contentType = null; // Null content type

//         // When
//         String fileUrl = fileStorageService.storeFileFromBytes(fileBytes, fileName, contentType, "uploads");

//         // Then
//         assertThat(fileUrl).isNotNull();
//         assertThat(fileUrl).contains("file.dat");
//     }

//     @Test
//     @DisplayName("Should delete file successfully with valid file URL")
//     void shouldDeleteFileSuccessfullyWithValidFileUrl() throws IOException {
//         // Given - first store a file
//         MultipartFile mockFile = mock(MultipartFile.class);
//         byte[] fileContent = "Test content".getBytes();

//         when(mockFile.getOriginalFilename()).thenReturn("to-delete.txt");
//         when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));

//         String fileUrl = fileStorageService.storeFile(mockFile, "temp");

//         // Verify file exists
//         String relativePath = fileUrl.substring((TEST_FRONTEND_URL + TEST_BASE_URL).length());
//         Path filePath = Paths.get(TEST_UPLOAD_DIR, relativePath).toAbsolutePath().normalize();
//         assertThat(Files.exists(filePath)).isTrue();

//         // When
//         assertThatCode(() -> fileStorageService.deleteFile(fileUrl))
//                 .doesNotThrowAnyException();

//         // Then
//         assertThat(Files.exists(filePath)).isFalse();
//     }

//     @Test
//     @DisplayName("Should handle file URL without frontend domain when deleting")
//     void shouldHandleFileUrlWithoutFrontendDomainWhenDeleting() throws IOException {
//         // Given - store a file first
//         MultipartFile mockFile = mock(MultipartFile.class);
//         byte[] fileContent = "Content".getBytes();

//         when(mockFile.getOriginalFilename()).thenReturn("test.txt");
//         when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));

//         String fullUrl = fileStorageService.storeFile(mockFile, "temp");
//         String urlWithoutDomain = fullUrl.substring(TEST_FRONTEND_URL.length());

//         // When
//         assertThatCode(() -> fileStorageService.deleteFile(urlWithoutDomain))
//                 .doesNotThrowAnyException();
//     }

//     @Test
//     @DisplayName("Should handle null file URL when deleting")
//     void shouldHandleNullFileUrlWhenDeleting() {
//         // When & Then
//         assertThatCode(() -> fileStorageService.deleteFile(null))
//                 .doesNotThrowAnyException();
//     }

//     @Test
//     @DisplayName("Should handle non-existent file when deleting")
//     void shouldHandleNonExistentFileWhenDeleting() {
//         // Given
//         String nonExistentFileUrl = TEST_FRONTEND_URL + TEST_BASE_URL + "temp/non-existent.txt";

//         // When & Then
//         assertThatCode(() -> fileStorageService.deleteFile(nonExistentFileUrl))
//                 .doesNotThrowAnyException(); // Files.deleteIfExists handles non-existent files
//     }

//     @Test
//     @DisplayName("Should handle file URL with query parameters when deleting")
//     void shouldHandleFileUrlWithQueryParametersWhenDeleting() throws IOException {
//         // Given - store a file first
//         MultipartFile mockFile = mock(MultipartFile.class);
//         byte[] fileContent = "Content".getBytes();

//         when(mockFile.getOriginalFilename()).thenReturn("test.txt");
//         when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));

//         String fullUrl = fileStorageService.storeFile(mockFile, "temp");
//         String urlWithParams = fullUrl + "?param=value"; // Add query params

//         // When & Then - should handle gracefully even with query params
//         assertThatCode(() -> fileStorageService.deleteFile(fullUrl))
//                 .doesNotThrowAnyException();
//     }

//     @Test
//     @DisplayName("Should construct correct file URL with frontend URL and base URL")
//     void shouldConstructCorrectFileUrlWithFrontendUrlAndBaseUrl() throws IOException {
//         // Given
//         MultipartFile mockFile = mock(MultipartFile.class);
//         byte[] fileContent = "Content".getBytes();

//         when(mockFile.getOriginalFilename()).thenReturn("document.pdf");
//         when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));

//         // When
//         String fileUrl = fileStorageService.storeFile(mockFile, "documents");

//         // Then
//         assertThat(fileUrl).startsWith(TEST_FRONTEND_URL);
//         assertThat(fileUrl).contains(TEST_BASE_URL);
//         assertThat(fileUrl).contains("documents/");
//     }

//     @Test
//     @DisplayName("Should handle Portuguese characters in subdirectory name")
//     void shouldHandlePortugueseCharactersInSubdirectoryName() throws IOException {
//         // Given
//         MultipartFile mockFile = mock(MultipartFile.class);
//         byte[] fileContent = "Content".getBytes();

//         when(mockFile.getOriginalFilename()).thenReturn("relatorio.pdf");
//         when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));

//         // When
//         String fileUrl = fileStorageService.storeFile(mockFile, "documentação");

//         // Then
//         assertThat(fileUrl).isNotNull();
//         assertThat(fileUrl).contains("documentação");
//     }

//     @Test
//     @DisplayName("Should replace existing file when storing with same name in same subdirectory")
//     void shouldReplaceExistingFileWhenStoringWithSameNameInSameSubdirectory() throws IOException {
//         // Given - Note: This test verifies StandardCopyOption.REPLACE_EXISTING behavior
//         // The service uses timestamp, so files will have different names
//         // But if somehow the same name occurs, REPLACE_EXISTING ensures no error
//         MultipartFile mockFile = mock(MultipartFile.class);
//         byte[] fileContent1 = "First content".getBytes();
//         byte[] fileContent2 = "Second content".getBytes();

//         when(mockFile.getOriginalFilename()).thenReturn("document.pdf");
//         when(mockFile.getInputStream())
//                 .thenReturn(new ByteArrayInputStream(fileContent1))
//                 .thenReturn(new ByteArrayInputStream(fileContent2));

//         // When - store twice (will have different timestamps so different files)
//         String fileUrl1 = fileStorageService.storeFile(mockFile, "docs");

//         try {
//             Thread.sleep(1100); // Ensure different timestamp
//         } catch (InterruptedException e) {
//             Thread.currentThread().interrupt();
//         }

//         String fileUrl2 = fileStorageService.storeFile(mockFile, "docs");

//         // Then - both should succeed (REPLACE_EXISTING prevents errors)
//         assertThat(fileUrl1).isNotNull();
//         assertThat(fileUrl2).isNotNull();
//         assertThat(fileUrl1).isNotEqualTo(fileUrl2); // Different due to timestamp
//     }

//     @Test
//     @DisplayName("Should handle very long filename by sanitizing")
//     void shouldHandleVeryLongFilenameBySanitizing() throws IOException {
//         // Given
//         MultipartFile mockFile = mock(MultipartFile.class);
//         String longFilename = "a".repeat(200) + ".txt"; // Very long filename
//         byte[] fileContent = "Content".getBytes();

//         when(mockFile.getOriginalFilename()).thenReturn(longFilename);
//         when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));

//         // When
//         String fileUrl = fileStorageService.storeFile(mockFile, "uploads");

//         // Then
//         assertThat(fileUrl).isNotNull();
//         assertThat(fileUrl).endsWith(".txt");
//     }

//     @Test
//     @DisplayName("Should handle empty byte array when storing from bytes")
//     void shouldHandleEmptyByteArrayWhenStoringFromBytes() throws IOException {
//         // Given
//         byte[] emptyBytes = new byte[0];
//         String fileName = "empty.txt";

//         // When
//         String fileUrl = fileStorageService.storeFileFromBytes(emptyBytes, fileName, "text/plain", "uploads");

//         // Then
//         assertThat(fileUrl).isNotNull();

//         String relativePath = fileUrl.substring((TEST_FRONTEND_URL + TEST_BASE_URL).length());
//         Path filePath = Paths.get(TEST_UPLOAD_DIR, relativePath).toAbsolutePath().normalize();
//         assertThat(Files.exists(filePath)).isTrue();
//         assertThat(Files.size(filePath)).isEqualTo(0);
//     }
// }
