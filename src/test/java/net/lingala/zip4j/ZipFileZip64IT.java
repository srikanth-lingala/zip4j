package net.lingala.zip4j;

import net.lingala.zip4j.headers.HeaderReader;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.RandomAccessFileMode;
import net.lingala.zip4j.testutils.RandomInputStream;
import net.lingala.zip4j.testutils.SlowTests;
import net.lingala.zip4j.testutils.ZipFileVerifier;
import net.lingala.zip4j.util.InternalZipConstants;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.lingala.zip4j.testutils.HeaderVerifier.verifyFileHeadersDoesNotExist;
import static net.lingala.zip4j.testutils.HeaderVerifier.verifyFileHeadersExist;
import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTests.class)
public class ZipFileZip64IT extends AbstractIT {

  private byte[] readBuffer = new byte[4096];

  @Test
  public void testZip64WithSingleLargeZipEntry() throws IOException {
    long entrySize = InternalZipConstants.ZIP_64_SIZE_LIMIT + 1;

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    zipParameters.setEntrySize(entrySize);

    createZip64FileWithEntries(1, entrySize, zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, 1, false);
    verifyZip64HeadersPresent();
  }

  @Test
  public void testZip64WithCentralDirectoryOffsetGreaterThanZip64Limit() throws IOException {
    long eachEntrySize = (InternalZipConstants.ZIP_64_SIZE_LIMIT / 2) + 100;

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setEntrySize(eachEntrySize);

    createZip64FileWithEntries(3, eachEntrySize, zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, 3, false);
    verifyZip64HeadersPresent();
  }

  @Test
  public void testZip64WithNumberOfEntriesGreaterThan70k() throws IOException {
    long eachEntrySize = 100;

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setEntrySize(eachEntrySize);

    createZip64FileWithEntries(70000, eachEntrySize, zipParameters);

    ZipFile zipFile = new ZipFile(generatedZipFile);
    assertThat(zipFile.getFileHeaders()).hasSize(70000);
    verifyZip64HeadersPresent();
  }

  @Test
  public void testZip64RenameFiles() throws IOException {
    long eachEntrySize = (InternalZipConstants.ZIP_64_SIZE_LIMIT / 2) + 100;

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setEntrySize(eachEntrySize);

    createZip64FileWithEntries(3, eachEntrySize, zipParameters);

    ZipFile zipFile = new ZipFile(generatedZipFile);
    Map<String, String> fileNamesMap = new HashMap<>();
    fileNamesMap.put("FILE_1", "NEW_FILE_1");
    fileNamesMap.put("FILE_2", "NEW_FILE_2");
    zipFile.renameFiles(fileNamesMap);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, 3, false);
    verifyZip64HeadersPresent();
    verifyFileHeadersDoesNotExist(zipFile, fileNamesMap.keySet());
    verifyFileHeadersExist(zipFile, fileNamesMap.values());
  }

  @Test
  public void testZip64RemoveFiles() throws IOException {
    long eachEntrySize = (InternalZipConstants.ZIP_64_SIZE_LIMIT / 2) + 100;

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setEntrySize(eachEntrySize);

    createZip64FileWithEntries(3, eachEntrySize, zipParameters);

    ZipFile zipFile = new ZipFile(generatedZipFile);
    List<String> filesToRemove = Collections.singletonList("FILE_1");
    zipFile.removeFiles(filesToRemove);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, 2, false);
    verifyZip64HeadersPresent();
    verifyFileHeadersDoesNotExist(zipFile, filesToRemove);
  }

  private void verifyZip64HeadersPresent() throws IOException {
    HeaderReader headerReader = new HeaderReader();
    ZipModel zipModel = headerReader.readAllHeaders(new RandomAccessFile(generatedZipFile,
        RandomAccessFileMode.READ.getValue()), InternalZipConstants.CHARSET_UTF_8);
    assertThat(zipModel.getZip64EndOfCentralDirectoryLocator()).isNotNull();
    assertThat(zipModel.getZip64EndOfCentralDirectoryRecord()).isNotNull();
    assertThat(zipModel.isZip64Format()).isTrue();
  }

  private void createZip64FileWithEntries(int numberOfEntries, long eachEntrySize, ZipParameters zipParameters)
      throws IOException {

    int readLen;
    try(ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(generatedZipFile))) {
      for (int i = 0; i < numberOfEntries; i++) {
        zipParameters.setFileNameInZip("FILE_" + i);
        zipOutputStream.putNextEntry(zipParameters);

        try(InputStream inputStream = new RandomInputStream(eachEntrySize)) {
          while ((readLen = inputStream.read(readBuffer)) != -1) {
            zipOutputStream.write(readBuffer, 0, readLen);
          }
        }
        zipOutputStream.closeEntry();
      }
    }
  }
}
