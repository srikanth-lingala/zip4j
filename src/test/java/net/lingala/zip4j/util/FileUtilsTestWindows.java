package net.lingala.zip4j.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.spi.FileSystemProvider;

import static net.lingala.zip4j.util.BitUtils.isBitSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class FileUtilsTestWindows {

  private static final String ACTUAL_OS = System.getProperty("os.name");

  @Before
  public void beforeClass() {
    System.setProperty("os.name", "windows");
  }

  @After
  public void cleanup() {
    System.setProperty("os.name", ACTUAL_OS);
  }

  @Test
  public void testSetFileAttributesWhenAttributesIsNullDoesNothing() {
    FileUtils.setFileAttributes(mock(Path.class), null);
  }

  @Test
  public void testSetFileAttributesWhenAttributesIsEmptyDoesNothing() {
    FileUtils.setFileAttributes(mock(Path.class), new byte[0]);
  }

  @Test
  public void testSetFileAttributesOnWindowsMachineWhenAttributesSet() throws IOException {
    Path mockedPath = mock(Path.class);
    DosFileAttributeView dosFileAttributeView = mockDosFileAttributeView(mockedPath, true, null);

    byte attribute = 0;
    attribute = BitUtils.setBit(attribute, 0);
    attribute = BitUtils.setBit(attribute, 1);
    attribute = BitUtils.setBit(attribute, 2);
    attribute = BitUtils.setBit(attribute, 5);

    FileUtils.setFileAttributes(mockedPath, new byte[]{attribute, 0, 0, 0});

    verify(dosFileAttributeView).setReadOnly(true);
    verify(dosFileAttributeView).setHidden(true);
    verify(dosFileAttributeView).setSystem(true);
    verify(dosFileAttributeView).setArchive(true);
  }

  @Test
  public void testSetFileAttributesOnWindowsWhenNoAttributesSetDoesNothing() throws IOException {
    Path mockedPath = mock(Path.class);
    DosFileAttributeView dosFileAttributeView = mockDosFileAttributeView(mockedPath, true, null);

    FileUtils.setFileAttributes(mockedPath, new byte[]{0, 0, 0, 0});

    verifyZeroInteractions(dosFileAttributeView);
  }

  @Test
  public void testGetFileAttributesWhenFileIsNullReturnsEmptyBytes() {
    byte[] attributes = FileUtils.getFileAttributes(null);
    assertThat(attributes).contains(0, 0, 0, 0);
  }

  @Test
  public void testGetFileAttributesWhenFileDoesNotExistReturnsEmptyBytes() throws IOException {
    File file = mock(File.class);
    Path path = mock(Path.class);
    when(file.toPath()).thenReturn(path);
    when(file.exists()).thenReturn(false);
    DosFileAttributes dosFileAttributes = mock(DosFileAttributes.class);
    mockDosFileAttributeView(path, true, dosFileAttributes);

    byte[] attributes = FileUtils.getFileAttributes(file);

    assertThat(attributes).contains(0, 0, 0, 0);
  }

  @Test
  public void testIsWindowsReturnsTrue() {
    assertThat(FileUtils.isWindows()).isTrue();
  }

  @Test
  public void testGetFileAttributesReturnsAttributesAsDefined() throws IOException {
    File file = mock(File.class);
    Path path = mock(Path.class);
    when(file.toPath()).thenReturn(path);
    when(file.exists()).thenReturn(true);
    DosFileAttributes dosFileAttributes = mock(DosFileAttributes.class);
    DosFileAttributeView dosFileAttributeView = mockDosFileAttributeView(path, true, dosFileAttributes);
    when(dosFileAttributeView.readAttributes()).thenReturn(dosFileAttributes);
    when(dosFileAttributes.isReadOnly()).thenReturn(true);
    when(dosFileAttributes.isHidden()).thenReturn(true);
    when(dosFileAttributes.isSystem()).thenReturn(true);
    when(dosFileAttributes.isArchive()).thenReturn(true);

    byte[] attributes = FileUtils.getFileAttributes(file);

    assertThat(isBitSet(attributes[0], 0)).isTrue();
    assertThat(isBitSet(attributes[0], 1)).isTrue();
    assertThat(isBitSet(attributes[0], 2)).isTrue();
    assertThat(isBitSet(attributes[0], 5)).isTrue();
  }

  private DosFileAttributeView mockDosFileAttributeView(Path path, boolean fileExists, DosFileAttributes dosFileAttributes) throws IOException {
    FileSystemProvider fileSystemProvider = mock(FileSystemProvider.class);
    FileSystem fileSystem = mock(FileSystem.class);
    DosFileAttributeView dosFileAttributeView = mock(DosFileAttributeView.class);

    when(fileSystemProvider.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)).thenReturn(dosFileAttributes);
    when(path.getFileSystem()).thenReturn(fileSystem);
    when(fileSystemProvider.getFileAttributeView(path, DosFileAttributeView.class, LinkOption.NOFOLLOW_LINKS))
        .thenReturn(dosFileAttributeView);
    when(path.getFileSystem().provider()).thenReturn(fileSystemProvider);

    if (!fileExists) {
      doThrow(new IOException()).when(fileSystemProvider).checkAccess(path);
    }

    return dosFileAttributeView;
  }
}