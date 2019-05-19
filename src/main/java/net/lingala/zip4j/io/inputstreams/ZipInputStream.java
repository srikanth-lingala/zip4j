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

package net.lingala.zip4j.io.inputstreams;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.headers.HeaderReader;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.zip.CompressionMethod;
import net.lingala.zip4j.zip.EncryptionMethod;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.zip.CRC32;

public class ZipInputStream extends InputStream {

  private PushbackInputStream inputStream;
  private DecompressedInputStream decompressedInputStream;
  private ZipModel zipModel;
  private HeaderReader headerReader = new HeaderReader();
  private char[] password;
  private boolean extendedLocalFileHeaderPresent = false;
  private CRC32 crc32 = new CRC32();

  public ZipInputStream(InputStream inputStream) {
    this(inputStream, null, null);
  }

  public ZipInputStream(InputStream inputStream, char[] password) {
    this(inputStream, password, null);
  }

  public ZipInputStream(InputStream inputStream, char[] password, ZipModel zipModel) {
    this.inputStream = new PushbackInputStream(inputStream, 512);
    this.zipModel = zipModel;
    this.password = password;
  }

  public LocalFileHeader getNextEntry() throws IOException {
    try {
      LocalFileHeader localFileHeader = headerReader.readLocalFileHeader(inputStream);

      if (localFileHeader == null) {
        return null;
      }

      localFileHeader.setPassword(password);
      crc32.reset();
      this.decompressedInputStream = initializeEntryInputStream(localFileHeader);
      this.extendedLocalFileHeaderPresent = isExtendedLocalFileHeaderPresent(localFileHeader);
      return localFileHeader;
    } catch (ZipException e) {
      throw new IOException(e);
    }
  }

  @Override
  public int read() throws IOException {
    return decompressedInputStream.read();
  }

  @Override
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int readLen = decompressedInputStream.read(b, off, (len - len %16));

    if (readLen == -1) {
      //With inflater, without knowing the compressed or uncompressed size, we over read necessary data
      //In such cases, we have to push back the inputstream to the end of data
      decompressedInputStream.pushBackInputStreamIfNecessary(inputStream);

      //First signal the end of data for this entry so that ciphers can read any header data if applicable
      decompressedInputStream.endOfEntryReached(inputStream);

      readExtendedLocalFileHeaderIfPresent();
    }

    return readLen;
  }

  @Override
  public void close() throws IOException {
    if (decompressedInputStream != null) {
      decompressedInputStream.close();
    }
  }

  private DecompressedInputStream initializeEntryInputStream(LocalFileHeader localFileHeader) throws IOException, ZipException {
    ZipEntryInputStream zipEntryInputStream = new ZipEntryInputStream(inputStream, zipModel);
    CipherInputStream cipherInputStream = initializeCipherInputStream(zipEntryInputStream, localFileHeader);
    return initializeDecompressorForThisEntry(cipherInputStream, localFileHeader);
  }

  private CipherInputStream initializeCipherInputStream(ZipEntryInputStream zipEntryInputStream, LocalFileHeader localFileHeader) throws IOException, ZipException {
    if (!localFileHeader.isEncrypted()) {
      return new NoCipherInputStream(zipEntryInputStream, localFileHeader);
    }

    if (localFileHeader.getEncryptionMethod() == EncryptionMethod.AES) {
      return new AesCipherInputStream(zipEntryInputStream, localFileHeader);
    } else {
      return new ZipStandardCipherInputStream(zipEntryInputStream, localFileHeader);
    }
  }

  private DecompressedInputStream initializeDecompressorForThisEntry(CipherInputStream cipherInputStream, LocalFileHeader localFileHeader) throws ZipException {
    CompressionMethod compressionMethod = getCompressionMethod(localFileHeader);

    if (compressionMethod == CompressionMethod.DEFLATE) {
      return new InflaterInputStream(cipherInputStream);
    }

    return new StoreInputStream(cipherInputStream, localFileHeader.getUncompressedSize());
  }

  private CompressionMethod getCompressionMethod(LocalFileHeader localFileHeader) throws ZipException {
    if (localFileHeader.getCompressionMethod() != CompressionMethod.AES_INTERNAL_ONLY) {
      return localFileHeader.getCompressionMethod();
    }

    if (localFileHeader.getAesExtraDataRecord() == null) {
      throw new ZipException("AesExtraDataRecord not present in localheader for aes encrypted data");
    }

    return localFileHeader.getAesExtraDataRecord().getCompressionMethod();
  }

  private LocalFileHeader readExtendedLocalFileHeaderIfPresent() throws IOException {
    if (!extendedLocalFileHeaderPresent) {
      return null;
    }

    return headerReader.readExtendedLocalFileHeader(inputStream);
  }

  private boolean isExtendedLocalFileHeaderPresent(LocalFileHeader localFileHeader) {
    byte[] generalPurposeFlags = localFileHeader.getGeneralPurposeFlag();
    return (generalPurposeFlags[0] & (1L << 3)) != 0;
  }

}
