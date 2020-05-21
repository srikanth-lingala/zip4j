package net.lingala.zip4j.headers;

import net.lingala.zip4j.AbstractIT;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.outputstream.SplitOutputStream;
import net.lingala.zip4j.model.CentralDirectory;
import net.lingala.zip4j.model.EndOfCentralDirectoryRecord;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.model.enums.RandomAccessFileMode;
import net.lingala.zip4j.util.BitUtils;
import net.lingala.zip4j.util.InternalZipConstants;
import net.lingala.zip4j.util.RawIO;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class HeaderReaderIT extends AbstractIT {

  private static final String FILE_NAME_PREFIX = "FILE_NAME_ÄÜß_";
  private static final String END_OF_CENTRAL_DIR_COMMENT = "END_OF_CENTRAL_DIR_COMMENT_ÜÄÖÖÖÄ";

  private FileHeaderFactory fileHeaderFactory = new FileHeaderFactory();
  private HeaderReader headerReader = new HeaderReader();
  private HeaderWriter headerWriter = new HeaderWriter();
  private RawIO rawIO = new RawIO();

  @Test
  public void testReadAllHeadersWith10Entries() throws IOException {
    int numberOfEntries = 10;
    ZipModel actualZipModel = generateZipHeadersFile(numberOfEntries, EncryptionMethod.NONE);

    try(RandomAccessFile randomAccessFile = initializeRandomAccessFile(actualZipModel.getZipFile())) {
      ZipModel readZipModel = headerReader.readAllHeaders(randomAccessFile, null);
      verifyZipModel(readZipModel, 10, false);
      assertThat(readZipModel.getEndOfCentralDirectoryRecord().getComment()).isEmpty();
    }
  }

  @Test
  public void testReadAllHeadersWithEndOfCentralDirectoryComment() throws IOException {
    ZipModel actualZipModel = generateZipModel(1);
    actualZipModel.getEndOfCentralDirectoryRecord().setComment(END_OF_CENTRAL_DIR_COMMENT);
    File headersFile = writeZipHeaders(actualZipModel);
    actualZipModel.setZipFile(headersFile);

    try(RandomAccessFile randomAccessFile = initializeRandomAccessFile(actualZipModel.getZipFile())) {
      ZipModel readZipModel = headerReader.readAllHeaders(randomAccessFile, InternalZipConstants.CHARSET_UTF_8);
      verifyZipModel(readZipModel, 1, false);

      EndOfCentralDirectoryRecord endOfCentralDirectoryRecord = readZipModel.getEndOfCentralDirectoryRecord();
      assertThat(endOfCentralDirectoryRecord.getComment()).isEqualTo(END_OF_CENTRAL_DIR_COMMENT);
    }
  }

  @Test
  public void testReadAllWithoutEndOfCentralDirectoryRecordThrowsException() throws IOException {
    try(RandomAccessFile randomAccessFile = new RandomAccessFile(temporaryFolder.newFile(),
        RandomAccessFileMode.WRITE.getValue())) {
      //Create an empty file
      randomAccessFile.seek(4000);
      randomAccessFile.write(1);

      headerReader.readAllHeaders(randomAccessFile, null);
      fail("Should throw an exception");
    } catch (ZipException e) {
      assertThat(e.getMessage()).isEqualTo("Zip headers not found. Probably not a zip file");
    }
  }

  @Test
  public void testReadAllWithoutEnoughHeaderDataThrowsException() throws IOException {
    try(RandomAccessFile randomAccessFile = new RandomAccessFile(temporaryFolder.newFile(),
        RandomAccessFileMode.WRITE.getValue())) {
      //Create an empty file
      randomAccessFile.seek(1000);
      randomAccessFile.write(1);

      headerReader.readAllHeaders(randomAccessFile, null);
      fail("Should throw an exception");
    } catch (ZipException e) {
      assertThat(e.getMessage()).isEqualTo("Zip headers not found. Probably not a zip file");
      assertThat(e.getCause() instanceof IOException);
    }
  }

  @Test
  public void testReadAllWithoutFileHeaderSignatureThrowsException() throws IOException {
    ZipModel actualZipModel = generateZipModel(2);
    actualZipModel.getCentralDirectory().getFileHeaders().get(1).setSignature(HeaderSignature.DIGITAL_SIGNATURE);
    File headersFile = writeZipHeaders(actualZipModel);
    actualZipModel.setZipFile(headersFile);

    try(RandomAccessFile randomAccessFile = new RandomAccessFile(actualZipModel.getZipFile(),
        RandomAccessFileMode.READ.getValue())) {
      headerReader.readAllHeaders(randomAccessFile, null);
      fail("Should throw an exception");
    } catch (ZipException e) {
      assertThat(e.getMessage()).isEqualTo("Expected central directory entry not found (#2)");
    }
  }

  @Test
  public void testReadAllWithFileNameContainsWindowsDriveExcludesIt() throws IOException {
    String fileName = "C:\\test.txt";
    ZipModel actualZipModel = generateZipModel(1);
    actualZipModel.getCentralDirectory().getFileHeaders().get(0).setFileName(fileName);
    File headersFile = writeZipHeaders(actualZipModel);
    actualZipModel.setZipFile(headersFile);

    try(RandomAccessFile randomAccessFile = new RandomAccessFile(actualZipModel.getZipFile(),
        RandomAccessFileMode.READ.getValue())) {
      ZipModel readZipModel = headerReader.readAllHeaders(randomAccessFile, null);
      FileHeader fileHeader = readZipModel.getCentralDirectory().getFileHeaders().get(0);
      assertThat(fileHeader.getFileName()).isEqualTo("test.txt");
    }
  }

  @Test
  public void testReadAllWithoutFileNameWritesNull() throws IOException {
    ZipModel actualZipModel = generateZipModel(1);
    actualZipModel.getCentralDirectory().getFileHeaders().get(0).setFileName(null);
    File headersFile = writeZipHeaders(actualZipModel);
    actualZipModel.setZipFile(headersFile);

    try(RandomAccessFile randomAccessFile = new RandomAccessFile(actualZipModel.getZipFile(),
        RandomAccessFileMode.READ.getValue())) {
      ZipModel readZipModel = headerReader.readAllHeaders(randomAccessFile, null);
      FileHeader fileHeader = readZipModel.getCentralDirectory().getFileHeaders().get(0);
      assertThat(fileHeader.getFileName()).isNull();
    }
  }

  @Test
  public void testReadAllWithJapaneseCharacters() throws IOException {
    testWithoutUtf8FileName("公ゃ的年社", "育ざどろめ", true, false);
  }

  @Test
  public void testReadAllWithoutUtf8FlagDecodesWithoutCharsetFlagForJapaneseCharactersDoesNotMatch()
      throws IOException {
    testWithoutUtf8FileName("公ゃ的年社", "育ざどろめ", false, true, InternalZipConstants.CHARSET_UTF_8);
  }

  @Test
  public void testReadAllWithoutUtf8FlagDecodesWithoutCharsetFlagForEnglishCharactersMatches()
      throws IOException {
    testWithoutUtf8FileName("SOME_TEXT", "SOME_COMMENT", true, true);
  }

  @Test
  public void testReadAllWithAesEncryption() throws IOException {
    ZipModel actualZipModel = generateZipHeadersFile(3, EncryptionMethod.AES);

    try(RandomAccessFile randomAccessFile = new RandomAccessFile(actualZipModel.getZipFile(),
        RandomAccessFileMode.READ.getValue())) {
      ZipModel readZipModel = headerReader.readAllHeaders(randomAccessFile, null);
      for (FileHeader fileHeader : readZipModel.getCentralDirectory().getFileHeaders()) {
        assertThat(fileHeader.getAesExtraDataRecord()).isNotNull();
        assertThat(fileHeader.getAesExtraDataRecord().getAesKeyStrength()).isEqualTo(AesKeyStrength.KEY_STRENGTH_256);
      }
    }
  }

  @Test
  public void testReadAllWithStandardZipEncryption() throws IOException {
    ZipModel actualZipModel = generateZipHeadersFile(3, EncryptionMethod.ZIP_STANDARD);

    try(RandomAccessFile randomAccessFile = new RandomAccessFile(actualZipModel.getZipFile(),
        RandomAccessFileMode.READ.getValue())) {
      ZipModel readZipModel = headerReader.readAllHeaders(randomAccessFile, null);
      for (FileHeader fileHeader : readZipModel.getCentralDirectory().getFileHeaders()) {
        assertThat(fileHeader.isEncrypted()).isTrue();
        assertThat(fileHeader.getEncryptionMethod()).isEqualTo(EncryptionMethod.ZIP_STANDARD);
        assertThat(fileHeader.getAesExtraDataRecord()).isNull();
      }
    }
  }

  @Test
  public void testReadAllZip64Format() throws IOException {
    ZipModel actualZipModel = generateZipModel(1);
    long entrySize = InternalZipConstants.ZIP_64_SIZE_LIMIT + 1;
    actualZipModel.getCentralDirectory().getFileHeaders().get(0).setUncompressedSize(entrySize);
    actualZipModel.getCentralDirectory().getFileHeaders().get(0).setCompressedSize(entrySize + 100);
    File headersFile = writeZipHeaders(actualZipModel);
    actualZipModel.setZipFile(headersFile);

    try(RandomAccessFile randomAccessFile = new RandomAccessFile(actualZipModel.getZipFile(),
        RandomAccessFileMode.READ.getValue())) {
      ZipModel readZipModel = headerReader.readAllHeaders(randomAccessFile, null);
      assertThat(readZipModel.isZip64Format()).isTrue();
      assertThat(readZipModel.getZip64EndOfCentralDirectoryRecord()).isNotNull();
      assertThat(readZipModel.getZip64EndOfCentralDirectoryLocator()).isNotNull();
      FileHeader fileHeader = actualZipModel.getCentralDirectory().getFileHeaders().get(0);
      assertThat(fileHeader.getUncompressedSize()).isEqualTo(entrySize);
      assertThat(fileHeader.getCompressedSize()).isEqualTo(entrySize + 100);
    }
  }

  @Test
  public void testReadLocalFileHeader() throws IOException {
    long entrySize = InternalZipConstants.ZIP_64_SIZE_LIMIT + 1;
    File headerFile = generateAndWriteLocalFileHeader(entrySize, EncryptionMethod.NONE);

    try(InputStream inputStream = new FileInputStream(headerFile)) {
      LocalFileHeader readLocalFileHeader = headerReader.readLocalFileHeader(inputStream, InternalZipConstants.CHARSET_UTF_8);
      assertThat(readLocalFileHeader).isNotNull();
      assertThat(readLocalFileHeader.getCompressedSize()).isEqualTo(entrySize);
      assertThat(readLocalFileHeader.getUncompressedSize()).isEqualTo(entrySize);
    }
  }

  @Test
  public void testReadLocalFileHeaderWithAesEncryption() throws IOException {
    long entrySize = InternalZipConstants.ZIP_64_SIZE_LIMIT - 1001 ;
    File headerFile = generateAndWriteLocalFileHeader(entrySize, EncryptionMethod.AES);

    try(InputStream inputStream = new FileInputStream(headerFile)) {
      LocalFileHeader readLocalFileHeader = headerReader.readLocalFileHeader(inputStream, InternalZipConstants.CHARSET_UTF_8);
      assertThat(readLocalFileHeader).isNotNull();
      assertThat(readLocalFileHeader.getCompressedSize()).isEqualTo(entrySize);
      assertThat(readLocalFileHeader.getUncompressedSize()).isEqualTo(entrySize);
      assertThat(readLocalFileHeader.getAesExtraDataRecord()).isNotNull();
      assertThat(readLocalFileHeader.getAesExtraDataRecord().getAesKeyStrength())
          .isEqualTo(AesKeyStrength.KEY_STRENGTH_256);
    }
  }

  @Test
  public void testReadDataDescriptorWithSignature() {

  }

  private void testWithoutUtf8FileName(String fileName, String entryComment, boolean shouldFileNamesMatch,
                                       boolean unsetUtf8Flag) throws IOException {
    testWithoutUtf8FileName(fileName, entryComment, shouldFileNamesMatch, unsetUtf8Flag, null);
  }

  private void testWithoutUtf8FileName(String fileName, String entryComment, boolean shouldFileNamesMatch,
                                       boolean unsetUtf8Flag, Charset charsetToUseForReading) throws IOException {
    ZipModel actualZipModel = generateZipModel(3);
    FileHeader secondFileHeader = actualZipModel.getCentralDirectory().getFileHeaders().get(1);

    if (unsetUtf8Flag) {
      // Unset utf8 flag
      byte[] generalPurposeBytes = secondFileHeader.getGeneralPurposeFlag();
      generalPurposeBytes[1] = BitUtils.unsetBit(generalPurposeBytes[1], 3);
      secondFileHeader.setGeneralPurposeFlag(generalPurposeBytes);
    }
    secondFileHeader.setFileName(fileName);
    secondFileHeader.setFileComment(entryComment);

    File headersFile = writeZipHeaders(actualZipModel);
    actualZipModel.setZipFile(headersFile);

    try(RandomAccessFile randomAccessFile = new RandomAccessFile(actualZipModel.getZipFile(),
        RandomAccessFileMode.READ.getValue())) {
      ZipModel readZipModel = headerReader.readAllHeaders(randomAccessFile, charsetToUseForReading);
      FileHeader fileHeader = readZipModel.getCentralDirectory().getFileHeaders().get(1);
      if (shouldFileNamesMatch) {
        assertThat(fileHeader.getFileName()).isEqualTo(fileName);
        assertThat(fileHeader.getFileComment()).isEqualTo(entryComment);
      } else {
        assertThat(fileHeader.getFileName()).isNotEqualTo(fileName);
        assertThat(fileHeader.getFileComment()).isNotEqualTo(entryComment);
      }

      assertThat(readZipModel.getCentralDirectory().getFileHeaders().get(0).getFileCommentLength()).isEqualTo(0);
      assertThat(readZipModel.getCentralDirectory().getFileHeaders().get(0).getFileComment()).isNull();
      assertThat(readZipModel.getCentralDirectory().getFileHeaders().get(2).getFileCommentLength()).isEqualTo(0);
      assertThat(readZipModel.getCentralDirectory().getFileHeaders().get(2).getFileComment()).isNull();
    }
  }

  private RandomAccessFile initializeRandomAccessFile(File headersFile) throws IOException {
    return new RandomAccessFile(headersFile, RandomAccessFileMode.READ.getValue());
  }

  private void verifyZipModel(ZipModel readZipModel, int numberOfExpectedEntries, boolean isZip64) {
    assertThat(readZipModel).isNotNull();
    verifyEndOfCentralDirectory(readZipModel.getEndOfCentralDirectoryRecord(), numberOfExpectedEntries, isZip64);
    verifyCentralDirectory(readZipModel.getCentralDirectory(), numberOfExpectedEntries);

  }

  private void verifyEndOfCentralDirectory(EndOfCentralDirectoryRecord endOfCentralDirectoryRecord, int numberOfEntries,
                                           boolean isZip64) {
    assertThat(endOfCentralDirectoryRecord).isNotNull();
    endOfCentralDirectoryRecord.setSignature(HeaderSignature.END_OF_CENTRAL_DIRECTORY);
    assertThat(endOfCentralDirectoryRecord.getTotalNumberOfEntriesInCentralDirectory()).isEqualTo(numberOfEntries);

    if (!isZip64) {
      assertThat(endOfCentralDirectoryRecord.getNumberOfThisDisk()).isEqualTo(0);
      assertThat(endOfCentralDirectoryRecord.getNumberOfThisDiskStartOfCentralDir()).isEqualTo(0);
      assertThat(endOfCentralDirectoryRecord.getTotalNumberOfEntriesInCentralDirectoryOnThisDisk())
          .isEqualTo(numberOfEntries);
      assertThat(endOfCentralDirectoryRecord.getSizeOfCentralDirectory()).isNotZero();
    }
  }

  private void verifyCentralDirectory(CentralDirectory centralDirectory, int numberOfExpectedEntries) {
    assertThat(centralDirectory).isNotNull();
    verifyFileHeaders(centralDirectory.getFileHeaders(), numberOfExpectedEntries);
  }

  private void verifyFileHeaders(List<FileHeader> fileHeaders, int numberOfExpectedEntries) {
    assertThat(fileHeaders).isNotEmpty();
    assertThat(fileHeaders).hasSize(numberOfExpectedEntries);

    for (int i = 0; i < numberOfExpectedEntries; i++) {
      String expectedFileName = FILE_NAME_PREFIX + i;
      FileHeader fileHeader = fileHeaders.get(i);
      assertThat(fileHeader.getFileName()).isEqualTo(expectedFileName);
      int expectedFileNameLength = expectedFileName.getBytes(InternalZipConstants.CHARSET_UTF_8).length;
      assertThat(fileHeader.getFileNameLength()).isEqualTo(expectedFileNameLength);
    }
  }

  private ZipModel generateZipHeadersFile(int numberOfEntries, EncryptionMethod encryptionMethod)
      throws IOException {
    ZipModel zipModel = generateZipModel(numberOfEntries, encryptionMethod);
    File headersFile = writeZipHeaders(zipModel);
    zipModel.setZipFile(headersFile);
    return zipModel;
  }

  private ZipModel generateZipModel(int numberOfEntries) throws ZipException {
    return generateZipModel(numberOfEntries, EncryptionMethod.NONE);
  }

  private ZipModel generateZipModel(int numberOfEntries, EncryptionMethod encryptionMethod)
      throws ZipException {
    ZipParameters zipParameters = generateZipParameters(encryptionMethod);
    ZipModel zipModel = new ZipModel();
    zipModel.getCentralDirectory().setFileHeaders(generateFileHeaders(zipParameters, numberOfEntries));
    return zipModel;
  }

  private ZipParameters generateZipParameters(EncryptionMethod encryptionMethod) {
    ZipParameters zipParameters = new ZipParameters();

    if (encryptionMethod != null && encryptionMethod != EncryptionMethod.NONE) {
      zipParameters.setEncryptFiles(true);
      zipParameters.setEncryptionMethod(encryptionMethod);
    }

    return zipParameters;
  }

  private List<FileHeader> generateFileHeaders(ZipParameters zipParameters, int numberOfEntries)
      throws ZipException {
    List<FileHeader> fileHeaders = new ArrayList<>();
    for (int i = 0; i < numberOfEntries; i++) {
      zipParameters.setFileNameInZip(FILE_NAME_PREFIX + i);
      FileHeader fileHeader = fileHeaderFactory.generateFileHeader(zipParameters, false, 0, InternalZipConstants.CHARSET_UTF_8, rawIO);
      fileHeaders.add(fileHeader);
    }
    return fileHeaders;
  }

  private LocalFileHeader generateLocalFileHeader(long entrySize, EncryptionMethod encryptionMethod)
      throws ZipException {
    List<FileHeader> fileHeaders = generateFileHeaders(generateZipParameters(encryptionMethod), 1);
    LocalFileHeader localFileHeader = fileHeaderFactory.generateLocalFileHeader(fileHeaders.get(0));
    localFileHeader.setCompressedSize(entrySize);
    localFileHeader.setUncompressedSize(entrySize);
    return localFileHeader;
  }

  private File generateAndWriteLocalFileHeader(long entrySize, EncryptionMethod encryptionMethod)
      throws IOException {
    LocalFileHeader localFileHeader = generateLocalFileHeader(entrySize, encryptionMethod);

    if (encryptionMethod != null && encryptionMethod != EncryptionMethod.NONE) {
      localFileHeader.setEncrypted(true);
      localFileHeader.setEncryptionMethod(encryptionMethod);
    }

    ZipModel zipModel = generateZipModel(1);
    File headerFile = temporaryFolder.newFile();

    try(OutputStream outputStream = new FileOutputStream(headerFile)) {
      headerWriter.writeLocalFileHeader(zipModel, localFileHeader, outputStream, InternalZipConstants.CHARSET_UTF_8);
    }

    return headerFile;
  }

  private File writeZipHeaders(ZipModel zipModel) throws IOException {
    File headersFile = temporaryFolder.newFile();
    try(SplitOutputStream splitOutputStream = new SplitOutputStream(headersFile)) {
      headerWriter.finalizeZipFile(zipModel, splitOutputStream, InternalZipConstants.CHARSET_UTF_8);
      return headersFile;
    }
  }
}