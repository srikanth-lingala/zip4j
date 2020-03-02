package net.lingala.zip4j;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.progress.ProgressMonitor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

// Tests only failure scenarios. All other tests are covered in the corresponding Integration test
public class ZipFileTest {

  private File sourceZipFile;
  private ZipFile zipFile;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setup() {
    sourceZipFile = mockFile(false);
    zipFile = new ZipFile(sourceZipFile);
  }

  @Test
  public void testCreateZipFileThrowsExceptionWhenZipFileExists() throws ZipException {
    reset(sourceZipFile);
    when(sourceZipFile.exists()).thenReturn(true);

    expectedException.expect(ZipException.class);
    expectedException.expectMessage("zip file: " + sourceZipFile + " already exists. " +
        "To add files to existing zip file use addFile method");

    zipFile.createSplitZipFile(Collections.emptyList(), new ZipParameters(), true, 10000);
  }

  @Test
  public void testCreateZipFileThrowsExceptionWhenFileListIsNull() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("input file List is null, cannot create zip file");

    zipFile.createSplitZipFile(null, new ZipParameters(), true, 10000);
  }

  @Test
  public void testCreateZipFileThrowsExceptionWhenFileListIsEmpty() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("input file List is null, cannot create zip file");

    zipFile.createSplitZipFile(Collections.emptyList(), new ZipParameters(), true, 10000);
  }

  @Test
  public void testCreateZipFileFromFolderThrowsExceptionWheFolderIsNull() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("folderToAdd is null, cannot create zip file from folder");

    zipFile.createSplitZipFileFromFolder(null, new ZipParameters(), true, 10000);
  }

  @Test
  public void testCreateZipFileFromFolderThrowsExceptionWhenParametersAreNull() throws ZipException {
    File folderToAdd = mockFile(true);

    expectedException.expect(ZipException.class);
    expectedException.expectMessage("input parameters are null, cannot create zip file from folder");

    zipFile.createSplitZipFileFromFolder(folderToAdd, null, true, 10000);
  }

  @Test
  public void testCreateZipFileFromFolderThrowsExceptionWhenZipFileExists() throws ZipException {
    reset(sourceZipFile);
    when(sourceZipFile.exists()).thenReturn(true);
    File folderToAdd = mockFile(true);

    expectedException.expect(ZipException.class);
    expectedException.expectMessage("zip file: " + sourceZipFile
        + " already exists. To add files to existing zip file use addFolder method");

    zipFile.createSplitZipFileFromFolder(folderToAdd, new ZipParameters(), true, 10000);
  }

  @Test
  public void testAddFileAsStringThrowsExceptionWhenFileIsNull() throws ZipException {
    expectedException.expectMessage("file to add is null or empty");
    expectedException.expect(ZipException.class);

    zipFile.addFile((String) null);
  }

  @Test
  public void testAddFileAsStringThrowsExceptionWhenFileIsEmpty() throws ZipException {
    expectedException.expectMessage("file to add is null or empty");
    expectedException.expect(ZipException.class);

    zipFile.addFile("   ");
  }

  @Test
  public void testAddFileAsStringWithParametersThrowsExceptionWhenFileIsNull() throws ZipException {
    expectedException.expectMessage("file to add is null or empty");
    expectedException.expect(ZipException.class);

    zipFile.addFile((String) null, new ZipParameters());
  }

  @Test
  public void testAddFileAsStringWithParametersThrowsExceptionWhenFileIsEmpty() throws ZipException {
    expectedException.expectMessage("file to add is null or empty");
    expectedException.expect(ZipException.class);

    zipFile.addFile("", new ZipParameters());
  }

  @Test
  public void testAddFileAsFileThrowsExceptionWhenParametersIsNull() throws ZipException {
    File fileToAdd = mockFile(true);

    expectedException.expect(ZipException.class);
    expectedException.expectMessage("input parameters are null");

    zipFile.addFile(fileToAdd, null);
  }

  @Test
  public void testAddFileAsFileThrowsExceptionWhenProgressMonitorStateIsBusy() throws ZipException {
    File fileToAdd = mockFile(true);
    zipFile.getProgressMonitor().setState(ProgressMonitor.State.BUSY);

    expectedException.expect(ZipException.class);
    expectedException.expectMessage("invalid operation - Zip4j is in busy state");

    zipFile.addFile(fileToAdd, new ZipParameters());
  }

  @Test
  public void testAddFilesThrowsExceptionWhenInputFilesIsNull() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("input file List is null or empty");

    zipFile.addFiles(null);
  }

  @Test
  public void testAddFilesThrowsExceptionWhenInputFilesIsEmpty() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("input file List is null or empty");

    zipFile.addFiles(Collections.emptyList());
  }

  @Test
  public void testAddFilesThrowsExceptionWhenProgressMonitorStateIsBusy() throws ZipException {
    zipFile.getProgressMonitor().setState(ProgressMonitor.State.BUSY);

    expectedException.expect(ZipException.class);
    expectedException.expectMessage("invalid operation - Zip4j is in busy state");

    zipFile.addFiles(Collections.singletonList(new File("Some_File")));
  }

  @Test
  public void testAddFilesWithParametersThrowsExceptionWhenInputFilesIsNull() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("input file List is null or empty");

    zipFile.addFiles(null, new ZipParameters());
  }

  @Test
  public void testAddFilesWithParametersThrowsExceptionWhenInputFilesIsEmpty() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("input file List is null or empty");

    zipFile.addFiles(Collections.emptyList(), new ZipParameters());
  }

  @Test
  public void testAddFilesWithParametersThrowsExceptionWhenParametersAreNull() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("input parameters are null");

    zipFile.addFiles(Collections.singletonList(new File("Some_File")), null);
  }


  @Test
  public void testAddFilesWithParametersThrowsExceptionWhenProgressMonitorStateIsBusy() throws ZipException {
    zipFile.getProgressMonitor().setState(ProgressMonitor.State.BUSY);

    expectedException.expect(ZipException.class);
    expectedException.expectMessage("invalid operation - Zip4j is in busy state");

    zipFile.addFiles(Collections.singletonList(new File("Some_File")), new ZipParameters());
  }

  @Test
  public void testAddFolderThrowsExceptionWhenFolderIsNull() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("input path is null, cannot add folder to zip file");

    zipFile.addFolder(null);
  }

  @Test
  public void testAddFolderThrowsExceptionWhenFolderDoesNotExist() throws ZipException {
    File folderToAdd = mockFile(false);

    expectedException.expect(ZipException.class);
    expectedException.expectMessage("folder does not exist");

    zipFile.addFolder(folderToAdd);
  }

  @Test
  public void testAddFolderThrowsExceptionWhenFolderNotADirectory() throws ZipException {
    File folderToAdd = mockFile(true);
    when(folderToAdd.isDirectory()).thenReturn(false);

    expectedException.expect(ZipException.class);
    expectedException.expectMessage("input folder is not a directory");

    zipFile.addFolder(folderToAdd);
  }

  @Test
  public void testAddFolderThrowsExceptionWhenCannotReadFolder() throws ZipException {
    File folderToAdd = mockFile(true);
    when(folderToAdd.isDirectory()).thenReturn(true);
    when(folderToAdd.canRead()).thenReturn(false);

    expectedException.expect(ZipException.class);
    expectedException.expectMessage("cannot read input folder");

    zipFile.addFolder(folderToAdd);
  }

  @Test
  public void testAddFolderWithParametersThrowsExceptionWhenFolderIsNull() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("input path is null, cannot add folder to zip file");

    zipFile.addFolder(null, new ZipParameters());
  }

  @Test
  public void testAddFolderWithParametersThrowsExceptionWhenFolderDoesNotExist() throws ZipException {
    File folderToAdd = mockFile(false);

    expectedException.expect(ZipException.class);
    expectedException.expectMessage("folder does not exist");

    zipFile.addFolder(folderToAdd, new ZipParameters());
  }

  @Test
  public void testAddFolderWithParametersThrowsExceptionWhenFolderNotADirectory() throws ZipException {
    File folderToAdd = mockFile(true);
    when(folderToAdd.isDirectory()).thenReturn(false);

    expectedException.expect(ZipException.class);
    expectedException.expectMessage("input folder is not a directory");

    zipFile.addFolder(folderToAdd, new ZipParameters());
  }

  @Test
  public void testAddFolderWithParametersThrowsExceptionWhenCannotReadFolder() throws ZipException {
    File folderToAdd = mockFile(true);
    when(folderToAdd.isDirectory()).thenReturn(true);
    when(folderToAdd.canRead()).thenReturn(false);

    expectedException.expect(ZipException.class);
    expectedException.expectMessage("cannot read input folder");

    zipFile.addFolder(folderToAdd, new ZipParameters());
  }

  @Test
  public void testAddFolderWithParametersThrowsExceptionWhenInputParametersAreNull() throws ZipException {
    File folderToAdd = mockFile(true);
    when(folderToAdd.isDirectory()).thenReturn(true);
    when(folderToAdd.canRead()).thenReturn(true);

    expectedException.expect(ZipException.class);
    expectedException.expectMessage("input parameters are null, cannot add folder to zip file");

    zipFile.addFolder(folderToAdd, null);
  }

  @Test
  public void testAddStreamThrowsExceptionWhenInputStreamIsNull() throws ZipException {
    expectedException.expectMessage("inputstream is null, cannot add file to zip");
    expectedException.expect(ZipException.class);

    zipFile.addStream(null, new ZipParameters());
  }

  @Test
  public void testAddStreamThrowsExceptionWhenParametersIsNull() throws ZipException {
    expectedException.expectMessage("zip parameters are null");
    expectedException.expect(ZipException.class);

    zipFile.addStream(new ByteArrayInputStream(new byte[2]), null);
  }

  @Test
  public void testExtractAllThrowsExceptionWhenDestinationIsNull() throws ZipException {
    expectedException.expectMessage("output path is null or invalid");
    expectedException.expect(ZipException.class);

    zipFile.extractAll(null);
  }

  @Test
  public void testExtractAllThrowsExceptionWhenDestinationIsEmpty() throws ZipException {
    expectedException.expectMessage("output path is null or invalid");
    expectedException.expect(ZipException.class);

    zipFile.extractAll(" ");
  }

  @Test
  public void testExtractFileWithFileHeaderWhenFileHeaderIsNullThrowsException() throws ZipException {
    expectedException.expectMessage("input file header is null, cannot extract file");
    expectedException.expect(ZipException.class);

    zipFile.extractFile((FileHeader) null, "SOME_DESTINATION");
  }

  @Test
  public void testExtractFileWithFileHeaderWhenDestinationIsNullThrowsException() throws ZipException {
    expectedException.expectMessage("destination path is empty or null, cannot extract file");
    expectedException.expect(ZipException.class);

    zipFile.extractFile(new FileHeader(), null);
  }

  @Test
  public void testExtractFileWithFileHeaderWhenDestinationIsEmptyThrowsException() throws ZipException {
    expectedException.expectMessage("destination path is empty or null, cannot extract file");
    expectedException.expect(ZipException.class);

    zipFile.extractFile(new FileHeader(), "");
  }

  @Test
  public void testExtractFileWithFileHeaderWhenBusyStateThrowsException() throws ZipException {
    zipFile.getProgressMonitor().setState(ProgressMonitor.State.BUSY);

    expectedException.expectMessage("invalid operation - Zip4j is in busy state");
    expectedException.expect(ZipException.class);

    zipFile.extractFile(new FileHeader(), "SOME_DESTINATION");
  }

  @Test
  public void testExtractFileWithFileNameThrowsExceptionWhenNameIsNull() throws ZipException {
    expectedException.expectMessage("file to extract is null or empty, cannot extract file");
    expectedException.expect(ZipException.class);

    zipFile.extractFile((String) null, "SOME_DESTINATION");
  }

  @Test
  public void testExtractFileWithFileNameThrowsExceptionWhenNameIsEmpty() throws ZipException {
    expectedException.expectMessage("file to extract is null or empty, cannot extract file");
    expectedException.expect(ZipException.class);

    zipFile.extractFile("", "SOME_DESTINATION");
  }

  @Test
  public void testExtractFileWithNewFileNameThrowsExceptionWhenNameIsNull() throws ZipException {
    expectedException.expectMessage("file to extract is null or empty, cannot extract file");
    expectedException.expect(ZipException.class);

    zipFile.extractFile((String) null, "SOME_DESTINATION", "NEW_FILE_NAME");
  }

  @Test
  public void testExtractFileWithNewFileNameThrowsExceptionWhenNameIsEmpty() throws ZipException {
    expectedException.expectMessage("file to extract is null or empty, cannot extract file");
    expectedException.expect(ZipException.class);

    zipFile.extractFile("", "SOME_DESTINATION");
  }

  @Test
  public void testGetFileHeadersReturnsEmptyListWhenZipFileDoesNotExist() throws ZipException {
    File mockFile = mockFile(false);
    ZipFile zipFile = new ZipFile(mockFile);
    assertThat(zipFile.getFileHeaders()).isEmpty();
  }

  @Test
  public void testGetFileHeaderThrowsExceptionWhenFileNameIsNull() throws ZipException {
    expectedException.expectMessage("input file name is emtpy or null, cannot get FileHeader");
    expectedException.expect(ZipException.class);

    zipFile.getFileHeader(null);
  }

  @Test
  public void testGetFileHeaderThrowsExceptionWhenFileNameIsEmpty() throws ZipException {
    expectedException.expectMessage("input file name is emtpy or null, cannot get FileHeader");
    expectedException.expect(ZipException.class);

    zipFile.getFileHeader("");
  }

  @Test
  public void testRemoveFileWithFileNameThrowsExceptionWhenFileNameIsNull() throws ZipException {
    expectedException.expectMessage("file name is empty or null, cannot remove file");
    expectedException.expect(ZipException.class);

    zipFile.removeFile((String) null);
  }

  @Test
  public void testRemoveFileWithFileNameThrowsExceptionWhenFileNameIsEmpty() throws ZipException {
    expectedException.expectMessage("file name is empty or null, cannot remove file");
    expectedException.expect(ZipException.class);

    zipFile.removeFile("");
  }

  @Test
  public void testRemoveFileWithFileHeaderThrowsExceptionWhenFileNameIsNull() throws ZipException {
    expectedException.expectMessage("input file header is null, cannot remove file");
    expectedException.expect(ZipException.class);

    zipFile.removeFile((FileHeader) null);
  }

  @Test
  public void testRemoveFilesWithListThrowsExceptionWhenListIsNull() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("fileNames list is null");

    zipFile.removeFiles(null);
  }

  @Test
  public void testRenameFileWithFileHeaderThrowsExceptionWhenHeaderIsNull() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("File header is null");

    zipFile.renameFile((FileHeader) null, "somename");
  }

  @Test
  public void testRenameFileWithFileHeaderThrowsExceptionWhenNewFileNameIsNull() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("newFileName is null or empty");

    FileHeader fileHeader = new FileHeader();
    fileHeader.setFileName("somename");

    zipFile.renameFile(fileHeader, null);
  }

  @Test
  public void testRenameFileWithFileHeaderThrowsExceptionWhenNewFileNameIsEmpty() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("newFileName is null or empty");

    FileHeader fileHeader = new FileHeader();
    fileHeader.setFileName("somename");

    zipFile.renameFile(fileHeader, "");
  }

  @Test
  public void testRenameFileWithFileNameThrowsExceptionWhenFileNameToBeChangedIsNull() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("file name to be changed is null or empty");

    zipFile.renameFile((String) null, "somename");
  }

  @Test
  public void testRenameFileWithFileNameThrowsExceptionWhenFileNameToBeChangedIsEmpty() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("file name to be changed is null or empty");

    zipFile.renameFile("", "somename");
  }

  @Test
  public void testRenameFileWithFileNameThrowsExceptionWhenNewFileNameIsNull() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("newFileName is null or empty");

    zipFile.renameFile("Somename", null);
  }

  @Test
  public void testRenameFileWithFileNameThrowsExceptionWhenNewFileNameIsEmpty() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("newFileName is null or empty");

    zipFile.renameFile("Somename", "   ");
  }

  @Test
  public void testRenameFileWithMapThrowsExceptionWhenMapIsNull() throws ZipException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("fileNamesMap is null");

    zipFile.renameFiles(null);
  }

  @Test
  public void testMergeSplitFilesWhenOutputFileIsNullThrowsException() throws ZipException {
    expectedException.expectMessage("outputZipFile is null, cannot merge split files");
    expectedException.expect(ZipException.class);

    zipFile.mergeSplitFiles(null);
  }

  @Test
  public void testMergeSplitFilesWhenOutputFileDoesAlreadyExistsThrowsException() throws ZipException {
    expectedException.expectMessage("output Zip File already exists");
    expectedException.expect(ZipException.class);

    File mergedZipFile = mockFile(true);

    zipFile.mergeSplitFiles(mergedZipFile);
  }

  @Test
  public void testSetCommentWhenCommentIsNullThrowsException() throws ZipException {
    expectedException.expectMessage("input comment is null, cannot update zip file");
    expectedException.expect(ZipException.class);

    zipFile.setComment(null);
  }

  @Test
  public void testSetCommentWhenZipFileDoesNotExistsThrowsException() throws ZipException {
    expectedException.expectMessage("zip file does not exist, cannot set comment for zip file");
    expectedException.expect(ZipException.class);

    zipFile.setComment("Some comment");
  }

  @Test
  public void testGetCommentWhenZipFileDoesNotExistThrowsException() throws ZipException {
    expectedException.expectMessage("zip file does not exist, cannot read comment");
    expectedException.expect(ZipException.class);

    zipFile.getComment();
  }

  @Test
  public void testGetInputStreamWhenFileHeaderIsNullThrowsException() throws IOException {
    expectedException.expectMessage("FileHeader is null, cannot get InputStream");
    expectedException.expect(ZipException.class);

    zipFile.getInputStream(null);
  }

  @Test
  public void testSetRunInThreadSetsFlag() {
    zipFile.setRunInThread(true);
    assertThat(zipFile.isRunInThread()).isTrue();

    zipFile.setRunInThread(false);
    assertThat(zipFile.isRunInThread()).isFalse();
  }

  @Test
  public void testGetFileReturnsValidFile() {
    assertThat(zipFile.getFile()).isSameAs(sourceZipFile);
  }

  @Test
  public void testToString() {
    assertThat(zipFile.toString()).isEqualTo("SOME_PATH");
  }

  private File mockFile(boolean fileExists) {
    File file = mock(File.class);
    when(file.exists()).thenReturn(fileExists);
    when(file.toString()).thenReturn("SOME_PATH");
    return file;
  }

}