package net.lingala.zip4j;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.UnzipParameters;
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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.lingala.zip4j.testutils.TestUtils.getFileNamesOfFiles;
import static net.lingala.zip4j.testutils.TestUtils.getTestFileFromResources;
import static net.lingala.zip4j.testutils.ZipFileVerifier.verifyZipFileByExtractingAllFiles;
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
  public void testExtractAllSkipsSymlinksWhenSymlinkExtractionSetToFalse() throws IOException {
    ZipFile zipFile = createZipFileWithASymlink(createSymlink());

    zipFile.extractAll(outputFolder.getPath(), buildUnzipParameters(false));

    ZipFileVerifier.verifyFolderContentsSameAsSourceFiles(outputFolder);
    verifyNumberOfFilesInOutputFolder(outputFolder, 4);
  }

  @Test
  public void testExtractAllExtractsSymlinksWhenSymlinkExtractionSetToTrue() throws IOException {
    ZipFile zipFile = createZipFileWithASymlink(createSymlink());

    zipFile.extractAll(outputFolder.getPath(), buildUnzipParameters(true));

    verifyNumberOfFilesInOutputFolder(outputFolder, 5);
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
    ZipFileVerifier.verifyFileContent(getTestFileFromResources("sample_text_large.txt"), outputFiles[0]);
  }

  @Test
  public void testExtractFileWithFileHeaderWithAes128AndInDirectory() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFolder(getTestFileFromResources(""), zipParameters);

    FileHeader fileHeader = zipFile.getFileHeader("test-files/öüäöäö/asöäööl");
    zipFile.extractFile(fileHeader, outputFolder.getPath());

    File outputFile = getFileWithNameFrom(outputFolder, "asöäööl");
    ZipFileVerifier.verifyFileContent(getTestFileFromResources("öüäöäö/asöäööl"), outputFile);
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
    ZipFileVerifier.verifyFileContent(getTestFileFromResources("sample_text_large.txt"), outputFile);
  }

  @Test
  public void testExtractFileWithFileHeaderDoesNotExtractSymlinkWhenSymlinkExtractionIsNotSet() throws IOException {
    File symlink = createSymlink();
    ZipFile zipFile = createZipFileWithASymlink(symlink);

    FileHeader fileHeader = zipFile.getFileHeader(symlink.getName());
    zipFile.extractFile(fileHeader, outputFolder.getPath(), buildUnzipParameters(false));

    assertThat(outputFolder.listFiles()).isNull();
  }

  @Test
  public void testExtractFileWithFileHeaderExtractsSymlinkWhenSymlinkExtractionIsSet() throws IOException {
    File symlink = createSymlink();
    ZipFile zipFile = createZipFileWithASymlink(symlink);

    FileHeader fileHeader = zipFile.getFileHeader(symlink.getName());
    zipFile.extractFile(fileHeader, outputFolder.getPath(), buildUnzipParameters(true));

    verifyNumberOfFilesInOutputFolder(outputFolder, 1);
    assertThat(outputFolder.listFiles()[0].getName()).isEqualTo(symlink.getName());
  }

  @Test
  public void testExtractFileWithFileHeaderAndNewFileDoesNotExtractSymlinkWhenSymlinkExtractionIsNotSet()
      throws IOException {
    File symlink = createSymlink();
    ZipFile zipFile = createZipFileWithASymlink(symlink);

    FileHeader fileHeader = zipFile.getFileHeader(symlink.getName());
    zipFile.extractFile(fileHeader, outputFolder.getPath(), "newFileName", buildUnzipParameters(false));

    assertThat(outputFolder.listFiles()).isNull();
  }

  @Test
  public void testExtractFileWithFileHeaderAndNewFileNameExtractsSymlinkWhenSymlinkExtractionIsSet()
      throws IOException {
    File symlink = createSymlink();
    ZipFile zipFile = createZipFileWithASymlink(symlink);

    FileHeader fileHeader = zipFile.getFileHeader(symlink.getName());
    String newFileName = "newName.link";
    zipFile.extractFile(fileHeader, outputFolder.getPath(), newFileName, buildUnzipParameters(true));

    verifyNumberOfFilesInOutputFolder(outputFolder, 1);
    assertThat(outputFolder.listFiles()[0].getName()).isEqualTo(newFileName);
  }

  @Test
  public void testExtractFileWithFileNameThrowsExceptionWhenFileNotFound() throws ZipException {
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFiles(FILES_TO_ADD);

    try {
      zipFile.extractFile("NOT_EXISTING", outputFolder.getPath());
      fail("Should throw an exception");
    } catch (ZipException e) {
      assertThat(e).isNotNull();
      assertThat(e.getType()).isEqualTo(ZipException.Type.FILE_NOT_FOUND);
      assertThat(e.getMessage()).isEqualTo("No file found with name NOT_EXISTING in zip file");
    }

  }

  @Test
  public void testExtractFileWithFileNameWithZipStandardEncryption() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.ZIP_STANDARD, null);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFolder(getTestFileFromResources(""), zipParameters);

    zipFile.extractFile("test-files/sample_directory/favicon.ico", outputFolder.getPath());

    File outputFile = getFileWithNameFrom(outputFolder, "favicon.ico");
    ZipFileVerifier.verifyFileContent(getTestFileFromResources("sample_directory/favicon.ico"), outputFile);
  }

  @Test
  public void testExtractFileWithFileNameWithZipStandardEncryptionAndNewFileName() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.ZIP_STANDARD, null);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFolder(getTestFileFromResources(""), zipParameters);

    String newFileName = "newFileName";
    zipFile.extractFile("test-files/sample_directory/favicon.ico", outputFolder.getPath(), newFileName);

    File outputFile = getFileWithNameFrom(outputFolder, newFileName);
    ZipFileVerifier.verifyFileContent(getTestFileFromResources("sample_directory/favicon.ico"), outputFile);
  }

  @Test
  public void testExtractFileWithFileNameDoesNotExtractSymlinkWhenSymlinkExtractionIsNotSet() throws IOException {
    File symlink = createSymlink();
    ZipFile zipFile = createZipFileWithASymlink(symlink);

    zipFile.extractFile(symlink.getName(), outputFolder.getPath(), buildUnzipParameters(false));

    assertThat(outputFolder.listFiles()).isNull();
  }

  @Test
  public void testExtractFileWithFileNameExtractsSymlinkWhenSymlinkExtractionIsSet() throws IOException {
    File symlink = createSymlink();
    ZipFile zipFile = createZipFileWithASymlink(symlink);

    zipFile.extractFile(symlink.getName(), outputFolder.getPath(), buildUnzipParameters(true));

    verifyNumberOfFilesInOutputFolder(outputFolder, 1);
    assertThat(outputFolder.listFiles()[0].getName()).isEqualTo(symlink.getName());
  }

  @Test
  public void testExtractFileWithFileNameAndNewFileNameDoesNotExtractSymlinkWhenSymlinkExtractionIsNotSet()
      throws IOException {
    File symlink = createSymlink();
    ZipFile zipFile = createZipFileWithASymlink(symlink);

    zipFile.extractFile(symlink.getName(), outputFolder.getPath(), "newFileName", buildUnzipParameters(false));

    assertThat(outputFolder.listFiles()).isNull();
  }

  @Test
  public void testExtractFileWithFileNameAndNewFileNameExtractsSymlinkWhenSymlinkExtractionIsSet() throws IOException {
    File symlink = createSymlink();
    ZipFile zipFile = createZipFileWithASymlink(symlink);

    String newFileName = "newName.link";
    zipFile.extractFile(symlink.getName(), outputFolder.getPath(), newFileName, buildUnzipParameters(true));

    verifyNumberOfFilesInOutputFolder(outputFolder, 1);
    assertThat(outputFolder.listFiles()[0].getName()).isEqualTo(newFileName);
  }

  @Test
  public void testSymlinkToDirectoryMaintainsSymlink() throws IOException {
    verifyZipFileByExtractingAllFiles(getTestArchiveFromResources("zipWithLinkToDirAndFolder.zip"), null, outputFolder, 5, false);

    Path aDirectory = Paths.get(outputFolder.getPath(), "a");
    Path symlinkToDir = Paths.get(outputFolder.getPath(), "b");
    Path aFile = Paths.get(outputFolder.getPath(), "c");
    Path symlinkToFile = Paths.get(outputFolder.getPath(), "d");
    assertThat(aDirectory).isDirectory();
    assertThat(Files.isSymbolicLink(symlinkToDir)).isTrue();
    assertThat(aFile).isRegularFile();
    assertThat(symlinkToFile).isSymbolicLink();
    assertThat(Files.readSymbolicLink(symlinkToDir)).isEqualTo(aDirectory.getFileName());
    assertThat(Files.readSymbolicLink(symlinkToFile)).isEqualTo(aFile.getFileName());
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
    verifyZipFileByExtractingAllFiles(zipArchiveToTest, "aaaaaaaa".toCharArray(), outputFolder, 5,
        false);
  }

  @Test
  public void testExtractFilesForZipFileWhileWithCorruptExtraDataRecordLength() throws IOException {
    ZipFile zipFile = new ZipFile(getTestArchiveFromResources("corrupt_extra_data_record_length.zip"));

    zipFile.extractAll(outputFolder.getPath());

    assertThat(zipFile.getFileHeaders()).hasSize(44);
    assertThat(getRegularFilesFromFolder(outputFolder)).hasSize(44);
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
    assertThat(getRegularFilesFromFolder(outputFolder)).hasSize(3);
  }

  @Test
  public void testExtractZipFileWithZip64ExtraDataRecordWithOnlyFileSizesInIt() throws IOException {
    ZipFile zipFile = new ZipFile(getTestArchiveFromResources("zip64_with_only_file_sizes.zip"),
        "Shu1an@2019GTS".toCharArray());

    zipFile.extractAll(outputFolder.getPath());

    assertThat(zipFile.getFileHeaders()).hasSize(1);
    assertThat(getRegularFilesFromFolder(outputFolder)).hasSize(1);
  }

  @Test
  public void testExtractZipFileWithChineseCharsetGBK() throws IOException {
    String expectedFileName = "fff - 副本.txt";
    ZipFile zipFile = new ZipFile(getTestArchiveFromResources("testfile_with_chinese_filename_by_7zip.zip"));

    zipFile.setCharset(CHARSET_GBK);
    zipFile.extractAll(outputFolder.getPath());

    assertThat(zipFile.getFileHeaders()).hasSize(2);
    @SuppressWarnings("ConstantConditions")
    List<String> filenameSet = getFileNamesOfFiles(Arrays.asList(outputFolder.listFiles()));
    assertThat(filenameSet.contains(expectedFileName)).isTrue();
  }

  @Test
  public void testExtractZipFileWithCheckoutMismatchThrowsExceptionWithType() {
    try {
      ZipFile zipFile = new ZipFile(getTestArchiveFromResources("entry_with_checksum_mismatch.zip"));
      zipFile.extractAll(outputFolder.getPath());
      fail("Should throw an exception");
    } catch (ZipException e) {
      assertThat(e.getType()).isEqualTo(ZipException.Type.CHECKSUM_MISMATCH);
      assertThat(e.getMessage()).isEqualTo("Reached end of entry, but crc verification failed for sample_text1.txt");
    }
  }

  @Test
  public void testExtractNestedZipFileWithNoEncryptionOnInnerAndOuter() throws IOException {
    testExtractNestedZipFileWithEncrpytion(EncryptionMethod.NONE, EncryptionMethod.NONE);
  }

  @Test
  public void testExtractNestedZipFileWithNoEncryptionOnInnerAndZipStandardOuter() throws IOException {
    testExtractNestedZipFileWithEncrpytion(EncryptionMethod.NONE, EncryptionMethod.ZIP_STANDARD);
  }

  @Test
  public void testExtractNestedZipFileWithNoEncryptionOnInnerAndAesdOuter() throws IOException {
    testExtractNestedZipFileWithEncrpytion(EncryptionMethod.NONE, EncryptionMethod.AES);
  }

  @Test
  public void testExtractNestedZipFileWithZipStandardEncryptionOnInnerAndNoneOuter() throws IOException {
    testExtractNestedZipFileWithEncrpytion(EncryptionMethod.ZIP_STANDARD, EncryptionMethod.NONE);
  }

  @Test
  public void testExtractNestedZipFileWitAesOnInnerAndNoneOuter() throws IOException {
    testExtractNestedZipFileWithEncrpytion(EncryptionMethod.AES, EncryptionMethod.NONE);
  }

  @Test
  public void testExtractNestedZipFileWithZipStandardEncryptionOnInnerAndAesOuter() throws IOException {
    testExtractNestedZipFileWithEncrpytion(EncryptionMethod.ZIP_STANDARD, EncryptionMethod.AES);
  }

  @Test
  public void testExtractNestedZipFileWithAesOnInnerAndZipStandardOuter() throws IOException {
    testExtractNestedZipFileWithEncrpytion(EncryptionMethod.AES, EncryptionMethod.ZIP_STANDARD);
  }

  @Test
  public void testExtractZipFileLessThanMinimumExpectedZipFileSizeThrowsException() throws IOException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("Zip file size less than minimum expected zip file size. " +
        "Probably not a zip file or a corrupted zip file");

    ZipFile zipFile = new ZipFile(getTestArchiveFromResources("invalid_zip_file_size_less_than_22kb.zip"));
    zipFile.extractAll(temporaryFolder.toString());
  }

  @Test
  public void testExtractZipFileEmptyZipFileExtractsNone() throws IOException {
    ZipFile zipFile = new ZipFile(getTestArchiveFromResources("valid_empty_zip_file.zip"));
    zipFile.extractAll(outputFolder.getPath());
    assertThat(outputFolder.listFiles()).isEmpty();
  }

  @Test
  public void testExtractZipFileCRCError() throws IOException {
    ZipFile zipFile = new ZipFile(getTestArchiveFromResources("archive_with_invalid_zip64_headers.zip"));
    zipFile.extractAll(outputFolder.getPath());
    assertThat(outputFolder.listFiles()).contains(
            new File(outputFolder, "index.html"),
            new File(outputFolder, "images"));
  }

  @Test
  public void testExtractZipFileOf7ZipFormatSplitWithoutEncryption() throws IOException {
    List<File> filesToAddToZip = new ArrayList<>(FILES_TO_ADD);
    filesToAddToZip.add(0, getTestFileFromResources("file_PDF_1MB.pdf"));
    File firstSplitFile = createZipFileAndSplit(filesToAddToZip, 102400, false, null);
    verifyZipFileByExtractingAllFiles(firstSplitFile, outputFolder, 4);
  }

  @Test
  public void testExtractZipFileOf7ZipFormatSplitWithAESEncryption() throws IOException {
    List<File> filesToAddToZip = new ArrayList<>(FILES_TO_ADD);
    filesToAddToZip.add(0, getTestFileFromResources("file_PDF_1MB.pdf"));
    File firstSplitFile = createZipFileAndSplit(filesToAddToZip, 102000, true, EncryptionMethod.AES);
    verifyZipFileByExtractingAllFiles(firstSplitFile, PASSWORD, outputFolder, 4);
  }

  @Test
  public void testExtractDifferentPasswordsInSameZip() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    addFileToZip(zipFile, "sample.pdf", EncryptionMethod.AES, "password1");
    addFileToZip(zipFile, "sample_text1.txt", EncryptionMethod.AES, "password2");
    addFileToZip(zipFile, "file_PDF_1MB.pdf", null, null);

    zipFile.setPassword("password1".toCharArray());
    zipFile.extractFile("sample.pdf", outputFolder.getPath());

    zipFile.setPassword("password2".toCharArray());
    zipFile.extractFile("sample_text1.txt", outputFolder.getPath());

    zipFile.setPassword(null);
    zipFile.extractFile("file_PDF_1MB.pdf", outputFolder.getPath());
  }

  @Test
  public void testExtractZipFileWithCommentLengthGreaterThanZipFileLength() throws IOException {
    verifyZipFileByExtractingAllFiles(getTestArchiveFromResources("zip_with_corrupt_comment_length.zip"), outputFolder, 1);
  }

  @Test
  public void testExtractZipFileWithEndOfCentralDirectoryNotAtExpectedPosition() throws IOException {
    ZipFile zipFile = new ZipFile(getTestArchiveFromResources("end_of_cen_dir_not_at_expected_position.zip"));
    zipFile.extractAll(outputFolder.getPath());
    List<File> outputFiles = FileUtils.getFilesInDirectoryRecursive(outputFolder, new ZipParameters());

    assertThat(outputFiles).hasSize(24);
    assertThat(zipFile.getFileHeaders()).hasSize(19);
  }

  @Test
  public void testExtractFileHeaderExtractAllFilesIfFileHeaderIsDirectory() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setIncludeRootFolder(false);
    zipFile.addFolder(TestUtils.getTestFileFromResources(""), zipParameters);

    zipFile.extractFile(zipFile.getFileHeader("öüäöäö/"), outputFolder.getPath());
    File outputFile = Paths.get(outputFolder.getPath(), "öüäöäö", "asöäööl").toFile();
    assertThat(outputFile).exists();
    ZipFileVerifier.verifyFileContent(TestUtils.getTestFileFromResources("öüäöäö/asöäööl"), outputFile);
  }

  @Test
  public void testExtractFileHeaderExtractAllFilesIfFileHeaderIsDirectoryAndRenameFile() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setIncludeRootFolder(false);
    zipFile.addFolder(TestUtils.getTestFileFromResources(""), zipParameters);

    zipFile.extractFile(zipFile.getFileHeader("öüäöäö/"), outputFolder.getPath(), "new_folder_name/");
    File outputFile = Paths.get(outputFolder.getPath(), "new_folder_name", "asöäööl").toFile();
    assertThat(outputFile).exists();
    ZipFileVerifier.verifyFileContent(TestUtils.getTestFileFromResources("öüäöäö/asöäööl"), outputFile);
  }

  @Test
  public void testExtractFileWhichIsAFolderExtractsContentsEvenWhenFolderEntryIsNotInZip() throws IOException {
    ZipFile zipFile = new ZipFile(getTestArchiveFromResources("archive-with-no-dir-entries.zip"));
    String outputFolderPath = outputFolder.getPath();

    zipFile.extractFile("items/", outputFolderPath);

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setReadHiddenFiles(false);
    zipParameters.setReadHiddenFolders(false);
    List<File> extractedFiles = FileUtils.getFilesInDirectoryRecursive(outputFolder, zipParameters);
    assertThat(extractedFiles).isNotEmpty();
    assertThat(extractedFiles).hasSize(3);
    assertThat(extractedFiles).contains(
        Paths.get(outputFolderPath, "items").toFile(),
        Paths.get(outputFolderPath, "items/subitems").toFile(),
        Paths.get(outputFolderPath, "items/subitems/beta.txt").toFile());
  }

  @Test
  public void testExtractJarFileWithFileHeaderCompressedSize2() throws IOException {
    extractFile(TestUtils.getTestArchiveFromResources("jar-dir-fh-entry-size-2.jar"));
  }

  @Test
  public void testExtractJarFileWithOnlyFileAndLocalFileHeaderCompressedSize2() throws IOException {
    extractFile(TestUtils.getTestArchiveFromResources("jar-dir-lfh-and-fh-entry-size-2.jar"));
  }

  @Test
  public void testExtractZipStrongEncryptionThrowsException() throws IOException {
    ZipFile zipFile = new ZipFile(getTestArchiveFromResources("strong_encrypted.zip"), "12345678".toCharArray());
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("Entry with name test.txt is encrypted with Strong Encryption. " +
        "Zip4j does not support Strong Encryption, as this is patented.");

    zipFile.extractAll(outputFolder.getPath());
  }

  @Test
  public void testExtractAllWithCustomBufferSize() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setIncludeRootFolder(false);
    zipFile.addFolder(getTestFileFromResources(""), zipParameters);
    zipFile.setBufferSize(32 * 1024);

    zipFile.extractAll(outputFolder.getPath());

    ZipFileVerifier.verifyFolderContentsSameAsSourceFiles(outputFolder);
    verifyNumberOfFilesInOutputFolder(outputFolder, 10);
  }

  @Test
  public void testExtractingZipStandardEncryptedZipOverLoopAlwaysThrowsWrongPasswordTypeException() throws IOException {
    for (int i = 0; i < 1000; i++) {
      if (generatedZipFile.exists() && !generatedZipFile.delete()) {
        throw new RuntimeException("Could not delete test zip file");
      }

      ZipFile zipFile = new ZipFile(generatedZipFile, "password".toCharArray());
      addFileToZip(zipFile, "sample.pdf", EncryptionMethod.ZIP_STANDARD, "password");

      try {
        zipFile = new ZipFile(generatedZipFile, "WRONG_password".toCharArray());
        zipFile.extractAll(outputFolder.getPath());
      } catch (ZipException e) {
        assertThat(e).isNotNull();
        assertThat(e.getType()).isEqualTo(ZipException.Type.WRONG_PASSWORD);
      }
    }
  }

  @Test
  public void testExtractZipFileByFileNameWhichTheDirectoryEntryAtTheEndOfCentralDirectoryExtractsSuccessfully()
      throws  ZipException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFolder(getTestFileFromResources("/"));

    zipFile.extractFile("test-files/", outputFolder.getPath());
    zipFile = new ZipFile(generatedZipFile);
    zipFile.extractFile("test-files/", outputFolder.getPath());
  }

  @Test
  public void testExtractZipFileThrowsExceptionOfTypeWrongPasswordForNullAesPassword() throws ZipException {
    testExtractZipFileThrowsExceptionOfTypeWrongPasswordForNullOrEmptyAesPassword(null);
  }

  @Test
  public void testExtractZipFileThrowsExceptionOfTypeWrongPasswordForEmptyAesPassword() throws ZipException {
    testExtractZipFileThrowsExceptionOfTypeWrongPasswordForNullOrEmptyAesPassword("".toCharArray());
  }

  private void addFileToZip(ZipFile zipFile, String fileName, EncryptionMethod encryptionMethod, String password) throws ZipException {
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setEncryptFiles(encryptionMethod != null);
    zipParameters.setEncryptionMethod(encryptionMethod);

    if (password != null) {
      zipFile.setPassword(password.toCharArray());
    }

    zipFile.addFile(getTestFileFromResources(fileName), zipParameters);
  }

  private void testExtractNestedZipFileWithEncrpytion(EncryptionMethod innerZipEncryption,
                                                       EncryptionMethod outerZipEncryption) throws IOException {
    File innerZipFile = temporaryFolder.newFile("inner.zip");
    File outerZipFile = temporaryFolder.newFile("outer.zip");

    innerZipFile.delete();
    outerZipFile.delete();

    createNestedZip(innerZipFile, outerZipFile, innerZipEncryption, outerZipEncryption);

    verifyNestedZipFile(outerZipFile, FILES_TO_ADD.size());
  }

  private void createNestedZip(File innerSourceZipFile, File outerSourceZipFile, EncryptionMethod innerEncryption,
                               EncryptionMethod outerEncryption) throws ZipException {

    ZipFile innerZipFile = new ZipFile(innerSourceZipFile, PASSWORD);
    ZipParameters innerZipParameters = createZipParametersForNestedZip(innerEncryption);
    innerZipFile.addFiles(FILES_TO_ADD, innerZipParameters);

    ZipFile outerZipFile = new ZipFile(outerSourceZipFile, PASSWORD);
    ZipParameters outerZipParameters = createZipParametersForNestedZip(outerEncryption);
    outerZipFile.addFile(innerSourceZipFile, outerZipParameters);
  }

  private void verifyNestedZipFile(File outerZipFileToVerify, int numberOfFilesInNestedZip) throws IOException {
    ZipFile zipFile = new ZipFile(outerZipFileToVerify, PASSWORD);
    FileHeader fileHeader = zipFile.getFileHeader("inner.zip");

    assertThat(fileHeader).isNotNull();

    int actualNumberOfFilesInNestedZip = 0;
    try(InputStream inputStream = zipFile.getInputStream(fileHeader)) {
      try(ZipInputStream zipInputStream = new ZipInputStream(inputStream, PASSWORD)) {
        while (zipInputStream.getNextEntry() != null) {
          actualNumberOfFilesInNestedZip++;
        }
      }
    }

    assertThat(actualNumberOfFilesInNestedZip).isEqualTo(numberOfFilesInNestedZip);
  }

  private ZipParameters createZipParametersForNestedZip(EncryptionMethod encryptionMethod) {
    ZipParameters zipParameters = new ZipParameters();
    if (encryptionMethod != null && !encryptionMethod.equals(EncryptionMethod.NONE)) {
      zipParameters.setEncryptFiles(true);
      zipParameters.setEncryptionMethod(encryptionMethod);
    }
    return zipParameters;
  }

  private void verifyNumberOfFilesInOutputFolder(File outputFolder, int numberOfExpectedFiles) {
    assertThat(outputFolder.listFiles()).hasSize(numberOfExpectedFiles);
  }

  private File getFileWithNameFrom(File outputFolder, String fileName) throws ZipException {
    List<File> filesInFolder = FileUtils.getFilesInDirectoryRecursive(outputFolder, new ZipParameters());
    for (File file : filesInFolder) {
      if (file.getName().equals(fileName)) {
        return file;
      }
    }
    fail("Could not find a file by name " + fileName);
    return null;
  }

  private File createZipFileAndSplit(List<File> filesToAddToZip, long splitLength, boolean encrypt,
                                     EncryptionMethod encryptionMethod) throws IOException {

    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setEncryptFiles(encrypt);
    zipParameters.setEncryptionMethod(encryptionMethod);
    zipFile.addFiles(filesToAddToZip, zipParameters);

    return TestUtils.splitFileWith7ZipFormat(zipFile.getFile(), temporaryFolder.getRoot(), splitLength);
  }

  private void extractFile(File fileToExtract) throws IOException {
    ZipFile zipFile = new ZipFile(fileToExtract);
    zipFile.extractAll(outputFolder.getPath());
  }

  private ZipFile createZipFileWithASymlink(File symlink) throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setSymbolicLinkAction(ZipParameters.SymbolicLinkAction.INCLUDE_LINK_AND_LINKED_FILE);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);
    zipFile.addFile(symlink, zipParameters);
    return zipFile;
  }

  private UnzipParameters buildUnzipParameters(boolean extractSymlinks) {
    UnzipParameters unzipParameters = new UnzipParameters();
    unzipParameters.setExtractSymbolicLinks(extractSymlinks);
    return unzipParameters;
  }

  private File createSymlink() throws IOException {
    File targetFile = getTestFileFromResources("file_PDF_1MB.pdf");
    return TestUtils.createSymlink(targetFile, temporaryFolder.getRoot());
  }

  private List<File> getRegularFilesFromFolder(File folder) throws ZipException {
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setReadHiddenFiles(false);
    zipParameters.setReadHiddenFolders(false);
    List<File> filesInFolder = FileUtils.getFilesInDirectoryRecursive(folder, zipParameters);

    List<File> regularFiles = new ArrayList<>();
    for (File file : filesInFolder) {
      if (Files.isRegularFile(file.toPath())) {
        regularFiles.add(file);
      }
    }
    return regularFiles;
  }

  private void testExtractZipFileThrowsExceptionOfTypeWrongPasswordForNullOrEmptyAesPassword(char[] password) throws ZipException {
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    addFileToZip(zipFile, "sample.pdf", EncryptionMethod.AES, new String(PASSWORD));

    expectedException.expect(ZipException.class);
    expectedException.expectMessage("empty or null password provided for AES decryption");

    zipFile = new ZipFile(generatedZipFile, password);
    zipFile.extractAll(outputFolder.getPath());
  }
}
