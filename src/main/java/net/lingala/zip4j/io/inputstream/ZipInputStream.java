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
import net.lingala.zip4j.model.DataDescriptor;
import net.lingala.zip4j.model.ExtraDataRecord;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.util.BitUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.List;
import java.util.zip.CRC32;

import static net.lingala.zip4j.util.FileUtils.isZipEntryDirectory;
import static net.lingala.zip4j.util.Zip4jUtil.getCompressionMethod;

public class ZipInputStream extends InputStream {

  private PushbackInputStream inputStream;
  private DecompressedInputStream decompressedInputStream;
  private HeaderReader headerReader = new HeaderReader();
  private char[] password;
  private LocalFileHeader localFileHeader;
  private CRC32 crc32 = new CRC32();

  public ZipInputStream(InputStream inputStream) {
    this(inputStream, null);
  }

  public ZipInputStream(InputStream inputStream, char[] password) {
    this.inputStream = new PushbackInputStream(inputStream, 512);
    this.password = password;
  }

  public LocalFileHeader getNextEntry() throws IOException {
    try {
      localFileHeader = headerReader.readLocalFileHeader(inputStream);

      if (localFileHeader == null) {
        return null;
      }

      verifyLocalFileHeader(localFileHeader);
      crc32.reset();

      if (!isZipEntryDirectory(localFileHeader.getFileName())) {
        this.decompressedInputStream = initializeEntryInputStream(localFileHeader);
      }
      return localFileHeader;
    } catch (ZipException e) {
      throw new IOException(e);
    }
  }

  @Override
  public int read() throws IOException {
    byte[] b = new byte[1];
    int readLen = read(b);

    if (readLen == -1) {
      return -1;
    }

    return b[0];
  }

  @Override
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    if (isZipEntryDirectory(localFileHeader.getFileName())) {
      return -1;
    }

    int readLen = decompressedInputStream.read(b, off, (len - len %16));

    if (readLen == -1) {
      endOfCompressedDataReached();
    } else {
      crc32.update(b, off, readLen);
    }

    return readLen;
  }

  @Override
  public void close() throws IOException {
    if (decompressedInputStream != null) {
      decompressedInputStream.close();
    }
  }

  private void endOfCompressedDataReached() throws IOException {
    //With inflater, without knowing the compressed or uncompressed size, we over read necessary data
    //In such cases, we have to push back the inputstream to the end of data
    decompressedInputStream.pushBackInputStreamIfNecessary(inputStream);

    //First signal the end of data for this entry so that ciphers can read any header data if applicable
    decompressedInputStream.endOfEntryReached(inputStream);

    DataDescriptor dataDescriptor = readExtendedLocalFileHeaderIfPresent();
    verifyCrc(dataDescriptor);
    resetFields();
  }

  private DecompressedInputStream initializeEntryInputStream(LocalFileHeader localFileHeader) throws IOException, ZipException {
    ZipEntryInputStream zipEntryInputStream = new ZipEntryInputStream(inputStream);
    CipherInputStream cipherInputStream = initializeCipherInputStream(zipEntryInputStream, localFileHeader);
    return initializeDecompressorForThisEntry(cipherInputStream, localFileHeader);
  }

  private CipherInputStream initializeCipherInputStream(ZipEntryInputStream zipEntryInputStream, LocalFileHeader localFileHeader) throws IOException, ZipException {
    if (!localFileHeader.isEncrypted()) {
      return new NoCipherInputStream(zipEntryInputStream, localFileHeader, password);
    }

    if (localFileHeader.getEncryptionMethod() == EncryptionMethod.AES) {
      return new AesCipherInputStream(zipEntryInputStream, localFileHeader, password);
    } else {
      return new ZipStandardCipherInputStream(zipEntryInputStream, localFileHeader, password);
    }
  }

  private DecompressedInputStream initializeDecompressorForThisEntry(CipherInputStream cipherInputStream, LocalFileHeader localFileHeader) {
    CompressionMethod compressionMethod = getCompressionMethod(localFileHeader);

    if (compressionMethod == CompressionMethod.DEFLATE) {
      return new InflaterInputStream(cipherInputStream, getCompressedSize(localFileHeader));
    }

    return new StoreInputStream(cipherInputStream, localFileHeader.getUncompressedSize());
  }

  private DataDescriptor readExtendedLocalFileHeaderIfPresent() throws IOException {
    if (!isExtendedLocalFileHeaderPresent(localFileHeader)) {
      return null;
    }

    return headerReader.readDataDescriptor(inputStream,
        checkIfZip64ExtraDataRecordPresentInLFH(localFileHeader.getExtraDataRecords()));
  }

  private boolean isExtendedLocalFileHeaderPresent(LocalFileHeader localFileHeader) {
    byte[] generalPurposeFlags = localFileHeader.getGeneralPurposeFlag();
    return BitUtils.isBitSet(generalPurposeFlags[0], 3);
  }

  private void verifyLocalFileHeader(LocalFileHeader localFileHeader) throws IOException {
    if (!isEntryDirectory(localFileHeader.getFileName())
        && localFileHeader.getCompressionMethod() == CompressionMethod.STORE
        && localFileHeader.getUncompressedSize() == 0) {
      throw new IOException("Invalid local file header for: " + localFileHeader.getFileName()
          + ". Uncompressed size has to be set for entry of compression type store which is not a directory");
    }
  }

  private boolean checkIfZip64ExtraDataRecordPresentInLFH(List<ExtraDataRecord> extraDataRecords) {
    if (extraDataRecords == null) {
      return false;
    }

    for (ExtraDataRecord extraDataRecord : extraDataRecords) {
      if (extraDataRecord.getSignature() == HeaderSignature.ZIP64_EXTRA_FIELD_SIGNATURE) {
        return true;
      }
    }

    return false;
  }

  private void verifyCrc(DataDescriptor dataDescriptor) throws IOException {
    if (localFileHeader.getEncryptionMethod() == EncryptionMethod.AES) {
      // Verification will be done in this case by AesCipherInputStream
      return;
    }

    long crcFromLFH = localFileHeader.getCrc32();
    if (isExtendedLocalFileHeaderPresent(localFileHeader)) {
      if (dataDescriptor == null) {
        throw new IOException("extended local file header flag is set, but could not locate data descriptor");
      }
      crcFromLFH = dataDescriptor.getCrc32();
    }

    if (crcFromLFH != crc32.getValue()) {
      throw new IOException("Reached end of entry, but crc verification failed");
    }
  }

  private void resetFields() {
    localFileHeader = null;
    crc32.reset();
  }

  private boolean isEntryDirectory(String entryName) {
    return entryName.endsWith("/") || entryName.endsWith("\\");
  }

  private long getCompressedSize(LocalFileHeader localFileHeader) {
    if (localFileHeader.isDataDescriptorExists()) {
      return -1;
    }

    if (localFileHeader.getZip64ExtendedInfo() != null) {
      return localFileHeader.getZip64ExtendedInfo().getCompressedSize();
    }

    return localFileHeader.getCompressedSize();
  }

}
