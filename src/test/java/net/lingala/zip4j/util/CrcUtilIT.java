package net.lingala.zip4j.util;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.utils.AbstractIT;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;

import static net.lingala.zip4j.TestUtils.getFileFromResources;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CrcUtilIT extends AbstractIT {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CrcUtil crcUtil = new CrcUtil();
  private ProgressMonitor progressMonitor = new ProgressMonitor();

  @Test
  public void testComputeFileCrcThrowsExceptionWhenFileIsNull() throws ZipException {
    expectedException.expectMessage("input file is null or does not exist or cannot read. " +
        "Cannot calculate CRC for the file");
    expectedException.expect(ZipException.class);

    CrcUtil.computeFileCrc(null, progressMonitor);
  }

  @Test
  public void testComputeFileCrcThrowsExceptionWhenCannotReadFile() throws ZipException {
    expectedException.expectMessage("input file is null or does not exist or cannot read. " +
        "Cannot calculate CRC for the file");
    expectedException.expect(ZipException.class);

    File unreadableFile = mock(File.class);
    when(unreadableFile.exists()).thenReturn(true);
    when(unreadableFile.canRead()).thenReturn(false);
    CrcUtil.computeFileCrc(unreadableFile, progressMonitor);
  }

  @Test
  public void testComputeFileCrcThrowsExceptionWhenFileDoesNotExist() throws ZipException {
    expectedException.expectMessage("input file is null or does not exist or cannot read. " +
        "Cannot calculate CRC for the file");
    expectedException.expect(ZipException.class);

    CrcUtil.computeFileCrc(new File("DoesNotExist"), progressMonitor);
  }

  @Test
  public void testComputeFileCrcGetsValueSuccessfully() throws ZipException {
    assertThat(CrcUtil.computeFileCrc(getFileFromResources("sample.pdf"), progressMonitor)).isEqualTo(2730662664L);
    assertThat(CrcUtil.computeFileCrc(getFileFromResources("sample_text1.txt"), progressMonitor))
        .isEqualTo(3543527034L);
    assertThat(CrcUtil.computeFileCrc(getFileFromResources("sample_text_large.txt"), progressMonitor))
        .isEqualTo(4081718931L);
  }
}