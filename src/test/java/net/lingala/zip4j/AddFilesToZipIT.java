package net.lingala.zip4j;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.AbstractFileHeader;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
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

  @Test
  public void testAddFilesWithoutParametersWhenZipFileDoesNotExistCreatesSuccessfully() throws ZipException {
    ZipFile zipFile = new ZipFile(generatedZipFile);

    zipFile.addFiles(asList(
          TestUtils.getFileFromResources("file_PDF_1MB.pdf"),
          TestUtils.getFileFromResources("zero_byte_file.txt")
    ));

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 2);
    verifyZipFileContainsFiles(generatedZipFile, asList("file_PDF_1MB.pdf", "zero_byte_file.txt"),
        CompressionMethod.DEFLATE, null, null);
  }

  @Test
  public void testAddFilesWhenZipFileDoesNotExistCreatesSuccessfully() throws ZipException {
    ZipFile zipFile = new ZipFile(generatedZipFile);

    zipFile.addFiles(asList(
        TestUtils.getFileFromResources("file_PDF_1MB.pdf"),
        TestUtils.getFileFromResources("sample_text1.txt")
    ), new ZipParameters());

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 2);
    verifyZipFileContainsFiles(generatedZipFile, asList("file_PDF_1MB.pdf", "sample_text1.txt"),
        CompressionMethod.DEFLATE, null, null);
  }

  @Test
  public void testAddFilesWithZeroByteFileWithAes128Encryption() throws ZipException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);

    zipFile.addFiles(singletonList(TestUtils.getFileFromResources("zero_byte_file.txt")), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("zero_byte_file.txt"),
        CompressionMethod.DEFLATE, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128);
  }

  @Test
  public void testAddFilesWithAes256Encryption() throws ZipException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);

    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, FILES_TO_ADD.size());
    List<String> fileNames = FILES_TO_ADD.stream().map(File::getName).collect(Collectors.toList());
    verifyZipFileContainsFiles(generatedZipFile, fileNames, CompressionMethod.DEFLATE, EncryptionMethod.AES,
        AesKeyStrength.KEY_STRENGTH_256);
  }

  @Test
  public void testAddFilesWhenFilesAlreadyExistsRemovesFiles() throws ZipException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    char[] newPassword = "SOME_OTHER_PASSWORD".toCharArray();
    zipFile = new ZipFile(generatedZipFile, newPassword);
    zipParameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);

    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, newPassword, outputFolder, FILES_TO_ADD.size());
    List<String> fileNames = FILES_TO_ADD.stream().map(File::getName).collect(Collectors.toList());
    verifyZipFileContainsFiles(generatedZipFile, fileNames, CompressionMethod.DEFLATE, EncryptionMethod.ZIP_STANDARD,
        null);
  }

  @Test
  public void testAddFilesThrowsExceptionForAES192() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("Invalid AES key strength");

    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_192);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);
  }

  @Test
  public void testAddFilesWithDifferentEncryptionType() throws ZipException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(singletonList(TestUtils.getFileFromResources("sample.pdf")), zipParameters);

    zipParameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);

    zipFile.addFiles(singletonList(TestUtils.getFileFromResources("file_PDF_1MB.pdf")), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 2);
  }

  @Test
  public void testAddFilesWithUtf8Characters() throws ZipException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);

    zipFile.addFiles(asList(
        TestUtils.getFileFromResources("sample.pdf"),
        TestUtils.getFileFromResources("бореиская.txt"),
        TestUtils.getFileFromResources("zero_byte_file.txt"),
        TestUtils.getFileFromResources("sample_text1.txt"),
        TestUtils.getFileFromResources("가나다.abc")
    ), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 5);
    List<String> fileNamesThatShouldExistInZip = asList(
        "sample.pdf",
        "бореиская.txt",
        "zero_byte_file.txt",
        "sample_text1.txt",
        "가나다.abc"
    );
    verifyZipFileContainsFiles(generatedZipFile, fileNamesThatShouldExistInZip, CompressionMethod.DEFLATE,
        EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
  }

  @Test
  public void testAddFolderWithoutZipParameters() throws ZipException, IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);

    zipFile.addFolder(TestUtils.getFileFromResources(""));

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 12);
    List<FileHeader> fileHeaders = getFileHeaders(generatedZipFile);
    verifyAllFilesInZipContainsPath(fileHeaders, "test-files/");
    verifyFoldersInZip(fileHeaders, generatedZipFile, null);
  }

  @Test
  public void testAddFolderWithStoreAndAes128() throws ZipException, IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128);
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);

    zipFile.addFolder(TestUtils.getFileFromResources(""), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 12);
    List<FileHeader> fileHeaders = getFileHeaders(generatedZipFile);
    verifyAllFilesInZipContainsPath(fileHeaders, "test-files/");
    verifyFoldersInZip(fileHeaders, generatedZipFile, PASSWORD);
  }

  @Test
  public void testAddFolderWithDeflateAndAes256AndWithoutRootFolder() throws ZipException, IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    zipParameters.setIncludeRootFolder(false);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);

    zipFile.addFolder(TestUtils.getFileFromResources(""), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 11);
    List<FileHeader> fileHeaders = getFileHeaders(generatedZipFile);
    verifyAllFilesInZipDoesNotContainPath(fileHeaders, "test-files/");
    verifyFoldersInZip(fileHeaders, generatedZipFile, PASSWORD);
  }

  @Test
  public void testAddStreamToZipThrowsExceptionWhenFileNameIsNull() throws ZipException, IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    InputStream inputStream = new FileInputStream(TestUtils.getFileFromResources("бореиская.txt"));
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setFileNameInZip(null);

    expectedException.expectMessage("fileNameInZip is null or empty");
    expectedException.expect(ZipException.class);

    zipFile.addStream(inputStream, zipParameters);
  }

  @Test
  public void testAddStreamToZipThrowsExceptionWhenFileNameIsEmpty() throws ZipException, IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    InputStream inputStream = new FileInputStream(TestUtils.getFileFromResources("бореиская.txt"));
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setFileNameInZip("");

    expectedException.expectMessage("fileNameInZip is null or empty");
    expectedException.expect(ZipException.class);

    zipFile.addStream(inputStream, zipParameters);
  }

  @Test
  public void testAddStreamToZipWithoutEncryptionForNewZipAddsSuccessfully() throws ZipException, IOException {
    File fileToAdd = TestUtils.getFileFromResources("бореиская.txt");
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setFileNameInZip(fileToAdd.getName());
    ZipFile zipFile = new ZipFile(generatedZipFile);
    InputStream inputStream = new FileInputStream(fileToAdd);

    zipFile.addStream(inputStream, zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("бореиская.txt"), CompressionMethod.DEFLATE, null, null);
  }

  @Test
  public void testAddStreamToZipWithAesEncryptionForNewZipAddsSuccessfully() throws ZipException, IOException {
    File fileToAdd = TestUtils.getFileFromResources("бореиская.txt");
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    zipParameters.setFileNameInZip(fileToAdd.getName());
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    InputStream inputStream = new FileInputStream(fileToAdd);

    zipFile.addStream(inputStream, zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("бореиская.txt"), CompressionMethod.DEFLATE,
        EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
  }

  @Test
  public void testAddStreamToZipWithoutEncryptionForExistingZipAddsSuccessfully() throws ZipException, IOException {
    File fileToAdd = TestUtils.getFileFromResources("가나다.abc");
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setFileNameInZip(fileToAdd.getName());
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFiles(FILES_TO_ADD);
    InputStream inputStream = new FileInputStream(fileToAdd);

    zipFile.addStream(inputStream, zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 4);
    verifyFileIsOf(generatedZipFile, "가나다.abc", CompressionMethod.DEFLATE, EncryptionMethod.NONE, null);
  }

  @Test
  public void testAddStreamToZipWithAesEncryptionForExistingZipAddsSuccessfully() throws ZipException, IOException {
    File fileToAdd = TestUtils.getFileFromResources("가나다.abc");
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128);
    zipParameters.setFileNameInZip(fileToAdd.getName());
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD);
    InputStream inputStream = new FileInputStream(fileToAdd);

    zipFile.addStream(inputStream, zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 4);
    verifyFileIsOf(generatedZipFile, "가나다.abc", CompressionMethod.DEFLATE, EncryptionMethod.AES,
        AesKeyStrength.KEY_STRENGTH_128);
  }

  private void verifyZipFileContainsFiles(File generatedZipFile, List<String> fileNames,
                                          CompressionMethod compressionMethod, EncryptionMethod encryptionMethod,
                                          AesKeyStrength aesKeyStrength) throws ZipException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    List<FileHeader> fileHeaders = zipFile.getFileHeaders();
    verifyFileHeadersContainsFiles(fileHeaders, fileNames);
    verifyAllFilesAreOf(fileHeaders, compressionMethod, encryptionMethod, aesKeyStrength);
  }

  private void verifyFoldersInZip(List<FileHeader> fileHeaders, File generatedZipFile, char[] password)
      throws IOException {
    verifyFoldersInFileHeaders(fileHeaders);
    verifyFoldersInLocalFileHeaders(generatedZipFile, password);
  }

  private void verifyFoldersInFileHeaders(List<FileHeader> fileHeaders) {
    fileHeaders.stream().filter(FileHeader::isDirectory).forEach(e -> {
      verifyFolderEntryInZip(e);
    });
  }

  private void verifyFoldersInLocalFileHeaders(File generatedZipFile, char[] password) throws IOException {
    try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(generatedZipFile), password)) {

      LocalFileHeader localFileHeader;
      while ((localFileHeader = zipInputStream.getNextEntry()) != null) {
        if (localFileHeader.isDirectory()) {
          verifyFolderEntryInZip(localFileHeader);
        }
      }
    }
  }

  private void verifyFolderEntryInZip(AbstractFileHeader fileHeader) {
    assertThat(fileHeader.getCrc()).isZero();
    assertThat(fileHeader.getCompressedSize()).isZero();
    assertThat(fileHeader.getUncompressedSize()).isZero();
    assertThat(fileHeader.getCompressionMethod()).isEqualTo(CompressionMethod.STORE);
    assertThat(fileHeader.getEncryptionMethod()).isEqualTo(EncryptionMethod.NONE);
  }

  private void verifyAllFilesInZipContainsPath(List<FileHeader> fileHeaders, String pathToBeChecked) {
    fileHeaders.forEach(e -> assertThat(e.getFileName()).startsWith(pathToBeChecked));
  }

  private void verifyAllFilesInZipDoesNotContainPath(List<FileHeader> fileHeaders, String pathToBeChecked) {
    fileHeaders.forEach(e -> assertThat(e.getFileName()).doesNotStartWith(pathToBeChecked));
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
            encryptionMethod == EncryptionMethod.AES, compressionMethod, fileHeader.getUncompressedSize());
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

  private void verifyFileIsOf(File generatedZipFile, String fileName, CompressionMethod compressionMethod,
                              EncryptionMethod encryptionMethod, AesKeyStrength aesKeyStrength) throws ZipException {
    List<FileHeader> fileHeaders = getFileHeaders(generatedZipFile);
    FileHeader fileHeader = getFileHeaderFrom(fileHeaders, fileName);

    if (encryptionMethod == null || encryptionMethod == EncryptionMethod.NONE) {
      assertThat(fileHeader.isEncrypted()).isFalse();
      assertThat(fileHeader.getEncryptionMethod()).isIn(null, EncryptionMethod.NONE);
    } else {
      verifyAllFilesAreOf(singletonList(fileHeader), compressionMethod, encryptionMethod, aesKeyStrength);
    }
  }

  private FileHeader getFileHeaderFrom(List<FileHeader> fileHeaders, String fileName) {
    Optional<FileHeader> fileHeader = fileHeaders.stream().filter(e -> e.getFileName().equals(fileName)).findFirst();
    assertThat(fileHeader).isPresent();
    return fileHeader.get();
  }

  private List<FileHeader> getFileHeaders(File generatedZipFile) throws ZipException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    List<FileHeader> fileHeaders = zipFile.getFileHeaders();

    assertThat(fileHeaders.size()).isNotZero();

    return fileHeaders;
  }

  private void verifyAesExtraDataRecord(AESExtraDataRecord aesExtraDataRecord, AesKeyStrength aesKeyStrength) {
    assertThat(aesExtraDataRecord).isNotNull();
    assertThat(aesExtraDataRecord.getAesKeyStrength()).isEqualTo(aesKeyStrength);
  }

  private CompressionMethod getShouldBeCompressionMethod(boolean isAesEncrypted, CompressionMethod compressionMethod,
                                                         long uncompressedSize) {
    if (isAesEncrypted) {
      return CompressionMethod.AES_INTERNAL_ONLY;
    }

    if (uncompressedSize == 0) {
      return CompressionMethod.STORE;
    }

    return compressionMethod;
  }
}
