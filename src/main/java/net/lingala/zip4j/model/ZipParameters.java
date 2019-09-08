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

package net.lingala.zip4j.model;

import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.AesVersion;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

public class ZipParameters {

  private CompressionMethod compressionMethod = CompressionMethod.DEFLATE;
  private CompressionLevel compressionLevel = CompressionLevel.NORMAL;
  private boolean encryptFiles = false;
  private EncryptionMethod encryptionMethod = EncryptionMethod.NONE;
  private boolean readHiddenFiles = true;
  private boolean readHiddenFolders = true;
  private AesKeyStrength aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256;
  private AesVersion aesVersion = AesVersion.TWO;
  private boolean includeRootFolder = true;
  private long entryCRC;
  private String defaultFolderPath;
  private String fileNameInZip;
  private long lastModifiedFileTime = System.currentTimeMillis();
  private long entrySize = -1;
  private boolean writeExtendedLocalFileHeader = true;
  private boolean overrideExistingFilesInZip = true;

  public ZipParameters() {
  }

  public ZipParameters(ZipParameters zipParameters) {
    this.compressionMethod = zipParameters.getCompressionMethod();
    this.compressionLevel = zipParameters.getCompressionLevel();
    this.encryptFiles = zipParameters.isEncryptFiles();
    this.encryptionMethod = zipParameters.getEncryptionMethod();
    this.readHiddenFiles = zipParameters.isReadHiddenFiles();
    this.readHiddenFolders = zipParameters.isReadHiddenFolders();
    this.aesKeyStrength = zipParameters.getAesKeyStrength();
    this.aesVersion = zipParameters.getAesVersion();
    this.includeRootFolder = zipParameters.isIncludeRootFolder();
    this.entryCRC = zipParameters.getEntryCRC();
    this.defaultFolderPath = zipParameters.getDefaultFolderPath();
    this.fileNameInZip = zipParameters.getFileNameInZip();
    this.lastModifiedFileTime = zipParameters.getLastModifiedFileTime();
    this.entrySize = zipParameters.getEntrySize();
    this.writeExtendedLocalFileHeader = zipParameters.isWriteExtendedLocalFileHeader();
    this.overrideExistingFilesInZip = zipParameters.isOverrideExistingFilesInZip();
  }

  public CompressionMethod getCompressionMethod() {
    return compressionMethod;
  }

  public void setCompressionMethod(CompressionMethod compressionMethod) {
    this.compressionMethod = compressionMethod;
  }

  public boolean isEncryptFiles() {
    return encryptFiles;
  }

  public void setEncryptFiles(boolean encryptFiles) {
    this.encryptFiles = encryptFiles;
  }

  public EncryptionMethod getEncryptionMethod() {
    return encryptionMethod;
  }

  public void setEncryptionMethod(EncryptionMethod encryptionMethod) {
    this.encryptionMethod = encryptionMethod;
  }

  public CompressionLevel getCompressionLevel() {
    return compressionLevel;
  }

  public void setCompressionLevel(CompressionLevel compressionLevel) {
    this.compressionLevel = compressionLevel;
  }

  public boolean isReadHiddenFiles() {
    return readHiddenFiles;
  }

  public void setReadHiddenFiles(boolean readHiddenFiles) {
    this.readHiddenFiles = readHiddenFiles;
  }

  public boolean isReadHiddenFolders() {
    return readHiddenFolders;
  }

  public void setReadHiddenFolders(boolean readHiddenFolders) {
    this.readHiddenFolders = readHiddenFolders;
  }

  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  public AesKeyStrength getAesKeyStrength() {
    return aesKeyStrength;
  }

  public void setAesKeyStrength(AesKeyStrength aesKeyStrength) {
    this.aesKeyStrength = aesKeyStrength;
  }

  public AesVersion getAesVersion() {
    return aesVersion;
  }

  public void setAesVersion(AesVersion aesVersion) {
    this.aesVersion = aesVersion;
  }

  public boolean isIncludeRootFolder() {
    return includeRootFolder;
  }

  public void setIncludeRootFolder(boolean includeRootFolder) {
    this.includeRootFolder = includeRootFolder;
  }

  public long getEntryCRC() {
    return entryCRC;
  }

  public void setEntryCRC(long entryCRC) {
    this.entryCRC = entryCRC;
  }

  public String getDefaultFolderPath() {
    return defaultFolderPath;
  }

  public void setDefaultFolderPath(String defaultFolderPath) {
    this.defaultFolderPath = defaultFolderPath;
  }

  public String getFileNameInZip() {
    return fileNameInZip;
  }

  public void setFileNameInZip(String fileNameInZip) {
    this.fileNameInZip = fileNameInZip;
  }

  public long getLastModifiedFileTime() {
    return lastModifiedFileTime;
  }

  public void setLastModifiedFileTime(long lastModifiedFileTime) {
    if (lastModifiedFileTime <= 0) {
      return;
    }

    this.lastModifiedFileTime = lastModifiedFileTime;
  }

  public long getEntrySize() {
    return entrySize;
  }

  public void setEntrySize(long entrySize) {
    this.entrySize = entrySize;
  }

  public boolean isWriteExtendedLocalFileHeader() {
    return writeExtendedLocalFileHeader;
  }

  public void setWriteExtendedLocalFileHeader(boolean writeExtendedLocalFileHeader) {
    this.writeExtendedLocalFileHeader = writeExtendedLocalFileHeader;
  }

  public boolean isOverrideExistingFilesInZip() {
    return overrideExistingFilesInZip;
  }

  public void setOverrideExistingFilesInZip(boolean overrideExistingFilesInZip) {
    this.overrideExistingFilesInZip = overrideExistingFilesInZip;
  }
}
