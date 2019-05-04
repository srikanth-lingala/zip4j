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
import net.lingala.zip4j.zip.Unzip;

public class FileHeader extends AbstractFileHeader {

  private int versionMadeBy;

  private int fileCommentLength;

  private int diskNumberStart;

  private byte[] internalFileAttr;

  private byte[] externalFileAttr;

  private long offsetLocalHeader;

  private String fileComment;

  private boolean isDirectory;

  /**
   * Extracts file to the specified directory
   *
   * @param zipModel
   * @param outPath
   * @throws ZipException
   */
  public void extractFile(ZipModel zipModel, String outPath,
                          ProgressMonitor progressMonitor, boolean runInThread) throws ZipException {
    extractFile(zipModel, outPath, null, progressMonitor, runInThread);
  }

  /**
   * Extracts file to the specified directory using any
   * user defined parameters in UnzipParameters
   *
   * @param zipModel
   * @param outPath
   * @param unzipParameters
   * @throws ZipException
   */
  public void extractFile(ZipModel zipModel, String outPath,
                          UnzipParameters unzipParameters, ProgressMonitor progressMonitor, boolean runInThread) throws ZipException {
    extractFile(zipModel, outPath, unzipParameters, null, progressMonitor, runInThread);
  }

  /**
   * Extracts file to the specified directory using any
   * user defined parameters in UnzipParameters. Output file name
   * will be overwritten with the value in newFileName. If this
   * parameter is null, then file name will be the same as in
   * FileHeader.getFileName
   *
   * @param zipModel
   * @param outPath
   * @param unzipParameters
   * @throws ZipException
   */
  public void extractFile(ZipModel zipModel, String outPath,
                          UnzipParameters unzipParameters, String newFileName,
                          ProgressMonitor progressMonitor, boolean runInThread) throws ZipException {
    if (zipModel == null) {
      throw new ZipException("input zipModel is null");
    }

    if (!Zip4jUtil.checkOutputFolder(outPath)) {
      throw new ZipException("Invalid output path");
    }

    if (this == null) {
      throw new ZipException("invalid file header");
    }

    Unzip unzip = new Unzip(zipModel);
    unzip.extractFile(this, outPath, unzipParameters, newFileName, progressMonitor, runInThread);
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

  public byte[] getInternalFileAttr() {
    return internalFileAttr;
  }

  public void setInternalFileAttr(byte[] internalFileAttr) {
    this.internalFileAttr = internalFileAttr;
  }

  public byte[] getExternalFileAttr() {
    return externalFileAttr;
  }

  public void setExternalFileAttr(byte[] externalFileAttr) {
    this.externalFileAttr = externalFileAttr;
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
