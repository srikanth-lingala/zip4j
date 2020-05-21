package net.lingala.zip4j;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.AbstractFileHeader;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.AesVersion;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.testutils.TestUtils;
import net.lingala.zip4j.testutils.ZipFileVerifier;
import net.lingala.zip4j.util.BitUtils;
import net.lingala.zip4j.util.InternalZipConstants;
import net.lingala.zip4j.util.RawIO;
import net.lingala.zip4j.util.ZipVersionUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static net.lingala.zip4j.testutils.HeaderVerifier.verifyLocalFileHeaderUncompressedSize;
import static org.assertj.core.api.Assertions.assertThat;

public class AddFilesToZipIT extends AbstractIT {

  private RawIO rawIO = new RawIO();

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
  public void testAddFileAsStringParameterWithoutZipParameterAddsAsDeflate() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFile(TestUtils.getTestFileFromResources("sample.pdf").getPath());

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("sample.pdf"), CompressionMethod.DEFLATE, null, null);
    verifyZipVersions(zipFile.getFileHeaders().get(0), new ZipParameters());
  }

  @Test
  public void testAddFileAsStringWithZipParametersStoreAndStandardEncryption() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.ZIP_STANDARD, null);
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);

    zipFile.addFile(TestUtils.getTestFileFromResources("sample_text_large.txt").getPath(), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("sample_text_large.txt"), CompressionMethod.STORE,
        EncryptionMethod.ZIP_STANDARD, null);
  }

  @Test
  public void testAddFileAsStringWithZipParametersStoreAndStandardEncryptionAndCharsetCp949() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.ZIP_STANDARD, null);
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);

    zipFile.setCharset(CHARSET_CP_949);
    String koreanFileName = "가나다.abc";
    zipFile.addFile(TestUtils.getTestFileFromResources(koreanFileName).getPath(), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 1, true, CHARSET_CP_949);
    assertThat(zipFile.getFileHeaders().get(0).getFileName()).isEqualTo(koreanFileName);
    verifyZipVersions(zipFile.getFileHeaders().get(0), zipParameters);
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

    zipFile.addFile(TestUtils.getTestFileFromResources("file_PDF_1MB.pdf"), zipParameters);
  }

  @Test
  public void testAddFileWithoutZipParameterAddsAsDeflate() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFile(TestUtils.getTestFileFromResources("file_PDF_1MB.pdf"));

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("file_PDF_1MB.pdf"), CompressionMethod.DEFLATE, null,
        null);
  }

  @Test
  public void testAddFileWithZipParametersStoreAndStandardZip() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.ZIP_STANDARD, null);
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);

    zipFile.addFile(TestUtils.getTestFileFromResources("sample_text_large.txt"), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("sample_text_large.txt"), CompressionMethod.STORE,
        EncryptionMethod.ZIP_STANDARD, null);
  }

  @Test
  public void testAddFileWithZipParametersStoreAndAes128Encryption() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128);
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);

    zipFile.addFile(TestUtils.getTestFileFromResources("sample_text_large.txt"), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("sample_text_large.txt"), CompressionMethod.STORE,
        EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128);
    verifyZipVersions(zipFile.getFileHeaders().get(0), zipParameters);
  }

  @Test
  public void testAddFileWithZipParametersStoreAndAes256Encryption() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);

    zipFile.addFile(TestUtils.getTestFileFromResources("sample_text_large.txt"), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("sample_text_large.txt"), CompressionMethod.STORE,
        EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
  }

  @Test
  public void testAddFileRemovesExistingFileNoEncryption() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFiles(FILES_TO_ADD);

    zipFile.addFile(TestUtils.getTestFileFromResources("sample_text_large.txt"));

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 3);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("sample_text_large.txt"), CompressionMethod.DEFLATE,
        null, null);
  }

  @Test
  public void testAddFileDoesNotOverrideFileIfFlagIsDisabled() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFiles(FILES_TO_ADD);

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
    zipParameters.setOverrideExistingFilesInZip(false);
    zipFile.setPassword(PASSWORD);
    zipFile.addFile(TestUtils.getTestFileFromResources("sample_text_large.txt"), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 3);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("sample_text_large.txt"), CompressionMethod.DEFLATE,
        null, null);
  }

  @Test
  public void testAddFileRemovesExistingFileNoEncryptionSingleFileInZip() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFile(TestUtils.getTestFileFromResources("sample_text_large.txt"));

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    zipFile.addFile(TestUtils.getTestFileFromResources("sample_text_large.txt"), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("sample_text_large.txt"), CompressionMethod.STORE,
        null, null);
  }

  @Test
  public void testAddFileWithDifferentFileNameSetsTheNewFileName() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setFileNameInZip("/data/newfile.txt");

    zipFile.addFile(TestUtils.getTestFileFromResources("sample_text_large.txt"), zipParameters);

    zipFile = new ZipFile(generatedZipFile);
    assertThat(zipFile.getFileHeaders()).hasSize(1);
    assertThat(zipFile.getFileHeader("/data/newfile.txt")).isNotNull();
    assertThat(zipFile.getFileHeader("sample_text_large.txt")).isNull();
    zipFile.extractAll(outputFolder.getPath());
  }

  @Test
  public void testAddFileRemovesExistingFileWithAesEncryption() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    List<File> filesToAdd = new ArrayList<>(FILES_TO_ADD);
    filesToAdd.add(1, TestUtils.getTestFileFromResources("file_PDF_1MB.pdf"));
    zipFile.addFiles(filesToAdd, zipParameters);

    zipFile.addFile(TestUtils.getTestFileFromResources("file_PDF_1MB.pdf"), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, FILES_TO_ADD.size()
        + 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("file_PDF_1MB.pdf"), CompressionMethod.DEFLATE,
        EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
  }

  @Test
  public void testAddFileWithAfterDeflateRemainingBytesTestFile() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    zipParameters.setAesVersion(AesVersion.TWO);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);

    zipFile.addFile(TestUtils.getTestFileFromResources("after_deflate_remaining_bytes.bin"), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("after_deflate_remaining_bytes.bin"),
        CompressionMethod.DEFLATE, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
  }

  @Test
  public void testAddFileProgressMonitorThrowsExceptionWhenPerformingActionInBusyState() throws ZipException {
    expectedException.expectMessage("invalid operation - Zip4j is in busy state");
    expectedException.expect(ZipException.class);

    ZipFile zipFile = new ZipFile(generatedZipFile);
    ProgressMonitor progressMonitor = zipFile.getProgressMonitor();
    progressMonitor.setState(ProgressMonitor.State.BUSY);

    zipFile.addFile(TestUtils.getTestFileFromResources("file_PDF_1MB.pdf"));
  }

  @Test
  public void testAddFileWithProgressMonitor() throws IOException, InterruptedException {
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    ProgressMonitor progressMonitor = zipFile.getProgressMonitor();
    boolean percentBetweenZeroAndHundred = false;
    boolean fileNameSet = false;
    boolean taskNameSet = false;

    zipFile.setRunInThread(true);
    zipFile.addFile(TestUtils.getTestFileFromResources("file_PDF_1MB.pdf"),
        createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256));

    while (!progressMonitor.getState().equals(ProgressMonitor.State.READY)) {
      int percentDone = progressMonitor.getPercentDone();
      String fileName = progressMonitor.getFileName();

      if (percentDone > 0 && percentDone < 100) {
        percentBetweenZeroAndHundred = true;
      }

      if (fileName != null) {
        assertThat(fileName).contains("file_PDF_1MB.pdf");
        fileNameSet = true;
      }

      Thread.sleep(10);

      if (!progressMonitor.getCurrentTask().equals(ProgressMonitor.Task.NONE)) {
        assertThat(progressMonitor.getCurrentTask()).isEqualTo(ProgressMonitor.Task.ADD_ENTRY);
        taskNameSet = true;
      }
    }

    assertThat(progressMonitor.getResult()).isEqualTo(ProgressMonitor.Result.SUCCESS);
    assertThat(progressMonitor.getState().equals(ProgressMonitor.State.READY));
    assertThat(progressMonitor.getException()).isNull();
    assertThat(percentBetweenZeroAndHundred).isTrue();
    assertThat(fileNameSet).isTrue();
    assertThat(taskNameSet).isTrue();

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 1);
  }

  @Test
  public void testAddFilesWithoutParametersWhenZipFileDoesNotExistCreatesSuccessfully() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);

    zipFile.addFiles(asList(
          TestUtils.getTestFileFromResources("file_PDF_1MB.pdf"),
          TestUtils.getTestFileFromResources("zero_byte_file.txt")
    ));

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 2);
    verifyZipFileContainsFiles(generatedZipFile, asList("file_PDF_1MB.pdf", "zero_byte_file.txt"),
        CompressionMethod.DEFLATE, null, null);
  }

  @Test
  public void testAddFilesWhenZipFileDoesNotExistCreatesSuccessfully() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);

    zipFile.addFiles(asList(
        TestUtils.getTestFileFromResources("file_PDF_1MB.pdf"),
        TestUtils.getTestFileFromResources("sample_text1.txt")
    ), new ZipParameters());

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 2);
    verifyZipFileContainsFiles(generatedZipFile, asList("file_PDF_1MB.pdf", "sample_text1.txt"),
        CompressionMethod.DEFLATE, null, null);
  }

  @Test
  public void testAddFilesWithZeroByteFileWithAes128Encryption() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);

    zipFile.addFiles(singletonList(TestUtils.getTestFileFromResources("zero_byte_file.txt")), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("zero_byte_file.txt"),
        CompressionMethod.DEFLATE, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128);
  }

  @Test
  public void testAddFilesWithAes256EncryptionV1() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    zipParameters.setAesVersion(AesVersion.ONE);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);

    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, FILES_TO_ADD.size());
    List<String> fileNames = FILES_TO_ADD.stream().map(File::getName).collect(Collectors.toList());
    verifyZipFileContainsFiles(generatedZipFile, fileNames, CompressionMethod.DEFLATE, EncryptionMethod.AES,
        AesKeyStrength.KEY_STRENGTH_256, AesVersion.ONE);
  }

  @Test
  public void testAddFilesWithAes256EncryptionV2() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);

    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, FILES_TO_ADD.size());
    List<String> fileNames = FILES_TO_ADD.stream().map(File::getName).collect(Collectors.toList());
    verifyZipFileContainsFiles(generatedZipFile, fileNames, CompressionMethod.DEFLATE, EncryptionMethod.AES,
        AesKeyStrength.KEY_STRENGTH_256);
  }

  @Test
  public void testAddFilesWithZipStandardEncryption() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.ZIP_STANDARD, null);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);

    zipFile.addFiles(FILES_TO_ADD, zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, FILES_TO_ADD.size());
    List<String> fileNames = FILES_TO_ADD.stream().map(File::getName).collect(Collectors.toList());
    verifyZipFileContainsFiles(generatedZipFile, fileNames, CompressionMethod.DEFLATE, EncryptionMethod.ZIP_STANDARD,
        null);
  }

  @Test
  public void testAddFilesWhenFilesAlreadyExistsRemovesFiles() throws IOException {
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
  public void testAddFilesToSplitZipThrowsException() throws ZipException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.createSplitZipFile(singletonList(TestUtils.getTestFileFromResources("file_PDF_1MB.pdf")), new ZipParameters(),
        true, InternalZipConstants.MIN_SPLIT_LENGTH);

    expectedException.expect(ZipException.class);
    expectedException.expectMessage("Zip file already exists. " +
        "Zip file format does not allow updating split/spanned files");

    zipFile.addFiles(singletonList(TestUtils.getTestFileFromResources("sample.pdf")));
  }

  @Test
  public void testAddFilesWithDifferentEncryptionType() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(singletonList(TestUtils.getTestFileFromResources("sample.pdf")), zipParameters);

    zipParameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);

    zipFile.addFiles(singletonList(TestUtils.getTestFileFromResources("file_PDF_1MB.pdf")), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 2);
  }

  @Test
  public void testAddFilesWithUtf8Characters() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);

    zipFile.addFiles(asList(
        TestUtils.getTestFileFromResources("sample.pdf"),
        TestUtils.getTestFileFromResources("бореиская.txt"),
        TestUtils.getTestFileFromResources("zero_byte_file.txt"),
        TestUtils.getTestFileFromResources("sample_text1.txt"),
        TestUtils.getTestFileFromResources("가나다.abc")
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
  public void testAddFilesWithProgressMonitor() throws IOException, InterruptedException {
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    ProgressMonitor progressMonitor = zipFile.getProgressMonitor();
    boolean percentBetweenZeroAndHundred = false;
    boolean fileNameSet = false;
    boolean taskNameSet = false;

    List<File> filesToAdd = new ArrayList<>(FILES_TO_ADD);
    filesToAdd.add(TestUtils.getTestFileFromResources("file_PDF_1MB.pdf"));
    zipFile.setRunInThread(true);
    zipFile.addFiles(filesToAdd, createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256));

    while (!progressMonitor.getState().equals(ProgressMonitor.State.READY)) {
      int percentDone = progressMonitor.getPercentDone();
      String fileName = progressMonitor.getFileName();

      if (percentDone > 0 && percentDone < 100) {
        percentBetweenZeroAndHundred = true;
      }

      if (fileName != null) {
        fileNameSet = true;
      }

      Thread.sleep(10);

      if (!progressMonitor.getCurrentTask().equals(ProgressMonitor.Task.NONE)) {
        assertThat(progressMonitor.getCurrentTask()).isEqualTo(ProgressMonitor.Task.ADD_ENTRY);
        taskNameSet = true;
      }
    }

    assertThat(progressMonitor.getResult()).isEqualTo(ProgressMonitor.Result.SUCCESS);
    assertThat(progressMonitor.getState().equals(ProgressMonitor.State.READY));
    assertThat(progressMonitor.getException()).isNull();
    assertThat(percentBetweenZeroAndHundred).isTrue();
    assertThat(fileNameSet).isTrue();
    assertThat(taskNameSet).isTrue();

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 4);
  }

  @Test
  public void testAddFolderWithoutZipParameters() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);

    zipFile.addFolder(TestUtils.getTestFileFromResources(""));

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 13);
    List<FileHeader> fileHeaders = getFileHeaders(generatedZipFile);
    verifyAllFilesInZipContainsPath(fileHeaders, "test-files/");
    verifyFoldersInZip(fileHeaders, generatedZipFile, null);
  }

  @Test
  public void testAddFolderWithStoreAndAes128() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128);
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);

    zipFile.addFolder(TestUtils.getTestFileFromResources(""), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 13);
    List<FileHeader> fileHeaders = getFileHeaders(generatedZipFile);
    verifyAllFilesInZipContainsPath(fileHeaders, "test-files/");
    verifyFoldersInZip(fileHeaders, generatedZipFile, PASSWORD);
  }

  @Test
  public void testAddFolderWithDeflateAndAes256AndWithoutRootFolder() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    zipParameters.setIncludeRootFolder(false);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);

    zipFile.addFolder(TestUtils.getTestFileFromResources(""), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 12);
    List<FileHeader> fileHeaders = getFileHeaders(generatedZipFile);
    verifyAllFilesInZipDoesNotContainPath(fileHeaders, "test-files/");
    verifyFoldersInZip(fileHeaders, generatedZipFile, PASSWORD);
  }

  @Test
  public void testAddFolderWithRootFolderNameInZipAndWithoutRootFolder() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    zipParameters.setIncludeRootFolder(false);
    zipParameters.setRootFolderNameInZip("root_folder_name");
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);

    zipFile.addFolder(TestUtils.getTestFileFromResources(""), zipParameters);

    List<FileHeader> fileHeaders = getFileHeaders(generatedZipFile);
    verifyAllFilesInZipContainsPath(fileHeaders, "root_folder_name/");
    verifyAllFilesInZipDoesNotContainPath(fileHeaders, "root_folder_name/test-files/");
    verifyFoldersInZip(fileHeaders, generatedZipFile, PASSWORD);
  }

  @Test
  public void testAddFolderWithRootFolderNameInZipAndWithRootFolder() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    zipParameters.setIncludeRootFolder(true);
    zipParameters.setRootFolderNameInZip("root_folder_name");
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);

    zipFile.addFolder(TestUtils.getTestFileFromResources(""), zipParameters);

    List<FileHeader> fileHeaders = getFileHeaders(generatedZipFile);
    verifyAllFilesInZipContainsPath(fileHeaders, "root_folder_name/");
    verifyAllFilesInZipContainsPath(fileHeaders, "root_folder_name/test-files/");
    verifyFoldersInZip(fileHeaders, generatedZipFile, PASSWORD);
  }

  @Test
  public void testAddFolderWithProgressMonitor() throws IOException, InterruptedException {
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    ProgressMonitor progressMonitor = zipFile.getProgressMonitor();
    boolean percentBetweenZeroAndHundred = false;
    boolean fileNameSet = false;

    zipFile.setRunInThread(true);
    zipFile.addFolder(TestUtils.getTestFileFromResources(""),
        createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256));

    while (!progressMonitor.getState().equals(ProgressMonitor.State.READY)) {
      int percentDone = progressMonitor.getPercentDone();
      String fileName = progressMonitor.getFileName();

      if (percentDone > 0 && percentDone < 100) {
        percentBetweenZeroAndHundred = true;
      }

      if (fileName != null) {
        fileNameSet = true;
      }

      Thread.sleep(10);
    }

    assertThat(progressMonitor.getResult()).isEqualTo(ProgressMonitor.Result.SUCCESS);
    assertThat(progressMonitor.getState().equals(ProgressMonitor.State.READY));
    assertThat(progressMonitor.getException()).isNull();
    assertThat(percentBetweenZeroAndHundred).isTrue();
    assertThat(fileNameSet).isTrue();
    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 13);
  }

  @Test
  public void testAddFolderWithNotNormalizedPath() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    ZipParameters parameters = new ZipParameters();

    String folderToAddPath = TestUtils.getTestFileFromResources("").getPath()
        + InternalZipConstants.FILE_SEPARATOR + ".."
        + InternalZipConstants.FILE_SEPARATOR
        + TestUtils.getTestFileFromResources("").getName();
    File folderToAdd = new File(folderToAddPath);
    zipFile.addFolder(folderToAdd, parameters);

    File fileToAdd = TestUtils.getTestFileFromResources("file_PDF_1MB.pdf");
    zipFile.addFile(fileToAdd, parameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 13);
  }

  @Test
  public void testAddFolderWithExcludeFileFilter() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    List<File> filesToExclude = Arrays.asList(
        TestUtils.getTestFileFromResources("sample.pdf"),
        TestUtils.getTestFileFromResources("sample_directory/favicon.ico")
    );
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setIncludeRootFolder(false);
    zipParameters.setExcludeFileFilter(filesToExclude::contains);

    zipFile.addFolder(TestUtils.getTestFileFromResources(""), zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 10);
    verifyZipFileDoesNotContainFiles(generatedZipFile, Arrays.asList("sample.pdf", "sample_directory/favicon.ico"));
  }

  @Test
  public void testAddStreamToZipThrowsExceptionWhenFileNameIsNull() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    InputStream inputStream = new FileInputStream(TestUtils.getTestFileFromResources("бореиская.txt"));
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setFileNameInZip(null);

    expectedException.expectMessage("fileNameInZip has to be set in zipParameters when adding stream");
    expectedException.expect(ZipException.class);

    zipFile.addStream(inputStream, zipParameters);
  }

  @Test
  public void testAddStreamToZipThrowsExceptionWhenFileNameIsEmpty() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    InputStream inputStream = new FileInputStream(TestUtils.getTestFileFromResources("бореиская.txt"));
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setFileNameInZip("");

    expectedException.expectMessage("fileNameInZip has to be set in zipParameters when adding stream");
    expectedException.expect(ZipException.class);

    zipFile.addStream(inputStream, zipParameters);
  }

  @Test
  public void testAddStreamToZipWithoutEncryptionForNewZipAddsSuccessfully() throws IOException {
    File fileToAdd = TestUtils.getTestFileFromResources("бореиская.txt");
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setFileNameInZip(fileToAdd.getName());
    ZipFile zipFile = new ZipFile(generatedZipFile);
    InputStream inputStream = new FileInputStream(fileToAdd);

    zipFile.addStream(inputStream, zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("бореиская.txt"), CompressionMethod.DEFLATE, null, null);
  }

  @Test
  public void testAddStreamToWithStoreCompressionAndWithoutEncryption() throws IOException {
    File fileToAdd = TestUtils.getTestFileFromResources("бореиская.txt");
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    zipParameters.setFileNameInZip(fileToAdd.getName());
    ZipFile zipFile = new ZipFile(generatedZipFile);
    InputStream inputStream = new FileInputStream(fileToAdd);

    zipFile.addStream(inputStream, zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("бореиская.txt"), CompressionMethod.STORE, null, null);

    zipFile = new ZipFile(generatedZipFile);
    byte[] generalPurposeBytes = zipFile.getFileHeaders().get(0).getGeneralPurposeFlag();
    // assert that extra data record is not present
    assertThat(BitUtils.isBitSet(generalPurposeBytes[0], 3)).isTrue();
  }

  @Test
  public void testAddStreamToWithStoreCompressionAndZipStandardEncryption() throws IOException {
    File fileToAdd = TestUtils.getTestFileFromResources("sample_text_large.txt");
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.ZIP_STANDARD, null);
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    zipParameters.setFileNameInZip(fileToAdd.getName());
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    InputStream inputStream = new FileInputStream(fileToAdd);

    zipFile.addStream(inputStream, zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("sample_text_large.txt"), CompressionMethod.STORE,
        EncryptionMethod.ZIP_STANDARD, null);

    zipFile = new ZipFile(generatedZipFile, PASSWORD);
    byte[] generalPurposeBytes = zipFile.getFileHeaders().get(0).getGeneralPurposeFlag();
    // assert that extra data record is not present
    assertThat(BitUtils.isBitSet(generalPurposeBytes[0], 3)).isTrue();
  }

  @Test
  public void testAddStreamWithStoreCompressionAndCharset() throws IOException {
    String koreanFileName = "가나다.abc";
    File fileToAdd = TestUtils.getTestFileFromResources(koreanFileName);
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    zipParameters.setFileNameInZip(fileToAdd.getName());
    ZipFile zipFile = new ZipFile(generatedZipFile);
    InputStream inputStream = new FileInputStream(fileToAdd);

    zipFile.setCharset(CHARSET_CP_949);
    zipFile.addStream(inputStream, zipParameters);

    byte[] generalPurposeBytes = zipFile.getFileHeaders().get(0).getGeneralPurposeFlag();
    // assert that extra data record is not present
    assertThat(BitUtils.isBitSet(generalPurposeBytes[1], 3)).isFalse();
    assertThat(zipFile.getFileHeaders().get(0).getFileName()).isEqualTo(koreanFileName);
  }

  @Test
  public void testAddStreamWithStoreCompressionAndDefaultCharset() throws IOException {
    String koreanFileName = "가나다.abc";
    File fileToAdd = TestUtils.getTestFileFromResources(koreanFileName);
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    zipParameters.setFileNameInZip(fileToAdd.getName());
    ZipFile zipFile = new ZipFile(generatedZipFile);
    InputStream inputStream = new FileInputStream(fileToAdd);

    zipFile.addStream(inputStream, zipParameters);

    byte[] generalPurposeBytes = zipFile.getFileHeaders().get(0).getGeneralPurposeFlag();
    // assert that extra data record is not present
    assertThat(BitUtils.isBitSet(generalPurposeBytes[1], 3)).isTrue();
    assertThat(zipFile.getFileHeaders().get(0).getFileName()).isEqualTo(koreanFileName);
  }

  @Test
  public void testAddStreamToZipWithAesEncryptionForNewZipAddsSuccessfully() throws IOException {
    File fileToAdd = TestUtils.getTestFileFromResources("бореиская.txt");
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    zipParameters.setFileNameInZip(fileToAdd.getName());
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    InputStream inputStream = new FileInputStream(fileToAdd);

    zipFile.addStream(inputStream, zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 1);
    verifyZipFileContainsFiles(generatedZipFile, singletonList("бореиская.txt"), CompressionMethod.DEFLATE,
        EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    verifyLocalFileHeaderUncompressedSize(generatedZipFile, "бореиская.txt", 0);
  }

  @Test
  public void testAddStreamToZipWithoutEncryptionForExistingZipAddsSuccessfully() throws IOException {
    File fileToAdd = TestUtils.getTestFileFromResources("가나다.abc");
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setFileNameInZip(fileToAdd.getName());
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFiles(FILES_TO_ADD);
    InputStream inputStream = new FileInputStream(fileToAdd);

    zipFile.addStream(inputStream, zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 4);
    verifyFileIsOf(generatedZipFile, "가나다.abc", CompressionMethod.DEFLATE, EncryptionMethod.NONE, null, null);
  }

  @Test
  public void testAddStreamToZipWithAesEncryptionV2ForExistingZipAddsSuccessfully() throws IOException {
    File fileToAdd = TestUtils.getTestFileFromResources("가나다.abc");
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128);
    zipParameters.setFileNameInZip(fileToAdd.getName());
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD);
    InputStream inputStream = new FileInputStream(fileToAdd);

    zipFile.addStream(inputStream, zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 4);
    verifyFileIsOf(generatedZipFile, "가나다.abc", CompressionMethod.DEFLATE, EncryptionMethod.AES,
        AesKeyStrength.KEY_STRENGTH_128, AesVersion.TWO);
  }

  @Test
  public void testAddStreamToZipWithAesEncryptionV1ForExistingZipAddsSuccessfully() throws IOException {
    File fileToAdd = TestUtils.getTestFileFromResources("가나다.abc");
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    zipParameters.setAesVersion(AesVersion.ONE);
    zipParameters.setFileNameInZip(fileToAdd.getName());
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD);
    InputStream inputStream = new FileInputStream(fileToAdd);

    zipFile.addStream(inputStream, zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 4);
    verifyFileIsOf(generatedZipFile, "가나다.abc", CompressionMethod.DEFLATE, EncryptionMethod.AES,
        AesKeyStrength.KEY_STRENGTH_256, AesVersion.ONE);
  }

  @Test
  public void testAddStreamToZipWithCharsetCp949() throws IOException {
    String koreanFileName = "가나다.abc";
    ZipFile zipFile = new ZipFile(generatedZipFile);
    File fileToAdd = TestUtils.getTestFileFromResources(koreanFileName);
    InputStream inputStream = new FileInputStream(fileToAdd);
    ZipParameters zipParameters = new ZipParameters();

    zipParameters.setFileNameInZip(fileToAdd.getName());
    zipFile.setCharset(CHARSET_CP_949);
    zipFile.addStream(inputStream, zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, 1, true, CHARSET_CP_949);
    assertThat(zipFile.getFileHeaders().get(0).getFileName()).isEqualTo(koreanFileName);
  }

  @Test
  public void testAddStreamToZipWithSameEntryNameRemovesOldEntry() throws IOException {
    File fileToAdd = TestUtils.getTestFileFromResources("sample.pdf");
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFile(fileToAdd);

    try(InputStream inputStream = new FileInputStream(fileToAdd)) {
      ZipParameters zipParameters = new ZipParameters();
      zipParameters.setFileNameInZip("sample.pdf");
      zipFile.addStream(inputStream, zipParameters);
    }

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 1);
  }

  private void verifyZipFileContainsFiles(File generatedZipFile, List<String> fileNames,
                                          CompressionMethod compressionMethod, EncryptionMethod encryptionMethod,
                                          AesKeyStrength aesKeyStrength) throws ZipException {
    verifyZipFileContainsFiles(generatedZipFile, fileNames, compressionMethod, encryptionMethod, aesKeyStrength,
        AesVersion.TWO);
  }

  private void verifyZipFileContainsFiles(File generatedZipFile, List<String> fileNames,
                                          CompressionMethod compressionMethod, EncryptionMethod encryptionMethod,
                                          AesKeyStrength aesKeyStrength, AesVersion aesVersion) throws ZipException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    List<FileHeader> fileHeaders = zipFile.getFileHeaders();
    verifyFileHeadersContainsFiles(fileHeaders, fileNames);
    verifyAllFilesAreOf(fileHeaders, compressionMethod, encryptionMethod, aesKeyStrength, aesVersion);
  }

  private void verifyZipFileDoesNotContainFiles(File generatedZipFile, List<String> fileNamesNotInZip) throws ZipException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    for (FileHeader fileHeader : zipFile.getFileHeaders()) {
      for (String fileNameNotInZip : fileNamesNotInZip) {
        assertThat(fileHeader.getFileName())
            .withFailMessage("Expected file " + fileNameNotInZip + " to not be present in zip file")
            .isNotEqualTo(fileNameNotInZip);
      }
    }
  }

  private void verifyFoldersInZip(List<FileHeader> fileHeaders, File generatedZipFile, char[] password)
      throws IOException {
    verifyFoldersInFileHeaders(fileHeaders);
    verifyFoldersInLocalFileHeaders(generatedZipFile, password);
  }

  private void verifyFoldersInFileHeaders(List<FileHeader> fileHeaders) {
    fileHeaders.stream().filter(FileHeader::isDirectory).forEach(this::verifyFolderEntryInZip);
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
                                   EncryptionMethod encryptionMethod, AesKeyStrength aesKeyStrength,
                                   AesVersion aesVersion) {
    for (FileHeader fileHeader : fileHeaders) {
      if (fileHeader.isDirectory()) {
        assertThat(fileHeader.getCompressionMethod()).isEqualTo(CompressionMethod.STORE);
        assertThat(fileHeader.isEncrypted()).isFalse();
        assertThat(fileHeader.getAesExtraDataRecord()).isNull();
        assertThat(fileHeader.getCrc()).isZero();
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
          verifyAesExtraDataRecord(fileHeader.getAesExtraDataRecord(), aesKeyStrength, aesVersion);

          if (fileHeader.getAesExtraDataRecord().getAesVersion().equals(AesVersion.TWO)) {
            assertThat(fileHeader.getCrc()).isZero();
          } else {
            if (fileHeader.getCompressedSize() != 0) {
              assertThat(fileHeader.getCrc()).isNotZero();
            }
          }
        } else {
          assertThat(fileHeader.getAesExtraDataRecord()).isNull();
        }
      }
    }
  }

  private void verifyFileIsOf(File generatedZipFile, String fileName, CompressionMethod compressionMethod,
                              EncryptionMethod encryptionMethod, AesKeyStrength aesKeyStrength, AesVersion aesVersion)
      throws ZipException {

    List<FileHeader> fileHeaders = getFileHeaders(generatedZipFile);
    FileHeader fileHeader = getFileHeaderFrom(fileHeaders, fileName);

    if (encryptionMethod == null || encryptionMethod == EncryptionMethod.NONE) {
      assertThat(fileHeader.isEncrypted()).isFalse();
      assertThat(fileHeader.getEncryptionMethod()).isIn(null, EncryptionMethod.NONE);
    } else {
      verifyAllFilesAreOf(singletonList(fileHeader), compressionMethod, encryptionMethod, aesKeyStrength, aesVersion);
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

  private void verifyAesExtraDataRecord(AESExtraDataRecord aesExtraDataRecord, AesKeyStrength aesKeyStrength,
                                        AesVersion aesVersion) {
    assertThat(aesExtraDataRecord).isNotNull();
    assertThat(aesExtraDataRecord.getAesKeyStrength()).isEqualTo(aesKeyStrength);
    assertThat(aesExtraDataRecord.getAesVersion()).isEqualTo(aesVersion);
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

  private void verifyZipVersions(FileHeader fileHeader, ZipParameters zipParameters) {
    int versionMadeBy = ZipVersionUtils.determineVersionMadeBy(zipParameters, rawIO);
    int versionNeededToExtract = ZipVersionUtils.determineVersionNeededToExtract(zipParameters).getCode();

    assertThat(fileHeader.getVersionMadeBy()).isEqualTo(versionMadeBy);
    assertThat(fileHeader.getVersionNeededToExtract()).isEqualTo(versionNeededToExtract);
  }
}
