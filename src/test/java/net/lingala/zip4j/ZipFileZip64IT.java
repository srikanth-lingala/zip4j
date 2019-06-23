package net.lingala.zip4j;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.headers.HeaderReader;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.RandomAccessFileMode;
import net.lingala.zip4j.util.InternalZipConstants;
import net.lingala.zip4j.utils.SlowTests;
import net.lingala.zip4j.utils.ZipFileVerifier;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTests.class)
public class ZipFileZip64IT extends AbstractIT {

  private Random random = new Random();

  @Test
  public void testZip64WithSingleLargeZipEntry() throws IOException, ZipException {
    long entrySize = InternalZipConstants.ZIP_64_SIZE_LIMIT + 1;

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    zipParameters.setEntrySize(entrySize);

    createZip64FileWithEntries(1, entrySize, zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, 1, false);
    verifyZip64HeadersPresent();
  }

  @Test
  public void testZip64WithCentralDirectoryOffsetGreaterThanZip64Limit() throws IOException, ZipException {
    long eachEntrySize = (InternalZipConstants.ZIP_64_SIZE_LIMIT / 2) + 100;

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setEntrySize(eachEntrySize);

    createZip64FileWithEntries(3, eachEntrySize, zipParameters);

    ZipFileVerifier.verifyZipFileByExtractingAllFiles(generatedZipFile, null, outputFolder, 3, false);
    verifyZip64HeadersPresent();
  }

  @Test
  public void testZip64WithNumberOfEntriesGreaterThan70k() throws IOException, ZipException {
    long eachEntrySize = 100;

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setEntrySize(eachEntrySize);

    createZip64FileWithEntries(70000, eachEntrySize, zipParameters);

    ZipFile zipFile = new ZipFile(generatedZipFile);
    assertThat(zipFile.getFileHeaders()).hasSize(70000);
    verifyZip64HeadersPresent();
  }

  private void verifyZip64HeadersPresent() throws FileNotFoundException, ZipException {
    HeaderReader headerReader = new HeaderReader();
    ZipModel zipModel = headerReader.readAllHeaders(new RandomAccessFile(generatedZipFile,
        RandomAccessFileMode.READ.getValue()));
    assertThat(zipModel.getZip64EndOfCentralDirectoryLocator()).isNotNull();
    assertThat(zipModel.getZip64EndOfCentralDirectoryRecord()).isNotNull();
    assertThat(zipModel.isZip64Format()).isTrue();
  }

  private void createZip64FileWithEntries(int numberOfEntries, long eachEntrySize, ZipParameters zipParameters)
      throws IOException {

    try(ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(generatedZipFile))) {
      for (int i = 0; i < numberOfEntries; i++) {
        zipParameters.setFileNameInZip("FILE_" + i);
        zipOutputStream.putNextEntry(zipParameters);

        long numberOfBytesWritten = 0;
        byte[] b = new byte[4096];

        while (numberOfBytesWritten < eachEntrySize) {
          random.nextBytes(b);
          int writeLength = b.length;

          if (writeLength + numberOfBytesWritten > eachEntrySize) {
            writeLength = (int) (eachEntrySize - numberOfBytesWritten);
          }

          zipOutputStream.write(b, 0, writeLength);
          numberOfBytesWritten += writeLength;
        }
        zipOutputStream.closeEntry();
      }
    }
  }

}
