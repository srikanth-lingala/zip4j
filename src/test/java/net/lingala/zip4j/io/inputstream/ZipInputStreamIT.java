package net.lingala.zip4j.io.inputstream;

import net.lingala.zip4j.AbstractIT;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.Zip4jConfig;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.AesVersion;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.testutils.TestUtils;
import net.lingala.zip4j.util.InternalZipConstants;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static net.lingala.zip4j.testutils.TestUtils.getTestFileFromResources;
import static net.lingala.zip4j.testutils.ZipFileVerifier.verifyFileContent;
import static net.lingala.zip4j.util.InternalZipConstants.MIN_BUFF_SIZE;
import static net.lingala.zip4j.util.InternalZipConstants.USE_UTF8_FOR_PASSWORD_ENCODING_DECODING;
import static org.assertj.core.api.Assertions.assertThat;

public class ZipInputStreamIT extends AbstractIT {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testZipInputStreamConstructorThrowsExceptionWhenBufferSizeIsLessThanExpected() {
    InputStream inputStream = new ByteArrayInputStream(new byte[1]);
    Zip4jConfig zip4jConfig = new Zip4jConfig(null, InternalZipConstants.MIN_BUFF_SIZE - 1, USE_UTF8_FOR_PASSWORD_ENCODING_DECODING);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Buffer size cannot be less than " + MIN_BUFF_SIZE + " bytes");

    new ZipInputStream(inputStream, (char[]) null, zip4jConfig);
  }

  @Test
  public void testExtractStoreWithoutEncryption() throws IOException {
    File createdZipFile = createZipFile(CompressionMethod.STORE);
    extractZipFileWithInputStreams(createdZipFile, null);
  }

  @Test
  public void testExtractStoreWithZipStandardEncryption() throws IOException {
    File createdZipFile = createZipFile(CompressionMethod.STORE, true, EncryptionMethod.ZIP_STANDARD, null, PASSWORD);
    extractZipFileWithInputStreams(createdZipFile, PASSWORD);
  }

  @Test
  public void testExtractStoreWithAesEncryption128() throws IOException {
    File createdZipFile = createZipFile(CompressionMethod.STORE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128, PASSWORD);
    extractZipFileWithInputStreams(createdZipFile, PASSWORD);
  }

  @Test
  public void testExtractStoreWithAesEncryption256() throws IOException {
    File createdZipFile = createZipFile(CompressionMethod.STORE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256, PASSWORD);
    extractZipFileWithInputStreams(createdZipFile, PASSWORD);
  }

  @Test
  public void testExtractDeflateWithoutEncryption() throws IOException {
    File createdZipFile = createZipFile(CompressionMethod.DEFLATE);
    extractZipFileWithInputStreams(createdZipFile, null);
  }

  @Test
  public void testExtractDeflateWithAesEncryption128() throws IOException {
    File createdZipFile = createZipFile(CompressionMethod.DEFLATE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128, PASSWORD);
    extractZipFileWithInputStreams(createdZipFile, PASSWORD);
  }

  @Test
  public void testExtractDeflateWithAesEncryption256() throws IOException {
    File createdZipFile = createZipFile(CompressionMethod.DEFLATE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256, PASSWORD);
    extractZipFileWithInputStreams(createdZipFile, PASSWORD);
  }

  @Test
  public void testExtractDeflateWithAesEncryption256AndV1() throws IOException {
    File createdZipFile = createZipFile(CompressionMethod.DEFLATE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256, PASSWORD, AesVersion.ONE);
    extractZipFileWithInputStreams(createdZipFile, PASSWORD);
  }

  @Test
  public void testExtractWithReadLengthLessThan16WithAesAndStoreCompression() throws IOException {
    File createZipFile = createZipFile(CompressionMethod.STORE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256, PASSWORD);
    extractZipFileWithInputStreams(createZipFile, PASSWORD, 15);
  }

  @Test
  public void testExtractWithReadLengthLessThan16WithAesAndDeflateCompression() throws IOException {
    File createZipFile = createZipFile(CompressionMethod.DEFLATE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256, PASSWORD);
    extractZipFileWithInputStreams(createZipFile, PASSWORD, 15);
  }

  @Test
  public void testExtractWithReadLengthLessThan16WithZipCryptoAndStoreCompression() throws IOException {
    File createZipFile = createZipFile(CompressionMethod.DEFLATE, true, EncryptionMethod.ZIP_STANDARD, null, PASSWORD);
    extractZipFileWithInputStreams(createZipFile, PASSWORD, 12);
  }

  @Test
  public void testExtractWithReadLengthLessThan16WithZipCryptoAndDeflateCompression() throws IOException {
    File createZipFile = createZipFile(CompressionMethod.DEFLATE, true, EncryptionMethod.ZIP_STANDARD, null, PASSWORD);
    extractZipFileWithInputStreams(createZipFile, PASSWORD, 5);
  }

  @Test
  public void testExtractWithReadLengthGreaterThanButNotMultipleOf16WithAesAndStoreCompression() throws IOException {
    File createZipFile = createZipFile(CompressionMethod.STORE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256, PASSWORD);
    extractZipFileWithInputStreams(createZipFile, PASSWORD, (16 * 4) + 1);
  }

  @Test
  public void testExtractWithReadLengthGreaterThanButNotMultipleOf16WithAesAndDeflateCompression() throws IOException {
    File createZipFile = createZipFile(CompressionMethod.DEFLATE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256, PASSWORD);
    extractZipFileWithInputStreams(createZipFile, PASSWORD, (16 * 8) - 10);
  }

  @Test
  public void testExtractWithReadLengthGreaterThanButNotMultipleOf16WithZipCryptoAndStoreCompression() throws IOException {
    File createZipFile = createZipFile(CompressionMethod.DEFLATE, true, EncryptionMethod.ZIP_STANDARD, null, PASSWORD);
    extractZipFileWithInputStreams(createZipFile, PASSWORD, (16 * 2) - 6);
  }

  @Test
  public void testExtractWithReadLengthGreaterThanButNotMultipleOf16WithZipCryptoAndDeflateCompression() throws IOException {
    File createZipFile = createZipFile(CompressionMethod.DEFLATE, true, EncryptionMethod.ZIP_STANDARD, null, PASSWORD);
    extractZipFileWithInputStreams(createZipFile, PASSWORD, (16 * 10) - 11);
  }

  @Test
  public void testExtractWithRandomLengthWithAesAndDeflateCompression() throws IOException {
    SecureRandom random = new SecureRandom();
    File createZipFile = createZipFile(CompressionMethod.DEFLATE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256, PASSWORD);
    LocalFileHeader localFileHeader;
    int readLen;
    byte[] readBuffer = new byte[4096];
    int numberOfEntriesExtracted = 0;

    try (FileInputStream fileInputStream = new FileInputStream(createZipFile)) {
      try (ZipInputStream zipInputStream = new ZipInputStream(fileInputStream, PASSWORD)) {
        while ((localFileHeader = zipInputStream.getNextEntry()) != null) {
          File extractedFile = temporaryFolder.newFile(localFileHeader.getFileName());
          try (OutputStream outputStream = new FileOutputStream(extractedFile)) {
            while ((readLen = zipInputStream.read(readBuffer, 0, random.nextInt((25 - 1) + 1) + 1)) != -1) {
              outputStream.write(readBuffer, 0, readLen);
            }
          }
          verifyFileContent(getTestFileFromResources(localFileHeader.getFileName()), extractedFile);
          numberOfEntriesExtracted++;
        }
      }
    }

    assertThat(numberOfEntriesExtracted).isEqualTo(FILES_TO_ADD.size());
  }

  @Test
  public void testExtractFilesForZipFileWithInvalidExtraDataRecordIgnoresIt() throws IOException {
    InputStream inputStream = new FileInputStream(getTestArchiveFromResources("invalid_extra_data_record.zip"));
    try (ZipInputStream zipInputStream = new ZipInputStream(inputStream, "password".toCharArray())) {
      byte[] b = new byte[4096];
      while (zipInputStream.getNextEntry() != null) {
        while (zipInputStream.read(b) != -1) {

        }
      }
    }
  }

  @Test
  public void testGetNextEntryReturnsNextEntryEvenIfEntryNotCompletelyRead() throws IOException {
    File createZipFile = createZipFile(CompressionMethod.DEFLATE);
    try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(createZipFile))) {
      int numberOfEntries = 0;
      while (zipInputStream.getNextEntry() != null) {
        numberOfEntries++;
      }
      assertThat(numberOfEntries).isEqualTo(FILES_TO_ADD.size());
    }
  }

  @Test
  public void testGetFileNamesWithChineseCharset() throws IOException {
    InputStream inputStream = new FileInputStream(getTestArchiveFromResources("testfile_with_chinese_filename_by_7zip.zip"));
    try (ZipInputStream zipInputStream = new ZipInputStream(inputStream, CHARSET_GBK)) {
      LocalFileHeader localFileHeader;
      String expactedFileName = "fff - 副本.txt";
      Set<String> filenameSet = new HashSet<>();

      while ((localFileHeader = zipInputStream.getNextEntry()) != null) {
        filenameSet.add(localFileHeader.getFileName());
      }
      assertThat(filenameSet.contains(expactedFileName)).isTrue();
    }
  }

  @Test
  public void testExtractJarFile() throws IOException {
    byte[] b = new byte[4096];
    File jarFile = getTestArchiveFromResources("jar-dir-fh-entry-size-2.jar");
    try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(jarFile))) {
      while (zipInputStream.getNextEntry() != null) {
        zipInputStream.read(b);
      }
    }
  }

  @Test
  public void testExtractZipStrongEncryptionThrowsException() throws IOException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("Entry [test.txt] Strong Encryption not supported");

    File strongEncryptionFile = getTestArchiveFromResources("strong_encrypted.zip");
    try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(strongEncryptionFile))) {
      zipInputStream.getNextEntry();
    }
  }

  @Test
  public void testReadingZipBySkippingDataCreatedWithJDKZipReadsAllEntries() throws IOException {
    List<File> filesToAdd = new ArrayList<>();
    // Add a directory first, then a few files, then a directory, and then a file to test all possibilities
    filesToAdd.add(getTestFileFromResources("sample_directory"));
    filesToAdd.addAll(FILES_TO_ADD);
    filesToAdd.add(getTestFileFromResources("öüäöäö"));
    filesToAdd.add(getTestFileFromResources("file_PDF_1MB.pdf"));
    File generatedZipFile = createZipFileWithJdkZip(filesToAdd, getTestFileFromResources(""));

    int totalNumberOfEntriesRead = 0;
    try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(generatedZipFile))) {
      while (zipInputStream.getNextEntry() != null) {
        totalNumberOfEntriesRead++;
      }
    }

    assertThat(totalNumberOfEntriesRead).isEqualTo(6);
  }

  @Test
  public void testAvailableThrowsExceptionWhenStreamClosed() throws IOException {
    expectedException.expect(IOException.class);
    expectedException.expectMessage("Stream closed");

    File createZipFile = createZipFile(CompressionMethod.DEFLATE);
    ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(createZipFile));
    zipInputStream.close();

    zipInputStream.available();
  }

  @Test
  public void testAvailableReturns1WhenEntryEOFNotReachedAnd0AfterEOFReached() throws IOException {
    byte[] b = new byte[InternalZipConstants.BUFF_SIZE];
    File createZipFile = createZipFile(CompressionMethod.DEFLATE);
    try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(createZipFile))) {
      while (zipInputStream.getNextEntry() != null) {
        while (zipInputStream.read(b) != -1) {
          assertThat(zipInputStream.available()).isEqualTo(1);
        }
        assertThat(zipInputStream.available()).isEqualTo(0);
      }
    }
  }

  @Test
  public void testExtractZipWithDifferentPasswords() throws IOException {
    byte[] buffer = new byte[InternalZipConstants.BUFF_SIZE];
    File zipFileUnderTest = TestUtils.getTestArchiveFromResources("zip_with_different_passwords.zip");
    try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFileUnderTest))) {
      readAndAssertNextEntry(zipInputStream, "after_deflate_remaining_bytes.bin", "password_1");
      readAndAssertNextEntry(zipInputStream, "file_PDF_1MB.pdf", "password_2");
      // 3rd entry is not encrypted, but it should not be impacted if a password is set
      readAndAssertNextEntry(zipInputStream, "sample.pdf", null);
    }
  }

  @Test
  public void testExtractZipFileWithDirectoriesContainingExtendedLocalFileHeader() throws IOException {
    extractZipFileWithInputStreams(TestUtils.getTestArchiveFromResources("dirs_with_extended_local_file_headers.zip"),
        null, InternalZipConstants.BUFF_SIZE, false, 2);
  }

  @Test
  public void testExtractZipFileWithNullAesExtraDataRecordThrowsException() throws IOException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("corrupt AES extra data records");

    extractZipFileWithInputStreams(TestUtils.getTestArchiveFromResources("null-aes-key-strength-in-aes-extra-data-record"),
        null, InternalZipConstants.BUFF_SIZE, false, 1);
  }

  @Test
  public void testExtractZipFileWithFileNameLength0ThrowsException() throws IOException {
    expectedException.expect(IOException.class);
    expectedException.expectMessage("Invalid entry name in local file header");

    extractZipFileWithInputStreams(TestUtils.getTestArchiveFromResources("file_name_size_is_0_in_local_file_header"),
        null, InternalZipConstants.BUFF_SIZE, false, 1);
  }

  @Test
  public void testExtractZipFileWithInvalidAesExtraDataRecordThrowsException() throws IOException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("corrupt AES extra data records");

    extractZipFileWithInputStreams(TestUtils.getTestArchiveFromResources("invalid_aes_extra_data_record_length_in_header"),
        null, InternalZipConstants.BUFF_SIZE, false, 1);
  }

  @Test
  public void testExtractZipFileWhenUnexpectedEofReachedThrowsException() throws IOException {
    expectedException.expect(IOException.class);
    expectedException.expectMessage("Unexpected EOF reached when trying to read stream");

    extractZipFileWithInputStreams(TestUtils.getTestArchiveFromResources("unexpected-eof-when-reading-stream"),
        null, InternalZipConstants.BUFF_SIZE, false, 1);
  }

  @Test
  public void readingJarFileWithCompressedSizeNotZeroForDirectoryIsSuccessful() throws IOException {
    File zipFileToRead = getTestArchiveFromResources("content_in_entry_which_is_directory.jar");
    try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFileToRead.toPath()))) {
      int numberOfEntries = 0;
      while (zipInputStream.getNextEntry() != null) {
        numberOfEntries++;
      }
      assertThat(numberOfEntries).isEqualTo(345);
    }
  }

  @Test
  public void testExtractZipFileWithExtraDataRecordAndCorruptAesMacFails() throws IOException {
    expectedException.expect(IOException.class);
    expectedException.expectMessage("Reached end of data for this entry, but aes verification failed");

    extractZipFileWithInputStreams(TestUtils.getTestArchiveFromResources("aes_with_extra_data_record_and_corrupt_mac.zip"),
            PASSWORD, InternalZipConstants.BUFF_SIZE, false, 1);
  }

  private void extractZipFileWithInputStreams(File zipFile, char[] password) throws IOException {
    extractZipFileWithInputStreams(zipFile, password, InternalZipConstants.BUFF_SIZE);
  }

  private void extractZipFileWithInputStreams(File zipFile, char[] password, int bufferLength) throws IOException {
    extractZipFileWithInputStreams(zipFile, password, bufferLength, true, FILES_TO_ADD.size());
  }

  private void extractZipFileWithInputStreams(File zipFile, char[] password, int bufferLength,
                                              boolean verifyFileContents, int numberOfEntriesExpected) throws IOException {
    LocalFileHeader localFileHeader;
    int readLen;
    byte[] readBuffer = new byte[bufferLength];
    int numberOfEntriesExtracted = 0;

    try (FileInputStream fileInputStream = new FileInputStream(zipFile)) {
      try (ZipInputStream zipInputStream = new ZipInputStream(fileInputStream, password)) {
        while ((localFileHeader = zipInputStream.getNextEntry()) != null) {
          File extractedFile = temporaryFolder.newFile(localFileHeader.getFileName());
          try (OutputStream outputStream = new FileOutputStream(extractedFile)) {
            while ((readLen = zipInputStream.read(readBuffer)) != -1) {
              outputStream.write(readBuffer, 0, readLen);
            }
          }
          verifyLocalFileHeader(localFileHeader);
          if (verifyFileContents) {
            verifyFileContent(getTestFileFromResources(localFileHeader.getFileName()), extractedFile);
          }
          numberOfEntriesExtracted++;
        }
      }
    }

    assertThat(numberOfEntriesExtracted).isEqualTo(numberOfEntriesExpected);
  }

  private void verifyLocalFileHeader(LocalFileHeader localFileHeader) {
    assertThat(localFileHeader).isNotNull();
    if (localFileHeader.isEncrypted()
        && localFileHeader.getEncryptionMethod().equals(EncryptionMethod.AES)
        && localFileHeader.getAesExtraDataRecord().getAesVersion().equals(AesVersion.TWO)) {
      assertThat(localFileHeader.getCrc()).isZero();
    }
  }

  private File createZipFile(CompressionMethod compressionMethod) throws IOException {
    return createZipFile(compressionMethod, false, null, null, null);
  }

  private File createZipFile(CompressionMethod compressionMethod, boolean encryptFiles,
                             EncryptionMethod encryptionMethod, AesKeyStrength aesKeyStrength, char[] password)
      throws IOException {

    return createZipFile(compressionMethod, encryptFiles, encryptionMethod, aesKeyStrength, password, AesVersion.TWO);
  }

  private File createZipFile(CompressionMethod compressionMethod, boolean encryptFiles,
                             EncryptionMethod encryptionMethod, AesKeyStrength aesKeyStrength, char[] password,
                             AesVersion aesVersion)
      throws IOException {

    File outputFile = temporaryFolder.newFile("output.zip");
    deleteFileIfExists(outputFile);

    ZipFile zipFile = new ZipFile(outputFile, password);

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(compressionMethod);
    zipParameters.setEncryptFiles(encryptFiles);
    zipParameters.setEncryptionMethod(encryptionMethod);
    zipParameters.setAesKeyStrength(aesKeyStrength);
    zipParameters.setAesVersion(aesVersion);

    zipFile.addFiles(AbstractIT.FILES_TO_ADD, zipParameters);

    return outputFile;
  }

  private void deleteFileIfExists(File file) {
    if (file.exists()) {
      if (!file.delete()) {
        throw new RuntimeException("Could not delete an existing zip file");
      }
    }
  }

  private File createZipFileWithJdkZip(List<File> filesToAdd, File rootFolder) throws IOException {
    int readLen;
    byte[] readBuffer = new byte[InternalZipConstants.BUFF_SIZE];
    try(ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(generatedZipFile))) {
      for (File fileToAdd : filesToAdd) {
        String path = rootFolder.toPath().relativize(fileToAdd.toPath()).toString().replaceAll("\\\\", "/");
        ZipEntry entry = new ZipEntry(path);
        zipOutputStream.putNextEntry(entry);
        if (!fileToAdd.isDirectory()) {
          try (InputStream fis = new FileInputStream(fileToAdd)) {
            while ((readLen = fis.read(readBuffer)) != -1) {
              zipOutputStream.write(readBuffer, 0, readLen);
            }
          }
        }
        zipOutputStream.closeEntry();
      }
    }
    return generatedZipFile;
  }

  private void readAndAssertNextEntry(ZipInputStream zipInputStream, String fileNameToExpect, String passwordToUse)
      throws IOException {
    byte[] buffer = new byte[InternalZipConstants.BUFF_SIZE];
    if (passwordToUse != null) {
      zipInputStream.setPassword(passwordToUse.toCharArray());
    }
    LocalFileHeader localFileHeader = zipInputStream.getNextEntry();
    assertThat(localFileHeader.getFileName()).isEqualTo(fileNameToExpect);
    //noinspection StatementWithEmptyBody
    while (zipInputStream.read(buffer) != -1);
  }
}
