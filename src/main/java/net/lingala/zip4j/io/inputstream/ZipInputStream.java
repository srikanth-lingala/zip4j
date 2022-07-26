/*
 * Copyright 2010 Srikanth Reddy Lingala
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.lingala.zip4j.io.inputstream;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.headers.HeaderReader;
import net.lingala.zip4j.headers.HeaderSignature;
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.DataDescriptor;
import net.lingala.zip4j.model.ExtraDataRecord;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.Zip4jConfig;
import net.lingala.zip4j.model.enums.AesVersion;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.util.InternalZipConstants;
import net.lingala.zip4j.util.PasswordCallback;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.zip.CRC32;

import static net.lingala.zip4j.util.InternalZipConstants.BUFF_SIZE;
import static net.lingala.zip4j.util.InternalZipConstants.MIN_BUFF_SIZE;
import static net.lingala.zip4j.util.InternalZipConstants.USE_UTF8_FOR_PASSWORD_ENCODING_DECODING;
import static net.lingala.zip4j.util.Zip4jUtil.getCompressionMethod;

public class ZipInputStream extends InputStream {

  private PushbackInputStream inputStream;
  private DecompressedInputStream decompressedInputStream;
  private HeaderReader headerReader = new HeaderReader();
  private char[] password;
  private PasswordCallback passwordCallback;
  private LocalFileHeader localFileHeader;
  private CRC32 crc32 = new CRC32();
  private byte[] endOfEntryBuffer;
  private boolean canSkipExtendedLocalFileHeader = false;
  private Zip4jConfig zip4jConfig;
  private boolean streamClosed = false;
  private boolean entryEOFReached = false;

  public ZipInputStream(InputStream inputStream) {
    this(inputStream, (char[]) null, (Charset) null);
  }

  public ZipInputStream(InputStream inputStream, Charset charset) {
    this(inputStream, (char[]) null, charset);
  }

  public ZipInputStream(InputStream inputStream, char[] password) {
    this(inputStream, password, (Charset) null);
  }

  public ZipInputStream(InputStream inputStream, PasswordCallback passwordCallback) {
    this(inputStream, passwordCallback, (Charset) null);
  }

  public ZipInputStream(InputStream inputStream, char[] password, Charset charset) {
    this(inputStream, password, new Zip4jConfig(charset, BUFF_SIZE, USE_UTF8_FOR_PASSWORD_ENCODING_DECODING));
  }

  public ZipInputStream(InputStream inputStream, PasswordCallback passwordCallback, Charset charset) {
    this(inputStream, passwordCallback, new Zip4jConfig(charset, BUFF_SIZE, USE_UTF8_FOR_PASSWORD_ENCODING_DECODING));
  }

  public ZipInputStream(InputStream inputStream, char[] password, Zip4jConfig zip4jConfig) {
    this(inputStream, password, null, zip4jConfig);
  }

  public ZipInputStream(InputStream inputStream, PasswordCallback passwordCallback, Zip4jConfig zip4jConfig) {
    this(inputStream, null, passwordCallback, zip4jConfig);
  }

  private ZipInputStream(InputStream inputStream, char[] password, PasswordCallback passwordCallback, Zip4jConfig zip4jConfig) {
    if (zip4jConfig.getBufferSize() < InternalZipConstants.MIN_BUFF_SIZE) {
      throw new IllegalArgumentException("Buffer size cannot be less than " + MIN_BUFF_SIZE + " bytes");
    }

    this.inputStream = new PushbackInputStream(inputStream, zip4jConfig.getBufferSize());
    this.password = password;
    this.passwordCallback = passwordCallback;
    this.zip4jConfig = zip4jConfig;
  }

  public LocalFileHeader getNextEntry() throws IOException {
    return getNextEntry(null, true);
  }

  public LocalFileHeader getNextEntry(FileHeader fileHeader, boolean readUntilEndOfCurrentEntryIfOpen)
      throws IOException {

    if (localFileHeader != null && readUntilEndOfCurrentEntryIfOpen) {
      readUntilEndOfEntry();
    }

    localFileHeader = headerReader.readLocalFileHeader(inputStream, zip4jConfig.getCharset());

    if (localFileHeader == null) {
      return null;
    }

    if (localFileHeader.isEncrypted() && password == null && passwordCallback != null) {
      setPassword(passwordCallback.getPassword());
    }

    verifyLocalFileHeader(localFileHeader);
    crc32.reset();

    if (fileHeader != null) {
      localFileHeader.setCrc(fileHeader.getCrc());
      localFileHeader.setCompressedSize(fileHeader.getCompressedSize());
      localFileHeader.setUncompressedSize(fileHeader.getUncompressedSize());
      // file header's directory flag is more reliable than local file header's directory flag as file header has
      // additional external file attributes which has a directory flag defined. In local file header, the only way
      // to determine if an entry is directory is to check if the file name has a trailing forward slash "/"
      localFileHeader.setDirectory(fileHeader.isDirectory());
      canSkipExtendedLocalFileHeader = true;
    } else {
      canSkipExtendedLocalFileHeader = false;
    }

    this.decompressedInputStream = initializeEntryInputStream(localFileHeader);
    this.entryEOFReached = false;
    return localFileHeader;
  }

  @Override
  public int read() throws IOException {
    byte[] b = new byte[1];
    int readLen = read(b);

    if (readLen == -1) {
      return -1;
    }

    return b[0] & 0xff;
  }

  @Override
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    if (streamClosed) {
      throw new IOException("Stream closed");
    }

    if (len < 0) {
      throw new IllegalArgumentException("Negative read length");
    }

    if (len == 0) {
      return 0;
    }

    if (localFileHeader == null) {
      // localfileheader can be null when end of compressed data is reached.  If null check is missing, read method will
      // throw a NPE when end of compressed data is reached and read is called again.
      return -1;
    }

    try {
      int readLen = decompressedInputStream.read(b, off, len);

      if (readLen == -1) {
        endOfCompressedDataReached();
      } else {
        crc32.update(b, off, readLen);
      }

      return readLen;
    } catch (IOException e) {
      if (isEncryptionMethodZipStandard(localFileHeader)) {
        throw new ZipException(e.getMessage(), e.getCause(), ZipException.Type.WRONG_PASSWORD);
      }

      throw e;
    }
  }

  @Override
  public void close() throws IOException {
    if (streamClosed) {
      return;
    }

    if (decompressedInputStream != null) {
      decompressedInputStream.close();
    }
    this.streamClosed = true;
  }

  @Override
  public int available() throws IOException {
    assertStreamOpen();
    return entryEOFReached ? 0 : 1;
  }

  /**
   * Sets the password for the inputstream. This password will be used for any subsequent encrypted entries that will be
   * read from this stream. If this method is called when an entry is being read, it has no effect on the read action
   * of the current entry, and the password will take effect from any subsequent entry reads.
   *
   * @param password Password to be used for reading of entries from the zip input stream
   */
  public void setPassword(char[] password) {
    this.password = password;
  }

  private void endOfCompressedDataReached() throws IOException {
    //With inflater, without knowing the compressed or uncompressed size, we over read necessary data
    //In such cases, we have to push back the inputstream to the end of data
    decompressedInputStream.pushBackInputStreamIfNecessary(inputStream);

    //First signal the end of data for this entry so that ciphers can read any header data if applicable
    decompressedInputStream.endOfEntryReached(inputStream);

    readExtendedLocalFileHeaderIfPresent();
    verifyCrc();
    resetFields();
    this.entryEOFReached = true;
  }

  private DecompressedInputStream initializeEntryInputStream(LocalFileHeader localFileHeader) throws IOException {
    ZipEntryInputStream zipEntryInputStream = new ZipEntryInputStream(inputStream, getCompressedSize(localFileHeader));
    CipherInputStream<?> cipherInputStream = initializeCipherInputStream(zipEntryInputStream, localFileHeader);
    return initializeDecompressorForThisEntry(cipherInputStream, localFileHeader);
  }

  private CipherInputStream<?> initializeCipherInputStream(ZipEntryInputStream zipEntryInputStream,
                                                        LocalFileHeader localFileHeader) throws IOException {
    if (!localFileHeader.isEncrypted()) {
      return new NoCipherInputStream(zipEntryInputStream, localFileHeader, password, zip4jConfig.getBufferSize());
    }

    if (localFileHeader.getEncryptionMethod() == EncryptionMethod.AES) {
      return new AesCipherInputStream(zipEntryInputStream, localFileHeader, password, zip4jConfig.getBufferSize(),
              zip4jConfig.isUseUtf8CharsetForPasswords());
    } else if (localFileHeader.getEncryptionMethod() == EncryptionMethod.ZIP_STANDARD) {
      return new ZipStandardCipherInputStream(zipEntryInputStream, localFileHeader, password, zip4jConfig.getBufferSize(),
              zip4jConfig.isUseUtf8CharsetForPasswords());
    } else {
      final String message = String.format("Entry [%s] Strong Encryption not supported", localFileHeader.getFileName());
      throw new ZipException(message, ZipException.Type.UNSUPPORTED_ENCRYPTION);
    }
  }

  private DecompressedInputStream initializeDecompressorForThisEntry(CipherInputStream<?> cipherInputStream,
                                                                     LocalFileHeader localFileHeader) throws ZipException {
    CompressionMethod compressionMethod = getCompressionMethod(localFileHeader);

    if (compressionMethod == CompressionMethod.DEFLATE) {
      return new InflaterInputStream(cipherInputStream, zip4jConfig.getBufferSize());
    }

    return new StoreInputStream(cipherInputStream);
  }

  private void readExtendedLocalFileHeaderIfPresent() throws IOException {
    if (!localFileHeader.isDataDescriptorExists() || canSkipExtendedLocalFileHeader) {
      return;
    }

    DataDescriptor dataDescriptor = headerReader.readDataDescriptor(inputStream,
        checkIfZip64ExtraDataRecordPresentInLFH(localFileHeader.getExtraDataRecords()));
    localFileHeader.setCompressedSize(dataDescriptor.getCompressedSize());
    localFileHeader.setUncompressedSize(dataDescriptor.getUncompressedSize());
    localFileHeader.setCrc(dataDescriptor.getCrc());
  }

  private void verifyLocalFileHeader(LocalFileHeader localFileHeader) throws IOException {
    if (!isEntryDirectory(localFileHeader.getFileName())
        && localFileHeader.getCompressionMethod() == CompressionMethod.STORE
        && localFileHeader.getUncompressedSize() < 0) {
      throw new IOException("Invalid local file header for: " + localFileHeader.getFileName()
          + ". Uncompressed size has to be set for entry of compression type store which is not a directory");
    }
  }

  private boolean checkIfZip64ExtraDataRecordPresentInLFH(List<ExtraDataRecord> extraDataRecords) {
    if (extraDataRecords == null) {
      return false;
    }

    for (ExtraDataRecord extraDataRecord : extraDataRecords) {
      if (extraDataRecord.getHeader() == HeaderSignature.ZIP64_EXTRA_FIELD_SIGNATURE.getValue()) {
        return true;
      }
    }

    return false;
  }

  private void verifyCrc() throws IOException {
    if (localFileHeader.getEncryptionMethod() == EncryptionMethod.AES
        && localFileHeader.getAesExtraDataRecord().getAesVersion().equals(AesVersion.TWO)) {
      // Verification will be done in this case by AesCipherInputStream
      return;
    }

    if (localFileHeader.getCrc() != crc32.getValue()) {
      ZipException.Type exceptionType = ZipException.Type.CHECKSUM_MISMATCH;

      if (isEncryptionMethodZipStandard(localFileHeader)) {
        exceptionType = ZipException.Type.WRONG_PASSWORD;
      }

      throw new ZipException("Reached end of entry, but crc verification failed for " + localFileHeader.getFileName(),
          exceptionType);
    }
  }

  private void resetFields() {
    localFileHeader = null;
    crc32.reset();
  }

  private boolean isEntryDirectory(String entryName) {
    return entryName.endsWith("/") || entryName.endsWith("\\");
  }

  private long getCompressedSize(LocalFileHeader localFileHeader) throws ZipException {
    if (getCompressionMethod(localFileHeader).equals(CompressionMethod.STORE)) {
      return localFileHeader.getUncompressedSize();
    }

    if (localFileHeader.isDataDescriptorExists() && !canSkipExtendedLocalFileHeader) {
      return -1;
    }

    return localFileHeader.getCompressedSize() - getEncryptionHeaderSize(localFileHeader);
  }

  private int getEncryptionHeaderSize(LocalFileHeader localFileHeader) throws ZipException {
    if (!localFileHeader.isEncrypted()) {
      return 0;
    }

    if (localFileHeader.getEncryptionMethod().equals(EncryptionMethod.AES)) {
      return getAesEncryptionHeaderSize(localFileHeader.getAesExtraDataRecord());
    } else if (localFileHeader.getEncryptionMethod().equals(EncryptionMethod.ZIP_STANDARD)) {
      return InternalZipConstants.STD_DEC_HDR_SIZE;
    } else {
      return 0;
    }
  }

  private void readUntilEndOfEntry() throws IOException {
    if (endOfEntryBuffer == null) {
      endOfEntryBuffer = new byte[512];
    }

    //noinspection StatementWithEmptyBody
    while (read(endOfEntryBuffer) != -1);
    this.entryEOFReached = true;
  }

  private int getAesEncryptionHeaderSize(AESExtraDataRecord aesExtraDataRecord) throws ZipException {
    if (aesExtraDataRecord == null || aesExtraDataRecord.getAesKeyStrength() == null) {
      throw new ZipException("AesExtraDataRecord not found or invalid for Aes encrypted entry");
    }

    return InternalZipConstants.AES_AUTH_LENGTH + InternalZipConstants.AES_PASSWORD_VERIFIER_LENGTH
        + aesExtraDataRecord.getAesKeyStrength().getSaltLength();
  }

  private boolean isEncryptionMethodZipStandard(LocalFileHeader localFileHeader) {
    return localFileHeader.isEncrypted() && EncryptionMethod.ZIP_STANDARD.equals(localFileHeader.getEncryptionMethod());
  }

  private void assertStreamOpen() throws IOException {
    if (streamClosed) {
      throw new IOException("Stream closed");
    }
  }
}
