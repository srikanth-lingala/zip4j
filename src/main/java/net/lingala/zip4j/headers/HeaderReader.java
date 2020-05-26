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
import net.lingala.zip4j.io.inputstream.NumberedSplitRandomAccessFile;
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.CentralDirectory;
import net.lingala.zip4j.model.DataDescriptor;
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
import net.lingala.zip4j.model.enums.AesVersion;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.util.RawIO;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.lingala.zip4j.headers.HeaderUtil.decodeStringWithCharset;
import static net.lingala.zip4j.util.BitUtils.isBitSet;
import static net.lingala.zip4j.util.InternalZipConstants.BUFF_SIZE;
import static net.lingala.zip4j.util.InternalZipConstants.ENDHDR;
import static net.lingala.zip4j.util.InternalZipConstants.ZIP_64_NUMBER_OF_ENTRIES_LIMIT;
import static net.lingala.zip4j.util.InternalZipConstants.ZIP_64_SIZE_LIMIT;
import static net.lingala.zip4j.util.Zip4jUtil.readFully;

/**
 * Helper class to read header information for the zip file
 */
public class HeaderReader {

  private ZipModel zipModel;
  private RawIO rawIO = new RawIO();
  private byte[] intBuff = new byte[4];

  public ZipModel readAllHeaders(RandomAccessFile zip4jRaf, Charset charset) throws IOException {

    if (zip4jRaf.length() < ENDHDR) {
      throw new ZipException("Zip file size less than minimum expected zip file size. " +
          "Probably not a zip file or a corrupted zip file");
    }

    zipModel = new ZipModel();

    try {
      zipModel.setEndOfCentralDirectoryRecord(readEndOfCentralDirectoryRecord(zip4jRaf, rawIO, charset));
    } catch (ZipException e){
      throw e;
    } catch (IOException e) {
      throw new ZipException("Zip headers not found. Probably not a zip file or a corrupted zip file", e);
    }

    if (zipModel.getEndOfCentralDirectoryRecord().getTotalNumberOfEntriesInCentralDirectory() == 0) {
      return zipModel;
    }

    // If file is Zip64 format, Zip64 headers have to be read before reading central directory
    zipModel.setZip64EndOfCentralDirectoryLocator(readZip64EndOfCentralDirectoryLocator(zip4jRaf, rawIO,
        zipModel.getEndOfCentralDirectoryRecord().getOffsetOfEndOfCentralDirectory()));

    if (zipModel.isZip64Format()) {
      zipModel.setZip64EndOfCentralDirectoryRecord(readZip64EndCentralDirRec(zip4jRaf, rawIO));
      if (zipModel.getZip64EndOfCentralDirectoryRecord() != null
          && zipModel.getZip64EndOfCentralDirectoryRecord().getNumberOfThisDisk() > 0) {
        zipModel.setSplitArchive(true);
      } else {
        zipModel.setSplitArchive(false);
      }
    }

    zipModel.setCentralDirectory(readCentralDirectory(zip4jRaf, rawIO, charset));

    return zipModel;
  }

  private EndOfCentralDirectoryRecord readEndOfCentralDirectoryRecord(RandomAccessFile zip4jRaf, RawIO rawIO, Charset charset)
      throws IOException {

    long offsetEndOfCentralDirectory = zip4jRaf.length() - ENDHDR;
    seekInCurrentPart(zip4jRaf, offsetEndOfCentralDirectory);
    int headerSignature = rawIO.readIntLittleEndian(zip4jRaf);

    if (headerSignature != HeaderSignature.END_OF_CENTRAL_DIRECTORY.getValue()) {
      offsetEndOfCentralDirectory = determineOffsetOfEndOfCentralDirectory(zip4jRaf);
      zip4jRaf.seek(offsetEndOfCentralDirectory + 4); // 4 to ignore reading signature again
    }

    EndOfCentralDirectoryRecord endOfCentralDirectoryRecord = new EndOfCentralDirectoryRecord();
    endOfCentralDirectoryRecord.setSignature(HeaderSignature.END_OF_CENTRAL_DIRECTORY);
    endOfCentralDirectoryRecord.setNumberOfThisDisk(rawIO.readShortLittleEndian(zip4jRaf));
    endOfCentralDirectoryRecord.setNumberOfThisDiskStartOfCentralDir(rawIO.readShortLittleEndian(zip4jRaf));
    endOfCentralDirectoryRecord.setTotalNumberOfEntriesInCentralDirectoryOnThisDisk(
        rawIO.readShortLittleEndian(zip4jRaf));
    endOfCentralDirectoryRecord.setTotalNumberOfEntriesInCentralDirectory(rawIO.readShortLittleEndian(zip4jRaf));
    endOfCentralDirectoryRecord.setSizeOfCentralDirectory(rawIO.readIntLittleEndian(zip4jRaf));
    endOfCentralDirectoryRecord.setOffsetOfEndOfCentralDirectory(offsetEndOfCentralDirectory);

    zip4jRaf.readFully(intBuff);
    endOfCentralDirectoryRecord.setOffsetOfStartOfCentralDirectory(rawIO.readLongLittleEndian(intBuff, 0));

    int commentLength = rawIO.readShortLittleEndian(zip4jRaf);
    endOfCentralDirectoryRecord.setComment(readZipComment(zip4jRaf, commentLength, charset));

    zipModel.setSplitArchive(endOfCentralDirectoryRecord.getNumberOfThisDisk() > 0);
    return endOfCentralDirectoryRecord;
  }

  private CentralDirectory readCentralDirectory(RandomAccessFile zip4jRaf, RawIO rawIO, Charset charset) throws IOException {
    CentralDirectory centralDirectory = new CentralDirectory();
    List<FileHeader> fileHeaders = new ArrayList<>();

    long offSetStartCentralDir = HeaderUtil.getOffsetStartOfCentralDirectory(zipModel);
    long centralDirEntryCount = getNumberOfEntriesInCentralDirectory(zipModel);

    if (zipModel.isZip64Format()) {
      offSetStartCentralDir = zipModel.getZip64EndOfCentralDirectoryRecord()
          .getOffsetStartCentralDirectoryWRTStartDiskNumber();
      centralDirEntryCount = (int) zipModel.getZip64EndOfCentralDirectoryRecord()
          .getTotalNumberOfEntriesInCentralDirectory();
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
      fileHeader.setEncrypted(isBitSet(generalPurposeFlags[0], 0));
      fileHeader.setDataDescriptorExists(isBitSet(generalPurposeFlags[0], 3));
      fileHeader.setFileNameUTF8Encoded(isBitSet(generalPurposeFlags[1], 3));
      fileHeader.setGeneralPurposeFlag(generalPurposeFlags.clone());

      fileHeader.setCompressionMethod(CompressionMethod.getCompressionMethodFromCode(rawIO.readShortLittleEndian(
          zip4jRaf)));
      fileHeader.setLastModifiedTime(rawIO.readIntLittleEndian(zip4jRaf));

      zip4jRaf.readFully(intBuff);
      fileHeader.setCrc(rawIO.readLongLittleEndian(intBuff, 0));
      fileHeader.setCrcRawData(intBuff);

      fileHeader.setCompressedSize(rawIO.readLongLittleEndian(zip4jRaf, 4));
      fileHeader.setUncompressedSize(rawIO.readLongLittleEndian(zip4jRaf, 4));

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

      zip4jRaf.readFully(intBuff);
      fileHeader.setOffsetLocalHeader(rawIO.readLongLittleEndian(intBuff, 0));

      if (fileNameLength > 0) {
        byte[] fileNameBuff = new byte[fileNameLength];
        zip4jRaf.readFully(fileNameBuff);
        String fileName = decodeStringWithCharset(fileNameBuff, fileHeader.isFileNameUTF8Encoded(), charset);

        if (fileName.contains(":\\")) {
          fileName = fileName.substring(fileName.indexOf(":\\") + 2);
        }

        fileHeader.setFileName(fileName);
        fileHeader.setDirectory(fileName.endsWith("/") || fileName.endsWith("\\"));
      } else {
        fileHeader.setFileName(null);
      }

      readExtraDataRecords(zip4jRaf, fileHeader);
      readZip64ExtendedInfo(fileHeader, rawIO);
      readAesExtraDataRecord(fileHeader, rawIO);

      if (fileCommentLength > 0) {
        byte[] fileCommentBuff = new byte[fileCommentLength];
        zip4jRaf.readFully(fileCommentBuff);
        fileHeader.setFileComment(decodeStringWithCharset(fileCommentBuff, fileHeader.isFileNameUTF8Encoded(), charset));
      }

      if (fileHeader.isEncrypted()) {
        if (fileHeader.getAesExtraDataRecord() != null) {
          fileHeader.setEncryptionMethod(EncryptionMethod.AES);
        } else {
          fileHeader.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);
        }
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
  }

  private void readExtraDataRecords(RandomAccessFile zip4jRaf, FileHeader fileHeader)
      throws IOException {
    int extraFieldLength = fileHeader.getExtraFieldLength();
    if (extraFieldLength <= 0) {
      return;
    }

    fileHeader.setExtraDataRecords(readExtraDataRecords(zip4jRaf, extraFieldLength));
  }

  private void readExtraDataRecords(InputStream inputStream, LocalFileHeader localFileHeader)
      throws IOException {
    int extraFieldLength = localFileHeader.getExtraFieldLength();
    if (extraFieldLength <= 0) {
      return;
    }

    localFileHeader.setExtraDataRecords(readExtraDataRecords(inputStream, extraFieldLength));

  }

  private List<ExtraDataRecord> readExtraDataRecords(RandomAccessFile zip4jRaf, int extraFieldLength)
      throws IOException {

    if (extraFieldLength < 4) {
      if (extraFieldLength > 0) {
        zip4jRaf.skipBytes(extraFieldLength);
      }

      return null;
    }

    byte[] extraFieldBuf = new byte[extraFieldLength];
    zip4jRaf.read(extraFieldBuf);

    try {
      return parseExtraDataRecords(extraFieldBuf, extraFieldLength);
    } catch (Exception e) {
      // Ignore any errors when parsing extra data records
      return Collections.emptyList();
    }
  }

  private List<ExtraDataRecord> readExtraDataRecords(InputStream inputStream, int extraFieldLength)
      throws IOException {

    if (extraFieldLength < 4) {
      if (extraFieldLength > 0) {
        inputStream.skip(extraFieldLength);
      }

      return null;
    }

    byte[] extraFieldBuf = new byte[extraFieldLength];
    readFully(inputStream, extraFieldBuf);

    try {
      return parseExtraDataRecords(extraFieldBuf, extraFieldLength);
    } catch (Exception e) {
      // Ignore any errors when parsing extra data records
      return Collections.emptyList();
    }
  }

  private List<ExtraDataRecord> parseExtraDataRecords(byte[] extraFieldBuf, int extraFieldLength) {
    int counter = 0;
    List<ExtraDataRecord> extraDataRecords = new ArrayList<>();
    while (counter < extraFieldLength) {
      ExtraDataRecord extraDataRecord = new ExtraDataRecord();
      int header = rawIO.readShortLittleEndian(extraFieldBuf, counter);
      extraDataRecord.setHeader(header);
      counter += 2;

      int sizeOfRec = rawIO.readShortLittleEndian(extraFieldBuf, counter);
      extraDataRecord.setSizeOfData(sizeOfRec);
      counter += 2;

      if (sizeOfRec > 0) {
        byte[] data = new byte[sizeOfRec];
        System.arraycopy(extraFieldBuf, counter, data, 0, sizeOfRec);
        extraDataRecord.setData(data);
      }
      counter += sizeOfRec;
      extraDataRecords.add(extraDataRecord);
    }
    return extraDataRecords.size() > 0 ? extraDataRecords : null;
  }

  private Zip64EndOfCentralDirectoryLocator readZip64EndOfCentralDirectoryLocator(RandomAccessFile zip4jRaf,
                            RawIO rawIO, long offsetEndOfCentralDirectoryRecord) throws IOException {

    Zip64EndOfCentralDirectoryLocator zip64EndOfCentralDirectoryLocator = new Zip64EndOfCentralDirectoryLocator();

    setFilePointerToReadZip64EndCentralDirLoc(zip4jRaf, offsetEndOfCentralDirectoryRecord);

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
  }

  private Zip64EndOfCentralDirectoryRecord readZip64EndCentralDirRec(RandomAccessFile zip4jRaf, RawIO rawIO)
      throws IOException {

    if (zipModel.getZip64EndOfCentralDirectoryLocator() == null) {
      throw new ZipException("invalid zip64 end of central directory locator");
    }

    long offSetStartOfZip64CentralDir = zipModel.getZip64EndOfCentralDirectoryLocator()
        .getOffsetZip64EndOfCentralDirectoryRecord();

    if (offSetStartOfZip64CentralDir < 0) {
      throw new ZipException("invalid offset for start of end of central directory record");
    }

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
    zip64EndOfCentralDirectoryRecord.setTotalNumberOfEntriesInCentralDirectory(rawIO.readLongLittleEndian(zip4jRaf));
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
  }

  private void readZip64ExtendedInfo(FileHeader fileHeader, RawIO rawIO) throws ZipException {
    if (fileHeader.getExtraDataRecords() == null || fileHeader.getExtraDataRecords().size() <= 0) {
      return;
    }

    Zip64ExtendedInfo zip64ExtendedInfo = readZip64ExtendedInfo(fileHeader.getExtraDataRecords(), rawIO,
        fileHeader.getUncompressedSize(), fileHeader.getCompressedSize(), fileHeader.getOffsetLocalHeader(),
        fileHeader.getDiskNumberStart());

    if (zip64ExtendedInfo == null) {
      return;
    }

    fileHeader.setZip64ExtendedInfo(zip64ExtendedInfo);

    if (zip64ExtendedInfo.getUncompressedSize() != -1) {
      fileHeader.setUncompressedSize(zip64ExtendedInfo.getUncompressedSize());
    }

    if (zip64ExtendedInfo.getCompressedSize() != -1) {
      fileHeader.setCompressedSize(zip64ExtendedInfo.getCompressedSize());
    }

    if (zip64ExtendedInfo.getOffsetLocalHeader() != -1) {
      fileHeader.setOffsetLocalHeader(zip64ExtendedInfo.getOffsetLocalHeader());
    }

    if (zip64ExtendedInfo.getDiskNumberStart() != -1) {
      fileHeader.setDiskNumberStart(zip64ExtendedInfo.getDiskNumberStart());
    }
  }

  private void readZip64ExtendedInfo(LocalFileHeader localFileHeader, RawIO rawIO) throws ZipException {
    if (localFileHeader == null) {
      throw new ZipException("file header is null in reading Zip64 Extended Info");
    }

    if (localFileHeader.getExtraDataRecords() == null || localFileHeader.getExtraDataRecords().size() <= 0) {
      return;
    }

    Zip64ExtendedInfo zip64ExtendedInfo = readZip64ExtendedInfo(localFileHeader.getExtraDataRecords(), rawIO,
        localFileHeader.getUncompressedSize(), localFileHeader.getCompressedSize(), 0, 0);

    if (zip64ExtendedInfo == null) {
      return;
    }

    localFileHeader.setZip64ExtendedInfo(zip64ExtendedInfo);

    if (zip64ExtendedInfo.getUncompressedSize() != -1) {
      localFileHeader.setUncompressedSize(zip64ExtendedInfo.getUncompressedSize());
    }

    if (zip64ExtendedInfo.getCompressedSize() != -1) {
      localFileHeader.setCompressedSize(zip64ExtendedInfo.getCompressedSize());
    }
  }

  private Zip64ExtendedInfo readZip64ExtendedInfo(List<ExtraDataRecord> extraDataRecords, RawIO rawIO,
                                                  long uncompressedSize, long compressedSize, long offsetLocalHeader,
                                                  int diskNumberStart) {

    for (ExtraDataRecord extraDataRecord : extraDataRecords) {
      if (extraDataRecord == null) {
        continue;
      }

      if (HeaderSignature.ZIP64_EXTRA_FIELD_SIGNATURE.getValue() == extraDataRecord.getHeader()) {

        Zip64ExtendedInfo zip64ExtendedInfo = new Zip64ExtendedInfo();
        byte[] extraData = extraDataRecord.getData();

        if (extraDataRecord.getSizeOfData() <= 0) {
          return null;
        }

        int counter = 0;
        if (counter < extraDataRecord.getSizeOfData() && uncompressedSize == ZIP_64_SIZE_LIMIT) {
          zip64ExtendedInfo.setUncompressedSize(rawIO.readLongLittleEndian(extraData, counter));
          counter += 8;
        }

        if (counter < extraDataRecord.getSizeOfData() && compressedSize == ZIP_64_SIZE_LIMIT) {
          zip64ExtendedInfo.setCompressedSize(rawIO.readLongLittleEndian(extraData, counter));
          counter += 8;
        }

        if (counter < extraDataRecord.getSizeOfData() && offsetLocalHeader == ZIP_64_SIZE_LIMIT) {
          zip64ExtendedInfo.setOffsetLocalHeader(rawIO.readLongLittleEndian(extraData, counter));
          counter += 8;
        }

        if (counter < extraDataRecord.getSizeOfData() && diskNumberStart == ZIP_64_NUMBER_OF_ENTRIES_LIMIT) {
          zip64ExtendedInfo.setDiskNumberStart(rawIO.readIntLittleEndian(extraData, counter));
        }

        return zip64ExtendedInfo;
      }
    }
    return null;
  }

  private void setFilePointerToReadZip64EndCentralDirLoc(RandomAccessFile zip4jRaf,
                                                         long offsetEndOfCentralDirectoryRecord) throws IOException {
    // Now the file pointer is at the end of signature of Central Dir Rec
    // Seek back with the following values
    // 4 -> total number of disks
    // 8 -> relative offset of the zip64 end of central directory record
    // 4 -> number of the disk with the start of the zip64 end of central directory
    // 4 -> zip64 end of central dir locator signature
    // Refer to Appnote for more information
    seekInCurrentPart(zip4jRaf, offsetEndOfCentralDirectoryRecord - 4 - 8 - 4 - 4);
  }

  public LocalFileHeader readLocalFileHeader(InputStream inputStream, Charset charset) throws IOException {
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
    if (readFully(inputStream, generalPurposeFlags) != 2) {
      throw new ZipException("Could not read enough bytes for generalPurposeFlags");
    }
    localFileHeader.setEncrypted(isBitSet(generalPurposeFlags[0], 0));
    localFileHeader.setDataDescriptorExists(isBitSet(generalPurposeFlags[0], 3));
    localFileHeader.setFileNameUTF8Encoded(isBitSet(generalPurposeFlags[1], 3));
    localFileHeader.setGeneralPurposeFlag(generalPurposeFlags.clone());

    localFileHeader.setCompressionMethod(CompressionMethod.getCompressionMethodFromCode(
        rawIO.readShortLittleEndian(inputStream)));
    localFileHeader.setLastModifiedTime(rawIO.readIntLittleEndian(inputStream));

    readFully(inputStream, intBuff);
    localFileHeader.setCrc(rawIO.readLongLittleEndian(intBuff, 0));
    localFileHeader.setCrcRawData(intBuff.clone());

    localFileHeader.setCompressedSize(rawIO.readLongLittleEndian(inputStream, 4));
    localFileHeader.setUncompressedSize(rawIO.readLongLittleEndian(inputStream, 4));

    int fileNameLength = rawIO.readShortLittleEndian(inputStream);
    localFileHeader.setFileNameLength(fileNameLength);

    localFileHeader.setExtraFieldLength(rawIO.readShortLittleEndian(inputStream));

    if (fileNameLength > 0) {
      byte[] fileNameBuf = new byte[fileNameLength];
      readFully(inputStream, fileNameBuf);
      // Modified after user reported an issue http://www.lingala.net/zip4j/forum/index.php?topic=2.0
//				String fileName = new String(fileNameBuf, "Cp850");
//				String fileName = Zip4jUtil.getCp850EncodedString(fileNameBuf);
      String fileName = decodeStringWithCharset(fileNameBuf, localFileHeader.isFileNameUTF8Encoded(), charset);

      if (fileName == null) {
        throw new ZipException("file name is null, cannot assign file name to local file header");
      }

      if (fileName.contains(":" + System.getProperty("file.separator"))) {
        fileName = fileName.substring(fileName.indexOf(":" + System.getProperty("file.separator")) + 2);
      }

      localFileHeader.setFileName(fileName);
      localFileHeader.setDirectory(fileName.endsWith("/") || fileName.endsWith("\\"));
    } else {
      localFileHeader.setFileName(null);
    }

    readExtraDataRecords(inputStream, localFileHeader);
    readZip64ExtendedInfo(localFileHeader, rawIO);
    readAesExtraDataRecord(localFileHeader, rawIO);

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
  }

  public DataDescriptor readDataDescriptor(InputStream inputStream, boolean isZip64Format) throws IOException {

    DataDescriptor dataDescriptor = new DataDescriptor();

    byte[] intBuff = new byte[4];
    readFully(inputStream, intBuff);
    long sigOrCrc = rawIO.readLongLittleEndian(intBuff, 0);

    //According to zip specification, presence of extra data record header signature is optional.
    //If this signature is present, read it and read the next 4 bytes for crc
    //If signature not present, assign the read 4 bytes for crc
    if (sigOrCrc == HeaderSignature.EXTRA_DATA_RECORD.getValue()) {
      dataDescriptor.setSignature(HeaderSignature.EXTRA_DATA_RECORD);
      readFully(inputStream, intBuff);
      dataDescriptor.setCrc(rawIO.readLongLittleEndian(intBuff, 0));
    } else {
      dataDescriptor.setCrc(sigOrCrc);
    }

    if (isZip64Format) {
      dataDescriptor.setCompressedSize(rawIO.readLongLittleEndian(inputStream));
      dataDescriptor.setUncompressedSize(rawIO.readLongLittleEndian(inputStream));
    } else {
      dataDescriptor.setCompressedSize(rawIO.readIntLittleEndian(inputStream));
      dataDescriptor.setUncompressedSize(rawIO.readIntLittleEndian(inputStream));
    }

    return dataDescriptor;
  }

  private void readAesExtraDataRecord(FileHeader fileHeader, RawIO rawIO) throws ZipException {
    if (fileHeader.getExtraDataRecords() == null || fileHeader.getExtraDataRecords().size() <= 0) {
      return;
    }

    AESExtraDataRecord aesExtraDataRecord = readAesExtraDataRecord(fileHeader.getExtraDataRecords(), rawIO);
    if (aesExtraDataRecord != null) {
      fileHeader.setAesExtraDataRecord(aesExtraDataRecord);
      fileHeader.setEncryptionMethod(EncryptionMethod.AES);
    }
  }

  private void readAesExtraDataRecord(LocalFileHeader localFileHeader, RawIO rawIO) throws ZipException {
    if (localFileHeader.getExtraDataRecords() == null || localFileHeader.getExtraDataRecords().size() <= 0) {
      return;
    }

    AESExtraDataRecord aesExtraDataRecord = readAesExtraDataRecord(localFileHeader.getExtraDataRecords(), rawIO);
    if (aesExtraDataRecord != null) {
      localFileHeader.setAesExtraDataRecord(aesExtraDataRecord);
      localFileHeader.setEncryptionMethod(EncryptionMethod.AES);
    }
  }

  private AESExtraDataRecord readAesExtraDataRecord(List<ExtraDataRecord> extraDataRecords, RawIO rawIO)
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
        aesExtraDataRecord.setAesVersion(AesVersion.getFromVersionNumber(rawIO.readShortLittleEndian(aesData, 0)));
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

  private long getNumberOfEntriesInCentralDirectory(ZipModel zipModel) {
    if (zipModel.isZip64Format()) {
      return zipModel.getZip64EndOfCentralDirectoryRecord().getTotalNumberOfEntriesInCentralDirectory();
    }

    return zipModel.getEndOfCentralDirectoryRecord().getTotalNumberOfEntriesInCentralDirectory();
  }

  private long determineOffsetOfEndOfCentralDirectory(RandomAccessFile randomAccessFile) throws IOException {
    byte[] buff = new byte[BUFF_SIZE];
    long currentFilePointer = randomAccessFile.getFilePointer();

    do {
      int toRead = currentFilePointer > BUFF_SIZE ? BUFF_SIZE : (int) currentFilePointer;
      // read 4 bytes again to make sure that the header is not spilled over
      long seekPosition = currentFilePointer - toRead + 4;
      if (seekPosition == 4) {
        seekPosition = 0;
      }

      seekInCurrentPart(randomAccessFile, seekPosition);
      randomAccessFile.read(buff, 0, toRead);
      currentFilePointer = seekPosition;

      for (int i = 0; i < toRead - 3; i++) {
        if (rawIO.readIntLittleEndian(buff, i) == HeaderSignature.END_OF_CENTRAL_DIRECTORY.getValue()) {
          return currentFilePointer + i;
        }
      }
    } while (currentFilePointer > 0);

    throw new ZipException("Zip headers not found. Probably not a zip file");
  }

  private void seekInCurrentPart(RandomAccessFile randomAccessFile, long pos) throws IOException {
    if (randomAccessFile instanceof NumberedSplitRandomAccessFile) {
      ((NumberedSplitRandomAccessFile) randomAccessFile).seekInCurrentPart(pos);
    } else {
      randomAccessFile.seek(pos);
    }
  }

  private String readZipComment(RandomAccessFile raf, int commentLength, Charset charset) {
    if (commentLength <= 0) {
      return null;
    }

    try {
      byte[] commentBuf = new byte[commentLength];
      raf.readFully(commentBuf);
      return new String(commentBuf, charset);
    } catch (IOException e) {
      // Ignore any exception and set comment to null if comment cannot be read
      return null;
    }
  }
}
