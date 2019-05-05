package net.lingala.zip4j.headers;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.InternalZipConstants;
import net.lingala.zip4j.util.Raw;
import net.lingala.zip4j.util.Zip4jUtil;
import net.lingala.zip4j.zip.AesKeyStrength;
import net.lingala.zip4j.zip.CompressionMethod;
import net.lingala.zip4j.zip.EncryptionMethod;

import java.io.File;

public class FileHeaderFactory {

  public FileHeader generateFileHeader(ZipParameters zipParameters, boolean isSplitZip, int currentDiskNumberStart,
                                       String fileNameCharset, File sourceFile) throws ZipException {

    FileHeader fileHeader = new FileHeader();
    fileHeader.setSignature((int) InternalZipConstants.CENSIG);
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

    String fileName = getFileName(zipParameters, fileHeader, sourceFile);
    fileHeader.setFileName(fileName);
    fileHeader.setFileNameLength(determineFileNameLength(fileName, fileNameCharset));
    fileHeader.setDiskNumberStart(isSplitZip ? currentDiskNumberStart: 0);

    byte[] externalFileAttrs = {(byte) getFileAttributes(zipParameters.isSourceExternalStream(), sourceFile), 0, 0, 0};
    fileHeader.setExternalFileAttr(externalFileAttrs);

    fileHeader.setDirectory(isDirectory(fileName, zipParameters.isSourceExternalStream(), sourceFile));

    if (fileHeader.isDirectory()) {
      fileHeader.setCompressedSize(0);
      fileHeader.setUncompressedSize(0);
    } else {
      if (!zipParameters.isSourceExternalStream()) {
        long fileSize = Zip4jUtil.getFileLengh(sourceFile);
        fileHeader.setCompressedSize(calculateCompressedSize(zipParameters, fileSize));
        fileHeader.setUncompressedSize(fileSize);
      }
    }

    if (zipParameters.isEncryptFiles() && zipParameters.getEncryptionMethod() == EncryptionMethod.ZIP_STANDARD) {
      fileHeader.setCrc32(zipParameters.getSourceFileCRC());
    }

    fileHeader.setGeneralPurposeFlag(determineGeneralPurposeBitFlag(fileNameCharset, fileHeader.isEncrypted(),
        zipParameters, fileName));

    return fileHeader;
  }

  public LocalFileHeader generateLocalFileHeaderFromFileHeader(FileHeader fileHeader) {
    LocalFileHeader localFileHeader = new LocalFileHeader();
    localFileHeader.setSignature((int) InternalZipConstants.LOCSIG);
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

  private byte[] determineGeneralPurposeBitFlag(String fileNameCharset, boolean isEncrypted,
                                                ZipParameters zipParameters, String fileName) throws ZipException {
    byte[] generalPurposeBitFlag = new byte[2];
    generalPurposeBitFlag[0] = Raw.bitArrayToByte(generateGeneralPurposeBitArray(isEncrypted, zipParameters.getCompressionMethod()));

    boolean isFileNameCharsetSet = Zip4jUtil.isStringNotNullAndNotEmpty(fileNameCharset);
    if ((isFileNameCharsetSet && fileNameCharset.equalsIgnoreCase(InternalZipConstants.CHARSET_UTF8)) ||
        (!isFileNameCharsetSet && Zip4jUtil.detectCharSet(fileName).equals(InternalZipConstants.CHARSET_UTF8))) {
      generalPurposeBitFlag[1] = 8;
    } else {
      generalPurposeBitFlag[1] = 0;
    }

    return generalPurposeBitFlag;
  }

  private String getFileName(ZipParameters zipParameters, FileHeader fileHeader, File sourceFile) throws ZipException {
    String fileName;
    if (zipParameters.isSourceExternalStream()) {
      fileHeader.setLastModifiedTime((int) Zip4jUtil.javaToDosTime(System.currentTimeMillis()));
      if (!Zip4jUtil.isStringNotNullAndNotEmpty(zipParameters.getFileNameInZip())) {
        throw new ZipException("fileNameInZip is null or empty");
      }
      fileName = zipParameters.getFileNameInZip();
    } else {
      fileHeader.setLastModifiedTime((int) Zip4jUtil.javaToDosTime((Zip4jUtil.getLastModifiedFileTime(
          sourceFile, zipParameters.getTimeZone()))));
      fileHeader.setUncompressedSize(sourceFile.length());
      fileName = Zip4jUtil.getRelativeFileName(
          sourceFile.getAbsolutePath(), zipParameters.getRootFolderInZip(), zipParameters.getDefaultFolderPath());
    }

    return fileName;
  }

  private AESExtraDataRecord generateAESExtraDataRecord(ZipParameters parameters) throws ZipException {
    AESExtraDataRecord aesDataRecord = new AESExtraDataRecord();
    aesDataRecord.setSignature(InternalZipConstants.AESSIG);
    aesDataRecord.setDataSize(7);
    aesDataRecord.setVendorID("AE");
    // Always set the version number to 2 as we do not store CRC for any AES encrypted files
    // only MAC is stored and as per the specification, if version number is 2, then MAC is read
    // and CRC is ignored
    aesDataRecord.setVersionNumber(2);

    if (parameters.getAesKeyStrength() == AesKeyStrength.KEY_STRENGTH_128) {
      aesDataRecord.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_128);
    } else if (parameters.getAesKeyStrength() == AesKeyStrength.KEY_STRENGTH_256) {
      aesDataRecord.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
    } else {
      throw new ZipException("invalid AES key strength, cannot generate AES Extra data record");
    }

    aesDataRecord.setCompressionMethod(parameters.getCompressionMethod());
    return aesDataRecord;
  }

  private int[] generateGeneralPurposeBitArray(boolean isEncrypted, CompressionMethod compressionMethod) {

    int[] generalPurposeBits = new int[8];
    if (isEncrypted) {
      generalPurposeBits[0] = 1;
    } else {
      generalPurposeBits[0] = 0;
    }

    if (compressionMethod == CompressionMethod.DEFLATE) {
      // Have to set flags for deflate
    } else {
      generalPurposeBits[1] = 0;
      generalPurposeBits[2] = 0;
    }

    generalPurposeBits[3] = 1;

    return generalPurposeBits;
  }

  private int getFileAttributes(boolean isSourceExternalStream, File file) {
    if (isSourceExternalStream || !file.exists()) {
      return 0;
    }

    if (file.isDirectory()) {
      if (file.isHidden()) {
        return InternalZipConstants.FOLDER_MODE_HIDDEN;
      } else {
        return InternalZipConstants.FOLDER_MODE_NONE;
      }
    } else {
      if (!file.canWrite() && file.isHidden()) {
        return InternalZipConstants.FILE_MODE_READ_ONLY_HIDDEN;
      } else if (!file.canWrite()) {
        return InternalZipConstants.FILE_MODE_READ_ONLY;
      } else if (file.isHidden()) {
        return InternalZipConstants.FILE_MODE_HIDDEN;
      } else {
        return InternalZipConstants.FILE_MODE_NONE;
      }
    }
  }

  private long calculateCompressedSize(ZipParameters zipParameters, long fileSize) {
    if (zipParameters.getCompressionMethod() == CompressionMethod.STORE) {
      if (zipParameters.getEncryptionMethod() == EncryptionMethod.ZIP_STANDARD) {
        return fileSize + InternalZipConstants.STD_DEC_HDR_SIZE;
      } else if (zipParameters.getEncryptionMethod() == EncryptionMethod.AES) {
        return fileSize + zipParameters.getAesKeyStrength().getSaltLength()
            + InternalZipConstants.AES_AUTH_LENGTH + 2; //2 is password verifier
      }
    }
    return 0;
  }

  private int determineFileNameLength(String fileName, String fileNameCharset) throws ZipException {
    if (Zip4jUtil.isStringNotNullAndNotEmpty(fileNameCharset)) {
      return Zip4jUtil.getEncodedStringLength(fileName, fileNameCharset);
    }
    return Zip4jUtil.getEncodedStringLength(fileName);
  }

  private boolean isDirectory(String fileName, boolean isSourceExternalStream, File sourceFile) {
    if (isSourceExternalStream) {
      return fileName.endsWith("/") || fileName.endsWith("\\");
    } else {
      return sourceFile.isDirectory();
    }
  }
}
