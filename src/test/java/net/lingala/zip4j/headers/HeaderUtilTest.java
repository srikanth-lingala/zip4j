package net.lingala.zip4j.headers;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.CentralDirectory;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class HeaderUtilTest {

  private static final String FILE_NAME = "test.txt";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testGetFileHeaderWithNullZipModelThrowsException() throws ZipException {
    expectZipException("zip model is null, cannot determine file header with exact match for fileName: " + FILE_NAME);
    HeaderUtil.getFileHeader(null, FILE_NAME);
  }

  @Test
  public void testGetFileHeaderWithNullFileNameThrowsException() throws ZipException {
    expectZipException("file name is null, cannot determine file header with exact match for fileName: null");
    HeaderUtil.getFileHeader(new ZipModel(), null);
  }

  @Test
  public void testGetFileHeaderWithEmptyFileNameThrowsException() throws ZipException {
    expectZipException("file name is null, cannot determine file header with exact match for fileName: ");
    HeaderUtil.getFileHeader(new ZipModel(), "");
  }

  @Test
  public void testGetFileHeaderWithNullCentralDirectoryThrowsException() throws ZipException {
    expectZipException("central directory is null, cannot determine file header with exact match for fileName: "
        + FILE_NAME);

    ZipModel zipModel = new ZipModel();
    zipModel.setCentralDirectory(null);
    HeaderUtil.getFileHeader(zipModel, FILE_NAME);
  }

  @Test
  public void testGetFileHeaderWithNullFileHeadersThrowsException() throws ZipException {
    expectedException.expect(ZipException.class);
    expectZipException("file Headers are null, cannot determine file header with exact match for fileName: "
        + FILE_NAME);

    ZipModel zipModel = new ZipModel();
    CentralDirectory centralDirectory = new CentralDirectory();
    centralDirectory.setFileHeaders(null);
    zipModel.setCentralDirectory(centralDirectory);

    HeaderUtil.getFileHeader(zipModel, FILE_NAME);
  }

  @Test
  public void testGetFileHeaderWithEmptyFileHeadersReturnsNull() throws ZipException {
    ZipModel zipModel = new ZipModel();
    CentralDirectory centralDirectory = new CentralDirectory();
    centralDirectory.setFileHeaders(Collections.emptyList());
    zipModel.setCentralDirectory(centralDirectory);

    FileHeader fileHeader = HeaderUtil.getFileHeader(zipModel, FILE_NAME);
    assertThat(fileHeader).isNull();
  }

  @Test
  public void testGetFileHeaderWithExactMatch() throws ZipException {
    ZipModel zipModel = new ZipModel();
    CentralDirectory centralDirectory = new CentralDirectory();
    centralDirectory.setFileHeaders(Arrays.asList(
        generateFileHeader(null),
        generateFileHeader(""),
        generateFileHeader("SOME_OTHER_NAME"),
        generateFileHeader(FILE_NAME)
    ));
    zipModel.setCentralDirectory(centralDirectory);

    FileHeader fileHeader = HeaderUtil.getFileHeader(zipModel, FILE_NAME);
    assertThat(fileHeader).isNotNull();
    assertThat(fileHeader.getFileName()).isEqualTo(FILE_NAME);
  }

  @Test
  public void testGetFileHeaderWithWindowsFileSeparator() throws ZipException {
    ZipModel zipModel = new ZipModel();
    CentralDirectory centralDirectory = new CentralDirectory();
    centralDirectory.setFileHeaders(Arrays.asList(
        generateFileHeader(FILE_NAME),
        generateFileHeader("SOME_OTHER_NAME\\")
    ));
    zipModel.setCentralDirectory(centralDirectory);

    FileHeader fileHeader = HeaderUtil.getFileHeader(zipModel, "SOME_OTHER_NAME\\");
    assertThat(fileHeader).isNotNull();
    assertThat(fileHeader.getFileName()).isEqualTo("SOME_OTHER_NAME\\");
  }

  @Test
  public void testGetFileHeaderWithUnixFileSeparator() throws ZipException {
    ZipModel zipModel = new ZipModel();
    CentralDirectory centralDirectory = new CentralDirectory();
    centralDirectory.setFileHeaders(Arrays.asList(
        generateFileHeader(FILE_NAME),
        generateFileHeader("SOME_OTHER_NAME/")
    ));
    zipModel.setCentralDirectory(centralDirectory);

    FileHeader fileHeader = HeaderUtil.getFileHeader(zipModel, "SOME_OTHER_NAME/");
    assertThat(fileHeader).isNotNull();
    assertThat(fileHeader.getFileName()).isEqualTo("SOME_OTHER_NAME/");
  }

  @Test
  public void testGetFileHeaderWithoutAMatch() throws ZipException {
    ZipModel zipModel = new ZipModel();
    CentralDirectory centralDirectory = new CentralDirectory();
    centralDirectory.setFileHeaders(Arrays.asList(
        generateFileHeader(FILE_NAME),
        generateFileHeader("SOME_OTHER_NAME")
    ));
    zipModel.setCentralDirectory(centralDirectory);

    assertThat(HeaderUtil.getFileHeader(zipModel, "SHOULD_NOT_EXIST")).isNull();
  }

  @Test
  public void testGetIndexOfFileHeaderWhenZipModelIsNullThrowsException() throws ZipException {
    expectZipException("input parameters is null, cannot determine index of file header");
    HeaderUtil.getIndexOfFileHeader(null, new FileHeader());
  }

  @Test
  public void testGetIndexOfFileHeaderWhenFileHeaderlIsNullThrowsException() throws ZipException {
    expectZipException("input parameters is null, cannot determine index of file header");
    HeaderUtil.getIndexOfFileHeader(new ZipModel(), null);
  }

  @Test
  public void testGetIndexOfFileHeaderWhenCentralDirectoryIsNullReturnsNegativeOne() throws ZipException {
    ZipModel zipModel = new ZipModel();
    zipModel.setCentralDirectory(null);

    assertThat(HeaderUtil.getIndexOfFileHeader(zipModel, new FileHeader())).isEqualTo(-1);
  }

  @Test
  public void testGetIndexOfFileHeaderWhenFileHeadersIsNullReturnsNegativeOne() throws ZipException {
    ZipModel zipModel = new ZipModel();
    zipModel.getCentralDirectory().setFileHeaders(null);

    assertThat(HeaderUtil.getIndexOfFileHeader(zipModel, new FileHeader())).isEqualTo(-1);
  }

  @Test
  public void testGetIndexOfFileHeaderWhenFileHeadersIsEmptyReturnsNegativeOne() throws ZipException {
    ZipModel zipModel = new ZipModel();
    zipModel.getCentralDirectory().setFileHeaders(Collections.emptyList());

    assertThat(HeaderUtil.getIndexOfFileHeader(zipModel, new FileHeader())).isEqualTo(-1);
  }

  @Test
  public void testGetIndexOfFileHeaderWithNullFileNameInFileHeaderThrowsException() throws ZipException {
    expectZipException("file name in file header is empty or null, cannot determine index of file header");

    ZipModel zipModel = new ZipModel();
    zipModel.getCentralDirectory().setFileHeaders(Collections.singletonList(new FileHeader()));

    HeaderUtil.getIndexOfFileHeader(zipModel, new FileHeader());
  }

  @Test
  public void testGetIndexOfFileHeaderWithEmptyFileNameInFileHeaderThrowsException() throws ZipException {
    expectZipException("file name in file header is empty or null, cannot determine index of file header");

    ZipModel zipModel = new ZipModel();
    zipModel.getCentralDirectory().setFileHeaders(Collections.singletonList(new FileHeader()));
    FileHeader fileHeader = new FileHeader();
    fileHeader.setFileName("");

    HeaderUtil.getIndexOfFileHeader(zipModel, new FileHeader());
  }

  @Test
  public void testGetIndexOfFileHeaderGetsIndexSuccessfully() throws ZipException {
    String fileNamePrefix = "FILE_NAME_";
    int numberOfEntriesToAdd = 10;
    List<FileHeader> fileHeadersInZipModel = generateFileHeaderWithFileNames(fileNamePrefix, numberOfEntriesToAdd);
    ZipModel zipModel = new ZipModel();
    zipModel.getCentralDirectory().setFileHeaders(fileHeadersInZipModel);

    FileHeader fileHeaderToFind = new FileHeader();
    for (int i = 0; i < numberOfEntriesToAdd; i++) {
      fileHeaderToFind.setFileName(fileNamePrefix + i);
      assertThat(HeaderUtil.getIndexOfFileHeader(zipModel, fileHeaderToFind)).isEqualTo(i);
    }

    fileHeaderToFind.setFileName(fileNamePrefix + numberOfEntriesToAdd);
    assertThat(HeaderUtil.getIndexOfFileHeader(zipModel, fileHeaderToFind)).isEqualTo(-1);
  }

  @Test
  public void testDecodeStringWithCharsetForUtf8() {
    String utf8StringToEncode = "asdäüöö";
    byte[] utf8EncodedBytes = utf8StringToEncode.getBytes(StandardCharsets.UTF_8);

    assertThat(HeaderUtil.decodeStringWithCharset(utf8EncodedBytes, true)).isEqualTo(utf8StringToEncode);

  }

  @Test
  public void testDecodeStringWithCharsetWithoutUtf8ForUtf8String() {
    String utf8StringToEncode = "asdäüöö";
    byte[] utf8EncodedBytes = utf8StringToEncode.getBytes(StandardCharsets.UTF_8);

    assertThat(HeaderUtil.decodeStringWithCharset(utf8EncodedBytes, false)).isNotEqualTo(utf8StringToEncode);

  }

  @Test
  public void testDecodeStringWithCharsetWithoutUtf8AndWithEnglishChars() {
    String plainString = "asdasda234234";
    byte[] plainEncodedBytes = plainString.getBytes();

    assertThat(HeaderUtil.decodeStringWithCharset(plainEncodedBytes, false)).isEqualTo(plainString);

  }

  private List<FileHeader> generateFileHeaderWithFileNames(String fileNamePrefix, int numberOfEntriesToAdd) {
    List<FileHeader> fileHeaders = new ArrayList<>();
    for (int i = 0; i < numberOfEntriesToAdd; i++) {
      fileHeaders.add(generateFileHeader(fileNamePrefix + i));
    }
    fileHeaders.add(generateFileHeader(""));
    fileHeaders.add(generateFileHeader(null));
    return fileHeaders;
  }

  private FileHeader generateFileHeader(String fileName) {
    FileHeader fileHeader = new FileHeader();
    fileHeader.setFileName(fileName);
    return fileHeader;
  }

  private void expectZipException(String message) {
    expectedException.expectMessage(message);
    expectedException.expect(ZipException.class);
  }
}