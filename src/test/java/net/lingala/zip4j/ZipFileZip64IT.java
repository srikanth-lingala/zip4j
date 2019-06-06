package net.lingala.zip4j;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.headers.HeaderReader;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.RandomAccessFileMode;
import net.lingala.zip4j.util.InternalZipConstants;
import net.lingala.zip4j.utils.AbstractIT;
import net.lingala.zip4j.utils.SlowTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import static net.lingala.zip4j.utils.ZipVerifier.verifyZipFile;
import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTests.class)
public class ZipFileZip64IT extends AbstractIT {

  @Test
  public void testReadZip64WithSingleLargeZipEntry() throws IOException, ZipException {
    long entrySize = InternalZipConstants.ZIP_64_LIMIT + 1;

    ZipParameters zipParameters = new ZipParameters();
    zipParameters.setFileNameInZip("single_large_entry.txt");
    zipParameters.setCompressionMethod(CompressionMethod.STORE);
    zipParameters.setUncompressedSize(entrySize);

    createZip64FileWithSingleFile(entrySize, zipParameters);

    verifyZipFile(generatedZipFile);
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

  private void createZip64FileWithSingleFile(long entrySize, ZipParameters zipParameters) throws IOException {
    try(ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(generatedZipFile))) {
      zipOutputStream.putNextEntry(zipParameters);

      long numberOfBytesWritten = 0;
      byte[] b = new byte[4096];

      while (numberOfBytesWritten < entrySize) {
        int writeLength = b.length;

        if (writeLength + numberOfBytesWritten > entrySize) {
          writeLength = (int) (entrySize - numberOfBytesWritten);
        }

        zipOutputStream.write(b, 0, writeLength);
        numberOfBytesWritten += b.length;
      }
      zipOutputStream.closeEntry();
    }
  }

}
