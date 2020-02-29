package net.lingala.zip4j.util;

import net.lingala.zip4j.AbstractIT;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.inputstream.NumberedSplitInputStream;
import net.lingala.zip4j.io.inputstream.SplitInputStream;
import net.lingala.zip4j.io.inputstream.ZipStandardSplitInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.testutils.TestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(FileUtils.class)
public class UnzipUtilIT extends AbstractIT {

  @Test
  public void testCreateZipInputStream() throws ZipException, IOException {
    ZipFile zipFile = createZipFile();
    ZipModel zipModel = createZipModel();
    FileHeader fileHeader = zipFile.getFileHeaders().get(1);
    File extractedFile = temporaryFolder.newFile();

    try (InputStream inputStream = UnzipUtil.createZipInputStream(zipModel, fileHeader, "password".toCharArray());
         OutputStream outputStream = new FileOutputStream(extractedFile)) {
      byte[] b = new byte[InternalZipConstants.BUFF_SIZE];
      int readLen = 0;

      while ((readLen = inputStream.read(b)) != -1) {
        outputStream.write(b, 0, readLen);
      }
    }

    assertThat(extractedFile.length()).isEqualTo(TestUtils.getTestFileFromResources("sample_text_large.txt").length());
  }

  @Test
  public void testApplyFileAttributes() {
    byte[] externalFileAttributes = new byte[] {12, 34, 0, 0};
    long currentTime = System.currentTimeMillis();
    FileHeader fileHeader = new FileHeader();
    fileHeader.setExternalFileAttributes(externalFileAttributes);
    fileHeader.setLastModifiedTime(currentTime);

    File file = mock(File.class);
    Path path = mock(Path.class);
    when(file.toPath()).thenReturn(path);

    PowerMockito.mockStatic(FileUtils.class);

    UnzipUtil.applyFileAttributes(fileHeader, file);

    verifyStatic(FileUtils.class);
    FileUtils.setFileLastModifiedTime(path, currentTime);

    verifyStatic(FileUtils.class);
    FileUtils.setFileAttributes(path, externalFileAttributes);
  }

  @Test
  public void testApplyFileFileAttributesSetsLastModifiedTimeWithoutNio() {
    byte[] externalFileAttributes = new byte[] {12, 34, 0, 0};
    long currentTime = System.currentTimeMillis();
    FileHeader fileHeader = new FileHeader();
    fileHeader.setExternalFileAttributes(externalFileAttributes);
    fileHeader.setLastModifiedTime(currentTime);

    File file = mock(File.class);
    Path path = mock(Path.class);
    when(file.toPath()).thenThrow(new NoSuchMethodError("No method"));

    PowerMockito.mockStatic(FileUtils.class);

    UnzipUtil.applyFileAttributes(fileHeader, file);

    verifyStatic(FileUtils.class, never());
    FileUtils.setFileLastModifiedTime(path, currentTime);

    verifyStatic(FileUtils.class, never());
    FileUtils.setFileAttributes(path, externalFileAttributes);

    verifyStatic(FileUtils.class);
    FileUtils.setFileLastModifiedTimeWithoutNio(file, currentTime);
  }

  @Test
  public void testCreateSplitInputStreamForNumberedSplitZipReturnsInstance() throws IOException {
    String zipFileName = "somename.zip.001";
    File zipFile = temporaryFolder.newFile(zipFileName);
    ZipModel zipModel = createZipModel();
    zipModel.setZipFile(zipFile);

    SplitInputStream splitInputStream = UnzipUtil.createSplitInputStream(zipModel);

    assertThat(splitInputStream).isInstanceOf(NumberedSplitInputStream.class);
  }

  @Test
  public void testCreateSplitInputStreamForNonNumberedSplitZipReturnsInstance() throws IOException {
    temporaryFolder.newFile(generatedZipFile.getName());
    ZipModel zipModel = createZipModel();

    SplitInputStream splitInputStream = UnzipUtil.createSplitInputStream(zipModel);

    assertThat(splitInputStream).isInstanceOf(ZipStandardSplitInputStream.class);
  }

  private ZipFile createZipFile() throws ZipException {
    ZipFile zipFile = new ZipFile(generatedZipFile, "password".toCharArray());
    zipFile.addFiles(Arrays.asList(
        TestUtils.getTestFileFromResources("sample_text1.txt"),
        TestUtils.getTestFileFromResources("sample_text_large.txt")
    ));
    return zipFile;
  }

  private ZipModel createZipModel() {
    ZipModel zipModel = new ZipModel();
    zipModel.setZipFile(generatedZipFile);
    zipModel.getEndOfCentralDirectoryRecord().setNumberOfThisDisk(0);
    zipModel.setSplitArchive(false);
    return zipModel;
  }

}