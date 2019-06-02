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

package net.lingala.zip4j.headers;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.outputstream.CountingOutputStream;
import net.lingala.zip4j.io.outputstream.SplitOutputStream;
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.Zip64EndOfCentralDirectoryLocator;
import net.lingala.zip4j.model.Zip64EndOfCentralDirectoryRecord;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.util.RawIO;
import net.lingala.zip4j.util.Zip4jUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static net.lingala.zip4j.util.InternalZipConstants.UPDATE_LFH_COMP_SIZE;
import static net.lingala.zip4j.util.InternalZipConstants.UPDATE_LFH_CRC;
import static net.lingala.zip4j.util.InternalZipConstants.UPDATE_LFH_UNCOMP_SIZE;
import static net.lingala.zip4j.util.InternalZipConstants.ZIP_64_LIMIT;
import static net.lingala.zip4j.util.Zip4jUtil.convertCharset;
import static net.lingala.zip4j.util.Zip4jUtil.isStringNotNullAndNotEmpty;

public class HeaderWriter {

  private static final int ZIP64_EXTRA_BUF = 50;

  private RawIO rawIO = new RawIO();

  public void writeLocalFileHeader(ZipModel zipModel, LocalFileHeader localFileHeader, OutputStream outputStream)
      throws ZipException {

    if (localFileHeader == null) {
      throw new ZipException("input parameters are null, cannot write local file header");
    }

    try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
      rawIO.writeIntLittleEndian(byteArrayOutputStream, (int) localFileHeader.getSignature().getValue());
      rawIO.writeShortLittleEndian(byteArrayOutputStream, (short) localFileHeader.getVersionNeededToExtract());
      byteArrayOutputStream.write(localFileHeader.getGeneralPurposeFlag());
      rawIO.writeShortLittleEndian(byteArrayOutputStream, (short) localFileHeader.getCompressionMethod().getCode());
      rawIO.writeIntLittleEndian(byteArrayOutputStream, localFileHeader.getLastModifiedTime());
      rawIO.writeIntLittleEndian(byteArrayOutputStream, (int) localFileHeader.getCrc32());
      long uncompressedSize = localFileHeader.getUncompressedSize();

      byte[] longByte = new byte[8];
      if (uncompressedSize + ZIP64_EXTRA_BUF >= ZIP_64_LIMIT) {
        rawIO.writeLongLittleEndian(longByte, 0, ZIP_64_LIMIT);

        //Set the uncompressed size to ZipConstants.ZIP_64_LIMIT as
        //these values will be stored in Zip64 extra record
        byteArrayOutputStream.write(longByte, 0, 4);
        byteArrayOutputStream.write(longByte, 0, 4);

        zipModel.setZip64Format(true);
        localFileHeader.setWriteCompressedSizeInZip64ExtraRecord(true);
      } else {
        rawIO.writeLongLittleEndian(longByte, 0, localFileHeader.getCompressedSize());
        byteArrayOutputStream.write(longByte, 0, 4);

        rawIO.writeLongLittleEndian(longByte, 0, localFileHeader.getUncompressedSize());
        byteArrayOutputStream.write(longByte, 0, 4);

        zipModel.setZip64Format(false);
        localFileHeader.setWriteCompressedSizeInZip64ExtraRecord(false);
      }

      rawIO.writeShortLittleEndian(byteArrayOutputStream, (short) localFileHeader.getFileNameLength());

      int extraFieldLength = 0;
      if (zipModel.isZip64Format()) {
        extraFieldLength += 20;
      }
      if (localFileHeader.getAesExtraDataRecord() != null) {
        extraFieldLength += 11;
      }
      rawIO.writeShortLittleEndian(byteArrayOutputStream, (short) (extraFieldLength));

      if (isStringNotNullAndNotEmpty(zipModel.getFileNameCharset())) {
        byte[] fileNameBytes = localFileHeader.getFileName().getBytes(zipModel.getFileNameCharset());
        byteArrayOutputStream.write(fileNameBytes);
      } else {
        byteArrayOutputStream.write(convertCharset(localFileHeader.getFileName()));
      }

      //Zip64 should be the first extra data record that should be written
      //This is NOT according to any specification but if this is changed
      //then take care of updateLocalFileHeader for compressed size
      if (zipModel.isZip64Format()) {
        rawIO.writeShortLittleEndian(byteArrayOutputStream,
            (short) HeaderSignature.ZIP64_EXTRA_FIELD_LENGTH.getValue());

        //Zip64 extra data record size
        //hardcoded it to 16 for local file header as we will just write
        //compressed and uncompressed file sizes
        rawIO.writeShortLittleEndian(byteArrayOutputStream, (short) 16);
        rawIO.writeLongLittleEndian(byteArrayOutputStream, localFileHeader.getUncompressedSize());
        //set compressed size to 0 for now
        byteArrayOutputStream.write(new byte[] {0, 0, 0, 0, 0, 0, 0, 0});
      }

      if (localFileHeader.getAesExtraDataRecord() != null) {
        AESExtraDataRecord aesExtraDataRecord = localFileHeader.getAesExtraDataRecord();
        rawIO.writeShortLittleEndian(byteArrayOutputStream, (short) aesExtraDataRecord.getSignature().getValue());
        rawIO.writeShortLittleEndian(byteArrayOutputStream, (short) aesExtraDataRecord.getDataSize());
        rawIO.writeShortLittleEndian(byteArrayOutputStream, (short) aesExtraDataRecord.getVersionNumber());
        byteArrayOutputStream.write(aesExtraDataRecord.getVendorID().getBytes());

        byte[] aesStrengthBytes = new byte[1];
        aesStrengthBytes[0] = (byte) aesExtraDataRecord.getAesKeyStrength().getRawCode();
        byteArrayOutputStream.write(aesStrengthBytes);

        rawIO.writeShortLittleEndian(byteArrayOutputStream, (short) aesExtraDataRecord.getCompressionMethod().getCode());
      }

      outputStream.write(byteArrayOutputStream.toByteArray());
    } catch (ZipException e) {
      throw e;
    } catch (Exception e) {
      throw new ZipException(e);
    }
  }

  public void writeExtendedLocalHeader(LocalFileHeader localFileHeader, OutputStream outputStream)
      throws ZipException, IOException {

    if (localFileHeader == null || outputStream == null) {
      throw new ZipException("input parameters is null, cannot write extended local header");
    }

    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
      rawIO.writeIntLittleEndian(byteArrayOutputStream, (int) HeaderSignature.EXTRA_DATA_RECORD.getValue());
      rawIO.writeIntLittleEndian(byteArrayOutputStream, (int) localFileHeader.getCrc32());

      long compressedSize = localFileHeader.getCompressedSize();
      if (compressedSize >= Integer.MAX_VALUE) {
        compressedSize = Integer.MAX_VALUE;
      }
      rawIO.writeIntLittleEndian(byteArrayOutputStream, (int) compressedSize);

      long uncompressedSize = localFileHeader.getUncompressedSize();
      if (uncompressedSize >= Integer.MAX_VALUE) {
        uncompressedSize = Integer.MAX_VALUE;
      }
      rawIO.writeIntLittleEndian(byteArrayOutputStream, (int) uncompressedSize);

      outputStream.write(byteArrayOutputStream.toByteArray());
    }
  }

  public void finalizeZipFile(ZipModel zipModel, OutputStream outputStream) throws ZipException {
    if (zipModel == null || outputStream == null) {
      throw new ZipException("input parameters is null, cannot finalize zip file");
    }

    try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
      processHeaderData(zipModel, outputStream);
      long offsetCentralDir = zipModel.getEndOfCentralDirectoryRecord().getOffsetOfStartOfCentralDirectory();
      writeCentralDirectory(zipModel, byteArrayOutputStream, rawIO);
      int sizeOfCentralDir = byteArrayOutputStream.size();

      if (zipModel.isZip64Format()) {
        if (zipModel.getZip64EndOfCentralDirectoryRecord() == null) {
          zipModel.setZip64EndOfCentralDirectoryRecord(new Zip64EndOfCentralDirectoryRecord());
        }
        if (zipModel.getZip64EndOfCentralDirectoryLocator() == null) {
          zipModel.setZip64EndOfCentralDirectoryLocator(new Zip64EndOfCentralDirectoryLocator());
        }

        zipModel.getZip64EndOfCentralDirectoryLocator().setOffsetZip64EndOfCentralDirectoryRecord(offsetCentralDir
            + sizeOfCentralDir);

        if (isSplitZipFile(outputStream)) {
          int currentSplitFileCounter = getCurrentSplitFileCounter(outputStream);
          zipModel.getZip64EndOfCentralDirectoryLocator().setNumberOfDiskStartOfZip64EndOfCentralDirectoryRecord(
              currentSplitFileCounter);
          zipModel.getZip64EndOfCentralDirectoryLocator().setTotalNumberOfDiscs(currentSplitFileCounter + 1);
        } else {
          zipModel.getZip64EndOfCentralDirectoryLocator().setNumberOfDiskStartOfZip64EndOfCentralDirectoryRecord(0);
          zipModel.getZip64EndOfCentralDirectoryLocator().setTotalNumberOfDiscs(1);
        }

        writeZip64EndOfCentralDirectoryRecord(zipModel, sizeOfCentralDir, offsetCentralDir, byteArrayOutputStream,
            rawIO);
        writeZip64EndOfCentralDirectoryLocator(zipModel, byteArrayOutputStream, rawIO);
      }

      writeEndOfCentralDirectoryRecord(zipModel, sizeOfCentralDir, offsetCentralDir, byteArrayOutputStream, rawIO);
      writeZipHeaderBytes(zipModel, outputStream, byteArrayOutputStream.toByteArray());
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  public void finalizeZipFileWithoutValidations(ZipModel zipModel, OutputStream outputStream) throws ZipException {

    if (zipModel == null || outputStream == null) {
      throw new ZipException("input parameters is null, cannot finalize zip file without validations");
    }

    try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
      long offsetCentralDir = zipModel.getEndOfCentralDirectoryRecord().getOffsetOfStartOfCentralDirectory();
      writeCentralDirectory(zipModel, byteArrayOutputStream, rawIO);
      int sizeOfCentralDir = byteArrayOutputStream.size();

      if (zipModel.isZip64Format()) {
        if (zipModel.getZip64EndOfCentralDirectoryRecord() == null) {
          zipModel.setZip64EndOfCentralDirectoryRecord(new Zip64EndOfCentralDirectoryRecord());
        }
        if (zipModel.getZip64EndOfCentralDirectoryLocator() == null) {
          zipModel.setZip64EndOfCentralDirectoryLocator(new Zip64EndOfCentralDirectoryLocator());
        }

        zipModel.getZip64EndOfCentralDirectoryLocator().setOffsetZip64EndOfCentralDirectoryRecord(offsetCentralDir
            + sizeOfCentralDir);

        writeZip64EndOfCentralDirectoryRecord(zipModel, sizeOfCentralDir, offsetCentralDir, byteArrayOutputStream,
            rawIO);
        writeZip64EndOfCentralDirectoryLocator(zipModel, byteArrayOutputStream, rawIO);
      }

      writeEndOfCentralDirectoryRecord(zipModel, sizeOfCentralDir, offsetCentralDir, byteArrayOutputStream, rawIO);
      writeZipHeaderBytes(zipModel, outputStream, byteArrayOutputStream.toByteArray());
    } catch (ZipException e) {
      throw e;
    } catch (Exception e) {
      throw new ZipException(e);
    }
  }

  private boolean isSplitZipFile(OutputStream outputStream) {
    if (outputStream instanceof SplitOutputStream) {
      return ((SplitOutputStream) outputStream).isSplitZipFile();
    } else if (outputStream instanceof CountingOutputStream) {
      return ((CountingOutputStream) outputStream).isSplitOutputStream();
    }

    return false;
  }

  private int getCurrentSplitFileCounter(OutputStream outputStream) {
    if (outputStream instanceof SplitOutputStream) {
      return ((SplitOutputStream) outputStream).getCurrentSplitFileCounter();
    }
    return ((CountingOutputStream) outputStream).getCurrentSplitFileCounter();
  }

  private void writeZipHeaderBytes(ZipModel zipModel, OutputStream outputStream, byte[] buff)
      throws ZipException {
    if (buff == null) {
      throw new ZipException("invalid buff to write as zip headers");
    }

    try {
      if (outputStream instanceof CountingOutputStream) {
        if (((CountingOutputStream) outputStream).checkBuffSizeAndStartNextSplitFile(buff.length)) {
          finalizeZipFile(zipModel, outputStream);
          return;
        }
      }

      outputStream.write(buff);
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  private void processHeaderData(ZipModel zipModel, OutputStream outputStream) throws ZipException {
    try {
      int currentSplitFileCounter = 0;
      if (outputStream instanceof CountingOutputStream) {
        zipModel.getEndOfCentralDirectoryRecord().setOffsetOfStartOfCentralDirectory(
            ((CountingOutputStream) outputStream).getFilePointer());
        currentSplitFileCounter = ((CountingOutputStream) outputStream).getCurrentSplitFileCounter();
      }

      if (zipModel.isZip64Format()) {
        if (zipModel.getZip64EndOfCentralDirectoryRecord() == null) {
          zipModel.setZip64EndOfCentralDirectoryRecord(new Zip64EndOfCentralDirectoryRecord());
        }
        if (zipModel.getZip64EndOfCentralDirectoryLocator() == null) {
          zipModel.setZip64EndOfCentralDirectoryLocator(new Zip64EndOfCentralDirectoryLocator());
        }

        zipModel.getZip64EndOfCentralDirectoryLocator().setNumberOfDiskStartOfZip64EndOfCentralDirectoryRecord(
            currentSplitFileCounter);
        zipModel.getZip64EndOfCentralDirectoryLocator().setTotalNumberOfDiscs(currentSplitFileCounter + 1);
      }
      zipModel.getEndOfCentralDirectoryRecord().setNumberOfThisDisk(currentSplitFileCounter);
      zipModel.getEndOfCentralDirectoryRecord().setNumberOfThisDiskStartOfCentralDir(currentSplitFileCounter);
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  private void writeCentralDirectory(ZipModel zipModel, ByteArrayOutputStream byteArrayOutputStream, RawIO rawIO)
      throws ZipException {

    if (zipModel.getCentralDirectory() == null || zipModel.getCentralDirectory().getFileHeaders() == null
        || zipModel.getCentralDirectory().getFileHeaders().size() <= 0) {
      return;
    }

    for (FileHeader fileHeader: zipModel.getCentralDirectory().getFileHeaders()) {
      writeFileHeader(zipModel, fileHeader, byteArrayOutputStream, rawIO);
    }
  }

  private void writeFileHeader(ZipModel zipModel, FileHeader fileHeader, ByteArrayOutputStream byteArrayOutputStream,
                              RawIO rawIO) throws ZipException {

    if (fileHeader == null) {
      throw new ZipException("input parameters is null, cannot write local file header");
    }

    try {
      byte[] longByte = new byte[8];
      final byte[] emptyShortByte = {0, 0};

      boolean writeZip64FileSize = false;
      boolean writeZip64OffsetLocalHeader = false;

      rawIO.writeIntLittleEndian(byteArrayOutputStream, (int) fileHeader.getSignature().getValue());
      rawIO.writeShortLittleEndian(byteArrayOutputStream, (short) fileHeader.getVersionMadeBy());
      rawIO.writeShortLittleEndian(byteArrayOutputStream, (short) fileHeader.getVersionNeededToExtract());
      byteArrayOutputStream.write(fileHeader.getGeneralPurposeFlag());
      rawIO.writeShortLittleEndian(byteArrayOutputStream, (short) fileHeader.getCompressionMethod().getCode());

      int lastModifiedTime = fileHeader.getLastModifiedTime();
      rawIO.writeIntLittleEndian(byteArrayOutputStream, lastModifiedTime);
      rawIO.writeIntLittleEndian(byteArrayOutputStream, (int) (fileHeader.getCrc32()));

      if (fileHeader.getCompressedSize() >= ZIP_64_LIMIT ||
          fileHeader.getUncompressedSize() + ZIP64_EXTRA_BUF >= ZIP_64_LIMIT) {
        rawIO.writeLongLittleEndian(longByte, 0, ZIP_64_LIMIT);
        byteArrayOutputStream.write(longByte, 0, 4);
        byteArrayOutputStream.write(longByte, 0, 4);
        writeZip64FileSize = true;
      } else {
        rawIO.writeLongLittleEndian(longByte, 0, fileHeader.getCompressedSize());
        byteArrayOutputStream.write(longByte, 0, 4);
        rawIO.writeLongLittleEndian(longByte, 0, fileHeader.getUncompressedSize());
        byteArrayOutputStream.write(longByte, 0, 4);
      }

      rawIO.writeShortLittleEndian(byteArrayOutputStream, (short) fileHeader.getFileNameLength());

      //Compute offset bytes before extra field is written for Zip64 compatibility
      //NOTE: this data is not written now, but written at a later point
      byte[] offsetLocalHeaderBytes = new byte[4];
      if (fileHeader.getOffsetLocalHeader() > ZIP_64_LIMIT) {
        rawIO.writeLongLittleEndian(longByte, 0, ZIP_64_LIMIT);
        System.arraycopy(longByte, 0, offsetLocalHeaderBytes, 0, 4);
        writeZip64OffsetLocalHeader = true;
      } else {
        rawIO.writeLongLittleEndian(longByte, 0, fileHeader.getOffsetLocalHeader());
        System.arraycopy(longByte, 0, offsetLocalHeaderBytes, 0, 4);
      }

      int extraFieldLength = 0;
      if (writeZip64FileSize || writeZip64OffsetLocalHeader) {
        extraFieldLength += 4;
        if (writeZip64FileSize)
          extraFieldLength += 16;
        if (writeZip64OffsetLocalHeader)
          extraFieldLength += 8;
      }
      if (fileHeader.getAesExtraDataRecord() != null) {
        extraFieldLength += 11;
      }
      rawIO.writeShortLittleEndian(byteArrayOutputStream, (short) (extraFieldLength));

      //Skip file comment length for now
      byteArrayOutputStream.write(emptyShortByte);

      //Skip disk number start for now
      rawIO.writeShortLittleEndian(byteArrayOutputStream, (short) fileHeader.getDiskNumberStart());

      //Skip internal file attributes for now
      byteArrayOutputStream.write(emptyShortByte);

      //External file attributes
      byteArrayOutputStream.write(new byte[] {1, 1, 1, 1});

      //offset local header - this data is computed above
      byteArrayOutputStream.write(offsetLocalHeaderBytes);

      if (isStringNotNullAndNotEmpty(zipModel.getFileNameCharset())) {
        byte[] fileNameBytes = fileHeader.getFileName().getBytes(zipModel.getFileNameCharset());
        byteArrayOutputStream.write(fileNameBytes);
      } else {
        byteArrayOutputStream.write(convertCharset(fileHeader.getFileName()));
      }

      if (writeZip64FileSize || writeZip64OffsetLocalHeader) {
        zipModel.setZip64Format(true);

        //Zip64 header
        rawIO.writeShortLittleEndian(byteArrayOutputStream,
            (short) HeaderSignature.ZIP64_EXTRA_FIELD_LENGTH.getValue());

        //Zip64 extra data record size
        int dataSize = 0;

        if (writeZip64FileSize) {
          dataSize += 16;
        }
        if (writeZip64OffsetLocalHeader) {
          dataSize += 8;
        }

        rawIO.writeShortLittleEndian(byteArrayOutputStream, (short) dataSize);

        if (writeZip64FileSize) {
          rawIO.writeLongLittleEndian(byteArrayOutputStream, fileHeader.getUncompressedSize());
          rawIO.writeLongLittleEndian(byteArrayOutputStream, fileHeader.getCompressedSize());
        }

        if (writeZip64OffsetLocalHeader) {
          rawIO.writeLongLittleEndian(byteArrayOutputStream, fileHeader.getOffsetLocalHeader());
        }
      }

      if (fileHeader.getAesExtraDataRecord() != null) {
        AESExtraDataRecord aesExtraDataRecord = fileHeader.getAesExtraDataRecord();
        rawIO.writeShortLittleEndian(byteArrayOutputStream, (short) aesExtraDataRecord.getSignature().getValue());
        rawIO.writeShortLittleEndian(byteArrayOutputStream, (short) aesExtraDataRecord.getDataSize());
        rawIO.writeShortLittleEndian(byteArrayOutputStream, (short) aesExtraDataRecord.getVersionNumber());
        byteArrayOutputStream.write(aesExtraDataRecord.getVendorID().getBytes());

        byte[] aesStrengthBytes = new byte[1];
        aesStrengthBytes[0] = (byte) aesExtraDataRecord.getAesKeyStrength().getRawCode();
        byteArrayOutputStream.write(aesStrengthBytes);

        rawIO.writeShortLittleEndian(byteArrayOutputStream,
            (short) aesExtraDataRecord.getCompressionMethod().getCode());
      }
    } catch (Exception e) {
      throw new ZipException(e);
    }
  }

  private void writeZip64EndOfCentralDirectoryRecord(ZipModel zipModel, int sizeOfCentralDir, long offsetCentralDir,
                                                     ByteArrayOutputStream byteArrayOutputStream, RawIO rawIO)
      throws ZipException {

    try {
      byte[] emptyShortByte = {0, 0};

      rawIO.writeIntLittleEndian(byteArrayOutputStream,
          (int) HeaderSignature.ZIP64_END_CENTRAL_DIRECTORY_RECORD.getValue());
      rawIO.writeLongLittleEndian(byteArrayOutputStream, (long) 44);

      if (zipModel.getCentralDirectory() != null &&
          zipModel.getCentralDirectory().getFileHeaders() != null &&
          zipModel.getCentralDirectory().getFileHeaders().size() > 0) {
        rawIO.writeShortLittleEndian(byteArrayOutputStream,
            (short) zipModel.getCentralDirectory().getFileHeaders().get(0).getVersionMadeBy());

        rawIO.writeShortLittleEndian(byteArrayOutputStream,
            (short) zipModel.getCentralDirectory().getFileHeaders().get(0).getVersionNeededToExtract());
      } else {
        byteArrayOutputStream.write(emptyShortByte);
        byteArrayOutputStream.write(emptyShortByte);
      }

      rawIO.writeIntLittleEndian(byteArrayOutputStream,
          zipModel.getEndOfCentralDirectoryRecord().getNumberOfThisDisk());
      rawIO.writeIntLittleEndian(byteArrayOutputStream, zipModel.getEndOfCentralDirectoryRecord()
          .getNumberOfThisDiskStartOfCentralDir());

      int numEntries = 0;
      int numEntriesOnThisDisk = 0;
      if (zipModel.getCentralDirectory() == null ||
          zipModel.getCentralDirectory().getFileHeaders() == null) {
        throw new ZipException("invalid central directory/file headers, " +
            "cannot write end of central directory record");
      } else {
        numEntries = zipModel.getCentralDirectory().getFileHeaders().size();
        if (zipModel.isSplitArchive()) {
          countNumberOfFileHeaderEntriesOnDisk(zipModel.getCentralDirectory().getFileHeaders(),
              zipModel.getEndOfCentralDirectoryRecord().getNumberOfThisDisk());
        } else {
          numEntriesOnThisDisk = numEntries;
        }
      }
      rawIO.writeLongLittleEndian(byteArrayOutputStream, numEntriesOnThisDisk);
      rawIO.writeLongLittleEndian(byteArrayOutputStream, numEntries);
      rawIO.writeLongLittleEndian(byteArrayOutputStream, sizeOfCentralDir);
      rawIO.writeLongLittleEndian(byteArrayOutputStream, offsetCentralDir);
    } catch (ZipException zipException) {
      throw zipException;
    } catch (Exception e) {
      throw new ZipException(e);
    }
  }

  private void writeZip64EndOfCentralDirectoryLocator(ZipModel zipModel, ByteArrayOutputStream byteArrayOutputStream,
                                                      RawIO rawIO) throws ZipException {
    try {
      rawIO.writeIntLittleEndian(byteArrayOutputStream, (int) HeaderSignature.ZIP64_END_CENTRAL_DIRECTORY_LOCATOR.getValue());
      rawIO.writeIntLittleEndian(byteArrayOutputStream,
          zipModel.getZip64EndOfCentralDirectoryLocator().getNumberOfDiskStartOfZip64EndOfCentralDirectoryRecord());
      rawIO.writeLongLittleEndian(byteArrayOutputStream,
          zipModel.getZip64EndOfCentralDirectoryLocator().getOffsetZip64EndOfCentralDirectoryRecord());
      rawIO.writeIntLittleEndian(byteArrayOutputStream,
          zipModel.getZip64EndOfCentralDirectoryLocator().getTotalNumberOfDiscs());
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  private void writeEndOfCentralDirectoryRecord(ZipModel zipModel, int sizeOfCentralDir, long offsetCentralDir,
                                                ByteArrayOutputStream byteArrayOutputStream, RawIO rawIO)
      throws ZipException {

    try {
      byte[] longByte = new byte[8];
      rawIO.writeIntLittleEndian(byteArrayOutputStream, (int) HeaderSignature.END_OF_CENTRAL_DIRECTORY.getValue());
      rawIO.writeShortLittleEndian(byteArrayOutputStream,
          (short) (zipModel.getEndOfCentralDirectoryRecord().getNumberOfThisDisk()));
      rawIO.writeShortLittleEndian(byteArrayOutputStream,
          (short) (zipModel.getEndOfCentralDirectoryRecord().getNumberOfThisDiskStartOfCentralDir()));

      int numEntriesOnThisDisk;
      int numEntries = zipModel.getCentralDirectory().getFileHeaders().size();
      if (zipModel.isSplitArchive()) {
        numEntriesOnThisDisk = countNumberOfFileHeaderEntriesOnDisk(zipModel.getCentralDirectory().getFileHeaders(),
            zipModel.getEndOfCentralDirectoryRecord().getNumberOfThisDisk());
      } else {
        numEntriesOnThisDisk = numEntries;
      }
      rawIO.writeShortLittleEndian(byteArrayOutputStream, (short) numEntriesOnThisDisk);
      rawIO.writeShortLittleEndian(byteArrayOutputStream, (short) numEntries);
      rawIO.writeIntLittleEndian(byteArrayOutputStream, sizeOfCentralDir);
      if (offsetCentralDir > ZIP_64_LIMIT) {
        rawIO.writeLongLittleEndian(longByte, 0, ZIP_64_LIMIT);
        byteArrayOutputStream.write(longByte, 0, 4);
      } else {
        rawIO.writeLongLittleEndian(longByte, 0, offsetCentralDir);
        byteArrayOutputStream.write(longByte, 0, 4);
      }

      int commentLength = 0;
      if (zipModel.getEndOfCentralDirectoryRecord().getComment() != null) {
        commentLength = zipModel.getEndOfCentralDirectoryRecord().getCommentLength();
      }
      rawIO.writeShortLittleEndian(byteArrayOutputStream, (short) commentLength);

      if (commentLength > 0) {
        byteArrayOutputStream.write(zipModel.getEndOfCentralDirectoryRecord().getCommentBytes());
      }
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  public void updateLocalFileHeader(LocalFileHeader localFileHeader, long offset, int toUpdate, ZipModel zipModel,
                                    byte[] bytesToWrite, int noOfDisk, SplitOutputStream outputStream)
      throws ZipException {

    try {
      boolean closeFlag = false;
      SplitOutputStream currOutputStream = null;

      if (noOfDisk != outputStream.getCurrentSplitFileCounter()) {
        String parentFile = zipModel.getZipFile().getParent();
        String fileNameWithoutExt = Zip4jUtil.getZipFileNameWithoutExt(zipModel.getZipFile().getName());
        String fileName = parentFile + System.getProperty("file.separator");
        if (noOfDisk < 9) {
          fileName += fileNameWithoutExt + ".z0" + (noOfDisk + 1);
        } else {
          fileName += fileNameWithoutExt + ".z" + (noOfDisk + 1);
        }
        currOutputStream = new SplitOutputStream(new File(fileName));
        closeFlag = true;
      } else {
        currOutputStream = outputStream;
      }

      long currOffset = currOutputStream.getFilePointer();

      switch (toUpdate) {
        case UPDATE_LFH_CRC:
          currOutputStream.seek(offset + toUpdate);
          currOutputStream.write(bytesToWrite);
          break;
        case UPDATE_LFH_COMP_SIZE:
        case UPDATE_LFH_UNCOMP_SIZE:
          updateCompressedSizeInLocalFileHeader(currOutputStream, localFileHeader,
              offset, toUpdate, bytesToWrite);
          break;
        default:
          break;
      }
      if (closeFlag) {
        currOutputStream.close();
      } else {
        outputStream.seek(currOffset);
      }

    } catch (Exception e) {
      throw new ZipException(e);
    }
  }

  private void updateCompressedSizeInLocalFileHeader(SplitOutputStream outputStream, LocalFileHeader localFileHeader,
                                                     long offset, long toUpdate, byte[] bytesToWrite)
      throws ZipException {

    try {
      if (localFileHeader.isWriteCompressedSizeInZip64ExtraRecord()) {
        if (bytesToWrite.length != 8) {
          throw new ZipException("attempting to write a non 8-byte compressed size block for a zip64 file");
        }

        //4 - compressed size
        //4 - uncomprssed size
        //2 - file name length
        //2 - extra field length
        //file name length
        //2 - Zip64 signature
        //2 - size of zip64 data
        //8 - crc
        //8 - compressed size
        //8 - uncompressed size
        long zip64CompressedSizeOffset = offset + toUpdate + 4 + 4 + 2 + 2 + localFileHeader.getFileNameLength()
            + 2 + 2 + 8;
        if (toUpdate == UPDATE_LFH_UNCOMP_SIZE) {
          zip64CompressedSizeOffset += 8;
        }
        outputStream.seek(zip64CompressedSizeOffset);
        outputStream.write(bytesToWrite);
      } else {
        outputStream.seek(offset + toUpdate);
        outputStream.write(bytesToWrite);
      }
    } catch (IOException e) {
      throw new ZipException(e);
    }

  }

  private int countNumberOfFileHeaderEntriesOnDisk(List<FileHeader> fileHeaders, int numOfDisk) throws ZipException {
    if (fileHeaders == null) {
      throw new ZipException("file headers are null, cannot calculate number of entries on this disk");
    }

    int noEntries = 0;
    for (FileHeader fileHeader : fileHeaders) {
      if (fileHeader.getDiskNumberStart() == numOfDisk) {
        noEntries++;
      }
    }
    return noEntries;
  }

}