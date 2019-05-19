package net.lingala.zip4j.utils;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.UnzipParameters;
import net.lingala.zip4j.model.ZipParameters;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static net.lingala.zip4j.TestUtils.getFileFromResources;
import static org.assertj.core.api.Assertions.assertThat;

public class ZipVerifier {

  public static void verifyZipFile(File generatedZipFile, ZipParameters zipParameters,
                            TemporaryFolder temporaryFolder) throws ZipException, IOException {
    assertThat(generatedZipFile).isNotNull();

    ZipFile zipFile = new ZipFile(generatedZipFile);
    if (zipParameters.isEncryptFiles()) {
      zipFile.setPassword(zipParameters.getPassword());
    }

    File folderToExtractTo = temporaryFolder.newFolder();
    zipFile.extractAll(folderToExtractTo.getAbsolutePath(), new UnzipParameters());

    verifyAllFiles(folderToExtractTo);
  }

  public static void verifyFileContent(File sourceFile, File extractedFile) throws IOException {
    assertThat(extractedFile.length()).isEqualTo(sourceFile.length());

    byte[] sourceFileContent = Files.readAllBytes(sourceFile.toPath());
    byte[] extractedFileContent = Files.readAllBytes(extractedFile.toPath());

    assertThat(extractedFileContent).as("Files do not match for file name: " + extractedFile.getName()).isEqualTo(sourceFileContent);
  }

  private static void verifyAllFiles(File folderContainingExtractedFiles) throws IOException {
    File[] allFiles = folderContainingExtractedFiles.listFiles();

    for (File fileToVerify : allFiles) {
      verifyFileContent(getFileFromResources(fileToVerify.getName()), fileToVerify);
    }
  }

}
