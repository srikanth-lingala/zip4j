package net.lingala.zip4j.headers;

import net.lingala.zip4j.AbstractIT;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.outputstream.CountingOutputStream;
import net.lingala.zip4j.io.outputstream.SplitOutputStream;
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.CentralDirectory;
import net.lingala.zip4j.model.DataDescriptor;
import net.lingala.zip4j.model.ExtraDataRecord;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.Zip64ExtendedInfo;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.AesVersion;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.model.enums.RandomAccessFileMode;
import net.lingala.zip4j.util.BitUtils;
import net.lingala.zip4j.util.InternalZipConstants;
import net.lingala.zip4j.util.RawIO;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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

import static net.lingala.zip4j.util.Zip4jUtil.javaToDosTime;
import static org.assertj.core.api.Assertions.assertThat;

public class HeaderWriterIT extends AbstractIT {

  private static final String FILE_NAME_PREFIX = "FILE_NAME_";
  private static final long COMPRESSED_SIZE = 4234L;
  private static final long UNCOMPRESSED_SIZE = 23423L;
  private static final long COMPRESSED_SIZE_ZIP64 = InternalZipConstants.ZIP_64_SIZE_LIMIT + 1;
  private static final long UNCOMPRESSED_SIZE_ZIP64 = InternalZipConstants.ZIP_64_SIZE_LIMIT + 1;
  private static final int VERSION_MADE_BY = 20;
  private static final int VERSION_NEEDED_TO_EXTRACT = 20;
  private static final long LAST_MODIFIED_FILE_TIME = javaToDosTime(System.currentTimeMillis());
  private static final byte[] EXTERNAL_FILE_ATTRIBUTES = new byte[] {23, 43, 0, 0};
  private static final String FILE_COMMENT_PREFIX = "FILE_COMMENT_PREFIX_";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private RawIO rawIO = new RawIO();
  private HeaderWriter headerWriter = new HeaderWriter();
  private HeaderReader headerReader = new HeaderReader();

  @Test
  public void testWriteLocalFileHeaderSimpleLocalFileHeaderSuccessScenario() throws IOException {
    ZipModel zipModel = createZipModel(10);
    LocalFileHeader localFileHeaderToWrite = createLocalFileHeader("LFH", COMPRESSED_SIZE, UNCOMPRESSED_SIZE, true);
    File headersFile = temporaryFolder.newFile();

    try(OutputStream outputStream = new FileOutputStream(headersFile)) {
      headerWriter.writeLocalFileHeader(zipModel, localFileHeaderToWrite, outputStream, InternalZipConstants.CHARSET_UTF_8);
    }

    try(InputStream inputStream = new FileInputStream(headersFile)) {
      LocalFileHeader readLocalFileHeader = headerReader.readLocalFileHeader(inputStream, InternalZipConstants.CHARSET_UTF_8);
      verifyLocalFileHeader(readLocalFileHeader, FILE_NAME_PREFIX + "LFH", COMPRESSED_SIZE, UNCOMPRESSED_SIZE);
    }
  }

  @Test
  public void testWriteLocalFileHeaderForZip64Format() throws IOException {
    ZipModel zipModel = createZipModel(10);
    LocalFileHeader localFileHeaderToWrite = createLocalFileHeader("LFH", COMPRESSED_SIZE_ZIP64,
        UNCOMPRESSED_SIZE_ZIP64, true);
    File headersFile = temporaryFolder.newFile();

    try(OutputStream outputStream = new FileOutputStream(headersFile)) {
      headerWriter.writeLocalFileHeader(zipModel, localFileHeaderToWrite, outputStream, InternalZipConstants.CHARSET_UTF_8);
    }

    try(InputStream inputStream = new FileInputStream(headersFile)) {
      LocalFileHeader readLocalFileHeader = headerReader.readLocalFileHeader(inputStream, InternalZipConstants.CHARSET_UTF_8);
      verifyLocalFileHeader(readLocalFileHeader, FILE_NAME_PREFIX + "LFH", COMPRESSED_SIZE_ZIP64,
          UNCOMPRESSED_SIZE_ZIP64);
      verifyZip64ExtendedInfo(readLocalFileHeader.getZip64ExtendedInfo(), COMPRESSED_SIZE_ZIP64,
          UNCOMPRESSED_SIZE_ZIP64, -1, -1);
    }

    verifyEntrySizesIsMaxValueInLFHWhenZip64Format(headersFile);
  }

  @Test
  public void testWriteLocalFileHeaderJapaneseCharactersInFileNameAndWithUtf8FlagShouldMatch()
      throws IOException {
    testWriteLocalFileHeaderWithFileName("公ゃ的年社", true, true);
  }

  @Test
  public void testWriteLocalFileHeaderEnglishCharactersInFileNameWithoutUtf8ShouldMatch()
      throws IOException {
    testWriteLocalFileHeaderWithFileName("SOME_TEXT", false, true);
  }

  @Test
  public void testWriteLocalFileHeaderJapaneseCharactersInFileNameWithoutUtf8ShouldNotMatch()
      throws IOException {
    testWriteLocalFileHeaderWithFileName("公ゃ的年社", false, false);
  }

  @Test
  public void testWriteLocalFileHeaderJapaneseCharactersInFileNameWithUtf8ShouldMatch()
      throws IOException {
    testWriteLocalFileHeaderWithFileName("公ゃ的年社", true, true);
  }

  @Test
  public void testWriteLocalFileHeaderJapaneseCharactersInFileNameWithCharsetMs932ShouldMatch()
          throws IOException {
    testWriteLocalFileHeaderWithFileNameAndCharset("公ゃ的年社", false, true, CHARSET_MS_932);
  }

  @Test
  public void testWriteLocalFileHeaderJapaneseCharactersInFileNameWithUTF8CharsetWithUtf8ShouldMatch()
          throws IOException {
    testWriteLocalFileHeaderWithFileNameAndCharset("公ゃ的年社", true, true, InternalZipConstants.CHARSET_UTF_8);
  }

  @Test
  public void testWriteLocalFileHeaderJapaneseCharactersInFileNameWithUTF8CharsetWithoutUtf8ShouldMatch()
          throws IOException {
    testWriteLocalFileHeaderWithFileNameAndCharset("公ゃ的年社", true, true, InternalZipConstants.CHARSET_UTF_8);
  }

  @Test
  public void testWriteLocalFileHeaderWithAes256v1() throws IOException {
    testWriteLocalFileHeaderWithAes(AesKeyStrength.KEY_STRENGTH_256, AesVersion.ONE);
  }

  @Test
  public void testWriteLocalFileHeaderWithAes192v1() throws IOException {
    testWriteLocalFileHeaderWithAes(AesKeyStrength.KEY_STRENGTH_192, AesVersion.ONE);
  }

  @Test
  public void testWriteLocalFileHeaderWithAes128v1() throws IOException {
    testWriteLocalFileHeaderWithAes(AesKeyStrength.KEY_STRENGTH_128, AesVersion.ONE);
  }

  @Test
  public void testWriteLocalFileHeaderWithAes256v2() throws IOException {
    testWriteLocalFileHeaderWithAes(AesKeyStrength.KEY_STRENGTH_256, AesVersion.TWO);
  }

  @Test
  public void testWriteLocalFileHeaderWithAes192v2() throws IOException {
    testWriteLocalFileHeaderWithAes(AesKeyStrength.KEY_STRENGTH_192, AesVersion.TWO);
  }

  @Test
  public void testWriteLocalFileHeaderWithAes128v2() throws IOException {
    testWriteLocalFileHeaderWithAes(AesKeyStrength.KEY_STRENGTH_128, AesVersion.TWO);
  }

  @Test
  public void testWriteExtendedLocalFileHeaderWhenLocalFileHeaderIsNullThrowsException() throws IOException {
    expectedException.expectMessage("input parameters is null, cannot write extended local header");
    expectedException.expect(ZipException.class);
    headerWriter.writeExtendedLocalHeader(null, new FileOutputStream(temporaryFolder.newFile()));
  }

  @Test
  public void testWriteExtendedLocalFileHeaderWhenOutputStreamIsNullThrowsException() throws IOException {
    expectedException.expectMessage("input parameters is null, cannot write extended local header");
    expectedException.expect(ZipException.class);
    headerWriter.writeExtendedLocalHeader(new LocalFileHeader(), null);
  }

  @Test
  public void testWriteExtendedLocalFileHeaderNonZip64FormatWritesSuccessfully() throws IOException {
    testWriteExtendedLocalFileHeader(COMPRESSED_SIZE + 99, UNCOMPRESSED_SIZE + 423, 2342342L, false);
  }

  @Test
  public void testWriteExtendedLocalFileHeaderZip64FormatWritesSuccessfully() throws IOException {
    testWriteExtendedLocalFileHeader(COMPRESSED_SIZE_ZIP64 + 99, UNCOMPRESSED_SIZE_ZIP64 + 423, 32452342L, true);
  }

  @Test
  public void testFinalizeZipFileWhenZipModelNullThrowsException() throws IOException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("input parameters is null, cannot finalize zip file");

    headerWriter.finalizeZipFile(null, new FileOutputStream(temporaryFolder.newFile()), InternalZipConstants.CHARSET_UTF_8);
  }

  @Test
  public void testFinalizeZipFileWhenOutputStreamIsNullThrowsException() throws IOException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("input parameters is null, cannot finalize zip file");

    headerWriter.finalizeZipFile(new ZipModel(), null, InternalZipConstants.CHARSET_UTF_8);
  }

  @Test
  public void testFinalizeZipFileForNonZip64Format() throws IOException {
    ZipModel zipModel = createZipModel(10);
    File headersFile = temporaryFolder.newFile();

    try(OutputStream outputStream = new FileOutputStream(headersFile)) {
      headerWriter.finalizeZipFile(zipModel, outputStream, InternalZipConstants.CHARSET_UTF_8);
    }

    try(RandomAccessFile randomAccessFile = new RandomAccessFile(headersFile, RandomAccessFileMode.READ.getValue())) {
      ZipModel readZipModel = headerReader.readAllHeaders(randomAccessFile, null);
      verifyZipModel(readZipModel, 10);

      for (FileHeader fileHeader : readZipModel.getCentralDirectory().getFileHeaders()) {
        assertThat(fileHeader.getZip64ExtendedInfo()).isNull();
        assertThat(fileHeader.getAesExtraDataRecord()).isNull();
        assertThat(fileHeader.getExtraFieldLength()).isZero();
      }
    }
  }

  @Test
  public void testFinalizeZipFileWithNullExtraDataWritesSuccessfully() throws IOException {
    testFinalizeZipFileWhenExtraDataRecordIsNullOrEmpty(null);
  }

  @Test
  public void testFinalizeZipFileWithEmptyExtraDataWritesSuccessfully() throws IOException {
    testFinalizeZipFileWhenExtraDataRecordIsNullOrEmpty(new byte[0]);
  }

  @Test
  public void testFinalizeZipFileForZip64Format() throws IOException {
    ZipModel zipModel = createZipModel(10, COMPRESSED_SIZE_ZIP64, UNCOMPRESSED_SIZE_ZIP64);
    File headersFile = temporaryFolder.newFile();

    try(OutputStream outputStream = new FileOutputStream(headersFile)) {
      headerWriter.finalizeZipFile(zipModel, outputStream, InternalZipConstants.CHARSET_UTF_8);
    }

    try(RandomAccessFile randomAccessFile = new RandomAccessFile(headersFile, RandomAccessFileMode.READ.getValue())) {
      ZipModel readZipModel = headerReader.readAllHeaders(randomAccessFile, null);
      verifyZipModel(readZipModel, 10, COMPRESSED_SIZE_ZIP64, UNCOMPRESSED_SIZE_ZIP64, true);

      List<FileHeader> fileHeaders = readZipModel.getCentralDirectory().getFileHeaders();
      for (FileHeader fileHeader : fileHeaders) {
        verifyZip64ExtendedInfo(fileHeader.getZip64ExtendedInfo(), COMPRESSED_SIZE_ZIP64, UNCOMPRESSED_SIZE_ZIP64, 0,
            0);
        assertThat(fileHeader.getAesExtraDataRecord()).isNull();
      }
    }
  }

  @Test
  public void testFinalizeZipFileForAes() throws IOException {
    ZipModel zipModel = createZipModel(10);
    setFileHeadersAsAesEncrypted(zipModel.getCentralDirectory().getFileHeaders(), AesKeyStrength.KEY_STRENGTH_192);
    File headersFile = temporaryFolder.newFile();

    try(OutputStream outputStream = new FileOutputStream(headersFile)) {
      headerWriter.finalizeZipFile(zipModel, outputStream, InternalZipConstants.CHARSET_UTF_8);
    }

    try(RandomAccessFile randomAccessFile = new RandomAccessFile(headersFile, RandomAccessFileMode.READ.getValue())) {
      ZipModel readZipModel = headerReader.readAllHeaders(randomAccessFile, null);
      verifyZipModel(readZipModel, 10);

      for (FileHeader fileHeader : readZipModel.getCentralDirectory().getFileHeaders()) {
        verifyAesExtraDataRecord(fileHeader.getAesExtraDataRecord(), AesKeyStrength.KEY_STRENGTH_192, AesVersion.TWO);
        assertThat(fileHeader.getZip64ExtendedInfo()).isNull();
      }
    }
  }

  @Test
  public void testFinalizeZipFileForZip64FormatForSplitFileWithSplitOutputStream() throws IOException {
    ZipModel zipModel = createZipModel(10, COMPRESSED_SIZE_ZIP64, UNCOMPRESSED_SIZE_ZIP64);
    zipModel.setZip64EndOfCentralDirectoryRecord(null);
    zipModel.setZip64EndOfCentralDirectoryLocator(null);
    File headersFile = temporaryFolder.newFile();

    try(SplitOutputStream outputStream = new SplitOutputStream(headersFile, 65536)) {
      headerWriter.finalizeZipFile(zipModel, outputStream, InternalZipConstants.CHARSET_UTF_8);
    }

    try(RandomAccessFile randomAccessFile = new RandomAccessFile(headersFile, RandomAccessFileMode.READ.getValue())) {
      ZipModel readZipModel = headerReader.readAllHeaders(randomAccessFile, null);
      verifyZipModel(readZipModel, 10, COMPRESSED_SIZE_ZIP64, UNCOMPRESSED_SIZE_ZIP64, true);

      List<FileHeader> fileHeaders = readZipModel.getCentralDirectory().getFileHeaders();
      for (FileHeader fileHeader : fileHeaders) {
        verifyZip64ExtendedInfo(fileHeader.getZip64ExtendedInfo(), COMPRESSED_SIZE_ZIP64, UNCOMPRESSED_SIZE_ZIP64, 0,
            0);
        assertThat(fileHeader.getAesExtraDataRecord()).isNull();
      }
    }
  }

  @Test
  public void testFinalizeZipFileForZip64FormatForSplitFileWithCountingOutputStream() throws IOException {
    ZipModel zipModel = createZipModel(10, COMPRESSED_SIZE_ZIP64, UNCOMPRESSED_SIZE_ZIP64);
    File headersFile = temporaryFolder.newFile();

    try(CountingOutputStream outputStream = new CountingOutputStream(new SplitOutputStream(headersFile, 65536))) {
      headerWriter.finalizeZipFile(zipModel, outputStream, InternalZipConstants.CHARSET_UTF_8);
    }

    try(RandomAccessFile randomAccessFile = new RandomAccessFile(headersFile, RandomAccessFileMode.READ.getValue())) {
      ZipModel readZipModel = headerReader.readAllHeaders(randomAccessFile, null);
      verifyZipModel(readZipModel, 10, COMPRESSED_SIZE_ZIP64, UNCOMPRESSED_SIZE_ZIP64, true);

      List<FileHeader> fileHeaders = readZipModel.getCentralDirectory().getFileHeaders();
      for (FileHeader fileHeader : fileHeaders) {
        verifyZip64ExtendedInfo(fileHeader.getZip64ExtendedInfo(), COMPRESSED_SIZE_ZIP64, UNCOMPRESSED_SIZE_ZIP64, 0,
            0);
        assertThat(fileHeader.getAesExtraDataRecord()).isNull();
      }
    }
  }

  @Test
  public void testFinalizeZipFileWithoutValidationsWhenZipModelNullThrowsException() throws IOException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("input parameters is null, cannot finalize zip file");

    headerWriter.finalizeZipFileWithoutValidations(null, new FileOutputStream(temporaryFolder.newFile()), null);
  }

  @Test
  public void testFinalizeZipFileWithoutValidationsWhenOutputStreamIsNullThrowsException()
      throws IOException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("input parameters is null, cannot finalize zip file");

    headerWriter.finalizeZipFileWithoutValidations(new ZipModel(), null, null);
  }

  @Test
  public void testFinalizeZipFileWithoutValidationsForNonZip64Format() throws IOException {
    ZipModel zipModel = createZipModel(10);
    File headersFile = temporaryFolder.newFile();

    try(OutputStream outputStream = new FileOutputStream(headersFile)) {
      headerWriter.finalizeZipFileWithoutValidations(zipModel, outputStream, InternalZipConstants.CHARSET_UTF_8);
    }

    try(RandomAccessFile randomAccessFile = new RandomAccessFile(headersFile, RandomAccessFileMode.READ.getValue())) {
      ZipModel readZipModel = headerReader.readAllHeaders(randomAccessFile, InternalZipConstants.CHARSET_UTF_8);
      verifyZipModel(readZipModel, 10);

      for (FileHeader fileHeader : readZipModel.getCentralDirectory().getFileHeaders()) {
        assertThat(fileHeader.getZip64ExtendedInfo()).isNull();
        assertThat(fileHeader.getAesExtraDataRecord()).isNull();
        assertThat(fileHeader.getExtraFieldLength()).isZero();
      }
    }
  }

  @Test
  public void testFinalizeZipFileWithoutValidationsForZip64Format() throws IOException {
    ZipModel zipModel = createZipModel(10, COMPRESSED_SIZE_ZIP64, UNCOMPRESSED_SIZE_ZIP64);
    File headersFile = temporaryFolder.newFile();

    try(OutputStream outputStream = new FileOutputStream(headersFile)) {
      headerWriter.finalizeZipFileWithoutValidations(zipModel, outputStream, InternalZipConstants.CHARSET_UTF_8);
    }

    try(RandomAccessFile randomAccessFile = new RandomAccessFile(headersFile, RandomAccessFileMode.READ.getValue())) {
      ZipModel readZipModel = headerReader.readAllHeaders(randomAccessFile, InternalZipConstants.CHARSET_UTF_8);
      verifyZipModel(readZipModel, 10, COMPRESSED_SIZE_ZIP64, UNCOMPRESSED_SIZE_ZIP64, true);

      List<FileHeader> fileHeaders = readZipModel.getCentralDirectory().getFileHeaders();
      for (FileHeader fileHeader : fileHeaders) {
        verifyZip64ExtendedInfo(fileHeader.getZip64ExtendedInfo(), COMPRESSED_SIZE_ZIP64, UNCOMPRESSED_SIZE_ZIP64, 0,
            0);
        assertThat(fileHeader.getAesExtraDataRecord()).isNull();
      }
    }
  }

  @Test
  public void testUpdateLocalFileHeaderWhenFileHeaderIsNullThrowsException() throws IOException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("invalid input parameters, cannot update local file header");

    headerWriter.updateLocalFileHeader(null, new ZipModel(), new SplitOutputStream(temporaryFolder.newFile()));
  }

  @Test
  public void testUpdateLocalFileHeaderWhenZipModelIsNullThrowsException() throws IOException {
    expectedException.expect(ZipException.class);
    expectedException.expectMessage("invalid input parameters, cannot update local file header");

    headerWriter.updateLocalFileHeader(new FileHeader(), null, new SplitOutputStream(temporaryFolder.newFile()));
  }

  @Test
  public void testUpdateLocalFileHeaderForNonZip64() throws IOException {
    File headersFile = temporaryFolder.newFile();
    createAndUpdateLocalFileHeader(headersFile, COMPRESSED_SIZE, UNCOMPRESSED_SIZE, 23423);

    try (InputStream inputStream = new FileInputStream(headersFile)) {
      LocalFileHeader readLocalFileHeader = headerReader.readLocalFileHeader(inputStream, InternalZipConstants.CHARSET_UTF_8);
      assertThat(readLocalFileHeader.getCompressedSize()).isEqualTo(COMPRESSED_SIZE + 100);
      assertThat(readLocalFileHeader.getUncompressedSize()).isEqualTo(UNCOMPRESSED_SIZE + 100);
      assertThat(readLocalFileHeader.getCrc()).isEqualTo(23423);
      assertThat(readLocalFileHeader.getZip64ExtendedInfo()).isNull();
    }
  }

  @Test
  public void testUpdateLocalFileHeaderForZip64() throws IOException {
    File headersFile = temporaryFolder.newFile();
    createAndUpdateLocalFileHeader(headersFile, COMPRESSED_SIZE_ZIP64, UNCOMPRESSED_SIZE_ZIP64, 546423);

    try (InputStream inputStream = new FileInputStream(headersFile)) {
      LocalFileHeader readLocalFileHeader = headerReader.readLocalFileHeader(inputStream, InternalZipConstants.CHARSET_UTF_8);
      assertThat(readLocalFileHeader.getCompressedSize()).isEqualTo(COMPRESSED_SIZE_ZIP64 + 100);
      assertThat(readLocalFileHeader.getUncompressedSize()).isEqualTo(UNCOMPRESSED_SIZE_ZIP64 + 100);
      assertThat(readLocalFileHeader.getCrc()).isEqualTo(546423);
      verifyZip64ExtendedInfo(readLocalFileHeader.getZip64ExtendedInfo(), COMPRESSED_SIZE_ZIP64 + 100,
          UNCOMPRESSED_SIZE_ZIP64 + 100, -1, -1);
    }

    verifyEntrySizesIsMaxValueInLFHWhenZip64Format(headersFile);
  }

  private void testFinalizeZipFileWhenExtraDataRecordIsNullOrEmpty(byte[] extraDataRecord) throws IOException {
    ZipModel zipModel = createZipModel(10);
    File headersFile = temporaryFolder.newFile();
    addExtraDataRecordToFirstFileHeader(zipModel, extraDataRecord);

    try(OutputStream outputStream = new FileOutputStream(headersFile)) {
      headerWriter.finalizeZipFile(zipModel, outputStream, InternalZipConstants.CHARSET_UTF_8);
    }

    try(RandomAccessFile randomAccessFile = new RandomAccessFile(headersFile, RandomAccessFileMode.READ.getValue())) {
      ZipModel readZipModel = headerReader.readAllHeaders(randomAccessFile, null);
      verifyZipModel(readZipModel, 10);

      for (int i = 0; i < zipModel.getCentralDirectory().getFileHeaders().size(); i++) {
        FileHeader fileHeader = readZipModel.getCentralDirectory().getFileHeaders().get(i);
        assertThat(fileHeader.getZip64ExtendedInfo()).isNull();
        assertThat(fileHeader.getAesExtraDataRecord()).isNull();
        assertThat(fileHeader.getExtraFieldLength()).isEqualTo(i == 0 ? 4 : 0);
      }
    }
  }

  private void createAndUpdateLocalFileHeader(File headersFile, long compressedSize, long uncompressedSize, long crc)
      throws IOException {
    ZipModel zipModel = createZipModel(3);
    LocalFileHeader localFileHeaderToWrite = createLocalFileHeader("LFH", compressedSize, uncompressedSize, false);
    localFileHeaderToWrite.setCompressedSize(compressedSize);
    localFileHeaderToWrite.setUncompressedSize(uncompressedSize);
    localFileHeaderToWrite.setCrc(10);

    try (OutputStream outputStream = new FileOutputStream(headersFile)) {
      headerWriter.writeLocalFileHeader(zipModel, localFileHeaderToWrite, outputStream, InternalZipConstants.CHARSET_UTF_8);
    }

    try (SplitOutputStream splitOutputStream = new SplitOutputStream(headersFile)) {
      FileHeader fileHeader = createFileHeaders(1, compressedSize + 100, uncompressedSize + 100).get(0);
      fileHeader.setFileName(localFileHeaderToWrite.getFileName());
      fileHeader.setFileNameLength(fileHeader.getFileName().getBytes(InternalZipConstants.CHARSET_UTF_8).length);
      fileHeader.setCrc(crc);
      headerWriter.updateLocalFileHeader(fileHeader, zipModel, splitOutputStream);
    }
  }

  private void setFileHeadersAsAesEncrypted(List<FileHeader> fileHeaders, AesKeyStrength aesKeyStrength) {
    for (FileHeader fileHeader : fileHeaders) {
      fileHeader.setEncrypted(true);
      fileHeader.setEncryptionMethod(EncryptionMethod.AES);
      fileHeader.setAesExtraDataRecord(createAesExtraDataRecord(aesKeyStrength, AesVersion.TWO));
    }
  }

  private void verifyZipModel(ZipModel zipModel, int numberOFEntriesInCentralDirectory) {
    verifyZipModel(zipModel, numberOFEntriesInCentralDirectory, COMPRESSED_SIZE, UNCOMPRESSED_SIZE, false);
  }

  private void verifyZipModel(ZipModel zipModel, int numberOFEntriesInCentralDirectory, long compressedSize,
                              long uncompressedSize, boolean isZip64) {
    assertThat(zipModel).isNotNull();
    assertThat(zipModel.getCentralDirectory()).isNotNull();
    assertThat(zipModel.isZip64Format()).isEqualTo(isZip64);
    assertThat(zipModel.getCentralDirectory().getFileHeaders()).hasSize(numberOFEntriesInCentralDirectory);
    verifyFileHeaders(zipModel.getCentralDirectory().getFileHeaders(), compressedSize, uncompressedSize);
  }

  private void verifyFileHeaders(List<FileHeader> fileHeaders, long compressedSize, long uncompressedSize) {
    for (int i = 0; i < fileHeaders.size(); i++) {
      FileHeader fileHeader = fileHeaders.get(i);
      assertThat(fileHeader).isNotNull();
      assertThat(fileHeader.getVersionMadeBy()).isEqualTo(VERSION_MADE_BY);
      assertThat(fileHeader.getVersionNeededToExtract()).isEqualTo(VERSION_NEEDED_TO_EXTRACT);
      assertThat(fileHeader.getFileName()).isEqualTo(FILE_NAME_PREFIX + i);
      assertThat(fileHeader.getCompressedSize()).isEqualTo(compressedSize);
      assertThat(fileHeader.getUncompressedSize()).isEqualTo(uncompressedSize);
      assertThat(fileHeader.getFileComment()).isEqualTo(FILE_COMMENT_PREFIX + i);
    }
  }

  private void testWriteExtendedLocalFileHeader(long compressedSize, long uncompressedSize, long crc,
                                                boolean isZip64Format) throws IOException {
    LocalFileHeader localFileHeader = createLocalFileHeader("SOME_NAME", compressedSize, uncompressedSize, true);
    localFileHeader.setCrc(crc);
    localFileHeader.setWriteCompressedSizeInZip64ExtraRecord(isZip64Format);
    File headersFile = temporaryFolder.newFile();

    try(OutputStream outputStream = new FileOutputStream(headersFile)) {
      headerWriter.writeExtendedLocalHeader(localFileHeader, outputStream);
    }

    try(InputStream inputStream = new FileInputStream(headersFile)) {
      DataDescriptor dataDescriptor = headerReader.readDataDescriptor(inputStream, isZip64Format);
      verifyDataDescriptor(dataDescriptor, compressedSize, uncompressedSize, crc);
    }
  }

  private void verifyDataDescriptor(DataDescriptor dataDescriptor, long compressedSize, long uncompressedSize,
                                    long crc) {
    assertThat(dataDescriptor).isNotNull();
    assertThat(dataDescriptor.getSignature()).isEqualTo(HeaderSignature.EXTRA_DATA_RECORD);
    assertThat(dataDescriptor.getCompressedSize()).isEqualTo(compressedSize);
    assertThat(dataDescriptor.getUncompressedSize()).isEqualTo(uncompressedSize);
    assertThat(dataDescriptor.getCrc()).isEqualTo(crc);
  }

  private void testWriteLocalFileHeaderWithAes(AesKeyStrength aesKeyStrength, AesVersion aesVersion)
      throws IOException {
    ZipModel zipModel = createZipModel(10);
    LocalFileHeader localFileHeaderToWrite = createLocalFileHeader("TEXT", COMPRESSED_SIZE, UNCOMPRESSED_SIZE, true);
    localFileHeaderToWrite.setEncryptionMethod(EncryptionMethod.AES);
    localFileHeaderToWrite.setAesExtraDataRecord(createAesExtraDataRecord(aesKeyStrength, aesVersion));
    File headersFile = temporaryFolder.newFile();

    try(OutputStream outputStream = new FileOutputStream(headersFile)) {
      headerWriter.writeLocalFileHeader(zipModel, localFileHeaderToWrite, outputStream, InternalZipConstants.CHARSET_UTF_8);
    }

    try(InputStream inputStream = new FileInputStream(headersFile)) {
      LocalFileHeader readLocalFileHeader = headerReader.readLocalFileHeader(inputStream, InternalZipConstants.CHARSET_UTF_8);
      assertThat(readLocalFileHeader.getEncryptionMethod()).isEqualTo(EncryptionMethod.AES);
      verifyAesExtraDataRecord(readLocalFileHeader.getAesExtraDataRecord(), aesKeyStrength, aesVersion);
    }
  }

  private AESExtraDataRecord createAesExtraDataRecord(AesKeyStrength aesKeyStrength, AesVersion aesVersion) {
    AESExtraDataRecord aesDataRecord = new AESExtraDataRecord();
    aesDataRecord.setSignature(HeaderSignature.AES_EXTRA_DATA_RECORD);
    aesDataRecord.setDataSize(7);
    aesDataRecord.setAesVersion(aesVersion);
    aesDataRecord.setVendorID("AE");
    aesDataRecord.setAesKeyStrength(aesKeyStrength);
    aesDataRecord.setCompressionMethod(CompressionMethod.DEFLATE);
    return aesDataRecord;
  }

  private void verifyAesExtraDataRecord(AESExtraDataRecord aesExtraDataRecord, AesKeyStrength aesKeyStrength,
                                        AesVersion aesVersion) {
    assertThat(aesExtraDataRecord).isNotNull();
    assertThat(aesExtraDataRecord.getAesKeyStrength()).isEqualTo(aesKeyStrength);
    assertThat(aesExtraDataRecord.getAesVersion()).isEqualTo(aesVersion);
    assertThat(aesExtraDataRecord.getCompressionMethod()).isEqualTo(CompressionMethod.DEFLATE);
    assertThat(aesExtraDataRecord.getVendorID()).isEqualTo("AE");
  }

  private void testWriteLocalFileHeaderWithFileName(String fileNameSuffix, boolean useUtf8,
                                                    boolean expectFileNamesToMatch) throws IOException {
    testWriteLocalFileHeaderWithFileNameAndCharset(fileNameSuffix, useUtf8, expectFileNamesToMatch, InternalZipConstants.CHARSET_UTF_8);
  }

  private void testWriteLocalFileHeaderWithFileNameAndCharset(String fileNameSuffix, boolean useUtf8,
                                                    boolean expectFileNamesToMatch, Charset charset) throws IOException {
    ZipModel zipModel = createZipModel(10);
    LocalFileHeader localFileHeaderToWrite = createLocalFileHeader(fileNameSuffix, COMPRESSED_SIZE, UNCOMPRESSED_SIZE,
        useUtf8);
    File headersFile = temporaryFolder.newFile();

    try(OutputStream outputStream = new FileOutputStream(headersFile)) {
      headerWriter.writeLocalFileHeader(zipModel, localFileHeaderToWrite, outputStream, charset);
    }

    try(InputStream inputStream = new FileInputStream(headersFile)) {
      LocalFileHeader readLocalFileHeader = headerReader.readLocalFileHeader(inputStream, charset);
      if (expectFileNamesToMatch) {
        assertThat(readLocalFileHeader.getFileName()).isEqualTo(FILE_NAME_PREFIX + fileNameSuffix);
      } else {
        assertThat(readLocalFileHeader.getFileName()).isNotEqualTo(FILE_NAME_PREFIX + fileNameSuffix);
      }
    }
  }

  private void verifyEntrySizesIsMaxValueInLFHWhenZip64Format(File headersFile) throws IOException {
    try(RandomAccessFile randomAccessFile = new RandomAccessFile(headersFile, RandomAccessFileMode.READ.getValue())) {
      randomAccessFile.seek(18);

      long compressedSize = rawIO.readLongLittleEndian(randomAccessFile, 4);
      assertThat(compressedSize).isEqualTo(InternalZipConstants.ZIP_64_SIZE_LIMIT);

      long uncompressedSize = rawIO.readLongLittleEndian(randomAccessFile, 4);
      assertThat(uncompressedSize).isEqualTo(InternalZipConstants.ZIP_64_SIZE_LIMIT);
    }
  }

  private void verifyLocalFileHeader(LocalFileHeader localFileHeader, String expectedFileName, long compressedSize,
                                     long uncompressedSize) {
    assertThat(localFileHeader).isNotNull();
    assertThat(localFileHeader.getVersionNeededToExtract()).isEqualTo(VERSION_NEEDED_TO_EXTRACT);
    assertThat(localFileHeader.getFileName()).isEqualTo(expectedFileName);
    assertThat(localFileHeader.getCompressedSize()).isEqualTo(compressedSize);
    assertThat(localFileHeader.getUncompressedSize()).isEqualTo(uncompressedSize);
    assertThat(localFileHeader.getCompressionMethod()).isEqualTo(CompressionMethod.DEFLATE);
    assertThat(localFileHeader.getLastModifiedTime()).isEqualTo(LAST_MODIFIED_FILE_TIME);
  }

  private void verifyZip64ExtendedInfo(Zip64ExtendedInfo zip64ExtendedInfo, long compressedSize, long uncompressedSize,
                                       int offsetStartOfLocalFileHeader, int diskNumberStart) {
    assertThat(zip64ExtendedInfo).isNotNull();
    assertThat(zip64ExtendedInfo.getCompressedSize()).isEqualTo(compressedSize);
    assertThat(zip64ExtendedInfo.getUncompressedSize()).isEqualTo(uncompressedSize);
    assertThat(zip64ExtendedInfo.getOffsetLocalHeader()).isEqualTo(offsetStartOfLocalFileHeader);
    assertThat(zip64ExtendedInfo.getDiskNumberStart()).isEqualTo(diskNumberStart);
  }

  private LocalFileHeader createLocalFileHeader(String fileNameSuffix, long compressedSize, long uncompressedSize,
                                                boolean useUtf8) {
    LocalFileHeader localFileHeader = new LocalFileHeader();
    localFileHeader.setVersionNeededToExtract(VERSION_NEEDED_TO_EXTRACT);
    localFileHeader.setFileName(FILE_NAME_PREFIX + fileNameSuffix);
    localFileHeader.setCompressedSize(compressedSize);
    localFileHeader.setUncompressedSize(uncompressedSize);
    localFileHeader.setGeneralPurposeFlag(generateGeneralPurposeBytes(useUtf8));
    localFileHeader.setCompressionMethod(CompressionMethod.DEFLATE);
    localFileHeader.setLastModifiedTime(LAST_MODIFIED_FILE_TIME);
    return localFileHeader;
  }

  private ZipModel createZipModel(int numberOfEntriesInCentralDirectory) {
    return createZipModel(numberOfEntriesInCentralDirectory, COMPRESSED_SIZE, UNCOMPRESSED_SIZE);
  }

  private ZipModel createZipModel(int numberOfEntriesInCentralDirectory, long compressedSize, long uncompressedSize) {
    ZipModel zipModel = new ZipModel();
    zipModel.setCentralDirectory(createCentralDirectory(numberOfEntriesInCentralDirectory, compressedSize,
        uncompressedSize));
    return zipModel;
  }

  private CentralDirectory createCentralDirectory(int numberOfEntriesInCentralDirectory, long compressedSize,
                                                  long uncompressedSize) {
    CentralDirectory centralDirectory = new CentralDirectory();
    centralDirectory.setFileHeaders(createFileHeaders(numberOfEntriesInCentralDirectory, compressedSize,
        uncompressedSize));
    return centralDirectory;
  }

  private byte[] generateGeneralPurposeBytes(boolean useUtf8) {
    byte[] generalPurposeBytes = new byte[2];

    if (useUtf8) {
      generalPurposeBytes[1] = BitUtils.setBit(generalPurposeBytes[1], 3);
    }

    return generalPurposeBytes;
  }

  private List<FileHeader> createFileHeaders(int numberOfEntriesInCentralDirectory, long compressedSize,
                                             long uncompressedSize) {
    List<FileHeader> fileHeaders = new ArrayList<>();

    for (int i = 0; i < numberOfEntriesInCentralDirectory; i++) {
      FileHeader fileHeader = new FileHeader();
      fileHeader.setVersionMadeBy(VERSION_MADE_BY);
      fileHeader.setVersionNeededToExtract(VERSION_NEEDED_TO_EXTRACT);
      fileHeader.setFileName(FILE_NAME_PREFIX + i);
      fileHeader.setGeneralPurposeFlag(generateGeneralPurposeBytes(true));
      fileHeader.setCompressedSize(compressedSize);
      fileHeader.setUncompressedSize(uncompressedSize);
      fileHeader.setCompressionMethod(CompressionMethod.DEFLATE);
      fileHeader.setExternalFileAttributes(EXTERNAL_FILE_ATTRIBUTES);
      fileHeader.setFileComment(FILE_COMMENT_PREFIX + i);
      fileHeaders.add(fileHeader);

    }

    return fileHeaders;
  }

  private void addExtraDataRecordToFirstFileHeader(ZipModel zipModel, byte[] data) {
    ExtraDataRecord extraDataRecord = new ExtraDataRecord();
    extraDataRecord.setHeader(12345);
    extraDataRecord.setSizeOfData(data == null ? 0 : data.length);
    extraDataRecord.setData(data);

    FileHeader firstFileHeader = zipModel.getCentralDirectory().getFileHeaders().get(0);

    if (firstFileHeader.getExtraDataRecords() == null) {
      firstFileHeader.setExtraDataRecords(new ArrayList<>());
    }

    firstFileHeader.getExtraDataRecords().add(extraDataRecord);
  }

}