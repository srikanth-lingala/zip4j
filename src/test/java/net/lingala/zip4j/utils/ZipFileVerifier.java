package net.lingala.zip4j.utils;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.util.CrcUtil;
import net.lingala.zip4j.util.FileUtils;

import java.io.File;
import java.util.List;

import static net.lingala.zip4j.util.BitUtils.isBitSet;
import static org.assertj.core.api.Assertions.assertThat;

public class ZipFileVerifier {

  public static void verifyZipFileByExtractingAllFiles(File zipFileToExtract, File outputFolder,
                                                       int expectedNumberOfEntries) throws ZipException {
    verifyZipFileByExtractingAllFiles(zipFileToExtract, null, outputFolder, expectedNumberOfEntries);
  }

  public static void verifyZipFileByExtractingAllFiles(File zipFileToExtract, char[] password, File outputFolder,
                                                       int expectedNumberOfEntries) throws ZipException {
    verifyZipFileByExtractingAllFiles(zipFileToExtract, password, outputFolder, expectedNumberOfEntries, true);
  }

  public static void verifyZipFileByExtractingAllFiles(File zipFileToExtract, char[] password, File outputFolder,
                                                       int expectedNumberOfEntries, boolean verifyFileContents)
      throws ZipException {

    assertThat(zipFileToExtract).isNotNull();
    assertThat(zipFileToExtract).exists();

    ZipFile zipFile = new ZipFile(zipFileToExtract, password);
    zipFile.extractAll(outputFolder.getPath());
    assertThat(zipFile.getFileHeaders().size()).as("Number of file headers").isEqualTo(expectedNumberOfEntries);

    List<File> extractedFiles = FileUtils.getFilesInDirectoryRecursive(outputFolder, true);
    assertThat(extractedFiles).hasSize(expectedNumberOfEntries);

    if (verifyFileContents) {
      verifyFolderContentsSameAsSourceFiles(outputFolder);
    }
  }

  public static void verifyFileContent(File sourceFile, File extractedFile) throws ZipException {
    assertThat(extractedFile.length()).isEqualTo(sourceFile.length());
    verifyFileCrc(sourceFile, extractedFile);
  }

  private static void verifyFolderContentsSameAsSourceFiles(File outputFolder) throws ZipException {
    File[] filesInOutputFolder = outputFolder.listFiles();

    for (File file : filesInOutputFolder) {
      File sourceFile = TestUtils.getFileFromResources(file.getName());
      verifyFileContent(sourceFile, file);
    }
  }

  private static long getUncompressedSize(LocalFileHeader localFileHeader) {
    if (localFileHeader.getZip64ExtendedInfo() != null && !isBitSet(localFileHeader.getGeneralPurposeFlag()[0], 3)) {
      return localFileHeader.getZip64ExtendedInfo().getUncompressedSize();
    }
    return localFileHeader.getUncompressedSize();
  }

  private static void verifyFileCrc(File sourceFile, File extractedFile) throws ZipException {
    ProgressMonitor progressMonitor = new ProgressMonitor();
    long sourceFileCrc = CrcUtil.computeFileCrc(sourceFile, progressMonitor);
    long extractedFileCrc = CrcUtil.computeFileCrc(extractedFile, progressMonitor);

    assertThat(sourceFileCrc).isEqualTo(extractedFileCrc);
  }
}
