package net.lingala.zip4j.util;

import net.lingala.zip4j.AbstractIT;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.enums.RandomAccessFileMode;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.utils.TestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import static org.assertj.core.api.Assertions.assertThat;

public class FileUtilsIT extends AbstractIT {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ProgressMonitor progressMonitor = new ProgressMonitor();

  @Test
  public void testCopyFileThrowsExceptionWhenStartsIsLessThanZero() throws IOException, ZipException {
    testInvalidOffsetsScenario(-1, 100);
  }

  @Test
  public void testCopyFileThrowsExceptionWhenEndIsLessThanZero() throws IOException, ZipException {
    testInvalidOffsetsScenario(0, -1);
  }

  @Test
  public void testCopyFileThrowsExceptionWhenStartIsGreaterThanEnd() throws IOException, ZipException {
    testInvalidOffsetsScenario(300, 100);
  }

  @Test
  public void testCopyFilesWhenStartIsSameAsEndDoesNothing() throws IOException, ZipException {
    File sourceFile = TestUtils.getTestFileFromResources("sample.pdf");
    File outputFile = temporaryFolder.newFile();
    try(RandomAccessFile randomAccessFile = new RandomAccessFile(sourceFile, RandomAccessFileMode.READ.getValue());
        OutputStream outputStream = new FileOutputStream(outputFile)) {
      FileUtils.copyFile(randomAccessFile, outputStream, 100, 100, progressMonitor);
    }

    assertThat(outputFile.exists()).isTrue();
    assertThat(outputFile.length()).isZero();
  }

  @Test
  public void testCopyFilesCopiesCompleteFile() throws IOException, ZipException {
    File sourceFile = TestUtils.getTestFileFromResources("sample.pdf");
    File outputFile = temporaryFolder.newFile();
    try(RandomAccessFile randomAccessFile = new RandomAccessFile(sourceFile, RandomAccessFileMode.READ.getValue());
        OutputStream outputStream = new FileOutputStream(outputFile)) {
      FileUtils.copyFile(randomAccessFile, outputStream, 0, sourceFile.length(), progressMonitor);
    }

    assertThat(outputFile.length()).isEqualTo(sourceFile.length());
  }

  @Test
  public void testCopyFilesCopiesPartOfFile() throws IOException, ZipException {
    File sourceFile = TestUtils.getTestFileFromResources("sample.pdf");
    File outputFile = temporaryFolder.newFile();
    try(RandomAccessFile randomAccessFile = new RandomAccessFile(sourceFile, RandomAccessFileMode.READ.getValue());
        OutputStream outputStream = new FileOutputStream(outputFile)) {
      FileUtils.copyFile(randomAccessFile, outputStream, 500, 800, progressMonitor);
    }

    assertThat(outputFile.length()).isEqualTo(300);
  }

  private void testInvalidOffsetsScenario(int start, int offset) throws IOException, ZipException {
    expectedException.expectMessage("invalid offsets");
    expectedException.expect(ZipException.class);

    File sourceFile = TestUtils.getTestFileFromResources("sample.pdf");
    try(RandomAccessFile randomAccessFile = new RandomAccessFile(sourceFile, RandomAccessFileMode.READ.getValue());
        OutputStream outputStream = new FileOutputStream(temporaryFolder.newFile())) {
      FileUtils.copyFile(randomAccessFile, outputStream, start, offset, progressMonitor);
    }
  }
}