package net.lingala.zip4j.headers;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.util.Zip4jUtil;

import java.nio.charset.StandardCharsets;

import static net.lingala.zip4j.util.BitUtils.setBitOfByte;
import static net.lingala.zip4j.util.BitUtils.unsetBitOfByte;

public class FileHeaderFactory {

  public FileHeader generateFileHeader(ZipParameters zipParameters, boolean isSplitZip, int currentDiskNumberStart)
      throws ZipException {

    FileHeader fileHeader = new FileHeader();
    fileHeader.setSignature(HeaderSignature.CENTRAL_DIRECTORY);
    fileHeader.setVersionMadeBy(20);
    fileHeader.setVersionNeededToExtract(20);

    if (zipParameters.isEncryptFiles() && zipParameters.getEncryptionMethod() == EncryptionMethod.AES) {
      fileHeader.setCompressionMethod(CompressionMethod.AES_INTERNAL_ONLY);
      fileHeader.setAesExtraDataRecord(generateAESExtraDataRecord(zipParameters));
    } else {
      fileHeader.setCompressionMethod(zipParameters.getCompressionMethod());
    }

    if (zipParameters.isEncryptFiles()) {
      fileHeader.setEncrypted(true);
      fileHeader.setEncryptionMethod(zipParameters.getEncryptionMethod());
    }

    String fileName = validateAndGetFileName(zipParameters.getFileNameInZip());
    fileHeader.setFileName(fileName);
    fileHeader.setFileNameLength(determineFileNameLength(fileName));
    fileHeader.setDiskNumberStart(isSplitZip ? currentDiskNumberStart : 0);

    if (zipParameters.getLastModifiedFileTime() > 0) {
      fileHeader.setLastModifiedTime((int) Zip4jUtil.javaToDosTime(zipParameters.getLastModifiedFileTime()));
    } else {
      fileHeader.setLastModifiedTime((int) Zip4jUtil.javaToDosTime(System.currentTimeMillis()));
    }

    //TODO add file attributes for internally added files
    fileHeader.setExternalFileAttributes(new byte[] {0, 0, 0, 0});
    fileHeader.setDirectory(Zip4jUtil.isZipEntryDirectory(fileName));
    fileHeader.setUncompressedSize(zipParameters.getUncompressedSize());

    if (zipParameters.isEncryptFiles() && zipParameters.getEncryptionMethod() == EncryptionMethod.ZIP_STANDARD) {
      fileHeader.setCrc32(zipParameters.getSourceFileCRC());
    }

    fileHeader.setGeneralPurposeFlag(determineGeneralPurposeBitFlag(fileHeader.isEncrypted(), zipParameters));
    return fileHeader;
  }

  public LocalFileHeader generateLocalFileHeader(FileHeader fileHeader) {
    LocalFileHeader localFileHeader = new LocalFileHeader();
    localFileHeader.setSignature(HeaderSignature.LOCAL_FILE_HEADER);
    localFileHeader.setVersionNeededToExtract(fileHeader.getVersionNeededToExtract());
    localFileHeader.setCompressionMethod(fileHeader.getCompressionMethod());
    localFileHeader.setLastModifiedTime(fileHeader.getLastModifiedTime());
    localFileHeader.setUncompressedSize(fileHeader.getUncompressedSize());
    localFileHeader.setFileNameLength(fileHeader.getFileNameLength());
    localFileHeader.setFileName(fileHeader.getFileName());
    localFileHeader.setEncrypted(fileHeader.isEncrypted());
    localFileHeader.setEncryptionMethod(fileHeader.getEncryptionMethod());
    localFileHeader.setAesExtraDataRecord(fileHeader.getAesExtraDataRecord());
    localFileHeader.setCrc32(fileHeader.getCrc32());
    localFileHeader.setCompressedSize(fileHeader.getCompressedSize());
    localFileHeader.setGeneralPurposeFlag(fileHeader.getGeneralPurposeFlag().clone());
    return localFileHeader;
  }

  private byte[] determineGeneralPurposeBitFlag(boolean isEncrypted, ZipParameters zipParameters) {
    byte[] generalPurposeBitFlag = new byte[2];
    generalPurposeBitFlag[0] = generateFirstGeneralPurposeByte(isEncrypted,
        zipParameters.getCompressionMethod(), zipParameters.getCompressionLevel());
    generalPurposeBitFlag[1] |= 1 << 3; // set 3rd bit which corresponds to utf-8 file name charset
    return generalPurposeBitFlag;
  }

  private byte generateFirstGeneralPurposeByte(boolean isEncrypted, CompressionMethod compressionMethod,
                                               CompressionLevel compressionLevel) {

    byte firstByte = 0;

    if (isEncrypted) {
      firstByte = setBitOfByte(firstByte, 0);
    }

    if (compressionMethod == CompressionMethod.DEFLATE) {
      if (compressionLevel == CompressionLevel.NORMAL) {
        firstByte = unsetBitOfByte(firstByte, 1);
        firstByte = unsetBitOfByte(firstByte, 2);
      } else if (compressionLevel == CompressionLevel.MAXIMUM) {
        firstByte = setBitOfByte(firstByte, 1);
        firstByte = unsetBitOfByte(firstByte, 2);
      } else if (compressionLevel == CompressionLevel.FAST) {
        firstByte = unsetBitOfByte(firstByte, 1);
        firstByte = setBitOfByte(firstByte, 2);
      } else if (compressionLevel == CompressionLevel.FASTEST) {
        firstByte = setBitOfByte(firstByte, 1);
        firstByte = setBitOfByte(firstByte, 2);
      }
    }

    // file name and comment encoded with utf-8 charset
    firstByte = setBitOfByte(firstByte, 3);

    return firstByte;
  }

  private String validateAndGetFileName(String fileNameInZip) throws ZipException {
    if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileNameInZip)) {
      throw new ZipException("fileNameInZip is null or empty");
    }
    return fileNameInZip;
  }

  private AESExtraDataRecord generateAESExtraDataRecord(ZipParameters parameters) throws ZipException {
    AESExtraDataRecord aesDataRecord = new AESExtraDataRecord();
    aesDataRecord.setSignature(HeaderSignature.AES_EXTRA_DATA_RECORD);
    aesDataRecord.setDataSize(7);
    aesDataRecord.setVendorID("AE");
    // Always set the version number to 2 as we do not store CRC for any AES encrypted files
    // only MAC is stored and as per the specification, if version number is 2, then MAC is read
    // and CRC is ignored
    aesDataRecord.setVersionNumber(2);

    if (parameters.getAesKeyStrength() == AesKeyStrength.KEY_STRENGTH_128) {
      aesDataRecord.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_128);
    } else if (parameters.getAesKeyStrength() == AesKeyStrength.KEY_STRENGTH_192) {
      aesDataRecord.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_192);
    } else if (parameters.getAesKeyStrength() == AesKeyStrength.KEY_STRENGTH_256) {
      aesDataRecord.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
    } else {
      throw new ZipException("invalid AES key strength, cannot generate AES Extra data record");
    }

    aesDataRecord.setCompressionMethod(parameters.getCompressionMethod());
    return aesDataRecord;
  }

  private int determineFileNameLength(String fileName) {
    return fileName.getBytes(StandardCharsets.UTF_8).length;
  }
}
