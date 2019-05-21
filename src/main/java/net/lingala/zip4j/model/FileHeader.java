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

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.util.Zip4jUtil;
import net.lingala.zip4j.zip.UnzipEngine;

public class FileHeader extends AbstractFileHeader {

  private int versionMadeBy;
  private int fileCommentLength;
  private int diskNumberStart;
  private byte[] internalFileAttributes;
  private byte[] externalFileAttributes;
  private long offsetLocalHeader;
  private String fileComment;
  private boolean isDirectory;

  /**
   * Extracts file to the specified directory
   *
   * @param zipModel
   * @param outputPath
   * @throws ZipException
   */
  public void extractFile(ZipModel zipModel, String outputPath, ProgressMonitor progressMonitor,
                          boolean runInThread, char[] password) throws ZipException {
    extractFile(zipModel, outputPath, null, progressMonitor, runInThread, password);
  }

  /**
   * Extracts file to the specified directory using any
   * user defined parameters in UnzipParameters
   *
   * @param zipModel
   * @param outputPath
   * @param unzipParameters
   * @throws ZipException
   */
  public void extractFile(ZipModel zipModel, String outputPath, UnzipParameters unzipParameters,
                          ProgressMonitor progressMonitor, boolean runInThread, char[] password) throws ZipException {
    extractFile(zipModel, outputPath, unzipParameters, null, progressMonitor, runInThread, password);
  }

  /**
   * Extracts file to the specified directory using any
   * user defined parameters in UnzipParameters. Output file name
   * will be overwritten with the value in newFileName. If this
   * parameter is null, then file name will be the same as in
   * FileHeader.getFileName
   *
   * @param zipModel
   * @param outputPath
   * @param unzipParameters
   * @throws ZipException
   */
  public void extractFile(ZipModel zipModel, String outputPath, UnzipParameters unzipParameters, String newFileName,
                          ProgressMonitor progressMonitor, boolean runInThread, char[] password) throws ZipException {
    if (zipModel == null) {
      throw new ZipException("input zipModel is null");
    }

    if (!Zip4jUtil.checkOutputFolder(outputPath)) {
      throw new ZipException("Invalid output path");
    }

    if (this == null) {
      throw new ZipException("invalid file header");
    }

    UnzipEngine unzipEngine = new UnzipEngine(zipModel, progressMonitor, password);
    unzipEngine.extractFile(this, outputPath, newFileName, runInThread, unzipParameters);
  }

  public int getVersionMadeBy() {
    return versionMadeBy;
  }

  public void setVersionMadeBy(int versionMadeBy) {
    this.versionMadeBy = versionMadeBy;
  }

  public int getFileCommentLength() {
    return fileCommentLength;
  }

  public void setFileCommentLength(int fileCommentLength) {
    this.fileCommentLength = fileCommentLength;
  }

  public int getDiskNumberStart() {
    return diskNumberStart;
  }

  public void setDiskNumberStart(int diskNumberStart) {
    this.diskNumberStart = diskNumberStart;
  }

  public byte[] getInternalFileAttributes() {
    return internalFileAttributes;
  }

  public void setInternalFileAttributes(byte[] internalFileAttributes) {
    this.internalFileAttributes = internalFileAttributes;
  }

  public byte[] getExternalFileAttributes() {
    return externalFileAttributes;
  }

  public void setExternalFileAttributes(byte[] externalFileAttributes) {
    this.externalFileAttributes = externalFileAttributes;
  }

  public long getOffsetLocalHeader() {
    return offsetLocalHeader;
  }

  public void setOffsetLocalHeader(long offsetLocalHeader) {
    this.offsetLocalHeader = offsetLocalHeader;
  }

  public String getFileComment() {
    return fileComment;
  }

  public void setFileComment(String fileComment) {
    this.fileComment = fileComment;
  }

  public boolean isDirectory() {
    return isDirectory;
  }

  public void setDirectory(boolean directory) {
    isDirectory = directory;
  }
}
