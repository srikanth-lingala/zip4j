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

/**
 * Encapsulates the parameters that that control how Zip4J encodes data
 */
public class ZipParameters {

  /**
   * Indicates the action to take when a symbolic link is added to the ZIP file
   */
  public enum SymbolicLinkAction {
    /**
     * Add only the symbolic link itself, not the target file or its contents
     */
    INCLUDE_LINK_ONLY, 
    /**
     * Add only the target file and its contents, using the filename of the symbolic link
     */
    INCLUDE_LINKED_FILE_ONLY, 
    /**
     * Add the symbolic link itself and the target file with its original filename and its contents
     */
    INCLUDE_LINK_AND_LINKED_FILE
  };

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
  private String rootFolderNameInZip;
  private String fileComment;
  private SymbolicLinkAction symbolicLinkAction = SymbolicLinkAction.INCLUDE_LINKED_FILE_ONLY;
  private ExcludeFileFilter excludeFileFilter;
  private boolean unixMode;

  /**
   * Create a ZipParameters instance with default values;
   * CompressionMethod.DEFLATE, CompressionLevel.NORMAL, EncryptionMethod.NONE,
   * AesKeyStrength.KEY_STRENGTH_256, AesVerson.Two, SymbolicLinkAction.INCLUDE_LINKED_FILE_ONLY,
   * readHiddenFiles is true, readHiddenFolders is true, includeRootInFolder is true,
   * writeExtendedLocalFileHeader is true, overrideExistingFilesInZip is true 
   */
  public ZipParameters() {
  }

  /**
   * Create a clone of given ZipParameters instance
   * @param zipParameters the ZipParameters instance to clone
   */
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
    this.rootFolderNameInZip = zipParameters.getRootFolderNameInZip();
    this.fileComment = zipParameters.getFileComment();
    this.symbolicLinkAction = zipParameters.getSymbolicLinkAction();
    this.excludeFileFilter = zipParameters.getExcludeFileFilter();
    this.unixMode = zipParameters.isUnixMode();
  }

  /**
   * Get the compression method specified in this ZipParameters
   * @return the ZIP compression method
   */
  public CompressionMethod getCompressionMethod() {
    return compressionMethod;
  }

  /** 
   * Set the ZIP compression method
   * @param compressionMethod the ZIP compression method
   */
  public void setCompressionMethod(CompressionMethod compressionMethod) {
    this.compressionMethod = compressionMethod;
  }

  /**
   * Test if files files are to be encrypted
   * @return true if files are to be encrypted
   */
  public boolean isEncryptFiles() {
    return encryptFiles;
  }

  /**
   * Set the flag indicating that files are to be encrypted
   * @param encryptFiles if true, files will be encrypted
   */
public void setEncryptFiles(boolean encryptFiles) {
    this.encryptFiles = encryptFiles;
  }

  /**
   * Get the encryption method used to encrypt files
   * @return the encryption method
   */
  public EncryptionMethod getEncryptionMethod() {
    return encryptionMethod;
  }

  /**
   * Set the encryption method used to encrypt files
   * @param encryptionMethod the encryption method to be used
   */
  public void setEncryptionMethod(EncryptionMethod encryptionMethod) {
    this.encryptionMethod = encryptionMethod;
  }

  /**
   * Get the compression level used to compress files
   * @return the compression level used to compress files
   */
  public CompressionLevel getCompressionLevel() {
    return compressionLevel;
  }

  /**
   * Set the compression level used to compress files
   * @param compressionLevel the compression level used to compress files
   */
  public void setCompressionLevel(CompressionLevel compressionLevel) {
    this.compressionLevel = compressionLevel;
  }

  /**
   * Test if hidden files will be included during folder recursion
   * 
   * @return true if hidden files will be included when adding folders to the zip
   */
  public boolean isReadHiddenFiles() {
    return readHiddenFiles;
  }
  
  /**
   * Indicate if hidden files will be included during folder recursion
   * 
   * @param readHiddenFiles if true, hidden files will be included when adding folders to the zip
   */
  public void setReadHiddenFiles(boolean readHiddenFiles) {
    this.readHiddenFiles = readHiddenFiles;
  }
  
  /**
   * Test if hidden folders will be included during folder recursion
   * 
   * @return true if hidden folders will be included when adding folders to the zip
   */
  public boolean isReadHiddenFolders() {
    return readHiddenFolders;
  }
  
  /**
   * Indicate if hidden folders will be included during folder recursion
   * @param readHiddenFolders if true, hidden folders will be included when added folders to the zip
   */
  public void setReadHiddenFolders(boolean readHiddenFolders) {
    this.readHiddenFolders = readHiddenFolders;
  }

  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  /**
   * Get the key strength of the AES encryption key
   * @return the key strength of the AES encryption key
   */
  public AesKeyStrength getAesKeyStrength() {
    return aesKeyStrength;
  }

  /**
   * Set the key strength of the AES encryption key 
   * @param aesKeyStrength the key strength of the AES encryption key
   */
  public void setAesKeyStrength(AesKeyStrength aesKeyStrength) {
    this.aesKeyStrength = aesKeyStrength;
  }

  /**
   * Get the AES format version used for encryption
   * @return the AES format version used for encryption
   */
  public AesVersion getAesVersion() {
    return aesVersion;
  }

  /**
   * Set the AES format version to use for encryption
   * @param aesVersion the AES format version to use
   */
  public void setAesVersion(AesVersion aesVersion) {
    this.aesVersion = aesVersion;
  }

  /**
   * Test if the parent folder of the added files will be included in the ZIP
   * @return true if the parent folder of the added files will be included into the zip
   */
  public boolean isIncludeRootFolder() {
    return includeRootFolder;
  }

  /**
   * Set the flag to indicate if the parent folder of added files will be included in the ZIP
   * @param includeRootFolder if true, the parent folder of added files will be included in the ZIP
   */
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

  /**
   * Set the filename that will be used to include a file into the ZIP file to a different name
   * that given by the source filename added to the ZIP file.  The filenameInZip must
   * adhere to the ZIP filename specification, including the use of forward slash '/' as the 
   * directory separator, and it must also be a relative file.  If the filenameInZip given is not null and 
   * not empty, the value specified by setRootFolderNameInZip() will be ignored.  
   * 
   * @param fileNameInZip the filename to set in the ZIP. Use null or an empty String to set the default behavior
   */
   public void setFileNameInZip(String fileNameInZip) {
    this.fileNameInZip = fileNameInZip;
  }

  /**
   * Get the last modified time to be used for files written to the ZIP 
   * @return the last modified time in milliseconds since the epoch
   */
  public long getLastModifiedFileTime() {
    return lastModifiedFileTime;
  }

  /**
   * Set the last modified time recorded in the ZIP file for the added files.  If less than 0,
   * the last modified time is cleared and the current time is used
   * @param lastModifiedFileTime the last modified time in milliseconds since the epoch
   */
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

  /**
   * Set the behavior if a file is added that already exists in the ZIP.
   * @param overrideExistingFilesInZip if true, remove the existing file in the ZIP; if false do not add the new file
   */
  public void setOverrideExistingFilesInZip(boolean overrideExistingFilesInZip) {
    this.overrideExistingFilesInZip = overrideExistingFilesInZip;
  }

  public String getRootFolderNameInZip() {
    return rootFolderNameInZip;
  }

  /**
   * Set the folder name that will be prepended to the filename in the ZIP.  This value is ignored
   * if setFileNameInZip() is specified with a non-null, non-empty string.
   * 
   * @param rootFolderNameInZip the name of the folder to be prepended to the filename
   * in the ZIP archive
   */
  public void setRootFolderNameInZip(String rootFolderNameInZip) {
    this.rootFolderNameInZip = rootFolderNameInZip;
  }

  /**
   * Get the file comment
   * @return the file comment
   */
  public String getFileComment() {
    return fileComment;
  }

  /**
   * Set the file comment
   * @param fileComment the file comment
   */
  public void setFileComment(String fileComment) {
    this.fileComment = fileComment;
  }

  /**
   * Get the behavior when adding a symbolic link
   * @return the behavior when adding a symbolic link
   */
  public SymbolicLinkAction getSymbolicLinkAction() {
    return symbolicLinkAction;
  }

  /**
   * Set the behavior when adding a symbolic link
   * @param symbolicLinkAction the behavior when adding a symbolic link
   */
  public void setSymbolicLinkAction(SymbolicLinkAction symbolicLinkAction) {
    this.symbolicLinkAction = symbolicLinkAction;
  }

  /**
   * Returns the file exclusion filter that is currently being used when adding files/folders to zip file
   * @return ExcludeFileFilter
   */
  public ExcludeFileFilter getExcludeFileFilter() {
    return excludeFileFilter;
  }

  /**
   * Set a filter to exclude any files from the list of files being added to zip. Mostly used when adding a folder
   * to a zip, and if certain files have to be excluded from adding to the zip file.
   */
  public void setExcludeFileFilter(ExcludeFileFilter excludeFileFilter) {
    this.excludeFileFilter = excludeFileFilter;
  }

  /**
   * Returns true if zip4j is using unix mode as default. Returns False otherwise.
   * @return true if zip4j is using unix mode as default, false otherwise
   */
  public boolean isUnixMode() {
    return unixMode;
  }

  /**
   * When set to true, zip4j uses unix mode as default when generating file headers.
   * @param unixMode
   */
  public void setUnixMode(boolean unixMode) {
    this.unixMode = unixMode;
  }
}
