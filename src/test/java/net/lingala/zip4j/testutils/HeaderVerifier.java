package net.lingala.zip4j.testutils;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.headers.HeaderReader;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.util.InternalZipConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

public class HeaderVerifier {

  private static HeaderReader headerReader = new HeaderReader();

  public static void verifyLocalFileHeaderUncompressedSize(File generatedZipFile, String fileNameInZipToVerify,
                                                    long expectedUncompressedSize) throws IOException {

    LocalFileHeader localFileHeader = getLocalFileHeaderForEntry(generatedZipFile, fileNameInZipToVerify);
    assertThat(localFileHeader.getUncompressedSize()).isEqualTo(expectedUncompressedSize);
  }

  public static void verifyFileHeadersExist(ZipFile zipFile, Collection<String> fileNamesToVerify) throws IOException {
    for (String fileNameToVerify : fileNamesToVerify) {
      assertThat(zipFile.getFileHeader(fileNameToVerify)).isNotNull();
    }
  }

  public static void verifyFileHeadersDoesNotExist(ZipFile zipFile, Collection<String> fileNamesToVerify) throws IOException {
    for (String fileNameToVerify : fileNamesToVerify) {
      assertThat(zipFile.getFileHeader(fileNameToVerify)).isNull();
    }
  }

  public static void verifyZipFileDoesNotContainFolders(ZipFile zipFile, Collection<String> folderNames) throws IOException {
    for (FileHeader fileHeader : zipFile.getFileHeaders()) {
      folderNames.forEach(e -> assertThat(fileHeader.getFileName().startsWith(e)).isFalse());
    }
  }

  private static LocalFileHeader getLocalFileHeaderForEntry(File generatedZipFile, String fileNameInZipToVerify)
      throws IOException {

    InputStream inputStream = positionRandomAccessFileToLocalFileHeaderStart(generatedZipFile,
        fileNameInZipToVerify);
    return headerReader.readLocalFileHeader(inputStream, InternalZipConstants.CHARSET_UTF_8);
  }

  private static InputStream positionRandomAccessFileToLocalFileHeaderStart(File generatedZipFile, String fileNameInZip)
      throws IOException{

    ZipFile zipFile = new ZipFile(generatedZipFile);
    FileHeader fileHeader = zipFile.getFileHeader(fileNameInZip);

    if (fileHeader == null) {
      throw new RuntimeException("Cannot find an entry with name: " + fileNameInZip + " in zip file: "
          + generatedZipFile);
    }

    InputStream inputStream = new FileInputStream(generatedZipFile);
    if (inputStream.skip(fileHeader.getOffsetLocalHeader()) != fileHeader.getOffsetLocalHeader()) {
      throw new IOException("Cannot skip " + fileHeader.getOffsetLocalHeader() + " bytes for entry "
          + fileHeader.getFileName());
    }
    return inputStream;
  }
}
