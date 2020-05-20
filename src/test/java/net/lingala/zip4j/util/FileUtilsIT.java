package net.lingala.zip4j.util;

import net.lingala.zip4j.AbstractIT;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.enums.RandomAccessFileMode;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.testutils.TestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FileUtilsIT extends AbstractIT {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ProgressMonitor progressMonitor = new ProgressMonitor();

  @Test
  public void testCopyFileThrowsExceptionWhenStartsIsLessThanZero() throws IOException {
    testInvalidOffsetsScenario(-1, 100);
  }

  @Test
  public void testCopyFileThrowsExceptionWhenEndIsLessThanZero() throws IOException {
    testInvalidOffsetsScenario(0, -1);
  }

  @Test
  public void testCopyFileThrowsExceptionWhenStartIsGreaterThanEnd() throws IOException {
    testInvalidOffsetsScenario(300, 100);
  }

  @Test
  public void testCopyFilesWhenStartIsSameAsEndDoesNothing() throws IOException {
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
  public void testCopyFilesCopiesCompleteFile() throws IOException {
    File sourceFile = TestUtils.getTestFileFromResources("sample.pdf");
    File outputFile = temporaryFolder.newFile();
    try(RandomAccessFile randomAccessFile = new RandomAccessFile(sourceFile, RandomAccessFileMode.READ.getValue());
        OutputStream outputStream = new FileOutputStream(outputFile)) {
      FileUtils.copyFile(randomAccessFile, outputStream, 0, sourceFile.length(), progressMonitor);
    }

    assertThat(outputFile.length()).isEqualTo(sourceFile.length());
  }

  @Test
  public void testCopyFilesCopiesPartOfFile() throws IOException {
    File sourceFile = TestUtils.getTestFileFromResources("sample.pdf");
    File outputFile = temporaryFolder.newFile();
    try(RandomAccessFile randomAccessFile = new RandomAccessFile(sourceFile, RandomAccessFileMode.READ.getValue());
        OutputStream outputStream = new FileOutputStream(outputFile)) {
      FileUtils.copyFile(randomAccessFile, outputStream, 500, 800, progressMonitor);
    }

    assertThat(outputFile.length()).isEqualTo(300);
  }

  @Test
  public void testGetAllSortedNumberedSplitFilesReturnsEmptyForNoFiles() throws IOException {
    File file = temporaryFolder.newFile("somename");
    assertThat(FileUtils.getAllSortedNumberedSplitFiles(file)).isEmpty();
  }

  @Test
  public void testGetAllSortedNumberedSplitFilesReturnsSortedList() throws IOException {
    File file001 = temporaryFolder.newFile("somename.zip.001");
    File file003 = temporaryFolder.newFile("somename.zip.003");
    File file002 = temporaryFolder.newFile("somename.zip.002");
    File file006 = temporaryFolder.newFile("somename.zip.006");
    File file005 = temporaryFolder.newFile("somename.zip.005");
    File file004 = temporaryFolder.newFile("somename.zip.004");

    File[] sortedList = FileUtils.getAllSortedNumberedSplitFiles(file001);

    assertThat(sortedList).containsExactly(file001, file002, file003, file004, file005, file006);
  }

  @Test
  public void testIsSymbolicLinkReturnsFalseWhenNotALink() throws IOException {
    File targetFile = temporaryFolder.newFile("target.file");
    assertThat(FileUtils.isSymbolicLink(targetFile)).isFalse();
  }

  @Test
  public void testIsSymbolicLinkReturnsTrueForALink() throws IOException {
    Path targetFile = temporaryFolder.newFile("target.file").toPath();
    Path linkFile = Paths.get(temporaryFolder.getRoot().getAbsolutePath(), "source.link");
    Files.createSymbolicLink(linkFile, targetFile);

    assertThat(FileUtils.isSymbolicLink(linkFile.toFile()));
  }

  @Test
  public void testGetFilesInDirectoryRecursiveWithExcludeFileFilter() throws IOException {
    File rootFolder = TestUtils.getTestFileFromResources("");
    List<File> filesToExclude = Arrays.asList(
        TestUtils.getTestFileFromResources("бореиская.txt"),
        TestUtils.getTestFileFromResources("sample_directory/favicon.ico")
    );
    List<File> allFiles = FileUtils.getFilesInDirectoryRecursive(rootFolder, true, true, filesToExclude::contains);

    assertThat(allFiles).hasSize(10);
    for (File file : allFiles) {
      assertThat(filesToExclude).doesNotContain(file);
    }
  }

  private void testInvalidOffsetsScenario(int start, int offset) throws IOException {
    expectedException.expectMessage("invalid offsets");
    expectedException.expect(ZipException.class);

    File sourceFile = TestUtils.getTestFileFromResources("sample.pdf");
    try(RandomAccessFile randomAccessFile = new RandomAccessFile(sourceFile, RandomAccessFileMode.READ.getValue());
        OutputStream outputStream = new FileOutputStream(temporaryFolder.newFile())) {
      FileUtils.copyFile(randomAccessFile, outputStream, start, offset, progressMonitor);
    }
  }
}