package net.lingala.zip4j.io.outputstream;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.headers.FileHeaderFactory;
import net.lingala.zip4j.headers.HeaderSignature;
import net.lingala.zip4j.headers.HeaderWriter;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesVersion;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.util.InternalZipConstants;
import net.lingala.zip4j.util.RawIO;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.zip.CRC32;

public class ZipOutputStream extends OutputStream {

  private CountingOutputStream countingOutputStream;
  private char[] password;
  private ZipModel zipModel;
  private CompressedOutputStream compressedOutputStream;
  private FileHeader fileHeader;
  private LocalFileHeader localFileHeader;
  private FileHeaderFactory fileHeaderFactory = new FileHeaderFactory();
  private HeaderWriter headerWriter = new HeaderWriter();
  private CRC32 crc32 = new CRC32();
  private RawIO rawIO = new RawIO();
  private long uncompressedSizeForThisEntry = 0;
  private Charset charset;
  private boolean streamClosed;

  public ZipOutputStream(OutputStream outputStream) throws IOException {
    this(outputStream, null, InternalZipConstants.CHARSET_UTF_8);
  }

  public ZipOutputStream(OutputStream outputStream, Charset charset) throws IOException {
    this(outputStream, null, charset);
  }

  public ZipOutputStream(OutputStream outputStream, char[] password) throws IOException {
    this(outputStream, password, InternalZipConstants.CHARSET_UTF_8);
  }

  public ZipOutputStream(OutputStream outputStream, char[] password, Charset charset) throws IOException {
    this(outputStream, password, charset, new ZipModel());
  }

  public ZipOutputStream(OutputStream outputStream, char[] password, Charset charset, ZipModel zipModel) throws IOException {
    if(charset == null) {
      charset = InternalZipConstants.CHARSET_UTF_8;
    }

    this.countingOutputStream = new CountingOutputStream(outputStream);
    this.password = password;
    this.charset = charset;
    this.zipModel = initializeZipModel(zipModel, countingOutputStream);
    this.streamClosed = false;
    writeSplitZipHeaderIfApplicable();
  }

  public void putNextEntry(ZipParameters zipParameters) throws IOException {
    verifyZipParameters(zipParameters);
    initializeAndWriteFileHeader(zipParameters);

    //Initialisation of below compressedOutputStream should happen after writing local file header
    //because local header data should be written first and then the encryption header data
    //and below initialisation writes encryption header data
    compressedOutputStream = initializeCompressedOutputStream(zipParameters);
  }

  public void write(int b) throws IOException {
    write(new byte[] {(byte)b});
  }

  public void write(byte[] b) throws IOException {
    write(b, 0, b.length);
  }

  public void write(byte[] b, int off, int len) throws IOException {
    ensureStreamOpen();
    crc32.update(b, off, len);
    compressedOutputStream.write(b, off, len);
    uncompressedSizeForThisEntry += len;
  }

  public FileHeader closeEntry() throws IOException {
    compressedOutputStream.closeEntry();

    long compressedSize = compressedOutputStream.getCompressedSize();
    fileHeader.setCompressedSize(compressedSize);
    localFileHeader.setCompressedSize(compressedSize);

    fileHeader.setUncompressedSize(uncompressedSizeForThisEntry);
    localFileHeader.setUncompressedSize(uncompressedSizeForThisEntry);

    if (writeCrc(fileHeader)) {
      fileHeader.setCrc(crc32.getValue());
      localFileHeader.setCrc(crc32.getValue());
    }

    zipModel.getLocalFileHeaders().add(localFileHeader);
    zipModel.getCentralDirectory().getFileHeaders().add(fileHeader);

    if (localFileHeader.isDataDescriptorExists()) {
      headerWriter.writeExtendedLocalHeader(localFileHeader, countingOutputStream);
    }
    reset();
    return fileHeader;
  }

  @Override
  public void close() throws IOException {
    zipModel.getEndOfCentralDirectoryRecord().setOffsetOfStartOfCentralDirectory(countingOutputStream.getNumberOfBytesWritten());
    headerWriter.finalizeZipFile(zipModel, countingOutputStream, charset);
    countingOutputStream.close();
    this.streamClosed = true;
  }

  public void setComment(String comment) throws IOException {
    ensureStreamOpen();
    zipModel.getEndOfCentralDirectoryRecord().setComment(comment);
  }

  private void ensureStreamOpen() throws IOException {
    if (streamClosed) {
      throw new IOException("Stream is closed");
    }
  }

  private ZipModel initializeZipModel(ZipModel zipModel, CountingOutputStream countingOutputStream) {
    if (zipModel == null) {
      zipModel = new ZipModel();
    }

    if (countingOutputStream.isSplitZipFile()) {
      zipModel.setSplitArchive(true);
      zipModel.setSplitLength(countingOutputStream.getSplitLength());
    }

    return zipModel;
  }

  private void initializeAndWriteFileHeader(ZipParameters zipParameters) throws IOException {
    fileHeader = fileHeaderFactory.generateFileHeader(zipParameters, countingOutputStream.isSplitZipFile(),
        countingOutputStream.getCurrentSplitFileCounter(), charset, rawIO);
    fileHeader.setOffsetLocalHeader(countingOutputStream.getOffsetForNextEntry());

    localFileHeader = fileHeaderFactory.generateLocalFileHeader(fileHeader);
    headerWriter.writeLocalFileHeader(zipModel, localFileHeader, countingOutputStream, charset);
  }

  private void reset() throws IOException {
    uncompressedSizeForThisEntry = 0;
    crc32.reset();
    compressedOutputStream.close();
  }

  private void writeSplitZipHeaderIfApplicable() throws IOException {
    if (!countingOutputStream.isSplitZipFile()) {
      return;
    }

    rawIO.writeIntLittleEndian(countingOutputStream, (int) HeaderSignature.SPLIT_ZIP.getValue());
  }

  private CompressedOutputStream initializeCompressedOutputStream(ZipParameters zipParameters) throws IOException {
    ZipEntryOutputStream zipEntryOutputStream = new ZipEntryOutputStream(countingOutputStream);
    CipherOutputStream cipherOutputStream = initializeCipherOutputStream(zipEntryOutputStream, zipParameters);
    return initializeCompressedOutputStream(cipherOutputStream, zipParameters);
  }

  private CipherOutputStream initializeCipherOutputStream(ZipEntryOutputStream zipEntryOutputStream,
                                                          ZipParameters zipParameters) throws IOException {
    if (!zipParameters.isEncryptFiles()) {
      return new NoCipherOutputStream(zipEntryOutputStream, zipParameters, null);
    }

    if (password == null || password.length == 0) {
      throw new ZipException("password not set");
    }

    if (zipParameters.getEncryptionMethod() == EncryptionMethod.AES) {
      return new AesCipherOutputStream(zipEntryOutputStream, zipParameters, password);
    } else if (zipParameters.getEncryptionMethod() == EncryptionMethod.ZIP_STANDARD) {
      return new ZipStandardCipherOutputStream(zipEntryOutputStream, zipParameters, password);
    } else {
      throw new ZipException("Invalid encryption method");
    }
  }

  private CompressedOutputStream initializeCompressedOutputStream(CipherOutputStream cipherOutputStream,
                                                                  ZipParameters zipParameters) {
    if (zipParameters.getCompressionMethod() == CompressionMethod.DEFLATE) {
      return new DeflaterOutputStream(cipherOutputStream, zipParameters.getCompressionLevel());
    }

    return new StoreOutputStream(cipherOutputStream);
  }

  private void verifyZipParameters(ZipParameters zipParameters) {
    if (zipParameters.getCompressionMethod() == CompressionMethod.STORE
        && zipParameters.getEntrySize() < 0
        && !isEntryDirectory(zipParameters.getFileNameInZip())
        && zipParameters.isWriteExtendedLocalFileHeader()) {
      throw new IllegalArgumentException("uncompressed size should be set for zip entries of compression type store");
    }
  }

  private boolean writeCrc(FileHeader fileHeader) {
    boolean isAesEncrypted = fileHeader.isEncrypted() && fileHeader.getEncryptionMethod().equals(EncryptionMethod.AES);

    if (!isAesEncrypted) {
      return true;
    }

    return fileHeader.getAesExtraDataRecord().getAesVersion().equals(AesVersion.ONE);
  }

  private boolean isEntryDirectory(String entryName) {
    return entryName.endsWith("/") || entryName.endsWith("\\");
  }
}
