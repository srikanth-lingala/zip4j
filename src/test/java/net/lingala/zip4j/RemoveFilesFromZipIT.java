package net.lingala.zip4j;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.testutils.TestUtils;
import net.lingala.zip4j.testutils.ZipFileVerifier;
import net.lingala.zip4j.util.InternalZipConstants;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.lingala.zip4j.testutils.HeaderVerifier.verifyFileHeadersDoesNotExist;
import static net.lingala.zip4j.testutils.HeaderVerifier.verifyZipFileDoesNotContainFolders;

public class RemoveFilesFromZipIT extends AbstractIT {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testRemoveFileAsFileNameThrowsExceptionForSplitArchive() throws ZipException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    List<File> filesToAdd = new ArrayList<>(FILES_TO_ADD);
    filesToAdd.add(TestUtils.getTestFileFromResources("file_PDF_1MB.pdf"));
    zipFile.createSplitZipFile(filesToAdd, new ZipParameters(), true, InternalZipConstants.MIN_SPLIT_LENGTH);

    expectedException.expect(ZipException.class);
    expectedException.expectMessage("Zip file format does not allow updating split/spanned files");

    zipFile.removeFile("file_PDF_1MB.pdf");
  }

  @Test
  public void testRemoveFileAsFileNameDoesNotModifyZipFileWhenFileDoesNotExistInZip() throws IOException {
    String fileNameToRemove = "SOME_NAME";

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFiles(FILES_TO_ADD);

    zipFile.removeFile(fileNameToRemove);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, FILES_TO_ADD.size());
  }

  @Test
  public void testRemoveFileAsFileNameRemovesSuccessfully() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFiles(FILES_TO_ADD);

    zipFile.removeFile("sample_text1.txt");

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 2);
    verifyFileHeadersDoesNotExist(zipFile, Collections.singletonList("sample_text1.txt"));
  }

  @Test
  public void testRemoveFileAsFileNameWithCharsetCp949RemovesSuccessfully() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    List<File> filesToAdd = new ArrayList<>();
    filesToAdd.add(TestUtils.getTestFileFromResources("가나다.abc"));
    filesToAdd.add(TestUtils.getTestFileFromResources("sample_text1.txt"));

    zipFile.setCharset(CHARSET_CP_949);
    zipFile.addFiles(filesToAdd);
    zipFile.removeFile("sample_text1.txt");

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, 1, true, CHARSET_CP_949);
    verifyFileHeadersDoesNotExist(zipFile, Collections.singletonList("sample_text1.txt"));
  }

  @Test
  public void testRemoveFileAsFileNameRemovesSuccessfullyWithFolderNameInPath() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFolder(TestUtils.getTestFileFromResources(""), zipParameters);

    zipFile.removeFile("test-files/öüäöäö/asöäööl");

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 12);
    verifyFileHeadersDoesNotExist(zipFile, Collections.singletonList("test-files/öüäöäö/asöäööl"));
  }

  @Test
  public void testRemoveFileAsFileHeaderRemovesSuccessfully() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFolder(TestUtils.getTestFileFromResources(""), zipParameters);

    zipFile.removeFile(zipFile.getFileHeader("test-files/sample_directory/favicon.ico"));

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 12);
    verifyFileHeadersDoesNotExist(zipFile, Collections.singletonList("sample_directory/favicon.ico"));
  }

  @Test
  public void testRemoveFilesRemovesFirstEntrySuccessfully() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFiles(FILES_TO_ADD);

    zipFile.removeFiles(Collections.singletonList("sample_text1.txt"));

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, FILES_TO_ADD.size() - 1);
    verifyFileHeadersDoesNotExist(zipFile, Collections.singletonList("sample_text1.txt"));
  }

  @Test
  public void testRemoveFilesRemovesLastEntrySuccessfully() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFiles(FILES_TO_ADD);

    zipFile.removeFiles(Collections.singletonList("sample.pdf"));

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, FILES_TO_ADD.size() - 1);
    verifyFileHeadersDoesNotExist(zipFile, Collections.singletonList("sample.pdf"));
  }

  @Test
  public void testRemoveFilesRemovesMultipleEntriesSuccessfully() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFiles(FILES_TO_ADD);
    List<String> filesToRemove = Arrays.asList("sample_text1.txt", "sample.pdf");

    zipFile.removeFiles(filesToRemove);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, FILES_TO_ADD.size() - 2);
    verifyFileHeadersDoesNotExist(zipFile, filesToRemove);
  }

  @Test
  public void testRemoveFilesRemovesMultipleEntriesFromEncryptedZipSuccessfully() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setEncryptFiles(true);
    zipParameters.setEncryptionMethod(EncryptionMethod.AES);
    zipFile.addFiles(FILES_TO_ADD, zipParameters);
    List<String> filesToRemove = Arrays.asList("sample_text1.txt", "sample.pdf");

    zipFile.removeFiles(filesToRemove);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, FILES_TO_ADD.size() - 2);
    verifyFileHeadersDoesNotExist(zipFile, filesToRemove);
  }

  @Test
  public void testRemoveFilesRemovesDirectorySuccessfully() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFolder(TestUtils.getTestFileFromResources(""));

    zipFile.removeFiles(Collections.singletonList("test-files/öüäöäö/"));

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 11);
    verifyZipFileDoesNotContainFolders(zipFile, Collections.singletonList("test-files/öüäöäö/"));
  }

  @Test
  public void testRemoveFilesRemovesMultipleFilesAndDirectoriesSuccessfully() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFolder(TestUtils.getTestFileFromResources(""));

    zipFile.removeFiles(Arrays.asList(
        "test-files/öüäöäö/",
        "test-files/sample_directory/",
        "test-files/after_deflate_remaining_bytes.bin",
        "test-files/бореиская.txt"
    ));

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 7);
    verifyZipFileDoesNotContainFolders(zipFile, Arrays.asList("test-files/öüäöäö/", "test-files/sample_directory/"));
    verifyFileHeadersDoesNotExist(zipFile, Arrays.asList("test-files/after_deflate_remaining_bytes.bin", "test-files/бореиская.txt"));
  }

  @Test
  public void testRemoveFilesRemovesSingleEntryFromAFolderInAZip() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFolder(TestUtils.getTestFileFromResources(""));
    List<String> fileToRemove = Collections.singletonList("test-files/öüäöäö/asöäööl");

    zipFile.removeFiles(fileToRemove);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 12);
    verifyFileHeadersDoesNotExist(zipFile, fileToRemove);
  }

  @Test
  public void testRemoveFilesFromZipWithDataDescriptors() throws IOException {
    TestUtils.createZipFileWithZipOutputStream(generatedZipFile, FILES_TO_ADD);
    ZipFile zipFile = new ZipFile(generatedZipFile);
    List<String> filesToRemove = Collections.singletonList("sample_text_large.txt");

    zipFile.removeFiles(filesToRemove);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 2);
    verifyFileHeadersDoesNotExist(zipFile, filesToRemove);
  }

  @Test
  public void testRemoveFirstEntryFromZipWhichHasCentralDirEntriesInDifferentOrderThanLocalEntries() throws IOException {
    testRemoveEntryFromZipWhichHasCentralDirEntriesInDifferentOrderThanLocalEntries("test-files/zero_byte_file.txt");
  }

  @Test
  public void testRemoveLastEntryFromZipWhichHasCentralDirEntriesInDifferentOrderThanLocalEntries() throws IOException {
    testRemoveEntryFromZipWhichHasCentralDirEntriesInDifferentOrderThanLocalEntries("test-files/бореиская.txt");
  }

  @Test
  public void testRemoveMiddleEntryFromZipWhichHasCentralDirEntriesInDifferentOrderThanLocalEntries() throws IOException {
    testRemoveEntryFromZipWhichHasCentralDirEntriesInDifferentOrderThanLocalEntries("test-files/file_PDF_1MB.pdf");
  }

  private void testRemoveEntryFromZipWhichHasCentralDirEntriesInDifferentOrderThanLocalEntries(
      String fileNameToRemove) throws IOException {
    TestUtils.copyFile(TestUtils.getTestArchiveFromResources("cen_dir_entries_diff_order_as_local_entries.zip"),
        generatedZipFile);
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.removeFile(fileNameToRemove);

    zipFile = new ZipFile(generatedZipFile);
    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 12);
    verifyFileHeadersDoesNotExist(zipFile, Collections.singletonList(fileNameToRemove));
  }
}
