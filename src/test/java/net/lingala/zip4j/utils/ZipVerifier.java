package net.lingala.zip4j.utils;

import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.util.InternalZipConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

public class ZipVerifier {

  public static void verifyZipFile(File generatedZipFile) throws IOException {
    verifyZipFile(generatedZipFile, null);
  }

  public static void verifyZipFile(File generatedZipFile, char[] password) throws IOException {
    assertThat(generatedZipFile).isNotNull();

    try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(generatedZipFile), password)) {
      byte[] b = new byte[InternalZipConstants.BUFF_SIZE];
      while (zipInputStream.getNextEntry() != null) {
        while (zipInputStream.read(b) != -1) {
          // Do nothing
        }
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

}
