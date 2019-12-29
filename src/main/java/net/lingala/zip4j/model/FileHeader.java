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

import net.lingala.zip4j.headers.HeaderSignature;

public class FileHeader extends AbstractFileHeader {

  private int versionMadeBy;
  private int fileCommentLength = 0;
  private int diskNumberStart;
  private byte[] internalFileAttributes;
  private byte[] externalFileAttributes;
  private long offsetLocalHeader;
  private String fileComment;

  public FileHeader() {
    setSignature(HeaderSignature.CENTRAL_DIRECTORY);
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

  @Override
  public String toString() {
    return getFileName();
  }
}
