package net.lingala.zip4j;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.testutils.TestUtils;
import net.lingala.zip4j.testutils.ZipFileVerifier;
import net.lingala.zip4j.util.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ExtractZipFileIT extends AbstractIT {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testExtractAllStoreAndNoEncryptionExtractsSuccessfully() throws IOException {
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    zipFile.extractAll(outputFolder.getPath());

    ZipFileVerifier.verifyFolderContentsSameAsSourceFiles(outputFolder);
    verifyNumberOfFilesInOutputFolder(outputFolder, 3);
  }

  @Test
  public void testExtractAllStoreAndZipStandardEncryptionExtractsSuccessfully() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.ZIP_STANDARD, null);
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    zipFile.extractAll(outputFolder.getPath());

    ZipFileVerifier.verifyFolderContentsSameAsSourceFiles(outputFolder);
    verifyNumberOfFilesInOutputFolder(outputFolder, 3);
  }

  @Test
  public void testExtractAllStoreAndAes128EncryptionExtractsSuccessfully() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128);
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    zipFile.extractAll(outputFolder.getPath());

    ZipFileVerifier.verifyFolderContentsSameAsSourceFiles(outputFolder);
    verifyNumberOfFilesInOutputFolder(outputFolder, 3);
  }

  @Test
  public void testExtractAllStoreAndAes256EncryptionExtractsSuccessfully() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    zipFile.extractAll(outputFolder.getPath());

    ZipFileVerifier.verifyFolderContentsSameAsSourceFiles(outputFolder);
    verifyNumberOfFilesInOutputFolder(outputFolder, 3);
  }

  @Test
  public void testExtractAllDeflateAndNoEncryptionExtractsSuccessfully() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFiles(FILES_TO_ADD);

    zipFile.extractAll(outputFolder.getPath());

    ZipFileVerifier.verifyFolderContentsSameAsSourceFiles(outputFolder);
    verifyNumberOfFilesInOutputFolder(outputFolder, 3);
  }

  @Test
  public void testExtractAllDeflateAndZipStandardEncryptionExtractsSuccessfully() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.ZIP_STANDARD, null);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    zipFile.extractAll(outputFolder.getPath());

    ZipFileVerifier.verifyFolderContentsSameAsSourceFiles(outputFolder);
    verifyNumberOfFilesInOutputFolder(outputFolder, 3);
  }

  @Test
  public void testExtractAllDeflateAndAes128EncryptionExtractsSuccessfully() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    zipFile.extractAll(outputFolder.getPath());

    ZipFileVerifier.verifyFolderContentsSameAsSourceFiles(outputFolder);
    verifyNumberOfFilesInOutputFolder(outputFolder, 3);
  }

  @Test
  public void testExtractAllDeflateAndAes256EncryptionExtractsSuccessfully() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    zipFile.extractAll(outputFolder.getPath());

    ZipFileVerifier.verifyFolderContentsSameAsSourceFiles(outputFolder);
    verifyNumberOfFilesInOutputFolder(outputFolder, 3);
  }

  @Test
  public void testExtractFileWithFileHeaderWithAes128() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    FileHeader fileHeader = zipFile.getFileHeader("sample_text_large.txt");
    zipFile.extractFile(fileHeader, outputFolder.getPath());

    File[] outputFiles = outputFolder.listFiles();
    assertThat(outputFiles).hasSize(1);
    ZipFileVerifier.verifyFileContent(TestUtils.getTestFileFromResources("sample_text_large.txt"), outputFiles[0]);
  }

  @Test
  public void testExtractFileWithFileHeaderWithAes128AndInDirectory() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFolder(TestUtils.getTestFileFromResources(""), zipParameters);

    FileHeader fileHeader = zipFile.getFileHeader("test-files/öüäöäö/asöäööl");
    zipFile.extractFile(fileHeader, outputFolder.getPath());

    File outputFile = getFileWithNameFrom(outputFolder, "asöäööl");
    ZipFileVerifier.verifyFileContent(TestUtils.getTestFileFromResources("öüäöäö/asöäööl"), outputFile);
  }

  @Test
  public void testExtractFileWithFileHeaderWithAes256AndWithANewFileName() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    String newFileName = "newFileName";
    FileHeader fileHeader = zipFile.getFileHeader("sample_text_large.txt");
    zipFile.extractFile(fileHeader, outputFolder.getPath(), newFileName);

    File outputFile = getFileWithNameFrom(outputFolder, newFileName);
    ZipFileVerifier.verifyFileContent(TestUtils.getTestFileFromResources("sample_text_large.txt"), outputFile);
  }

  @Test
  public void testExtractFileWithFileNameThrowsExceptionWhenFileNotFound() throws ZipException {
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD);

    expectedException.expect(ZipException.class);
    expectedException.expectMessage("No file found with name NOT_EXISTING in zip file");

    zipFile.extractFile("NOT_EXISTING", outputFolder.getPath());
  }

  @Test
  public void testExtractFileWithFileNameWithZipStandardEncryption() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.ZIP_STANDARD, null);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFolder(TestUtils.getTestFileFromResources(""), zipParameters);

    zipFile.extractFile("test-files/sample_directory/favicon.ico", outputFolder.getPath());

    File outputFile = getFileWithNameFrom(outputFolder, "favicon.ico");
    ZipFileVerifier.verifyFileContent(TestUtils.getTestFileFromResources("sample_directory/favicon.ico"), outputFile);
  }

  @Test
  public void testExtractFileWithFileNameWithZipStandardEncryptionAndNewFileName() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.ZIP_STANDARD, null);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFolder(TestUtils.getTestFileFromResources(""), zipParameters);

    String newFileName = "newFileName";
    zipFile.extractFile("test-files/sample_directory/favicon.ico", outputFolder.getPath(), newFileName);

    File outputFile = getFileWithNameFrom(outputFolder, newFileName);
    ZipFileVerifier.verifyFileContent(TestUtils.getTestFileFromResources("sample_directory/favicon.ico"), outputFile);
  }

  @Test
  public void testExtractFilesThrowsExceptionForWrongPasswordForAes() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    try {
      zipFile = new ZipFile(generatedZipFile, "WRONG_PASSWORD".toCharArray());
      zipFile.extractAll(outputFolder.getPath());
      fail("Should throw an exception");
    } catch (ZipException e) {
      assertThat(e).isNotNull();
      assertThat(e.getType()).isEqualTo(ZipException.Type.WRONG_PASSWORD);
    }
  }

  @Test
  public void testExtractFilesThrowsExceptionForWrongPasswordForZipStandardAndDeflate() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.ZIP_STANDARD, null);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    try {
      zipFile = new ZipFile(generatedZipFile, "WRONG_PASSWORD".toCharArray());
      zipFile.extractAll(outputFolder.getPath());
      fail("Should throw an exception");
    } catch (ZipException e) {
      assertThat(e).isNotNull();
      assertThat(e.getType()).isEqualTo(ZipException.Type.WRONG_PASSWORD);
    }
  }

  @Test
  public void testExtractFilesThrowsExceptionForWrongPasswordForZipStandardAndStore() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.ZIP_STANDARD, null);
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    try {
      zipFile = new ZipFile(generatedZipFile, "WRONG_PASSWORD".toCharArray());
      zipFile.extractAll(outputFolder.getPath());
      fail("Should throw an exception");
    } catch (ZipException e) {
      assertThat(e).isNotNull();
      assertThat(e.getType()).isEqualTo(ZipException.Type.WRONG_PASSWORD);
    }
  }

  @Test
  public void testExtractFilesForAZipMadeWithZip4jv1AndStoreCompressionWithAES() throws IOException {
    File zipArchiveToTest = getTestArchiveFromResources("store_compression_made_with_v1.3.3.zip");
    ZipFileVerifier.verifyZipFileByExtractingAllFiles(zipArchiveToTest, "aaaaaaaa".toCharArray(), outputFolder, 5,
        false);
  }

  @Test
  public void testExtractFilesForZipFileWhileWithCorruptExtraDataRecordLength() throws IOException {
    ZipFile zipFile = new ZipFile(getTestArchiveFromResources("corrupt_extra_data_record_length.zip"));

    zipFile.extractAll(outputFolder.getPath());

    assertThat(zipFile.getFileHeaders()).hasSize(44);
    assertThat(Files.walk(outputFolder.toPath()).filter(Files::isRegularFile)).hasSize(44);
   }

  @Test
  public void testExtractFilesWithSetPasswordSetter() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.ZIP_STANDARD, null);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    zipFile = new ZipFile(generatedZipFile, "WRONG_PASSWORD".toCharArray());
    zipFile.setPassword(PASSWORD);
    zipFile.extractAll(outputFolder.getCanonicalPath());
  }

  @Test
  public void testExtractZipFileWithMissingExtendedLocalFileHeader() throws IOException {
    ZipFile zipFile = new ZipFile(getTestArchiveFromResources("missing_exended_local_file_header.zip"));

    zipFile.extractAll(outputFolder.getPath());

    assertThat(zipFile.getFileHeaders()).hasSize(3);
    assertThat(Files.walk(outputFolder.toPath()).filter(Files::isRegularFile)).hasSize(3);
  }

  @Test
  public void testExtractZipFileWithZip64ExtraDataRecordWithOnlyFileSizesInIt() throws IOException {
    ZipFile zipFile = new ZipFile(getTestArchiveFromResources("zip64_with_only_file_sizes.zip"),
        "Shu1an@2019GTS".toCharArray());

    zipFile.extractAll(outputFolder.getPath());

    assertThat(zipFile.getFileHeaders()).hasSize(1);
    assertThat(Files.walk(outputFolder.toPath()).filter(Files::isRegularFile)).hasSize(1);
  }

  private void verifyNumberOfFilesInOutputFolder(File outputFolder, int numberOfExpectedFiles) {
    assertThat(outputFolder.listFiles()).hasSize(numberOfExpectedFiles);
  }

  private File getFileWithNameFrom(File outputFolder, String fileName) throws ZipException {
    List<File> filesInFolder = FileUtils.getFilesInDirectoryRecursive(outputFolder, true, true);
    Optional<File> file = filesInFolder.stream().filter(e -> e.getName().equals(fileName)).findFirst();
    assertThat(file).isPresent();
    return file.get();
  }

}
