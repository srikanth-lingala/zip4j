package net.lingala.zip4j.utils;

import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.util.InternalZipConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

import static net.lingala.zip4j.util.BitUtils.isBitSet;
import static org.assertj.core.api.Assertions.assertThat;

public class ZipVerifier {

  public static void verifyZipFile(File generatedZipFile) throws IOException {
    verifyZipFile(generatedZipFile, null);
  }

  public static void verifyZipFile(File generatedZipFile, char[] password) throws IOException {
    assertThat(generatedZipFile).isNotNull();

    try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(generatedZipFile), password)) {
      byte[] b = new byte[InternalZipConstants.BUFF_SIZE];
      LocalFileHeader localFileHeader;
      long bytesWritten = 0;
      int readLen;
      while ((localFileHeader = zipInputStream.getNextEntry()) != null) {
        bytesWritten = 0;
        while ((readLen = zipInputStream.read(b)) != -1) {
          bytesWritten += readLen;
        }
        assertThat(bytesWritten).isEqualTo(getUncompressedSize(localFileHeader));
      }
    }
  }

  public static void verifyFileContent(File sourceFile, File extractedFile) throws IOException {
    assertThat(extractedFile.length()).isEqualTo(sourceFile.length());

    byte[] sourceFileContent = Files.readAllBytes(sourceFile.toPath());
    byte[] extractedFileContent = Files.readAllBytes(extractedFile.toPath());

    assertThat(extractedFileContent).as("Files do not match for file name: " + extractedFile.getName())
        .isEqualTo(sourceFileContent);
  }

  private static long getUncompressedSize(LocalFileHeader localFileHeader) {
    if (localFileHeader.getZip64ExtendedInfo() != null && !isBitSet(localFileHeader.getGeneralPurposeFlag()[0], 3)) {
      return localFileHeader.getZip64ExtendedInfo().getUncompressedSize();
    }
    return localFileHeader.getUncompressedSize();
  }
}
