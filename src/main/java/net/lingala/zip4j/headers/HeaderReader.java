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
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.CentralDirectory;
import net.lingala.zip4j.model.DigitalSignature;
import net.lingala.zip4j.model.EndOfCentralDirectoryRecord;
import net.lingala.zip4j.model.ExtraDataRecord;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.Zip64EndOfCentralDirectoryLocator;
import net.lingala.zip4j.model.Zip64EndOfCentralDirectoryRecord;
import net.lingala.zip4j.model.Zip64ExtendedInfo;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.util.RawIO;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static net.lingala.zip4j.headers.HeaderUtil.decodeFileName;
import static net.lingala.zip4j.util.InternalZipConstants.ENDHDR;

/**
 * Helper class to read header information for the zip file
 */
public class HeaderReader {

  private ZipModel zipModel;
  private RawIO rawIO = new RawIO();

  public ZipModel readAllHeaders(RandomAccessFile zip4jRaf) throws ZipException {
    zipModel = new ZipModel();
    zipModel.setEndOfCentralDirectoryRecord(readEndOfCentralDirectoryRecord(zip4jRaf, rawIO));

    // If file is Zip64 format, Zip64 headers have to be read before reading central directory
    zipModel.setZip64EndOfCentralDirectoryLocator(readZip64EndCentralDirLocator(zip4jRaf, rawIO));

    if (zipModel.isZip64Format()) {
      zipModel.setZip64EndOfCentralDirectoryRecord(readZip64EndCentralDirRec(zip4jRaf, rawIO));
      if (zipModel.getZip64EndOfCentralDirectoryRecord() != null
          && zipModel.getZip64EndOfCentralDirectoryRecord().getNumberOfThisDisk() > 0) {
        zipModel.setSplitArchive(true);
      } else {
        zipModel.setSplitArchive(false);
      }
    }

    zipModel.setCentralDirectory(readCentralDirectory(zip4jRaf, rawIO));

    return zipModel;
  }

  private EndOfCentralDirectoryRecord readEndOfCentralDirectoryRecord(RandomAccessFile zip4jRaf, RawIO rawIO)
      throws ZipException {
    try {
      long pos = zip4jRaf.length() - ENDHDR;

      EndOfCentralDirectoryRecord endOfCentralDirectoryRecord = new EndOfCentralDirectoryRecord();

      int counter = 0;
      int headerSignature;
      do {
        zip4jRaf.seek(pos--);
        counter++;
      } while (((headerSignature = rawIO.readIntLittleEndian(zip4jRaf))
          != HeaderSignature.END_OF_CENTRAL_DIRECTORY.getValue()) && counter <= 3000);

      if (headerSignature != HeaderSignature.END_OF_CENTRAL_DIRECTORY.getValue()) {
        throw new ZipException("zip headers not found. probably not a zip file");
      }

      endOfCentralDirectoryRecord.setSignature(HeaderSignature.END_OF_CENTRAL_DIRECTORY);
      endOfCentralDirectoryRecord.setNumberOfThisDisk(rawIO.readShortLittleEndian(zip4jRaf));
      endOfCentralDirectoryRecord.setNumberOfThisDiskStartOfCentralDir(rawIO.readShortLittleEndian(zip4jRaf));
      endOfCentralDirectoryRecord.setTotalNumberOfEntriesInCentralDirectoryOnThisDisk(
          rawIO.readShortLittleEndian(zip4jRaf));
      endOfCentralDirectoryRecord.setTotalNumberOfEntriesInCentralDirectory(rawIO.readShortLittleEndian(zip4jRaf));
      endOfCentralDirectoryRecord.setSizeOfCentralDirectory(rawIO.readIntLittleEndian(zip4jRaf));
      endOfCentralDirectoryRecord.setOffsetOfStartOfCentralDirectory(rawIO.readIntLittleEndian(zip4jRaf));

      int commentLength = rawIO.readShortLittleEndian(zip4jRaf);

      if (commentLength > 0) {
        byte[] commentBuf = new byte[commentLength];
        zip4jRaf.readFully(commentBuf);
        endOfCentralDirectoryRecord.setComment(new String(commentBuf, StandardCharsets.UTF_8));
      } else {
        endOfCentralDirectoryRecord.setComment(null);
      }

      zipModel.setSplitArchive(endOfCentralDirectoryRecord.getNumberOfThisDisk() > 0);
      return endOfCentralDirectoryRecord;
    } catch (IOException e) {
      throw new ZipException("Probably not a zip file or a corrupted zip file", e);
    }
  }

  private CentralDirectory readCentralDirectory(RandomAccessFile zip4jRaf, RawIO rawIO) throws ZipException {
    try {
      CentralDirectory centralDirectory = new CentralDirectory();
      List<FileHeader> fileHeaders = new ArrayList<>();

      EndOfCentralDirectoryRecord endOfCentralDirectoryRecord = zipModel.getEndOfCentralDirectoryRecord();
      long offSetStartCentralDir = endOfCentralDirectoryRecord.getOffsetOfStartOfCentralDirectory();
      int centralDirEntryCount = endOfCentralDirectoryRecord.getTotalNumberOfEntriesInCentralDirectory();

      if (zipModel.isZip64Format()) {
        offSetStartCentralDir = zipModel.getZip64EndOfCentralDirectoryRecord()
            .getOffsetStartCentralDirectoryWRTStartDiskNumber();
        centralDirEntryCount = (int) zipModel.getZip64EndOfCentralDirectoryRecord()
            .getTotNumberOfEntriesInCentralDirectory();
      }

      zip4jRaf.seek(offSetStartCentralDir);

      byte[] shortBuff = new byte[2];
      byte[] intBuff = new byte[4];

      for (int i = 0; i < centralDirEntryCount; i++) {
        FileHeader fileHeader = new FileHeader();
        if (rawIO.readIntLittleEndian(zip4jRaf) != HeaderSignature.CENTRAL_DIRECTORY.getValue()) {
          throw new ZipException("Expected central directory entry not found (#" + (i + 1) + ")");
        }
        fileHeader.setSignature(HeaderSignature.CENTRAL_DIRECTORY);
        fileHeader.setVersionMadeBy(rawIO.readShortLittleEndian(zip4jRaf));
        fileHeader.setVersionNeededToExtract(rawIO.readShortLittleEndian(zip4jRaf));

        byte[] generalPurposeFlags = new byte[2];
        zip4jRaf.readFully(generalPurposeFlags);
        BigInteger firstByteAsBigInteger = BigInteger.valueOf(generalPurposeFlags[0]);
        fileHeader.setEncrypted(firstByteAsBigInteger.testBit(0));
        fileHeader.setDataDescriptorExists(firstByteAsBigInteger.testBit(3));
        fileHeader.setFileNameUTF8Encoded(BigInteger.valueOf(generalPurposeFlags[1]).testBit(11));
        fileHeader.setGeneralPurposeFlag(generalPurposeFlags.clone());

        fileHeader.setCompressionMethod(CompressionMethod.getCompressionMethodFromCode(rawIO.readShortLittleEndian(
            zip4jRaf)));
        fileHeader.setLastModifiedTime(rawIO.readIntLittleEndian(zip4jRaf));

        zip4jRaf.readFully(intBuff);
        fileHeader.setCrc32(rawIO.readIntLittleEndian(intBuff));
        fileHeader.setCrcRawData(intBuff);

        fileHeader.setCompressedSize(rawIO.readIntLittleEndian(zip4jRaf));
        fileHeader.setUncompressedSize(rawIO.readIntLittleEndian(zip4jRaf));

        int fileNameLength = rawIO.readShortLittleEndian(zip4jRaf);
        fileHeader.setFileNameLength(fileNameLength);

        fileHeader.setExtraFieldLength(rawIO.readShortLittleEndian(zip4jRaf));

        int fileCommentLength = rawIO.readShortLittleEndian(zip4jRaf);
        fileHeader.setFileCommentLength(fileCommentLength);

        fileHeader.setDiskNumberStart(rawIO.readShortLittleEndian(zip4jRaf));

        zip4jRaf.readFully(shortBuff);
        fileHeader.setInternalFileAttributes(shortBuff.clone());

        zip4jRaf.readFully(intBuff);
        fileHeader.setExternalFileAttributes(intBuff.clone());

        fileHeader.setOffsetLocalHeader(rawIO.readIntLittleEndian(zip4jRaf));

        if (fileNameLength > 0) {
          byte[] fileNameBuff = new byte[fileNameLength];
          zip4jRaf.readFully(fileNameBuff);
          String fileName = decodeFileName(fileNameBuff, fileHeader.isFileNameUTF8Encoded());

          if (fileName.contains(":" + System.getProperty("file.separator"))) {
            fileName = fileName.substring(fileName.indexOf(":" + System.getProperty("file.separator")) + 2);
          }

          fileHeader.setFileName(fileName);
          fileHeader.setDirectory(fileName.endsWith("/") || fileName.endsWith("\\"));
        } else {
          fileHeader.setFileName(null);
        }

        readAndSaveExtraDataRecord(zip4jRaf, fileHeader, rawIO);
        readAndSaveZip64ExtendedInfo(fileHeader, rawIO);
        readAndSaveAESExtraDataRecord(fileHeader, rawIO);

        if (fileCommentLength > 0) {
          byte[] fileCommentBuff = new byte[fileCommentLength];
          zip4jRaf.readFully(fileCommentBuff);
          fileHeader.setFileComment(new String(fileCommentBuff));
        }

        fileHeaders.add(fileHeader);
      }

      centralDirectory.setFileHeaders(fileHeaders);

      DigitalSignature digitalSignature = new DigitalSignature();
      if (rawIO.readIntLittleEndian(zip4jRaf) == HeaderSignature.DIGITAL_SIGNATURE.getValue()) {
        digitalSignature.setSignature(HeaderSignature.DIGITAL_SIGNATURE);
        digitalSignature.setSizeOfData(rawIO.readShortLittleEndian(zip4jRaf));

        if (digitalSignature.getSizeOfData() > 0) {
          byte[] signatureDataBuff = new byte[digitalSignature.getSizeOfData()];
          zip4jRaf.readFully(signatureDataBuff);
          digitalSignature.setSignatureData(new String(signatureDataBuff));
        }
      }

      return centralDirectory;
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  private void readAndSaveExtraDataRecord(RandomAccessFile zip4jRaf, FileHeader fileHeader, RawIO rawIO)
      throws ZipException {
    int extraFieldLength = fileHeader.getExtraFieldLength();
    if (extraFieldLength <= 0) {
      return;
    }

    fileHeader.setExtraDataRecords(readExtraDataRecords(zip4jRaf, extraFieldLength, rawIO));
  }

  private void readAndSaveExtraDataRecord(InputStream inputStream, LocalFileHeader localFileHeader, RawIO rawIO)
      throws ZipException {
    int extraFieldLength = localFileHeader.getExtraFieldLength();
    if (extraFieldLength <= 0) {
      return;
    }

    localFileHeader.setExtraDataRecords(readExtraDataRecords(inputStream, extraFieldLength, rawIO));

  }

  private List<ExtraDataRecord> readExtraDataRecords(RandomAccessFile zip4jRaf, int extraFieldLength, RawIO rawIO)
      throws ZipException {

    if (extraFieldLength <= 0) {
      return null;
    }

    try {
      byte[] extraFieldBuf = new byte[extraFieldLength];
      zip4jRaf.read(extraFieldBuf);

      int counter = 0;
      List<ExtraDataRecord> extraDataRecords = new ArrayList<>();
      while (counter < extraFieldLength) {
        ExtraDataRecord extraDataRecord = new ExtraDataRecord();
        extraDataRecord.setSignature(HeaderSignature.EXTRA_DATA_RECORD);
        counter = counter + 2; // first 2 bytes are for signature which we skip reading and use an enum for it
        int sizeOfRec = rawIO.readShortLittleEndian(extraFieldBuf, counter);

        if ((2 + sizeOfRec) > extraFieldLength) {
          sizeOfRec = rawIO.readShortBigEndian(extraFieldBuf, counter);
          if ((2 + sizeOfRec) > extraFieldLength) {
            //If this is the case, then extra data record is corrupt
            //skip reading any further extra data records
            break;
          }
        }

        extraDataRecord.setSizeOfData(sizeOfRec);
        counter = counter + 2;

        if (sizeOfRec > 0) {
          byte[] data = new byte[sizeOfRec];
          System.arraycopy(extraFieldBuf, counter, data, 0, sizeOfRec);
          extraDataRecord.setData(data);
        }
        counter = counter + sizeOfRec;
        extraDataRecords.add(extraDataRecord);
      }
      return extraDataRecords.size() > 0 ? extraDataRecords : null;
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  private List<ExtraDataRecord> readExtraDataRecords(InputStream inputStream, int extraFieldLength, RawIO rawIO)
      throws ZipException {

    if (extraFieldLength <= 0) {
      return null;
    }

    try {
      byte[] extraFieldBuf = new byte[extraFieldLength];
      inputStream.read(extraFieldBuf);

      int counter = 0;
      List<ExtraDataRecord> extraDataRecords = new ArrayList<>();
      while (counter < extraFieldLength) {
        ExtraDataRecord extraDataRecord = new ExtraDataRecord();
        int header = rawIO.readShortLittleEndian(extraFieldBuf, counter);
        extraDataRecord.setHeader(header);
        counter = counter + 2;
        int sizeOfRec = rawIO.readShortLittleEndian(extraFieldBuf, counter);

        if ((2 + sizeOfRec) > extraFieldLength) {
          sizeOfRec = rawIO.readShortBigEndian(extraFieldBuf, counter);
          if ((2 + sizeOfRec) > extraFieldLength) {
            //If this is the case, then extra data record is corrupt
            //skip reading any further extra data records
            break;
          }
        }

        extraDataRecord.setSizeOfData(sizeOfRec);
        counter = counter + 2;

        if (sizeOfRec > 0) {
          byte[] data = new byte[sizeOfRec];
          System.arraycopy(extraFieldBuf, counter, data, 0, sizeOfRec);
          extraDataRecord.setData(data);
        }
        counter = counter + sizeOfRec;
        extraDataRecords.add(extraDataRecord);
      }
      return extraDataRecords.size() > 0 ? extraDataRecords : null;
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  private Zip64EndOfCentralDirectoryLocator readZip64EndCentralDirLocator(RandomAccessFile zip4jRaf, RawIO rawIO)
      throws ZipException {

    try {
      Zip64EndOfCentralDirectoryLocator zip64EndOfCentralDirectoryLocator = new Zip64EndOfCentralDirectoryLocator();

      setFilePointerToReadZip64EndCentralDirLoc(zip4jRaf, rawIO);

      int signature = rawIO.readIntLittleEndian(zip4jRaf);
      if (signature == HeaderSignature.ZIP64_END_CENTRAL_DIRECTORY_LOCATOR.getValue()) {
        zipModel.setZip64Format(true);
        zip64EndOfCentralDirectoryLocator.setSignature(HeaderSignature.ZIP64_END_CENTRAL_DIRECTORY_LOCATOR);
      } else {
        zipModel.setZip64Format(false);
        return null;
      }

      zip64EndOfCentralDirectoryLocator.setNumberOfDiskStartOfZip64EndOfCentralDirectoryRecord(
          rawIO.readIntLittleEndian(zip4jRaf));
      zip64EndOfCentralDirectoryLocator.setOffsetZip64EndOfCentralDirectoryRecord(
          rawIO.readLongLittleEndian(zip4jRaf));
      zip64EndOfCentralDirectoryLocator.setTotalNumberOfDiscs(rawIO.readIntLittleEndian(zip4jRaf));

      return zip64EndOfCentralDirectoryLocator;
    } catch (Exception e) {
      throw new ZipException(e);
    }

  }

  private Zip64EndOfCentralDirectoryRecord readZip64EndCentralDirRec(RandomAccessFile zip4jRaf, RawIO rawIO)
      throws ZipException {

    if (zipModel.getZip64EndOfCentralDirectoryLocator() == null) {
      throw new ZipException("invalid zip64 end of central directory locator");
    }

    long offSetStartOfZip64CentralDir =
        zipModel.getZip64EndOfCentralDirectoryLocator().getOffsetZip64EndOfCentralDirectoryRecord();

    if (offSetStartOfZip64CentralDir < 0) {
      throw new ZipException("invalid offset for start of end of central directory record");
    }

    try {
      zip4jRaf.seek(offSetStartOfZip64CentralDir);

      Zip64EndOfCentralDirectoryRecord zip64EndOfCentralDirectoryRecord = new Zip64EndOfCentralDirectoryRecord();

      int signature = rawIO.readIntLittleEndian(zip4jRaf);
      if (signature != HeaderSignature.ZIP64_END_CENTRAL_DIRECTORY_RECORD.getValue()) {
        throw new ZipException("invalid signature for zip64 end of central directory record");
      }
      zip64EndOfCentralDirectoryRecord.setSignature(HeaderSignature.ZIP64_END_CENTRAL_DIRECTORY_RECORD);
      zip64EndOfCentralDirectoryRecord.setSizeOfZip64EndCentralDirectoryRecord(rawIO.readLongLittleEndian(zip4jRaf));
      zip64EndOfCentralDirectoryRecord.setVersionMadeBy(rawIO.readShortLittleEndian(zip4jRaf));
      zip64EndOfCentralDirectoryRecord.setVersionNeededToExtract(rawIO.readShortLittleEndian(zip4jRaf));
      zip64EndOfCentralDirectoryRecord.setNumberOfThisDisk(rawIO.readIntLittleEndian(zip4jRaf));
      zip64EndOfCentralDirectoryRecord.setNumberOfThisDiskStartOfCentralDirectory(rawIO.readIntLittleEndian(zip4jRaf));
      zip64EndOfCentralDirectoryRecord.setTotalNumberOfEntriesInCentralDirectoryOnThisDisk(
          rawIO.readLongLittleEndian(zip4jRaf));
      zip64EndOfCentralDirectoryRecord.setTotNumberOfEntriesInCentralDirectory(rawIO.readLongLittleEndian(zip4jRaf));
      zip64EndOfCentralDirectoryRecord.setSizeOfCentralDirectory(rawIO.readLongLittleEndian(zip4jRaf));
      zip64EndOfCentralDirectoryRecord.setOffsetStartCentralDirectoryWRTStartDiskNumber(
          rawIO.readLongLittleEndian(zip4jRaf));

      //zip64 extensible data sector
      //44 is the size of fixed variables in this record
      long extDataSecSize = zip64EndOfCentralDirectoryRecord.getSizeOfZip64EndCentralDirectoryRecord() - 44;
      if (extDataSecSize > 0) {
        byte[] extDataSecRecBuf = new byte[(int) extDataSecSize];
        zip4jRaf.readFully(extDataSecRecBuf);
        zip64EndOfCentralDirectoryRecord.setExtensibleDataSector(extDataSecRecBuf);
      }

      return zip64EndOfCentralDirectoryRecord;
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  private void readAndSaveZip64ExtendedInfo(FileHeader fileHeader, RawIO rawIO) throws ZipException {
    if (fileHeader == null) {
      throw new ZipException("file header is null in reading Zip64 Extended Info");
    }

    if (fileHeader.getExtraDataRecords() == null || fileHeader.getExtraDataRecords().size() <= 0) {
      return;
    }

    Zip64ExtendedInfo zip64ExtendedInfo = readZip64ExtendedInfo(fileHeader.getExtraDataRecords(),
        fileHeader.getUncompressedSize(), fileHeader.getCompressedSize(), fileHeader.getOffsetLocalHeader(),
        fileHeader.getDiskNumberStart(), rawIO);

    if (zip64ExtendedInfo != null) {
      fileHeader.setZip64ExtendedInfo(zip64ExtendedInfo);
      if (zip64ExtendedInfo.getUncompressedSize() != -1)
        fileHeader.setUncompressedSize(zip64ExtendedInfo.getUncompressedSize());

      if (zip64ExtendedInfo.getCompressedSize() != -1)
        fileHeader.setCompressedSize(zip64ExtendedInfo.getCompressedSize());

      if (zip64ExtendedInfo.getOffsetLocalHeader() != -1)
        fileHeader.setOffsetLocalHeader(zip64ExtendedInfo.getOffsetLocalHeader());

      if (zip64ExtendedInfo.getDiskNumberStart() != -1)
        fileHeader.setDiskNumberStart(zip64ExtendedInfo.getDiskNumberStart());
    }
  }

  private void readAndSaveZip64ExtendedInfo(LocalFileHeader localFileHeader, RawIO rawIO) throws ZipException {
    if (localFileHeader == null) {
      throw new ZipException("file header is null in reading Zip64 Extended Info");
    }

    if (localFileHeader.getExtraDataRecords() == null || localFileHeader.getExtraDataRecords().size() <= 0) {
      return;
    }

    Zip64ExtendedInfo zip64ExtendedInfo = readZip64ExtendedInfo(localFileHeader.getExtraDataRecords(),
        localFileHeader.getUncompressedSize(), localFileHeader.getCompressedSize(), -1, -1, rawIO);

    if (zip64ExtendedInfo != null) {
      localFileHeader.setZip64ExtendedInfo(zip64ExtendedInfo);

      if (zip64ExtendedInfo.getUncompressedSize() != -1)
        localFileHeader.setUncompressedSize(zip64ExtendedInfo.getUncompressedSize());

      if (zip64ExtendedInfo.getCompressedSize() != -1)
        localFileHeader.setCompressedSize(zip64ExtendedInfo.getCompressedSize());
    }
  }

  private Zip64ExtendedInfo readZip64ExtendedInfo(List<ExtraDataRecord> extraDataRecords, long unCompressedSize,
      long compressedSize, long offsetLocalHeader, int diskNumberStart, RawIO rawIO) {

    for (ExtraDataRecord extraDataRecord : extraDataRecords) {
      if (extraDataRecord == null) {
        continue;
      }

      if (extraDataRecord.getHeader() == 0x0001) {

        Zip64ExtendedInfo zip64ExtendedInfo = new Zip64ExtendedInfo();
        byte[] byteBuff = extraDataRecord.getData();

        if (extraDataRecord.getSizeOfData() <= 0) {
          break;
        }
        byte[] longByteBuff = new byte[8];
        byte[] intByteBuff = new byte[4];
        int counter = 0;
        boolean valueAdded = false;

        if (((unCompressedSize & 0xFFFF) == 0xFFFF) && counter < extraDataRecord.getSizeOfData()) {
          System.arraycopy(byteBuff, counter, longByteBuff, 0, 8);
          zip64ExtendedInfo.setUncompressedSize(rawIO.readLongLittleEndian(longByteBuff, 0));
          counter += 8;
          valueAdded = true;
        }

        if (((compressedSize & 0xFFFF) == 0xFFFF) && counter < extraDataRecord.getSizeOfData()) {
          System.arraycopy(byteBuff, counter, longByteBuff, 0, 8);
          zip64ExtendedInfo.setCompressedSize(rawIO.readLongLittleEndian(longByteBuff, 0));
          counter += 8;
          valueAdded = true;
        }

        if (((offsetLocalHeader & 0xFFFF) == 0xFFFF) && counter < extraDataRecord.getSizeOfData()) {
          System.arraycopy(byteBuff, counter, longByteBuff, 0, 8);
          zip64ExtendedInfo.setOffsetLocalHeader(rawIO.readLongLittleEndian(longByteBuff, 0));
          counter += 8;
          valueAdded = true;
        }

        if (((diskNumberStart & 0xFFFF) == 0xFFFF) && counter < extraDataRecord.getSizeOfData()) {
          System.arraycopy(byteBuff, counter, intByteBuff, 0, 4);
          zip64ExtendedInfo.setDiskNumberStart(rawIO.readIntLittleEndian(intByteBuff));
          valueAdded = true;
        }

        if (valueAdded) {
          return zip64ExtendedInfo;
        }

        break;
      }
    }
    return null;
  }

  private void setFilePointerToReadZip64EndCentralDirLoc(RandomAccessFile zip4jRaf, RawIO rawIO) throws ZipException {
    try {
      long pos = zip4jRaf.length() - ENDHDR;

      do {
        zip4jRaf.seek(pos--);
      } while (rawIO.readIntLittleEndian(zip4jRaf) != HeaderSignature.END_OF_CENTRAL_DIRECTORY.getValue());

      // Now the file pointer is at the end of signature of Central Dir Rec
      // Seek back with the following values
      // 4 -> end of central dir signature
      // 4 -> total number of disks
      // 8 -> relative offset of the zip64 end of central directory record
      // 4 -> number of the disk with the start of the zip64 end of central directory
      // 4 -> zip64 end of central dir locator signature
      // Refer to Appnote for more information
      zip4jRaf.seek(zip4jRaf.getFilePointer() - 4 - 4 - 8 - 4 - 4);
    } catch (IOException e) {
      throw new ZipException(e);
    }
  }

  public LocalFileHeader readLocalFileHeader(InputStream inputStream) throws IOException {
    try {
      LocalFileHeader localFileHeader = new LocalFileHeader();
      byte[] intBuff = new byte[4];

      //signature
      int sig = rawIO.readIntLittleEndian(inputStream);
      if (sig != HeaderSignature.LOCAL_FILE_HEADER.getValue()) {
        return null;
      }
      localFileHeader.setSignature(HeaderSignature.LOCAL_FILE_HEADER);
      localFileHeader.setVersionNeededToExtract(rawIO.readShortLittleEndian(inputStream));

      byte[] generalPurposeFlags = new byte[2];
      if (inputStream.read(generalPurposeFlags) != 2) {
        throw new ZipException("Could not read enough bytes for generalPurposeFlags");
      }
      BigInteger firstByteAsBigInteger = BigInteger.valueOf(generalPurposeFlags[0]);
      localFileHeader.setEncrypted(firstByteAsBigInteger.testBit(0));
      localFileHeader.setDataDescriptorExists(firstByteAsBigInteger.testBit(3));
      localFileHeader.setFileNameUTF8Encoded(BigInteger.valueOf(generalPurposeFlags[1]).testBit(11));
      localFileHeader.setGeneralPurposeFlag(generalPurposeFlags.clone());

      localFileHeader.setCompressionMethod(CompressionMethod.getCompressionMethodFromCode(
          rawIO.readShortLittleEndian(inputStream)));
      localFileHeader.setLastModifiedTime(rawIO.readIntLittleEndian(inputStream));

      inputStream.read(intBuff);
      localFileHeader.setCrc32(rawIO.readIntLittleEndian(intBuff));
      localFileHeader.setCrcRawData(intBuff.clone());

      localFileHeader.setCompressedSize(rawIO.readIntLittleEndian(inputStream));
      localFileHeader.setUncompressedSize(rawIO.readIntLittleEndian(inputStream));

      int fileNameLength = rawIO.readShortLittleEndian(inputStream);
      localFileHeader.setFileNameLength(fileNameLength);

      localFileHeader.setExtraFieldLength(rawIO.readShortLittleEndian(inputStream));

      if (fileNameLength > 0) {
        byte[] fileNameBuf = new byte[fileNameLength];
        inputStream.read(fileNameBuf);
        // Modified after user reported an issue http://www.lingala.net/zip4j/forum/index.php?topic=2.0
//				String fileName = new String(fileNameBuf, "Cp850");
//				String fileName = Zip4jUtil.getCp850EncodedString(fileNameBuf);
        String fileName = decodeFileName(fileNameBuf, localFileHeader.isFileNameUTF8Encoded());

        if (fileName == null) {
          throw new ZipException("file name is null, cannot assign file name to local file header");
        }

        if (fileName.contains(":" + System.getProperty("file.separator"))) {
          fileName = fileName.substring(fileName.indexOf(":" + System.getProperty("file.separator")) + 2);
        }

        localFileHeader.setFileName(fileName);
      } else {
        localFileHeader.setFileName(null);
      }

      readAndSaveExtraDataRecord(inputStream, localFileHeader, rawIO);
      readAndSaveZip64ExtendedInfo(localFileHeader, rawIO);
      readAndSaveAESExtraDataRecord(localFileHeader, rawIO);

      if (localFileHeader.isEncrypted()) {

        if (localFileHeader.getEncryptionMethod() == EncryptionMethod.AES) {
          //Do nothing
        } else {
          if (BigInteger.valueOf(localFileHeader.getGeneralPurposeFlag()[0]).testBit(6)) {
            localFileHeader.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD_VARIANT_STRONG);
          } else {
            localFileHeader.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);
          }
        }

      }

      return localFileHeader;
    } catch (ZipException e) {
      throw new IOException(e);
    }
  }

  public LocalFileHeader readExtendedLocalFileHeader(InputStream inputStream) throws IOException {
    try {
      LocalFileHeader localFileHeader = new LocalFileHeader();

      byte[] intBuff = new byte[4];

      int sig = rawIO.readIntLittleEndian(inputStream);
      if (sig != HeaderSignature.EXTRA_DATA_RECORD.getValue()) {
        throw new ZipException("Extended local file header flag is set, but could not find signature");
      }
      localFileHeader.setSignature(HeaderSignature.EXTRA_DATA_RECORD);

      inputStream.read(intBuff);
      localFileHeader.setCrc32(rawIO.readIntLittleEndian(intBuff));
      localFileHeader.setCrcRawData(intBuff.clone());

      localFileHeader.setCompressedSize(rawIO.readIntLittleEndian(inputStream));
      localFileHeader.setUncompressedSize(rawIO.readIntLittleEndian(inputStream));
      return localFileHeader;
    } catch (ZipException e) {
      throw new IOException(e);
    }
  }

  private void readAndSaveAESExtraDataRecord(FileHeader fileHeader, RawIO rawIO) throws ZipException {
    if (fileHeader.getExtraDataRecords() == null || fileHeader.getExtraDataRecords().size() <= 0) {
      return;
    }

    AESExtraDataRecord aesExtraDataRecord = readAESExtraDataRecord(fileHeader.getExtraDataRecords(), rawIO);
    if (aesExtraDataRecord != null) {
      fileHeader.setAesExtraDataRecord(aesExtraDataRecord);
      fileHeader.setEncryptionMethod(EncryptionMethod.AES);
    }
  }

  private void readAndSaveAESExtraDataRecord(LocalFileHeader localFileHeader, RawIO rawIO) throws ZipException {
    if (localFileHeader.getExtraDataRecords() == null || localFileHeader.getExtraDataRecords().size() <= 0) {
      return;
    }

    AESExtraDataRecord aesExtraDataRecord = readAESExtraDataRecord(localFileHeader.getExtraDataRecords(), rawIO);
    if (aesExtraDataRecord != null) {
      localFileHeader.setAesExtraDataRecord(aesExtraDataRecord);
      localFileHeader.setEncryptionMethod(EncryptionMethod.AES);
    }
  }

  private AESExtraDataRecord readAESExtraDataRecord(List<ExtraDataRecord> extraDataRecords, RawIO rawIO)
      throws ZipException {

    if (extraDataRecords == null) {
      return null;
    }

    for (ExtraDataRecord extraDataRecord : extraDataRecords) {
      if (extraDataRecord == null) {
        continue;
      }

      if (extraDataRecord.getHeader() == HeaderSignature.AES_EXTRA_DATA_RECORD.getValue()) {

        if (extraDataRecord.getData() == null) {
          throw new ZipException("corrupt AES extra data records");
        }

        AESExtraDataRecord aesExtraDataRecord = new AESExtraDataRecord();

        aesExtraDataRecord.setSignature(HeaderSignature.AES_EXTRA_DATA_RECORD);
        aesExtraDataRecord.setDataSize(extraDataRecord.getSizeOfData());

        byte[] aesData = extraDataRecord.getData();
        aesExtraDataRecord.setVersionNumber(rawIO.readShortLittleEndian(aesData, 0));
        byte[] vendorIDBytes = new byte[2];
        System.arraycopy(aesData, 2, vendorIDBytes, 0, 2);
        aesExtraDataRecord.setVendorID(new String(vendorIDBytes));
        aesExtraDataRecord.setAesKeyStrength(AesKeyStrength.getAesKeyStrengthFromRawCode(aesData[4] & 0xFF));
        aesExtraDataRecord.setCompressionMethod(
            CompressionMethod.getCompressionMethodFromCode(rawIO.readShortLittleEndian(aesData, 5)));

        return aesExtraDataRecord;
      }
    }

    return null;
  }
}
