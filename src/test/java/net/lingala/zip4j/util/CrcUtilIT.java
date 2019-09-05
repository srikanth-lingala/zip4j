package net.lingala.zip4j.util;

import net.lingala.zip4j.AbstractIT;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.progress.ProgressMonitor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;

import static net.lingala.zip4j.testutils.TestUtils.getTestFileFromResources;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CrcUtilIT extends AbstractIT {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ProgressMonitor progressMonitor = new ProgressMonitor();

  @Test
  public void testComputeFileCrcThrowsExceptionWhenFileIsNull() throws IOException {
    expectedException.expectMessage("input file is null or does not exist or cannot read. " +
        "Cannot calculate CRC for the file");
    expectedException.expect(ZipException.class);

    CrcUtil.computeFileCrc(null, progressMonitor);
  }

  @Test
  public void testComputeFileCrcThrowsExceptionWhenCannotReadFile() throws IOException {
    expectedException.expectMessage("input file is null or does not exist or cannot read. " +
        "Cannot calculate CRC for the file");
    expectedException.expect(ZipException.class);

    File unreadableFile = mock(File.class);
    when(unreadableFile.exists()).thenReturn(true);
    when(unreadableFile.canRead()).thenReturn(false);
    CrcUtil.computeFileCrc(unreadableFile, progressMonitor);
  }

  @Test
  public void testComputeFileCrcThrowsExceptionWhenFileDoesNotExist() throws IOException {
    expectedException.expectMessage("input file is null or does not exist or cannot read. " +
        "Cannot calculate CRC for the file");
    expectedException.expect(ZipException.class);

    CrcUtil.computeFileCrc(new File("DoesNotExist"), progressMonitor);
  }

  @Test
  public void testComputeFileCrcGetsValueSuccessfully() throws IOException {
    testComputeFileCrcForFile(getTestFileFromResources("sample.pdf"));
    testComputeFileCrcForFile(getTestFileFromResources("sample_text1.txt"));
    testComputeFileCrcForFile(getTestFileFromResources("sample_text_large.txt"));
  }

  private void testComputeFileCrcForFile(File file) throws IOException {
    long actualFileCrc = calculateFileCrc(file);
    assertThat(CrcUtil.computeFileCrc(file, progressMonitor)).isEqualTo(actualFileCrc);
  }

  private long calculateFileCrc(File file) throws IOException {
    try(InputStream inputStream = new FileInputStream(file)) {
      byte[] buffer = new byte[InternalZipConstants.BUFF_SIZE];
      int readLen = -1;
      CRC32 crc32 = new CRC32();
      while((readLen = inputStream.read(buffer)) != -1) {
        crc32.update(buffer, 0, readLen);
      }
      return crc32.getValue();
    }
  }
}