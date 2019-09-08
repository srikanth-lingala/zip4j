package net.lingala.zip4j.io.inputstream;

import net.lingala.zip4j.AbstractIT;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.AesVersion;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.util.InternalZipConstants;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import static net.lingala.zip4j.testutils.TestUtils.getTestFileFromResources;
import static net.lingala.zip4j.testutils.ZipFileVerifier.verifyFileContent;
import static org.assertj.core.api.Assertions.assertThat;

public class ZipInputStreamIT extends AbstractIT {

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
    extractZipFileWithInputStreams(createdZipFile, PASSWORD, InternalZipConstants.BUFF_SIZE, AesVersion.ONE);
  }

  @Test
  public void testExtractWithReadLengthLessThan16WithAesAndStoreCompression() throws IOException {
    File createZipFile = createZipFile(CompressionMethod.STORE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256, PASSWORD);
    extractZipFileWithInputStreams(createZipFile, PASSWORD, 15, AesVersion.TWO);
  }

  @Test
  public void testExtractWithReadLengthLessThan16WithAesAndDeflateCompression() throws IOException {
    File createZipFile = createZipFile(CompressionMethod.DEFLATE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256, PASSWORD);
    extractZipFileWithInputStreams(createZipFile, PASSWORD, 15, AesVersion.TWO);
  }

  @Test
  public void testExtractWithReadLengthLessThan16WithZipCryptoAndStoreCompression() throws IOException {
    File createZipFile = createZipFile(CompressionMethod.DEFLATE, true, EncryptionMethod.ZIP_STANDARD, null, PASSWORD);
    extractZipFileWithInputStreams(createZipFile, PASSWORD, 12, null);
  }

  @Test
  public void testExtractWithReadLengthLessThan16WithZipCryptoAndDeflateCompression() throws IOException {
    File createZipFile = createZipFile(CompressionMethod.DEFLATE, true, EncryptionMethod.ZIP_STANDARD, null, PASSWORD);
    extractZipFileWithInputStreams(createZipFile, PASSWORD, 5, null);
  }

  @Test
  public void testExtractWithReadLengthGreaterThanButNotMultipleOf16WithAesAndStoreCompression() throws IOException {
    File createZipFile = createZipFile(CompressionMethod.STORE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256, PASSWORD);
    extractZipFileWithInputStreams(createZipFile, PASSWORD, (16 * 4) + 1, AesVersion.TWO);
  }

  @Test
  public void testExtractWithReadLengthGreaterThanButNotMultipleOf16WithAesAndDeflateCompression() throws IOException {
    File createZipFile = createZipFile(CompressionMethod.DEFLATE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256, PASSWORD);
    extractZipFileWithInputStreams(createZipFile, PASSWORD, (16 * 8) - 10, AesVersion.TWO);
  }

  @Test
  public void testExtractWithReadLengthGreaterThanButNotMultipleOf16WithZipCryptoAndStoreCompression() throws IOException {
    File createZipFile = createZipFile(CompressionMethod.DEFLATE, true, EncryptionMethod.ZIP_STANDARD, null, PASSWORD);
    extractZipFileWithInputStreams(createZipFile, PASSWORD, (16 * 2) - 6, null);
  }

  @Test
  public void testExtractWithReadLengthGreaterThanButNotMultipleOf16WithZipCryptoAndDeflateCompression() throws IOException {
    File createZipFile = createZipFile(CompressionMethod.DEFLATE, true, EncryptionMethod.ZIP_STANDARD, null, PASSWORD);
    extractZipFileWithInputStreams(createZipFile, PASSWORD, (16 * 10) - 11, null);
  }

  @Test
  public void testExtractWithRandomLengthWithAesAndDeflateCompression() throws IOException {
    Random random = new Random();
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
    ZipInputStream zipInputStream = new ZipInputStream(inputStream, "password".toCharArray());
    byte[] b = new byte[4096];
    while (zipInputStream.getNextEntry() != null) {
      while (zipInputStream.read(b) != -1) {

      }
    }
    zipInputStream.close();
  }

  @Test
  public void testGetNextEntryReturnsNextEntryEvenIfEntryNotCompletelyRead() throws IOException {
    File createZipFile = createZipFile(CompressionMethod.DEFLATE);
    ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(createZipFile));
    int numberOfEntries = 0;
    while (zipInputStream.getNextEntry() != null) {
      numberOfEntries++;
    }
    assertThat(numberOfEntries).isEqualTo(FILES_TO_ADD.size());
  }

  private void extractZipFileWithInputStreams(File zipFile, char[] password) throws IOException {
    extractZipFileWithInputStreams(zipFile, password, 4096, AesVersion.TWO);
  }

  private void extractZipFileWithInputStreams(File zipFile, char[] password, int bufferLength, AesVersion aesVersion)
      throws IOException {
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
          verifyFileContent(getTestFileFromResources(localFileHeader.getFileName()), extractedFile);
          numberOfEntriesExtracted++;
        }
      }
    }

    assertThat(numberOfEntriesExtracted).isEqualTo(FILES_TO_ADD.size());
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
}