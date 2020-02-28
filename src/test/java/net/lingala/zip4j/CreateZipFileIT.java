package net.lingala.zip4j;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.testutils.TestUtils;
import net.lingala.zip4j.testutils.ZipFileVerifier;
import net.lingala.zip4j.util.FileUtils;
import net.lingala.zip4j.util.InternalZipConstants;
import org.assertj.core.data.Offset;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static net.lingala.zip4j.testutils.TestUtils.getTestFileFromResources;
import static net.lingala.zip4j.testutils.ZipFileVerifier.verifyZipFileByExtractingAllFiles;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Contains Integration tests for create operations of ZipFile
 */
public class CreateZipFileIT extends AbstractIT {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testCreateSplitZipFileNotSplitArchiveWithZipNameAsString() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile.getPath());
    zipFile.createSplitZipFile(FILES_TO_ADD, new ZipParameters(), false, -1);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, FILES_TO_ADD.size());
  }

  @Test
  public void testCreateSplitZipFileNotSplitArchiveWithZipNameAsStringAndCharsetCp949() throws IOException {
    String koreanFileName = "가나다.abc";
    ZipFile zipFile = new ZipFile(generatedZipFile.getPath());
    List<File> filesToAdd = new ArrayList<>();
    filesToAdd.add(getTestFileFromResources(koreanFileName));

    zipFile.setCharset(CHARSET_CP_949);
    zipFile.createSplitZipFile(filesToAdd, new ZipParameters(), false, -1);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, filesToAdd.size(), true, CHARSET_CP_949);
    assertThat(zipFile.getFileHeaders().get(0).getFileName()).isEqualTo(koreanFileName);
  }

  @Test
  public void testCreateSplitZipFileNotSplitArchiveWithZipNameAsStringWithAESEncryption256() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES,
        AesKeyStrength.KEY_STRENGTH_256);
    ZipFile zipFile = new ZipFile(generatedZipFile.getPath(), PASSWORD);
    zipFile.createSplitZipFile(FILES_TO_ADD, zipParameters, false, -1);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, FILES_TO_ADD.size());
    verifyFileHeadersEncrypted(zipFile.getFileHeaders(), EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256,
        CompressionMethod.DEFLATE);
  }

  @Test
  public void testCreateSplitZipFileNotSplitArchiveWithZipNameAsStringWithAESEncryption128() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES,
        AesKeyStrength.KEY_STRENGTH_128);
    ZipFile zipFile = new ZipFile(generatedZipFile.getPath(), PASSWORD);
    zipFile.createSplitZipFile(FILES_TO_ADD, zipParameters, false, -1);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, FILES_TO_ADD.size());
    verifyFileHeadersEncrypted(zipFile.getFileHeaders(), EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128,
        CompressionMethod.DEFLATE);
  }

  @Test
  public void testCreateSplitZipFileNotSplitArchiveWithFile() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.createSplitZipFile(FILES_TO_ADD, new ZipParameters(), false, -1);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, FILES_TO_ADD.size());
  }

  @Test
  public void testCreateSplitZipFileNotSplitArchiveWithFileAndWithAESEncryption256() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES,
        AesKeyStrength.KEY_STRENGTH_256);
    ZipFile zipFile = new ZipFile(generatedZipFile.getPath(), PASSWORD);
    zipFile.createSplitZipFile(FILES_TO_ADD, zipParameters, false, -1);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, FILES_TO_ADD.size());
    verifyFileHeadersEncrypted(zipFile.getFileHeaders(), EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256,
        CompressionMethod.DEFLATE);
  }

  @Test
  public void testCreateSplitZipFileNotSplitArchiveWithFileAndWithAESEncryption128() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES,
        AesKeyStrength.KEY_STRENGTH_128);
    ZipFile zipFile = new ZipFile(generatedZipFile.getPath(), PASSWORD);
    zipFile.createSplitZipFile(FILES_TO_ADD, zipParameters, false, -1);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, FILES_TO_ADD.size());
    verifyFileHeadersEncrypted(zipFile.getFileHeaders(), EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128,
        CompressionMethod.DEFLATE);
  }

  @Test
  public void testCreateSplitZipFileNotSplitArchiveWithFileAndWithStandardEncryption() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.ZIP_STANDARD,
        AesKeyStrength.KEY_STRENGTH_128);
    ZipFile zipFile = new ZipFile(generatedZipFile.getPath(), PASSWORD);
    zipFile.createSplitZipFile(FILES_TO_ADD, zipParameters, false, -1);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, FILES_TO_ADD.size());
    verifyFileHeadersEncrypted(zipFile.getFileHeaders(), EncryptionMethod.ZIP_STANDARD, AesKeyStrength.KEY_STRENGTH_128,
        CompressionMethod.DEFLATE);
  }

  @Test
  public void testCreateSplitZipFileThrowsExceptionWhenSplitSizeLessThanMinimumAllowed() throws ZipException {
    expectedException.expectMessage("split length less than minimum allowed split length of "
        + InternalZipConstants.MIN_SPLIT_LENGTH + " Bytes");
    expectedException.expect(ZipException.class);

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.createSplitZipFile(FILES_TO_ADD, new ZipParameters(), true, InternalZipConstants.MIN_SPLIT_LENGTH - 1);
  }

  @Test
  public void testCreateSplitZipFileStoreAndWithoutEncryption() throws IOException {
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.createSplitZipFile(FILES_TO_ADD, zipParameters, true, InternalZipConstants.MIN_SPLIT_LENGTH);

    verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, FILES_TO_ADD.size());
    verifySplitZip(generatedZipFile, 2, InternalZipConstants.MIN_SPLIT_LENGTH);
  }

  @Test
  public void testCreateSplitZipFileStoreAndStandardZipEncryption() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.ZIP_STANDARD, null);
    zipParameters.setCompressionMethod(CompressionMethod.STORE);

    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.createSplitZipFile(FILES_TO_ADD, zipParameters, true, InternalZipConstants.MIN_SPLIT_LENGTH);

    verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, FILES_TO_ADD.size());
    verifySplitZip(generatedZipFile, 2, InternalZipConstants.MIN_SPLIT_LENGTH);
  }

  @Test
  public void testCreateSplitZipFileStoreAndWithAesEncryptionKeyStrength256() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES,
        AesKeyStrength.KEY_STRENGTH_256);
    zipParameters.setCompressionMethod(CompressionMethod.STORE);

    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.createSplitZipFile(FILES_TO_ADD, zipParameters, true, InternalZipConstants.MIN_SPLIT_LENGTH);

    verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, FILES_TO_ADD.size());
    verifySplitZip(generatedZipFile, 2, InternalZipConstants.MIN_SPLIT_LENGTH);
  }

  @Test
  public void testCreateSplitZipFileStoreAndWithAesEncryptionKeyStrength128() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES,
        AesKeyStrength.KEY_STRENGTH_128);
    zipParameters.setCompressionMethod(CompressionMethod.STORE);

    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    List<File> filesToAdd = new ArrayList<>(FILES_TO_ADD);
    filesToAdd.add(TestUtils.getTestFileFromResources("file_PDF_1MB.pdf"));
    zipFile.createSplitZipFile(filesToAdd, zipParameters, true, InternalZipConstants.MIN_SPLIT_LENGTH);

    verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, FILES_TO_ADD.size() + 1);
    verifySplitZip(generatedZipFile, 18, InternalZipConstants.MIN_SPLIT_LENGTH);
  }

  @Test
  public void testCreateSplitZipFileDeflateAndWithoutEncryption() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    List<File> filesToAdd = new ArrayList<>(FILES_TO_ADD);
    filesToAdd.add(TestUtils.getTestFileFromResources("file_PDF_1MB.pdf"));

    zipFile.createSplitZipFile(filesToAdd, new ZipParameters(), true, 716800);

    verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, FILES_TO_ADD.size() + 1);
    verifySplitZip(generatedZipFile, 2, InternalZipConstants.MIN_SPLIT_LENGTH);
  }

  @Test
  public void testCreateSplitZipFileDeflateAndStandardZipEncryption() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.ZIP_STANDARD, null);
    zipParameters.setCompressionMethod(CompressionMethod.DEFLATE);

    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    List<File> filesToAdd = new ArrayList<>(FILES_TO_ADD);
    filesToAdd.add(TestUtils.getTestFileFromResources("file_PDF_1MB.pdf"));
    zipFile.createSplitZipFile(filesToAdd, zipParameters, true, 512000);

    verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, FILES_TO_ADD.size() + 1);
    verifySplitZip(generatedZipFile, 2, InternalZipConstants.MIN_SPLIT_LENGTH);
  }

  @Test
  public void testCreateSplitZipFileDeflateAndWithAesEncryptionKeyStrength256() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES,
        AesKeyStrength.KEY_STRENGTH_256);

    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    List<File> filesToAdd = new ArrayList<>(FILES_TO_ADD);
    filesToAdd.add(TestUtils.getTestFileFromResources("file_PDF_1MB.pdf"));
    zipFile.createSplitZipFile(filesToAdd, zipParameters, true, InternalZipConstants.MIN_SPLIT_LENGTH);

    verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, FILES_TO_ADD.size() + 1);
    verifySplitZip(generatedZipFile, 15, InternalZipConstants.MIN_SPLIT_LENGTH);
  }

  @Test
  public void testCreateSplitZipFileDeflateAndWithAesEncryptionKeyStrength128() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES,
        AesKeyStrength.KEY_STRENGTH_128);

    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    List<File> filesToAdd = new ArrayList<>(FILES_TO_ADD);
    filesToAdd.add(TestUtils.getTestFileFromResources("file_PDF_1MB.pdf"));
    zipFile.createSplitZipFile(filesToAdd, zipParameters, true, InternalZipConstants.MIN_SPLIT_LENGTH + 2000);

    verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, FILES_TO_ADD.size() + 1);
    verifySplitZip(generatedZipFile, 15, InternalZipConstants.MIN_SPLIT_LENGTH + 2000);
  }

  @Test
  public void testCreateZipFileWithSetPasswordSetter() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128);

    ZipFile zipFile = new ZipFile(generatedZipFile, "WRONG_PASSWORD".toCharArray());
    zipFile.setPassword(PASSWORD);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, FILES_TO_ADD.size());
  }

  @Test
  public void testAddFileWithFileEntryCommentAndUtf8Charset() throws IOException {
    testCreateZipFileWithFileEntryComment("FILE_COMMET_", StandardCharsets.UTF_8);
  }

  @Test
  public void testAddFileWithFileEntryCommentAndNullCharsetUsesUtf8() throws IOException {
    testCreateZipFileWithFileEntryComment("FILE_COMMET_", null);
  }

  @Test
  public void testAddFileWithFileEntryCommentAndGBKCharset() throws IOException {
    testCreateZipFileWithFileEntryComment("测试中文_", Charset.forName("GBK"));
  }

  @Test
  public void testAddingSameFileMultipleTimesResultsInOnlyOneFileInZip() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setFileNameInZip("renamed-file.pdf");

    zipFile.addFile(getTestFileFromResources("sample.pdf"), zipParameters);
    zipFile.addFile(getTestFileFromResources("sample.pdf"), zipParameters);
    zipFile.addFile(getTestFileFromResources("sample.pdf"), zipParameters);

    assertThat(zipFile.getFileHeaders()).hasSize(1);
    assertThat(zipFile.getFileHeaders().get(0).getFileName()).isEqualTo("renamed-file.pdf");

    verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, 1, false);
  }

  @Test
  public void testAddSymlinkWithLinkOnly() throws IOException {
    File targetFile = getTestFileFromResources("sample.pdf");
    File symlink = createSymlink(targetFile, temporaryFolder.getRoot());
    ZipFile zipFile = new ZipFile(generatedZipFile);

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setSymbolicLinkAction(ZipParameters.SymbolicLinkAction.INCLUDE_LINK_ONLY);

    zipFile.addFile(symlink, zipParameters);

    assertThat(zipFile.getFileHeaders()).hasSize(1);
    assertThat(zipFile.getFileHeaders().get(0).getFileName()).isEqualTo(symlink.getName());
    verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, 1, false);

    verifyGeneratedSymlink(symlink, targetFile);
  }

  @Test
  public void testAddSymlinkWithLinkedFileOnly() throws IOException {
    File targetFile = getTestFileFromResources("sample.pdf");
    File symlink = createSymlink(targetFile, temporaryFolder.getRoot());
    ZipFile zipFile = new ZipFile(generatedZipFile);

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setSymbolicLinkAction(ZipParameters.SymbolicLinkAction.INCLUDE_LINKED_FILE_ONLY);

    zipFile.addFile(symlink, zipParameters);

    assertThat(zipFile.getFileHeaders()).hasSize(1);
    assertThat(zipFile.getFileHeaders().get(0).getFileName()).isEqualTo(targetFile.getName());
    verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 1);
  }

  @Test
  public void testAddSymlinkWithLinkAndLinkedFile() throws IOException {
    File targetFile = getTestFileFromResources("sample.pdf");
    File symlink = createSymlink(targetFile, temporaryFolder.getRoot());
    ZipFile zipFile = new ZipFile(generatedZipFile);

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setSymbolicLinkAction(ZipParameters.SymbolicLinkAction.INCLUDE_LINK_AND_LINKED_FILE);

    zipFile.addFile(symlink, zipParameters);

    List<FileHeader> fileHeaders = zipFile.getFileHeaders();
    assertThat(fileHeaders).hasSize(2);
    assertThat(fileHeaders.get(0).getFileName()).isEqualTo(symlink.getName());
    assertThat(fileHeaders.get(1).getFileName()).isEqualTo(targetFile.getName());
    verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, 2, false);

    verifyGeneratedSymlink(symlink, targetFile);
    File generatedTargetFile = Paths.get(outputFolder.getAbsolutePath(), targetFile.getName()).toFile();
    ZipFileVerifier.verifyFileCrc(targetFile, generatedTargetFile);
  }

  @Test
  public void testAddSymlinksInAFolderWithLinkOnly() throws IOException {
    File testFolder = createTestFolderWithSymlinks();
    ZipFile zipFile = new ZipFile(generatedZipFile);
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setSymbolicLinkAction(ZipParameters.SymbolicLinkAction.INCLUDE_LINK_ONLY);

    zipFile.addFolder(testFolder, zipParameters);

    verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, 6, false);
    verifyFileNamesInZip(zipFile,
        "test-folder/",
        "test-folder/symlink.link",
        "test-folder/sub-folder1/",
        "test-folder/sub-folder1/symlink.link",
        "test-folder/sub-folder2/",
        "test-folder/sub-folder2/symlink.link");
  }

  @Test
  public void testAddSymlinksInAFolderWithLinkedFilesOnly() throws IOException {
    File testFolder = createTestFolderWithSymlinks();
    ZipFile zipFile = new ZipFile(generatedZipFile);
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setSymbolicLinkAction(ZipParameters.SymbolicLinkAction.INCLUDE_LINKED_FILE_ONLY);

    zipFile.addFolder(testFolder, zipParameters);

    verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, 6, false);
    verifyFileNamesInZip(zipFile,
        "test-folder/",
        "test-folder/sample.pdf",
        "test-folder/sub-folder1/",
        "test-folder/sub-folder1/file_PDF_1MB.pdf",
        "test-folder/sub-folder2/",
        "test-folder/sub-folder2/sample_text_large.txt");
  }

  @Test
  public void testAddSymlinksInAFolderWithLinkAndLinkedFilesOnly() throws IOException {
    File testFolder = createTestFolderWithSymlinks();
    ZipFile zipFile = new ZipFile(generatedZipFile);
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setSymbolicLinkAction(ZipParameters.SymbolicLinkAction.INCLUDE_LINK_AND_LINKED_FILE);

    zipFile.addFolder(testFolder, zipParameters);

    verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, 9, false);
    verifyFileNamesInZip(zipFile,
        "test-folder/",
        "test-folder/symlink.link",
        "test-folder/sample.pdf",
        "test-folder/sub-folder1/",
        "test-folder/sub-folder1/symlink.link",
        "test-folder/sub-folder1/file_PDF_1MB.pdf",
        "test-folder/sub-folder2/",
        "test-folder/sub-folder2/sample_text_large.txt",
        "test-folder/sub-folder2/symlink.link");
  }

  @Test
  public void testAddSymlinksInAFolderWithLinkAndLinkedFilesOnlyWithoutRootFolder() throws IOException {
    File testFolder = createTestFolderWithSymlinks();
    ZipFile zipFile = new ZipFile(generatedZipFile);
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setIncludeRootFolder(false);
    zipParameters.setSymbolicLinkAction(ZipParameters.SymbolicLinkAction.INCLUDE_LINK_AND_LINKED_FILE);

    zipFile.addFolder(testFolder, zipParameters);

    verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, 8, false);
    verifyFileNamesInZip(zipFile,
        "symlink.link",
        "sample.pdf",
        "sub-folder1/",
        "sub-folder1/symlink.link",
        "sub-folder1/file_PDF_1MB.pdf",
        "sub-folder2/",
        "sub-folder2/sample_text_large.txt",
        "sub-folder2/symlink.link");
  }

  private void testCreateZipFileWithFileEntryComment(String fileCommentPrefix, Charset charset) throws IOException {
    ZipParameters zipParameters = new ZipParameters();
    ZipFile zipFile = initializeZipFileWithCharset(charset);

    for (int i = 0; i < FILES_TO_ADD.size(); i++) {
      if (i == 0) {
        zipParameters.setFileComment(fileCommentPrefix + i);
      } else {
        zipParameters.setFileComment(null);
      }
      zipFile.addFile(FILES_TO_ADD.get(i), zipParameters);
    }

    verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, FILES_TO_ADD.size());
    verifyFileEntryComment(fileCommentPrefix, charset);
  }

  private void verifySplitZip(File zipFile, int numberOfExpectedSplitFiles, long splitLength) throws ZipException {
    assertNumberOfSplitFile(zipFile, numberOfExpectedSplitFiles);
    assertSplitFileSizes(zipFile, numberOfExpectedSplitFiles, splitLength);

  }

  private void assertSplitFileSizes(File zipFile, int numberOfExpectedSplitFiles, long splitLength) {
    for (int i = 0; i < numberOfExpectedSplitFiles - 2; i++) {
      String zipExtension = ".z0";
      if (i >= 9) {
        zipExtension = ".z";
      }

      String fileNameWithoutExtension = zipFile.getPath().substring(0, zipFile.getPath().lastIndexOf(".zip"));
      File splitZip = new File(fileNameWithoutExtension + zipExtension + (i + 1));
      assertThat(splitZip).exists();

      assertThat(splitZip.length()).isCloseTo(splitLength, Offset.offset(1000L));
    }
  }

  private void assertNumberOfSplitFile(File zipFile, int numberOfExpectedSplitFiles) throws ZipException {
    File[] allSplitFiles = getAllSplitZipFilesInFolder(zipFile.getParentFile(),
        FileUtils.getZipFileNameWithoutExtension(zipFile.getName()));
    assertThat(allSplitFiles.length).as("Number of split files").isEqualTo(numberOfExpectedSplitFiles);
  }

  private void verifyFileHeadersEncrypted(List<FileHeader> fileHeaders, EncryptionMethod encryptionMethod,
                                          AesKeyStrength aesKeyStrength, CompressionMethod compressionMethod) {
    for (FileHeader fileHeader : fileHeaders) {
      if (fileHeader.isDirectory()) {
        assertThat(fileHeader.isEncrypted()).isFalse();
        assertThat(fileHeader.getAesExtraDataRecord()).isNull();
      } else {
        assertThat(fileHeader.isEncrypted()).isTrue();
        assertThat(fileHeader.getEncryptionMethod()).isEqualTo(encryptionMethod);

        if (encryptionMethod == EncryptionMethod.AES) {
          verifyAesExtraDataRecord(fileHeader.getAesExtraDataRecord(), compressionMethod, aesKeyStrength);
        } else {
          assertThat(fileHeader.getAesExtraDataRecord()).isNull();
        }
      }
    }
  }

  private void verifyAesExtraDataRecord(AESExtraDataRecord aesExtraDataRecord, CompressionMethod compressionMethod,
                                        AesKeyStrength aesKeyStrength) {
    assertThat(aesExtraDataRecord).isNotNull();
    assertThat(aesExtraDataRecord.getCompressionMethod()).isEqualTo(compressionMethod);
    assertThat(aesExtraDataRecord.getAesKeyStrength()).isEqualTo(aesKeyStrength);
  }

  private File[] getAllSplitZipFilesInFolder(File folder, String fileNameWithoutExtension) {
    FilenameFilter filenameFilter = (dir, name) -> name.contains(fileNameWithoutExtension + ".");
    return folder.listFiles(filenameFilter);
  }

  private void verifyFileEntryComment(String commentPrefix, Charset charset) throws IOException {
    ZipFile zipFile = initializeZipFileWithCharset(charset);
    List<FileHeader> fileHeaders = zipFile.getFileHeaders();
    for (int i = 0; i < fileHeaders.size(); i++) {
      FileHeader fileHeader = fileHeaders.get(i);
      if (i == 0) {
        assertThat(fileHeader.getFileComment()).isEqualTo(commentPrefix + i);
      } else {
        assertThat(fileHeader.getFileComment()).isNull();
      }
    }
  }

  private ZipFile initializeZipFileWithCharset(Charset charset) {
    ZipFile zipFile = new ZipFile(generatedZipFile);

    if (charset != null) {
      zipFile.setCharset(charset);
    }

    return zipFile;
  }

  private void verifyGeneratedSymlink(File actualSymlink, File targetFile) throws IOException {
    File extractedSymlink = Paths.get(outputFolder.getAbsolutePath(), actualSymlink.getName()).toFile();
    assertThat(Files.isSymbolicLink(extractedSymlink.toPath())).isTrue();
    assertThat(actualSymlink.length()).isEqualTo(extractedSymlink.length());
    File generatedTarget = Files.readSymbolicLink(extractedSymlink.toPath()).toFile();
    assertThat(generatedTarget).isEqualTo(targetFile);
  }

  private File createSymlink(File targetFile, File rootFolder) throws IOException {
    Path link = Paths.get(rootFolder.getAbsolutePath(), "symlink.link");
    Files.createSymbolicLink(link, targetFile.toPath());
    return link.toFile();
  }

  private File createTestFolderWithSymlinks() throws IOException {
    Path testFolder = temporaryFolder.newFolder("test-folder").toPath();
    Path subFolder1 = Files.createDirectory(Paths.get(testFolder.toString(), "sub-folder1"));
    Path subFolder2 = Files.createDirectory(Paths.get(testFolder.toString(), "sub-folder2"));

    createSymlink(getTestFileFromResources("sample.pdf"), testFolder.toFile());
    createSymlink(getTestFileFromResources("file_PDF_1MB.pdf"), subFolder1.toFile());
    createSymlink(getTestFileFromResources("sample_text_large.txt"), subFolder2.toFile());

    return testFolder.toFile();
  }

  private void verifyFileNamesInZip(ZipFile zipFile, String... fileNames) throws ZipException {
    List<FileHeader> fileHeaders = zipFile.getFileHeaders();

    for (String fileName : fileNames) {
      boolean entryFound = false;
      for (FileHeader fileHeader : fileHeaders) {
        if (fileHeader.getFileName().equals(fileName)) {
          entryFound = true;
          break;
        }
      }

      if (!entryFound) {
        fail("Could not find entry: " + fileName + " in zip");
      }
    }
  }

}
