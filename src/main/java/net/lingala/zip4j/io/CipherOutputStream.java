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

package net.lingala.zip4j.io;

import net.lingala.zip4j.crypto.AESEncrpyter;
import net.lingala.zip4j.crypto.Encrypter;
import net.lingala.zip4j.crypto.StandardEncrypter;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.headers.FileHeaderFactory;
import net.lingala.zip4j.headers.HeaderWriter;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.InternalZipConstants;
import net.lingala.zip4j.util.Raw;
import net.lingala.zip4j.util.Zip4jUtil;
import net.lingala.zip4j.zip.CompressionMethod;
import net.lingala.zip4j.zip.EncryptionMethod;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.CRC32;

public class CipherOutputStream extends OutputStream {

  protected OutputStream outputStream;
  protected FileHeader fileHeader;
  protected LocalFileHeader localFileHeader;
  protected ZipParameters zipParameters;
  protected ZipModel zipModel;
  protected CRC32 crc = new CRC32();

  private Encrypter encrypter;
  private long totalBytesWritten;
  private long bytesWrittenForThisFile;
  private byte[] pendingBuffer = new byte[InternalZipConstants.AES_BLOCK_SIZE];;
  private int pendingBufferLength;
  private long totalBytesRead;
  private FileHeaderFactory fileHeaderFactory = new FileHeaderFactory();

  public CipherOutputStream(OutputStream outputStream, ZipModel zipModel) {
    this.outputStream = outputStream;
    this.zipModel = initializeZipModel(zipModel);
  }

  public void putNextEntry(File sourceFile, ZipParameters zipParameters) throws ZipException {
    verifyZipParameters(zipParameters, sourceFile);

    try {
      this.zipParameters = (ZipParameters) zipParameters.clone();

      if (zipParameters.isSourceExternalStream()) {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(this.zipParameters.getFileNameInZip())) {
          throw new ZipException("file name is empty for external stream");
        }
        if (this.zipParameters.getFileNameInZip().endsWith("/") || this.zipParameters.getFileNameInZip().endsWith("\\")) {
          this.zipParameters.setEncryptFiles(false);
          this.zipParameters.setEncryptionMethod(EncryptionMethod.NONE);
          this.zipParameters.setCompressionMethod(CompressionMethod.STORE);
        }
      } else {
        if (sourceFile.isDirectory()) {
          this.zipParameters.setEncryptFiles(false);
          this.zipParameters.setEncryptionMethod(EncryptionMethod.NONE);
          this.zipParameters.setCompressionMethod(CompressionMethod.STORE);
        }
      }

      boolean isSplitOutputStream = outputStream instanceof SplitOutputStream;
      int diskNumberStart = 0;
      if (isSplitOutputStream) {
        diskNumberStart = ((SplitOutputStream) outputStream).getCurrSplitFileCounter();
      }

      fileHeader = fileHeaderFactory.generateFileHeader(zipParameters, isSplitOutputStream, diskNumberStart,
          zipModel.getFileNameCharset(), sourceFile);
      localFileHeader = fileHeaderFactory.generateLocalFileHeaderFromFileHeader(fileHeader);

      if (zipModel.isSplitArchive()) {
        if (zipModel.getCentralDirectory() == null ||
            zipModel.getCentralDirectory().getFileHeaders() == null ||
            zipModel.getCentralDirectory().getFileHeaders().size() == 0) {
          byte[] intByte = new byte[4];
          Raw.writeIntLittleEndian(intByte, 0, (int) InternalZipConstants.SPLITSIG);
          outputStream.write(intByte);
          totalBytesWritten += 4;
        }
      }

      if (this.outputStream instanceof SplitOutputStream) {
        if (totalBytesWritten == 4) {
          fileHeader.setOffsetLocalHeader(4);
        } else {
          fileHeader.setOffsetLocalHeader(((SplitOutputStream) outputStream).getFilePointer());
        }
      } else {
        if (totalBytesWritten == 4) {
          fileHeader.setOffsetLocalHeader(4);
        } else {
          fileHeader.setOffsetLocalHeader(totalBytesWritten);
        }
      }

      HeaderWriter headerWriter = new HeaderWriter();
      totalBytesWritten += headerWriter.writeLocalFileHeader(zipModel, localFileHeader, outputStream);

      initializeEncrypterAndWriteData();
      crc.reset();

    } catch (ZipException e) {
      throw e;
    } catch (Exception e) {
      throw new ZipException(e);
    }
  }

  private void verifyZipParameters(ZipParameters zipParameters, File sourceFile) throws ZipException {
    if (!zipParameters.isSourceExternalStream() && sourceFile == null) {
      throw new ZipException("input file is null");
    }

    if (!zipParameters.isSourceExternalStream() && !Zip4jUtil.checkFileExists(sourceFile)) {
      throw new ZipException("input file does not exist");
    }
  }

  private void initializeEncrypterAndWriteData() throws ZipException, IOException {
    if (!zipParameters.isEncryptFiles()) {
      return;
    }

    encrypter = initializeEncrypter();

    if (zipParameters.getEncryptionMethod() == EncryptionMethod.ZIP_STANDARD) {
      writeStandardEncryptionHeaderData();
    } else if (zipParameters.getEncryptionMethod() == EncryptionMethod.AES) {
      writeAesEncryptionHeaderData();
    }
  }

  private Encrypter initializeEncrypter() throws ZipException {
    if (zipParameters.getEncryptionMethod() == EncryptionMethod.ZIP_STANDARD) {
      // Since we do not know the crc here, we use the modification time for encrypting.
      return new StandardEncrypter(zipParameters.getPassword(), (localFileHeader.getLastModifiedTime() & 0x0000ffff) << 16);
    } else if (zipParameters.getEncryptionMethod() == EncryptionMethod.AES) {
      return new AESEncrpyter(zipParameters.getPassword(), zipParameters.getAesKeyStrength());
    } else {
      return null;
    }
  }

  private void writeStandardEncryptionHeaderData() throws IOException {
    byte[] headerBytes = ((StandardEncrypter) encrypter).getHeaderBytes();
    outputStream.write(headerBytes);
    totalBytesWritten += headerBytes.length;
    bytesWrittenForThisFile += headerBytes.length;
  }

  private void writeAesEncryptionHeaderData() throws IOException {
    byte[] saltBytes = ((AESEncrpyter) encrypter).getSaltBytes();
    byte[] passwordVerifier = ((AESEncrpyter) encrypter).getDerivedPasswordVerifier();
    outputStream.write(saltBytes);
    outputStream.write(passwordVerifier);
    totalBytesWritten += saltBytes.length + passwordVerifier.length;
    bytesWrittenForThisFile += saltBytes.length + passwordVerifier.length;
  }

  private ZipModel initializeZipModel(ZipModel zipModel) {
    if (zipModel == null) {
      zipModel = new ZipModel();
    }

    if (this.outputStream instanceof SplitOutputStream && ((SplitOutputStream) outputStream).isSplitZipFile()) {
      zipModel.setSplitArchive(true);
      zipModel.setSplitLength(((SplitOutputStream) outputStream).getSplitLength());
    }

    zipModel.getEndOfCentralDirRecord().setSignature(InternalZipConstants.ENDSIG);
    return zipModel;
  }

  public void write(int bval) throws IOException {
    byte[] b = new byte[1];
    b[0] = (byte) bval;
    write(b, 0, 1);
  }

  public void write(byte[] b) throws IOException {
    if (b == null)
      throw new NullPointerException();

    if (b.length == 0) return;

    write(b, 0, b.length);
  }

  public void write(byte[] b, int off, int len) throws IOException {
    if (len == 0) return;

    if (zipParameters.isEncryptFiles() && zipParameters.getEncryptionMethod() == EncryptionMethod.AES) {
      if (pendingBufferLength != 0) {
        if (len >= (InternalZipConstants.AES_BLOCK_SIZE - pendingBufferLength)) {
          System.arraycopy(b, off, pendingBuffer, pendingBufferLength,
              (InternalZipConstants.AES_BLOCK_SIZE - pendingBufferLength));
          encryptAndWrite(pendingBuffer, 0, pendingBuffer.length);
          off = (InternalZipConstants.AES_BLOCK_SIZE - pendingBufferLength);
          len = len - off;
          pendingBufferLength = 0;
        } else {
          System.arraycopy(b, off, pendingBuffer, pendingBufferLength, len);
          pendingBufferLength += len;
          return;
        }
      }
      if (len != 0 && len % 16 != 0) {
        System.arraycopy(b, (len + off) - (len % 16), pendingBuffer, 0, len % 16);
        pendingBufferLength = len % 16;
        len = len - pendingBufferLength;
      }
    }
    if (len != 0)
      encryptAndWrite(b, off, len);
  }

  private void encryptAndWrite(byte[] b, int off, int len) throws IOException {
    if (encrypter != null) {
      try {
        encrypter.encryptData(b, off, len);
      } catch (ZipException e) {
        throw new IOException(e);
      }
    }
    outputStream.write(b, off, len);
    totalBytesWritten += len;
    bytesWrittenForThisFile += len;
  }

  public void closeEntry() throws IOException, ZipException {

    if (this.pendingBufferLength != 0) {
      encryptAndWrite(pendingBuffer, 0, pendingBufferLength);
      pendingBufferLength = 0;
    }

    if (this.zipParameters.isEncryptFiles() && this.zipParameters.getEncryptionMethod() == EncryptionMethod.AES) {
      outputStream.write(((AESEncrpyter) encrypter).getFinalMac());
      bytesWrittenForThisFile += 10;
      totalBytesWritten += 10;
    }

    fileHeader.setCompressedSize(bytesWrittenForThisFile);
    localFileHeader.setCompressedSize(bytesWrittenForThisFile);

    if (zipParameters.isSourceExternalStream()) {
      fileHeader.setUncompressedSize(totalBytesRead);
      if (localFileHeader.getUncompressedSize() != totalBytesRead) {
        localFileHeader.setUncompressedSize(totalBytesRead);
      }
    }

    long crc32 = crc.getValue();
    if (fileHeader.isEncrypted()) {
      if (fileHeader.getEncryptionMethod() == EncryptionMethod.AES) {
        crc32 = 0;
      }
    }

    if (zipParameters.isEncryptFiles() &&
        zipParameters.getEncryptionMethod() == EncryptionMethod.AES) {
      fileHeader.setCrc32(0);
      localFileHeader.setCrc32(0);
    } else {
      fileHeader.setCrc32(crc32);
      localFileHeader.setCrc32(crc32);
    }

    zipModel.getLocalFileHeaders().add(localFileHeader);
    zipModel.getCentralDirectory().getFileHeaders().add(fileHeader);

    HeaderWriter headerWriter = new HeaderWriter();
    totalBytesWritten += headerWriter.writeExtendedLocalHeader(localFileHeader, outputStream);

    crc.reset();
    bytesWrittenForThisFile = 0;
    encrypter = null;
    totalBytesRead = 0;
  }

  public void finish() throws IOException, ZipException {
    zipModel.getEndOfCentralDirRecord().setOffsetOfStartOfCentralDir(totalBytesWritten);

    HeaderWriter headerWriter = new HeaderWriter();
    headerWriter.finalizeZipFile(zipModel, outputStream);
  }

  public void close() throws IOException {
    if (outputStream != null) {
      outputStream.close();
    }
  }

  public void decrementCompressedFileSize(int value) {
    if (value <= 0) {
      return;
    }

    if (value <= this.bytesWrittenForThisFile) {
      this.bytesWrittenForThisFile -= value;
    }
  }

  protected void updateTotalBytesRead(int toUpdate) {
    if (toUpdate > 0) {
      totalBytesRead += toUpdate;
    }
  }
}
