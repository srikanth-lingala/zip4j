package net.lingala.zip4j;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.testutils.TestUtils;
import net.lingala.zip4j.testutils.ZipFileVerifier;
import net.lingala.zip4j.util.InternalZipConstants;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class RenameFileInZipIT extends AbstractIT {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testRenameFileAsFileNameThrowsExceptionWhenZipFileDoesNotExist() throws ZipException {
    String fileNameToRename = "SOME_NAME";
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("could not find file header for file: " + fileNameToRename);

    new ZipFile(generatedZipFile).renameFile(fileNameToRename, "NEW_NAME");
  }

  @Test
  public void testRenameFileAsFileNameThrowsExceptionWhenFileDoesNotExistInZip() throws ZipException {
    String fileNameToRename = "SOME_NAME";
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("could not find file header for file: " + fileNameToRename);

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFiles(FILES_TO_ADD);

    zipFile.renameFile(fileNameToRename, "NEW_NAME");
  }

  @Test
  public void testRenameFileAsFileNameThrowsExceptionWhenFileAlreadyExistInZip() throws ZipException {
    String fileNameToRename = "sample_text1.txt";
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("The new file name already exists in the zip file.");

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFiles(FILES_TO_ADD);

    zipFile.renameFile(fileNameToRename, "sample.pdf");
  }

  @Test
  public void testRenameFileAsFileNameThrowsExceptionForSplitArchive() throws ZipException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    List<File> filesToAdd = new ArrayList<>(FILES_TO_ADD);
    filesToAdd.add(TestUtils.getTestFileFromResources("file_PDF_1MB.pdf"));
    zipFile.createSplitZipFile(filesToAdd, new ZipParameters(), true, InternalZipConstants.MIN_SPLIT_LENGTH);

    expectedException.expect(ZipException.class);
    expectedException.expectMessage("Zip file format does not allow updating split/spanned files");

    zipFile.renameFile("file_PDF_1MB.pdf", "NEW_NAME");
  }

  @Test
  public void testRenameFileAsFileNameSuccessfully() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFiles(FILES_TO_ADD);
    String oldName = "sample_text1.txt";
    String newName = "NEW_NAME.txt";

    zipFile.renameFile(oldName, newName);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, 3, false);
    verifyZipFileDoesNotContainFile(generatedZipFile, "sample_text1.txt", null);
    verifyZipFileContainFile(generatedZipFile, "NEW_NAME.txt", null);
    verifyRenamedFileContentByExtractingFile(generatedZipFile, outputFolder, oldName, newName);
  }

  @Test
  public void testRenameFileAsFileNameWithCharsetCp949Successfully() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.setCharset(CHARSET_CP_949);

    List<File> filesToAdd = new ArrayList<>();
    filesToAdd.add(TestUtils.getTestFileFromResources("sample_text1.txt"));
    zipFile.addFiles(filesToAdd);

    zipFile.renameFile("sample_text1.txt", "가나다.abc");

    verifyZipFileContainFile(generatedZipFile, "가나다.abc", CHARSET_CP_949);
  }

  @Test
  public void testRenameFolderSuccessfully() throws IOException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.addFolder(TestUtils.getTestFileFromResources(""));
    String oldName = "test-files/öüäöäö/";
    String newName = "NEW_NAME/";

    zipFile.renameFile(oldName, newName);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, 13, false);
    verifyZipFileDoesNotContainFile(generatedZipFile, "test-files/öüäöäö/asöäööl", null);
    verifyZipFileContainFile(generatedZipFile, "NEW_NAME/asöäööl", null);
    verifyRenamedFileContentByExtractingFile(generatedZipFile, outputFolder, "öüäöäö/asöäööl", "NEW_NAME/asöäööl");
  }

  private void verifyRenamedFileContentByExtractingFile(File zipFileToExtract, File outputFolder, String oldFileName, String newFileName) throws IOException {
    ZipFile zipFile = new ZipFile(zipFileToExtract);
    zipFile.extractFile(newFileName, outputFolder.getAbsolutePath());
    File fileWithNewName = new File(outputFolder.getAbsolutePath() + InternalZipConstants.FILE_SEPARATOR + newFileName);
    File originalFile = TestUtils.getTestFileFromResources(oldFileName);
    ZipFileVerifier.verifyFileContent(fileWithNewName, originalFile);
  }

  private void verifyZipFileDoesNotContainFile(File generatedZipFile, String fileNameToCheck, Charset charset) throws ZipException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    if(charset != null) {
      zipFile.setCharset(charset);
    }
    Optional<FileHeader> fileHeader = zipFile.getFileHeaders().stream()
            .filter(e -> e.getFileName().equals(fileNameToCheck)).findFirst();
    assertThat(fileHeader).isNotPresent();
  }

  private void verifyZipFileContainFile(File generatedZipFile, String fileNameToCheck, Charset charset) throws ZipException {
    ZipFile zipFile = new ZipFile(generatedZipFile);
    if(charset != null) {
      zipFile.setCharset(charset);
    }
    Optional<FileHeader> fileHeader = zipFile.getFileHeaders().stream()
            .filter(e -> e.getFileName().equals(fileNameToCheck)).findFirst();
    assertThat(fileHeader).isPresent();
  }
}
