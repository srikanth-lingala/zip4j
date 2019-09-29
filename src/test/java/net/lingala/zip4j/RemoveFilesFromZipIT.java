package net.lingala.zip4j;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class RemoveFilesFromZipIT extends AbstractIT {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testRemoveFileAsFileNameThrowsExceptionWhenZipFileDoesNotExist() throws ZipException {
    String fileNameToRemove = "SOME_NAME";
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("could not find file header for file: " + fileNameToRemove);

    new ZipFile(generatedZipFile).removeFile(fileNameToRemove);
  }

  @Test
  public void testRemoveFileAsFileNameThrowsExceptionWhenFileDoesNotExistInZip() throws ZipException {
    String fileNameToRemove = "SOME_NAME";
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("could not find file header for file: " + fileNameToRemove);

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFiles(FILES_TO_ADD);

    zipFile.removeFile(fileNameToRemove);
  }

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
  public void testRemoveFileAsFileNameRemovesSuccessfully() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFiles(FILES_TO_ADD);

    zipFile.removeFile("sample_text1.txt");

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, outputFolder, 2);
    verifyZipFileDoesNotContainFile(generatedZipFile, "sample_text1.txt");
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
    verifyZipFileDoesNotContainFile(generatedZipFile, "sample_text1.txt");
  }

  @Test
  public void testRemoveFileAsFileNameRemovesSuccessfullyWithFolderNameInPath() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFolder(TestUtils.getTestFileFromResources(""), zipParameters);

    zipFile.removeFile("test-files/öüäöäö/asöäööl");

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 12);
    verifyZipFileDoesNotContainFile(generatedZipFile, "test-files/öüäöäö/asöäööl");
  }

  @Test
  public void testRemoveFileAsFileHeaderThrowsExceptionForSplitArchive() throws ZipException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    List<File> filesToAdd = new ArrayList<>(FILES_TO_ADD);
    filesToAdd.add(TestUtils.getTestFileFromResources("file_PDF_1MB.pdf"));
    zipFile.createSplitZipFile(filesToAdd, new ZipParameters(), true, InternalZipConstants.MIN_SPLIT_LENGTH);

    expectedException.expect(ZipException.class);
    expectedException.expectMessage("Zip file format does not allow updating split/spanned files");

    zipFile.removeFile(zipFile.getFileHeader("file_PDF_1MB.pdf"));
  }

  @Test
  public void testRemoveFileAsFileHeaderRemovesSuccessfully() throws IOException {
    ZipParameters zipParameters = createZipParameters(EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256);
    ZipFile zipFile = new ZipFile(generatedZipFile, PASSWORD);
    zipFile.addFolder(TestUtils.getTestFileFromResources(""), zipParameters);

    zipFile.removeFile(zipFile.getFileHeader("test-files/sample_directory/favicon.ico"));

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, PASSWORD, outputFolder, 12);
    verifyZipFileDoesNotContainFile(generatedZipFile, "sample_directory/favicon.ico");
  }

  private void verifyZipFileDoesNotContainFile(File generatedZipFile, String fileNameToCheck) throws ZipException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    Optional<FileHeader> fileHeader = zipFile.getFileHeaders().stream()
        .filter(e -> e.getFileName().equals(fileNameToCheck)).findFirst();
    assertThat(fileHeader).isNotPresent();
  }
}
