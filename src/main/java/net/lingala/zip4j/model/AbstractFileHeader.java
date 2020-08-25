package net.lingala.zip4j.model;

import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.util.Zip4jUtil;

import java.util.List;

public abstract class AbstractFileHeader extends ZipHeader {

  private int versionNeededToExtract;
  private byte[] generalPurposeFlag;
  private CompressionMethod compressionMethod;
  private long lastModifiedTime;
  private long crc = 0;
  private byte[] crcRawData;
  private long compressedSize = 0;
  private long uncompressedSize = 0;
  private int fileNameLength;
  private int extraFieldLength;
  private String fileName;
  private boolean isEncrypted;
  private EncryptionMethod encryptionMethod = EncryptionMethod.NONE;
  private boolean dataDescriptorExists;
  private Zip64ExtendedInfo zip64ExtendedInfo;
  private AESExtraDataRecord aesExtraDataRecord;
  private boolean fileNameUTF8Encoded;
  private List<ExtraDataRecord> extraDataRecords;
  private boolean isDirectory;

  public int getVersionNeededToExtract() {
    return versionNeededToExtract;
  }

  public void setVersionNeededToExtract(int versionNeededToExtract) {
    this.versionNeededToExtract = versionNeededToExtract;
  }

  public byte[] getGeneralPurposeFlag() {
    return generalPurposeFlag;
  }

  public void setGeneralPurposeFlag(byte[] generalPurposeFlag) {
    this.generalPurposeFlag = generalPurposeFlag;
  }

  public CompressionMethod getCompressionMethod() {
    return compressionMethod;
  }

  public void setCompressionMethod(CompressionMethod compressionMethod) {
    this.compressionMethod = compressionMethod;
  }

  public long getLastModifiedTime() {
    return lastModifiedTime;
  }

  public void setLastModifiedTime(long lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
  }

  public long getLastModifiedTimeEpoch() {
    return Zip4jUtil.dosToExtendedEpochTme(lastModifiedTime);
  }

  public long getCrc() {
    return crc;
  }

  public void setCrc(long crc) {
    this.crc = crc;
  }

  public byte[] getCrcRawData() {
    return crcRawData;
  }

  public void setCrcRawData(byte[] crcRawData) {
    this.crcRawData = crcRawData;
  }

  public long getCompressedSize() {
    return compressedSize;
  }

  public void setCompressedSize(long compressedSize) {
    this.compressedSize = compressedSize;
  }

  public long getUncompressedSize() {
    return uncompressedSize;
  }

  public void setUncompressedSize(long uncompressedSize) {
    this.uncompressedSize = uncompressedSize;
  }

  public int getFileNameLength() {
    return fileNameLength;
  }

  public void setFileNameLength(int fileNameLength) {
    this.fileNameLength = fileNameLength;
  }

  public int getExtraFieldLength() {
    return extraFieldLength;
  }

  public void setExtraFieldLength(int extraFieldLength) {
    this.extraFieldLength = extraFieldLength;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public boolean isEncrypted() {
    return isEncrypted;
  }

  public void setEncrypted(boolean encrypted) {
    isEncrypted = encrypted;
  }

  public EncryptionMethod getEncryptionMethod() {
    return encryptionMethod;
  }

  public void setEncryptionMethod(EncryptionMethod encryptionMethod) {
    this.encryptionMethod = encryptionMethod;
  }

  public boolean isDataDescriptorExists() {
    return dataDescriptorExists;
  }

  public void setDataDescriptorExists(boolean dataDescriptorExists) {
    this.dataDescriptorExists = dataDescriptorExists;
  }

  public Zip64ExtendedInfo getZip64ExtendedInfo() {
    return zip64ExtendedInfo;
  }

  public void setZip64ExtendedInfo(Zip64ExtendedInfo zip64ExtendedInfo) {
    this.zip64ExtendedInfo = zip64ExtendedInfo;
  }

  public AESExtraDataRecord getAesExtraDataRecord() {
    return aesExtraDataRecord;
  }

  public void setAesExtraDataRecord(AESExtraDataRecord aesExtraDataRecord) {
    this.aesExtraDataRecord = aesExtraDataRecord;
  }

  public boolean isFileNameUTF8Encoded() {
    return fileNameUTF8Encoded;
  }

  public void setFileNameUTF8Encoded(boolean fileNameUTF8Encoded) {
    this.fileNameUTF8Encoded = fileNameUTF8Encoded;
  }

  public List<ExtraDataRecord> getExtraDataRecords() {
    return extraDataRecords;
  }

  public void setExtraDataRecords(List<ExtraDataRecord> extraDataRecords) {
    this.extraDataRecords = extraDataRecords;
  }

  public boolean isDirectory() {
    return isDirectory;
  }

  public void setDirectory(boolean directory) {
    isDirectory = directory;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }

    if (!(obj instanceof AbstractFileHeader)) {
      return false;
    }

    return this.getFileName().equals(((AbstractFileHeader) obj).getFileName());
  }
}
