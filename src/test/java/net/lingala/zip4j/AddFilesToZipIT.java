package net.lingala.zip4j;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.utils.TestUtils;
import net.lingala.zip4j.utils.ZipFileVerifier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class AddFilesToZipIT extends AbstractIT {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testAddFileAsStringParameterThrowsExceptionWhenFileDoesNotExist() throws ZipException {
    expectedException.expectMessage("File does not exist: somefile.txt");
    expectedException.expect(ZipException.class);

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFile("somefile.txt");
  }

  @Test
  public void testAddFileAsStringParameterWithoutZipParameterAddsAsDeflate() throws ZipException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFile(TestUtils.getFileFromResources("sample.pdf").getPath());

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("sample.pdf"), CompressionMethod.DEFLATE, null, null);
  }

  @Test
  public void testAddFileAsStringWithZipParametersStoreAndStandardEncryption() throws ZipException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.ZIP_STANDARD, null);
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);

    zipFile.addFile(TestUtils.getFileFromResources("sample_text_large.txt").getPath(), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("sample_text_large.txt"), CompressionMethod.STORE,
        EncryptionMethod.ZIP_STANDARD, null);
  }

  @Test
  public void testAddFileThrowsExceptionWhenFileDoesNotExist() throws ZipException {
    expectedException.expectMessage("File does not exist: somefile.txt");
    expectedException.expect(ZipException.class);

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFile(new File("somefile.txt"));
  }

  @Test
  public void testAddFileThrowsExceptionWhenPasswordNotSet() throws ZipException {
    expectedException.expectMessage("input password is empty or null");
    expectedException.expect(ZipException.class);

    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    ZipFile zipFile = new ZipFile(generatedZipFile);

    zipFile.addFile(TestUtils.getFileFromResources("file_PDF_1MB.pdf"), zipParameters);
  }

  @Test
  public void testAddFileWithoutZipParameterAddsAsDeflate() throws ZipException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFile(TestUtils.getFileFromResources("file_PDF_1MB.pdf"));

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("file_PDF_1MB.pdf"), CompressionMethod.DEFLATE, null,
        null);
  }

  @Test
  public void testAddFileWithZipParametersStoreAndStandardZip() throws ZipException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.ZIP_STANDARD, null);
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);

    zipFile.addFile(TestUtils.getFileFromResources("sample_text_large.txt"), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("sample_text_large.txt"), CompressionMethod.STORE,
        EncryptionMethod.ZIP_STANDARD, null);
  }

  @Test
  public void testAddFileWithZipParametersStoreAndAes128Encryption() throws ZipException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128);
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);

    zipFile.addFile(TestUtils.getFileFromResources("sample_text_large.txt"), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("sample_text_large.txt"), CompressionMethod.STORE,
        EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128);
  }

  @Test
  public void testAddFileWithZipParametersStoreAndAes256Encryption() throws ZipException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);

    zipFile.addFile(TestUtils.getFileFromResources("sample_text_large.txt"), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("sample_text_large.txt"), CompressionMethod.STORE,
        EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
  }

  @Test
  public void testAddFileRemovesExistingFileNoEncryption() throws ZipException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFiles(FILES_TO_ADD);

    zipFile.addFile(TestUtils.getFileFromResources("sample_text_large.txt"));

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 3);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("sample_text_large.txt"), CompressionMethod.DEFLATE,
        null, null);
  }

  @Test
  public void testAddFileRemovesExistingFileNoEncryptionSingleFileInZip() throws ZipException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFile(TestUtils.getFileFromResources("sample_text_large.txt"));

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    zipFile.addFile(TestUtils.getFileFromResources("sample_text_large.txt"), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("sample_text_large.txt"), CompressionMethod.STORE,
        null, null);
  }

  @Test
  public void testAddFileRemovesExistingFileWithAesEncryption() throws ZipException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    List<File> filesToAdd = new ArrayList<>(FILES_TO_ADD);
    filesToAdd.add(1, TestUtils.getFileFromResources("file_PDF_1MB.pdf"));
    zipFile.addFiles(filesToAdd, zipParameters);

    zipFile.addFile(TestUtils.getFileFromResources("file_PDF_1MB.pdf"), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, FILES_TO_ADD.size()
        + 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("file_PDF_1MB.pdf"), CompressionMethod.DEFLATE,
        EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
  }

  private void verifyZipFileContainsFiles(File generatedZipFile, List<String> fileNames,
                                          CompressionMethod compressionMethod, EncryptionMethod encryptionMethod,
                                          AesKeyStrength aesKeyStrength) throws ZipException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    List<FileHeader> fileHeaders = zipFile.getFileHeaders();
    verifyFileHeadersContainsFiles(fileHeaders, fileNames);
    verifyAllFilesAreOf(fileHeaders, compressionMethod, encryptionMethod, aesKeyStrength);
  }

  private void verifyFileHeadersContainsFiles(List<FileHeader> fileHeaders, List<String> fileNames) {
    for (String fileName : fileNames) {
      boolean fileFound = false;
      for (FileHeader fileHeader : fileHeaders) {
        if (fileHeader.getFileName().equals(fileName)) {
          fileFound = true;
          break;
        }
      }

      assertThat(fileFound).as("File with name %s not found in zip file", fileName).isTrue();
    }
  }

  private void verifyAllFilesAreOf(List<FileHeader> fileHeaders, CompressionMethod compressionMethod,
                                   EncryptionMethod encryptionMethod, AesKeyStrength aesKeyStrength) {
    for (FileHeader fileHeader : fileHeaders) {
      if (fileHeader.isDirectory()) {
        assertThat(fileHeader.getCompressionMethod()).isEqualTo(CompressionMethod.STORE);
        assertThat(fileHeader.isEncrypted()).isFalse();
        assertThat(fileHeader.getAesExtraDataRecord()).isNull();
      } else {
        CompressionMethod shouldBeCompressionMethod = getShouldBeCompressionMethod(
            encryptionMethod == EncryptionMethod.AES, compressionMethod);
        assertThat(fileHeader.getCompressionMethod()).isEqualTo(shouldBeCompressionMethod);

        if (encryptionMethod == null) {
          assertThat(fileHeader.getEncryptionMethod()).isEqualTo(EncryptionMethod.NONE);
        } else {
          assertThat(fileHeader.getEncryptionMethod()).isEqualTo(encryptionMethod);
        }

        if (encryptionMethod == EncryptionMethod.AES) {
          verifyAesExtraDataRecord(fileHeader.getAesExtraDataRecord(), aesKeyStrength);
        } else {
          assertThat(fileHeader.getAesExtraDataRecord()).isNull();
        }
      }
    }
  }

  private void verifyAesExtraDataRecord(AESExtraDataRecord aesExtraDataRecord, AesKeyStrength aesKeyStrength) {
    assertThat(aesExtraDataRecord).isNotNull();
    assertThat(aesExtraDataRecord.getAesKeyStrength()).isEqualTo(aesKeyStrength);
  }

  private CompressionMethod getShouldBeCompressionMethod(boolean isAesEncrypted, CompressionMethod compressionMethod) {
    if (isAesEncrypted) {
      return CompressionMethod.AES_INTERNAL_ONLY;
    }

    return compressionMethod;
  }
}
