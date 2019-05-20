package net.lingala.zip4j.io.inputstream;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.utils.AbstractIT;
import net.lingala.zip4j.zip.AesKeyStrength;
import net.lingala.zip4j.zip.CompressionMethod;
import net.lingala.zip4j.zip.EncryptionMethod;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static net.lingala.zip4j.TestUtils.getFileFromResources;
import static net.lingala.zip4j.utils.ZipVerifier.verifyFileContent;
import static org.assertj.core.api.Assertions.assertThat;

public class ZipInputStreamIT extends AbstractIT {

  @Test
  public void testExtractStoreWithoutEncryption() throws IOException, ZipException {
    File createdZipFile = createZipFile(CompressionMethod.STORE, FILES_TO_ADD);
    extractZipFileWithInputStreams(createdZipFile, null);
  }

  @Test
  public void testExtractStoreWithZipStandardEncryption() throws IOException, ZipException {
    File createdZipFile = createZipFile(CompressionMethod.STORE, true, EncryptionMethod.ZIP_STANDARD, null, PASSWORD, FILES_TO_ADD);
    extractZipFileWithInputStreams(createdZipFile, PASSWORD);
  }

  @Test
  public void testExtractStoreWithAesEncryption128() throws IOException, ZipException {
    File createdZipFile = createZipFile(CompressionMethod.STORE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128, PASSWORD, FILES_TO_ADD);
    extractZipFileWithInputStreams(createdZipFile, PASSWORD);
  }

  @Test
  public void testExtractStoreWithAesEncryption256() throws IOException, ZipException {
    File createdZipFile = createZipFile(CompressionMethod.STORE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256, PASSWORD, FILES_TO_ADD);
    extractZipFileWithInputStreams(createdZipFile, PASSWORD);
  }

  @Test
  public void testExtractDeflateWithoutEncryption() throws IOException, ZipException {
    File createdZipFile = createZipFile(CompressionMethod.DEFLATE, FILES_TO_ADD);
    extractZipFileWithInputStreams(createdZipFile, null);
  }

  @Test
  public void testExtractDeflateWithAesEncryption128() throws IOException, ZipException {
    File createdZipFile = createZipFile(CompressionMethod.DEFLATE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_128, PASSWORD, FILES_TO_ADD);
    extractZipFileWithInputStreams(createdZipFile, PASSWORD);
  }

  @Test
  public void testExtractDeflateWithAesEncryption256() throws IOException, ZipException {
    File createdZipFile = createZipFile(CompressionMethod.DEFLATE, true, EncryptionMethod.AES, AesKeyStrength.KEY_STRENGTH_256, PASSWORD, FILES_TO_ADD);
    extractZipFileWithInputStreams(createdZipFile, PASSWORD);
  }

  private void extractZipFileWithInputStreams(File zipFile, char[] password) throws IOException, ZipException {
    LocalFileHeader localFileHeader;
    int readLen;
    byte[] readBuffer = new byte[4096];
    int numberOfEntriesExtracted = 0;

    try (FileInputStream fileInputStream = new FileInputStream(zipFile)) {
      try (ZipInputStream zipInputStream = new ZipInputStream(fileInputStream, password)) {
        while ((localFileHeader = zipInputStream.getNextEntry()) != null) {
          File extractedFile = temporaryFolder.newFile(localFileHeader.getFileName());
          try (OutputStream outputStream = new FileOutputStream(extractedFile)) {
            while ((readLen = zipInputStream.read(readBuffer)) != -1) {
              outputStream.write(readBuffer, 0, readLen);
            }
          }
          verifyFileContent(getFileFromResources(localFileHeader.getFileName()), extractedFile);
          numberOfEntriesExtracted++;
        }
      }
    }

    assertThat(numberOfEntriesExtracted).isEqualTo(FILES_TO_ADD.size());
  }

  private File createZipFile(CompressionMethod compressionMethod, List<File> filesToAdd) throws IOException, ZipException {
    return createZipFile(compressionMethod, false, null, null, null, filesToAdd);
  }

  private File createZipFile(CompressionMethod compressionMethod, boolean encryptFiles,
                             EncryptionMethod encryptionMethod, AesKeyStrength aesKeyStrength, char[] password,
                             List<File> filesToAdd) throws IOException, ZipException {

    File outputFile = temporaryFolder.newFile("output.zip");
    deleteFileIfExists(outputFile);

    ZipFile zipFile = new ZipFile(outputFile, password);

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(compressionMethod);
    zipParameters.setEncryptFiles(encryptFiles);
    zipParameters.setEncryptionMethod(encryptionMethod);
    zipParameters.setAesKeyStrength(aesKeyStrength);

    zipFile.createZipFile(filesToAdd, zipParameters);

    return outputFile;
  }

  private void deleteFileIfExists(File file) {
    if (file.exists()) {
      if (!file.delete()) {
        throw new RuntimeException("Could not delete an existing zip file");
      }
    }
  }
}