package net.lingala.zip4j.testutils;

import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.FileUtils;
import net.lingala.zip4j.util.InternalZipConstants;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

public class TestUtils {

  private static final String TEST_FILES_FOLDER_NAME = "test-files";
  private static final String TEST_ARCHIVES_FOLDER_NAME = "test-archives";

  public static File getTestFileFromResources(String fileName) {
    return getFileFromResources(TEST_FILES_FOLDER_NAME, fileName);
  }

  public static File getTestArchiveFromResources(String fileName) {
   return getFileFromResources(TEST_ARCHIVES_FOLDER_NAME, fileName);
  }

  public static Boolean isWindows() {
    String os = System.getProperty("os.name").toLowerCase();
    return (os.contains("win"));
  }

  /**
   * Splits files with extension .001, .002, etc
   * @param fileToSplit file to be split
   * @param splitLength the length of each split file
   * @return File - first split file
   * @throws IOException if any exception occurs dealing with streams
   */
  public static File splitFileWith7ZipFormat(File fileToSplit, File outputFolder, long splitLength) throws IOException {
    if (splitLength < InternalZipConstants.MIN_SPLIT_LENGTH) {
      throw new IllegalArgumentException("split length less than minimum allowed split length of " + InternalZipConstants.MIN_SPLIT_LENGTH);
    }

    int splitCounter = 0;
    byte[] buff = new byte[InternalZipConstants.BUFF_SIZE];
    int readLen = 0;
    long numberOfBytesWrittenInThisPart = 0;

    try (InputStream inputStream = new FileInputStream(fileToSplit)) {
      OutputStream outputStream = startNext7ZipSplitStream(fileToSplit, outputFolder, splitCounter);
      splitCounter++;

      while ((readLen = inputStream.read(buff)) != -1) {
        if (numberOfBytesWrittenInThisPart + readLen > splitLength) {
          int numberOfBytesToWriteInThisCounter = (int) (splitLength - numberOfBytesWrittenInThisPart);
          outputStream.write(buff, 0, numberOfBytesToWriteInThisCounter);
          outputStream.close();
          outputStream = startNext7ZipSplitStream(fileToSplit, outputFolder, splitCounter);
          splitCounter++;
          outputStream.write(buff, numberOfBytesToWriteInThisCounter, readLen - numberOfBytesToWriteInThisCounter);
          numberOfBytesWrittenInThisPart = readLen - numberOfBytesToWriteInThisCounter;
        } else {
          outputStream.write(buff, 0, readLen);
          numberOfBytesWrittenInThisPart += readLen;
        }
      }

      outputStream.close();
    }

    return getFileNameFor7ZipSplitIndex(fileToSplit, outputFolder, 0);
  }

  public static void copyFile(File sourceFile, File destinationFile) throws IOException {
    Files.copy(sourceFile.toPath(), destinationFile.toPath());
  }

  public static void copyFileToFolder(File sourceFile, File outputFolder, int numberOfCopiesToMake) throws IOException {
    for (int i = 0; i < numberOfCopiesToMake; i++) {
      File destinationFile = Paths.get(outputFolder.getAbsolutePath(), i + ".pdf").toFile();
      copyFile(sourceFile, destinationFile);
    }
  }

  public static void createZipFileWithZipOutputStream(File zipFile, List<File> filesToAdd) throws IOException {

    byte[] buff = new byte[InternalZipConstants.BUFF_SIZE];
    int readLen = -1;
    ZipParameters zipParameters = new ZipParameters();

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
      for (File fileToAdd : filesToAdd) {
        zipParameters.setFileNameInZip(fileToAdd.getName());
        zipOutputStream.putNextEntry(zipParameters);

        try(InputStream inputStream = new FileInputStream(fileToAdd)) {
          while ((readLen = inputStream.read(buff)) != -1) {
            zipOutputStream.write(buff, 0, readLen);
          }
        }

        zipOutputStream.closeEntry();
      }
    }
  }

  public static File generateFileOfSize(TemporaryFolder temporaryFolder, long fileSize) throws IOException {
    File outputFile = temporaryFolder.newFile();
    byte[] b = new byte[8 * InternalZipConstants.BUFF_SIZE];
    Random random = new Random();
    long bytesWritten = 0;
    int bufferWriteLength;

    try (OutputStream outputStream = new FileOutputStream(outputFile)) {
      while (bytesWritten < fileSize) {
        random.nextBytes(b);
        bufferWriteLength = bytesWritten + b.length > fileSize ? ((int) (fileSize - bytesWritten)) : b.length;
        outputStream.write(b, 0, bufferWriteLength);
        bytesWritten += bufferWriteLength;
      }
    }

    return outputFile;
  }

  private static OutputStream startNext7ZipSplitStream(File sourceFile, File outputFolder, int index) throws IOException {
    File outputFile = getFileNameFor7ZipSplitIndex(sourceFile, outputFolder, index);
    return new FileOutputStream(outputFile);
  }

  private static File getFileNameFor7ZipSplitIndex(File sourceFile, File outputFolder, int index) throws IOException {
    return new File(outputFolder.getCanonicalPath() + File.separator + sourceFile.getName()
        + FileUtils.getNextNumberedSplitFileCounterAsExtension(index));
  }

  private static File getFileFromResources(String parentFolder, String fileName) {
    try {
      String path = "/" + parentFolder + "/" + fileName;
      String utfDecodedFilePath = URLDecoder.decode(TestUtils.class.getResource(path).getFile(),
          InternalZipConstants.CHARSET_UTF_8.toString());
      return new File(utfDecodedFilePath);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}
