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
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.util.Zip4jUtil;

import static net.lingala.zip4j.util.InternalZipConstants.FILE_SEPARATOR;

public class ZipParameters {

  private CompressionMethod compressionMethod = CompressionMethod.DEFLATE;
  private CompressionLevel compressionLevel = CompressionLevel.NORMAL;
  private boolean encryptFiles = false;
  private EncryptionMethod encryptionMethod = EncryptionMethod.NONE;
  private boolean readHiddenFiles = true;
  private AesKeyStrength aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256;
  private boolean includeRootFolder = true;
  private String rootFolderInZip;
  private int sourceFileCRC;
  private String defaultFolderPath;
  private String fileNameInZip;
  private int lastModifiedFileTime;
  private long uncompressedSize;
  private boolean writeExtendedLocalFileHeader = true;

  public ZipParameters() {
  }

  public ZipParameters(ZipParameters zipParameters) {
    this.compressionMethod = zipParameters.getCompressionMethod();
    this.compressionLevel = zipParameters.getCompressionLevel();
    this.encryptFiles = zipParameters.isEncryptFiles();
    this.encryptionMethod = zipParameters.getEncryptionMethod();
    this.readHiddenFiles = zipParameters.isReadHiddenFiles();
    this.aesKeyStrength = zipParameters.getAesKeyStrength();
    this.includeRootFolder = zipParameters.isIncludeRootFolder();
    this.rootFolderInZip = zipParameters.getRootFolderInZip();
    this.sourceFileCRC = zipParameters.getSourceFileCRC();
    this.defaultFolderPath = zipParameters.getDefaultFolderPath();
    this.fileNameInZip = zipParameters.getFileNameInZip();
    this.lastModifiedFileTime = zipParameters.getLastModifiedFileTime();
    this.uncompressedSize = zipParameters.getUncompressedSize();
    this.writeExtendedLocalFileHeader = zipParameters.isWriteExtendedLocalFileHeader();
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

  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  public AesKeyStrength getAesKeyStrength() {
    return aesKeyStrength;
  }

  public void setAesKeyStrength(AesKeyStrength aesKeyStrength) {
    this.aesKeyStrength = aesKeyStrength;
  }

  public boolean isIncludeRootFolder() {
    return includeRootFolder;
  }

  public void setIncludeRootFolder(boolean includeRootFolder) {
    this.includeRootFolder = includeRootFolder;
  }

  public String getRootFolderInZip() {
    return rootFolderInZip;
  }

  public void setRootFolderInZip(String rootFolderInZip) {
    if (Zip4jUtil.isStringNotNullAndNotEmpty(rootFolderInZip)) {

      if (!rootFolderInZip.endsWith("\\") && !rootFolderInZip.endsWith("/")) {
        rootFolderInZip = rootFolderInZip + FILE_SEPARATOR;
      }

      rootFolderInZip = rootFolderInZip.replaceAll("\\\\", "/");
    }
    this.rootFolderInZip = rootFolderInZip;
  }

  public int getSourceFileCRC() {
    return sourceFileCRC;
  }

  public void setSourceFileCRC(int sourceFileCRC) {
    this.sourceFileCRC = sourceFileCRC;
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

  public int getLastModifiedFileTime() {
    return lastModifiedFileTime;
  }

  public void setLastModifiedFileTime(int lastModifiedFileTime) {
    this.lastModifiedFileTime = lastModifiedFileTime;
  }

  public long getUncompressedSize() {
    return uncompressedSize;
  }

  public void setUncompressedSize(long uncompressedSize) {
    this.uncompressedSize = uncompressedSize;
  }

  public boolean isWriteExtendedLocalFileHeader() {
    return writeExtendedLocalFileHeader;
  }

  public void setWriteExtendedLocalFileHeader(boolean writeExtendedLocalFileHeader) {
    this.writeExtendedLocalFileHeader = writeExtendedLocalFileHeader;
  }
}
