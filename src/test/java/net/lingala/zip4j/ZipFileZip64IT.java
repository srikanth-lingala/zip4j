package net.lingala.zip4j;

import net.lingala.zip4j.headers.HeaderReader;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.RandomAccessFileMode;
import net.lingala.zip4j.testutils.HeaderVerifier;
import net.lingala.zip4j.testutils.RandomInputStream;
import net.lingala.zip4j.testutils.SlowTests;
import net.lingala.zip4j.testutils.TestUtils;
import net.lingala.zip4j.testutils.ZipFileVerifier;
import net.lingala.zip4j.util.InternalZipConstants;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTests.class)
public class ZipFileZip64IT extends AbstractIT {

  private byte[] readBuffer = new byte[2 * InternalZipConstants.BUFF_SIZE];

  @Test
  public void testZip64WithSingleLargeZipEntry() throws IOException {
    long entrySize = InternalZipConstants.ZIP_64_SIZE_LIMIT + 1;

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    zipParameters.setEntrySize(entrySize);

    createZip64FileWithEntries(1, entrySize, zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, 1, false);
    verifyZip64HeadersPresent();

    cleanupOutputFolder();

    ZipFile zipFile = new ZipFile(generatedZipFile);
    zipFile.renameFile("FILE_0", "NEW_FILE_0");

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, 1, false);
    verifyZip64HeadersPresent();
    HeaderVerifier.verifyFileHeadersExist(zipFile, Collections.singletonList("NEW_FILE_0"));
    HeaderVerifier.verifyFileHeadersDoesNotExist(zipFile, Collections.singletonList("FILE_0"));

    cleanupOutputFolder();

    zipFile = new ZipFile(generatedZipFile);
    zipFile.removeFile("NEW_FILE_0");

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, 0, false);
    HeaderVerifier.verifyFileHeadersDoesNotExist(zipFile, Collections.singletonList("NEW_FILE_0"));
  }

  @Test
  public void testZip64WithCentralDirectoryOffsetGreaterThanZip64Limit() throws IOException {
    long eachEntrySize = (InternalZipConstants.ZIP_64_SIZE_LIMIT / 2) + 100;

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setEntrySize(eachEntrySize);

    createZip64FileWithEntries(3, eachEntrySize, zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, 3, false);
    verifyZip64HeadersPresent();

    cleanupOutputFolder();

    ZipFile zipFile = new ZipFile(generatedZipFile);
    Map<String, String> fileNamesMap = new HashMap<>();
    fileNamesMap.put("FILE_1", "NEW_FILE_1");
    fileNamesMap.put("FILE_2", "NEW_FILE_2");
    zipFile.renameFiles(fileNamesMap);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, 3, false);
    verifyZip64HeadersPresent();
    HeaderVerifier.verifyFileHeadersExist(zipFile, fileNamesMap.values());
    HeaderVerifier.verifyFileHeadersDoesNotExist(zipFile, fileNamesMap.keySet());

    cleanupOutputFolder();

    zipFile = new ZipFile(generatedZipFile);
    List<String> filesToRemove = new ArrayList<>();
    filesToRemove.add("FILE_0");
    filesToRemove.add("NEW_FILE_2");
    zipFile.removeFiles(filesToRemove);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, 1, false);
    HeaderVerifier.verifyFileHeadersDoesNotExist(zipFile, filesToRemove);
    HeaderVerifier.verifyFileHeadersExist(zipFile, Collections.singleton("NEW_FILE_1"));
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

    zipFile = new ZipFile(generatedZipFile);
    Map<String, String> fileNamesMap = new HashMap<>();
    fileNamesMap.put("FILE_10", "NEW_FILE_10");
    fileNamesMap.put("FILE_20", "NEW_FILE_20");
    fileNamesMap.put("FILE_30", "NEW_FILE_30");
    zipFile.renameFiles(fileNamesMap);

    verifyZip64HeadersPresent();
    HeaderVerifier.verifyFileHeadersExist(zipFile, fileNamesMap.values());
    HeaderVerifier.verifyFileHeadersDoesNotExist(zipFile, fileNamesMap.keySet());

    zipFile = new ZipFile(generatedZipFile);
    List<String> filesToRemove = new ArrayList<>();
    filesToRemove.add("FILE_0");
    filesToRemove.add("NEW_FILE_10");
    filesToRemove.add("NEW_FILE_30");
    zipFile.removeFiles(filesToRemove);

    HeaderVerifier.verifyFileHeadersDoesNotExist(zipFile, filesToRemove);
  }

  @Test
  public void testZip64WhenAddingFilesWithNewlyInstantiatedZipFile() throws IOException {
    File testFileToAdd = TestUtils.generateFileOfSize(temporaryFolder, 1073741824); // 1 GB
    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);

    for (int i = 0; i < 6; i++) {
      zipParameters.setFileNameInZip(Integer.toString(i));
      new ZipFile(generatedZipFile).addFile(testFileToAdd, zipParameters);
    }

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, 6, false);
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
