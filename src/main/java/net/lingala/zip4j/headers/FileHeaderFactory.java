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

public class FileHeaderFactory {

  public FileHeader generateFileHeader(ZipParameters zipParameters, boolean isSplitZip, int currentDiskNumberStart,
                                       String fileNameCharset) throws ZipException {

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

    String fileName = validateAndGetFileName(zipParameters.getFileNameInZip());
    fileHeader.setFileName(fileName);
    fileHeader.setFileNameLength(determineFileNameLength(fileName, fileNameCharset));
    fileHeader.setDiskNumberStart(isSplitZip ? currentDiskNumberStart : 0);

    if (zipParameters.getLastModifiedFileTime() > 0) {
      fileHeader.setLastModifiedTime((int) Zip4jUtil.javaToDosTime(zipParameters.getLastModifiedFileTime()));
    } else {
      fileHeader.setLastModifiedTime((int) Zip4jUtil.javaToDosTime(System.currentTimeMillis()));
    }

    //TODO add file atttributes for interally added files
    fileHeader.setExternalFileAttr(new byte[] {0, 0, 0, 0});
    fileHeader.setDirectory(Zip4jUtil.isZipEntryDirectory(fileName));

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

  private String validateAndGetFileName(String fileNameInZip) throws ZipException {
    if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileNameInZip)) {
      throw new ZipException("fileNameInZip is null or empty");
    }
    return fileNameInZip;
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

  private int determineFileNameLength(String fileName, String fileNameCharset) throws ZipException {
    if (Zip4jUtil.isStringNotNullAndNotEmpty(fileNameCharset)) {
      return Zip4jUtil.getEncodedStringLength(fileName, fileNameCharset);
    }
    return Zip4jUtil.getEncodedStringLength(fileName);
  }
}
