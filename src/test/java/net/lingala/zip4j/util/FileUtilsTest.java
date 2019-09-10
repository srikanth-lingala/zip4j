package net.lingala.zip4j.util;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipModel;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FileUtilsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testLastModifiedFileTimeWhenTimeIsLessThanZeroDoesNothing() {
    FileUtils.setFileLastModifiedTime(mock(Path.class), -1);
  }

  @Test
  public void testLastModifiedFileTimeWhenTimeIEqualToZeroDoesNothing() {
    FileUtils.setFileLastModifiedTime(mock(Path.class), 0);
  }

  @Test
  public void testLastModifiedFileTimeWhenFileDoesNotExistDoesNothing() throws IOException {
    Path path = mock(Path.class);
    FileSystemProvider fileSystemProvider = mockPath(path);
    doThrow(new IOException("Some exception")).when(fileSystemProvider).checkAccess(path);

    FileUtils.setFileLastModifiedTime(path, 1);
  }

  @Test
  public void testLastModifiedFileTimeForValidTimeSetsTime() throws IOException {
    Path path = mock(Path.class);
    FileSystemProvider fileSystemProvider = mockPath(path);
    BasicFileAttributeView basicFileAttributeView = mock(BasicFileAttributeView.class);
    when(fileSystemProvider.getFileAttributeView(path, BasicFileAttributeView.class))
        .thenReturn(basicFileAttributeView);

    long currentTime = System.currentTimeMillis();
    FileUtils.setFileLastModifiedTime(path, currentTime);

    verify(basicFileAttributeView).setTimes(FileTime.fromMillis(Zip4jUtil.dosToJavaTme(currentTime)), null, null);
  }

  @Test
  public void testLastModifiedFileTimeWhenIOExceptionDoesNothing() {
    Path path = mock(Path.class);
    FileSystemProvider fileSystemProvider = mockPath(path);
    when(fileSystemProvider.getFileAttributeView(path, BasicFileAttributeView.class))
        .thenThrow(new RuntimeException());

    long currentTime = System.currentTimeMillis();
    FileUtils.setFileLastModifiedTime(path, currentTime);
  }

  @Test
  public void testLastModifiedFileTimeWithoutNIOSetsSuccessfully() {
    File file = mock(File.class);
    long currentTime = System.currentTimeMillis();

    FileUtils.setFileLastModifiedTimeWithoutNio(file, currentTime);

    verify(file).setLastModified(Zip4jUtil.dosToJavaTme(currentTime));
  }

  @Test
  public void testGetFileAttributesReturnsEmptyBytesWhenNIONotSupported() {
    File file = mock(File.class);
    when(file.toPath()).thenThrow(new NoSuchMethodError("No method"));

    byte[] fileAttributes = FileUtils.getFileAttributes(file);
    assertThat(fileAttributes).hasSize(4);
    assertThat(fileAttributes).contains(0, 0, 0, 0);
  }

  @Test
  public void testGetFilesInDirectoryRecursiveThrowsExceptionWhenFileIsNull() throws ZipException {
    expectedException.expectMessage("input path is null, cannot read files in the directory");
    expectedException.expect(ZipException.class);

    FileUtils.getFilesInDirectoryRecursive(null, true, true);
  }

  @Test
  public void testGetFilesInDirectoryRecursiveReturnsEmptyWhenInputFileIsNotDirectory() throws ZipException {
    File[] filesInDirectory = generateFilesForDirectory();
    testGetFilesInDirectory(false, true, filesInDirectory, 0, true, true);
  }

  @Test
  public void testGetFilesInDirectoryRecursiveReturnsEmptyWhenCannotReadInputFile() throws ZipException {
    File[] filesInDirectory = generateFilesForDirectory();
    testGetFilesInDirectory(true, false, filesInDirectory, 0, true, true);
  }

  @Test
  public void testGetFilesInDirectoryRecursiveReturnsEmptyWhenFilesInDirIsNull() throws ZipException {
    testGetFilesInDirectory(true, true, null, 0, true, true);
  }

  @Test
  public void testGetFilesInDirectoryRecursiveWithHiddenModeOnListsHiddenFiles() throws ZipException {
    File[] filesInDirectory = generateFilesForDirectory();
    testGetFilesInDirectory(true, true, filesInDirectory, 6, true, true);
  }

  @Test
  public void testGetFilesInDirectoryRecursiveWithHiddenModeOffDoesNotListsHiddenFiles() throws ZipException {
    File[] filesInDirectory = generateFilesForDirectory();
    testGetFilesInDirectory(true, true, filesInDirectory, 5, false, true);
  }

  @Test
  public void testGetFilesInDirectoryRecursiveWithHiddenModeOffDoesNotListsHiddenFolders() throws ZipException {
    File[] filesInDirectory = generateFilesForDirectory();
    testGetFilesInDirectory(true, true, filesInDirectory, 4, true, false);
  }

  @Test
  public void testGetFilesInDirectoryRecursiveWithHiddenModeOffForFilesAndFolders() throws ZipException {
    File[] filesInDirectory = generateFilesForDirectory();
    testGetFilesInDirectory(true, true, filesInDirectory, 3, false, false);
  }

  @Test
  public void testGetZipFileNameWithoutExtensionThrowsExceptionWhenNull() throws ZipException {
    expectedException.expectMessage("zip file name is empty or null, cannot determine zip file name");
    expectedException.expect(ZipException.class);

    FileUtils.getZipFileNameWithoutExtension(null);
  }

  @Test
  public void testGetZipFileNameWithoutExtensionThrowsExceptionWhenEmpty() throws ZipException {
    expectedException.expectMessage("zip file name is empty or null, cannot determine zip file name");
    expectedException.expect(ZipException.class);

    FileUtils.getZipFileNameWithoutExtension("");
  }

  @Test
  public void testGetZipFileNameWithoutExtensionForWindowsFileSeparator() throws ZipException {
    final String ACTUAL_FILE_SEPARATOR = System.getProperty("file.separator");
    System.setProperty("file.separator", "\\");
    assertThat(FileUtils.getZipFileNameWithoutExtension("c:\\mydir\\somefile.zip")).isEqualTo("somefile");
    System.setProperty("file.separator", ACTUAL_FILE_SEPARATOR);
  }

  @Test
  public void testGetZipFileNameWithoutExtensionForUnixFileSeparator() throws ZipException {
    final String ACTUAL_FILE_SEPARATOR = System.getProperty("file.separator");
    System.setProperty("file.separator", "/");
    assertThat(FileUtils.getZipFileNameWithoutExtension("/usr/srikanth/somezip.zip")).isEqualTo("somezip");
    System.setProperty("file.separator", ACTUAL_FILE_SEPARATOR);
  }

  @Test
  public void testGetSplitZipFilesThrowsExceptionWhenZipModelIsNull() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("cannot get split zip files: zipmodel is null");

    FileUtils.getSplitZipFiles(null);
  }

  @Test
  public void testGetSplitZipFilesReturnsNullWhenEndOfCentralDirectoryIsNull() throws ZipException {
    ZipModel zipModel = new ZipModel();
    zipModel.setEndOfCentralDirectoryRecord(null);

    assertThat(FileUtils.getSplitZipFiles(zipModel)).isNull();
  }

  @Test
  public void testGetSplitZipFilesThrowsExceptionWhenZipFileDoesNotExits() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("zip file does not exist");

    ZipModel zipModel = new ZipModel();
    zipModel.setZipFile(new File("Does not exist"));

    FileUtils.getSplitZipFiles(zipModel);
  }

  @Test
  public void testGetSplitZipFilesForNonSplitArchiveReturnsSameFile() throws ZipException {
    File zipFile = mockZipFileAsExists("somepath", "somefile");
    ZipModel zipModel = new ZipModel();
    zipModel.setSplitArchive(false);
    zipModel.setZipFile(zipFile);

    List<File> splitFiles = FileUtils.getSplitZipFiles(zipModel);

    assertThat(splitFiles).hasSize(1);
    assertThat(splitFiles.get(0)).isSameAs(zipFile);
  }

  @Test
  public void testGetSplitZipFilesWhenNumberOfDiskIsZeroReturnsSameFile() throws ZipException {
    File zipFile = mockZipFileAsExists("somepath", "somefile");
    ZipModel zipModel = new ZipModel();
    zipModel.setSplitArchive(false);
    zipModel.setZipFile(zipFile);
    zipModel.getEndOfCentralDirectoryRecord().setNumberOfThisDisk(0);

    List<File> splitFiles = FileUtils.getSplitZipFiles(zipModel);

    assertThat(splitFiles).hasSize(1);
    assertThat(splitFiles.get(0)).isSameAs(zipFile);
  }

  @Test
  public void testGetSplitZipFilesReturnsValidWhenSplitFile() throws ZipException {
    final String FILE_SEPARATOR = System.getProperty("file.separator");
    String path = FILE_SEPARATOR + "usr" + FILE_SEPARATOR + "parentdir" + FILE_SEPARATOR;
    String zipFileName = "SomeName";
    File zipFile = mockZipFileAsExists(path, zipFileName);
    ZipModel zipModel = new ZipModel();
    zipModel.setSplitArchive(true);
    zipModel.setZipFile(zipFile);
    zipModel.getEndOfCentralDirectoryRecord().setNumberOfThisDisk(4);

    List<File> splitFiles = FileUtils.getSplitZipFiles(zipModel);

    assertThat(splitFiles).hasSize(5);
    assertThat(splitFiles.get(0).getPath()).isEqualTo(path + zipFileName + ".z01");
    assertThat(splitFiles.get(1).getPath()).isEqualTo(path + zipFileName + ".z02");
    assertThat(splitFiles.get(2).getPath()).isEqualTo(path + zipFileName + ".z03");
    assertThat(splitFiles.get(3).getPath()).isEqualTo(path + zipFileName + ".z04");
    assertThat(splitFiles.get(4).getPath()).isEqualTo(path + zipFileName + ".zip");
  }

  @Test
  public void testGetRelativeFileNameWhenRootFoldersAreNull() throws ZipException {
    assertThat(FileUtils.getRelativeFileName("somefile.txt", null)).isEqualTo("somefile.txt");
  }

  @Test
  public void testIsZipEntryDirectoryWithWindowsFileSeparatorReturnsTrue() {
    assertThat(FileUtils.isZipEntryDirectory("somename\\")).isTrue();
  }

  @Test
  public void testIsZipEntryDirectoryWithUnixFileSeparatorReturnsTrue() {
    assertThat(FileUtils.isZipEntryDirectory("somename/")).isTrue();
  }

  @Test
  public void testIsZipEntryDirectoryWhichIsNotDirectoryReturnsFalse() {
    assertThat(FileUtils.isZipEntryDirectory("somename")).isFalse();
  }

  private File mockZipFileAsExists(String path, String zipFileNameWithoutExtension) {
    File file = mock(File.class);
    when(file.exists()).thenReturn(true);
    when(file.getName()).thenReturn(zipFileNameWithoutExtension + ".zip");
    when(file.getPath()).thenReturn(path + zipFileNameWithoutExtension + ".zip");
    return file;
  }

  private void testGetFilesInDirectory(boolean isDirectory, boolean canRead, File[] filesInDirectory,
                                       int expectedReturnSize, boolean shouldReadHiddenFiles,
                                       boolean shouldReadHiddenFolders) throws ZipException {
    File file = mock(File.class);
    when(file.isDirectory()).thenReturn(isDirectory);
    when(file.canRead()).thenReturn(canRead);
    when(file.listFiles()).thenReturn(filesInDirectory);

    List<File> returnedFiles = FileUtils.getFilesInDirectoryRecursive(file, shouldReadHiddenFiles,
        shouldReadHiddenFolders);

    assertThat(returnedFiles).hasSize(expectedReturnSize);
  }

  private File[] generateFilesForDirectory() {
    return new File[]{
        mockFile(false, false),
        mockFile(false, false),
        mockFile(true, false),
        mockFile(true, true),
        mockFile(false, true),
        mockFile(true, true),
    };
  }

  private File mockFile(boolean isHidden, boolean isDirectory) {
    File file = mock(File.class);
    when(file.isHidden()).thenReturn(isHidden);
    when(file.isDirectory()).thenReturn(isDirectory);
    return file;
  }

  private FileSystemProvider mockPath(Path path) {
    FileSystemProvider fileSystemProvider = mock(FileSystemProvider.class);
    FileSystem fileSystem = mock(FileSystem.class);

    when(path.getFileSystem()).thenReturn(fileSystem);
    when(path.getFileSystem().provider()).thenReturn(fileSystemProvider);
    return fileSystemProvider;
  }
}