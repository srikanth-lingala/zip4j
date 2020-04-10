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

public class EndOfCentralDirectoryRecord extends ZipHeader {

  private int numberOfThisDisk;
  private int numberOfThisDiskStartOfCentralDir;
  private int totalNumberOfEntriesInCentralDirectoryOnThisDisk;
  private int totalNumberOfEntriesInCentralDirectory;
  private int sizeOfCentralDirectory;
  private long offsetOfStartOfCentralDirectory;
  private long offsetOfEndOfCentralDirectory;
  private String comment = "";

  public EndOfCentralDirectoryRecord() {
    setSignature(HeaderSignature.END_OF_CENTRAL_DIRECTORY);
  }

  public int getNumberOfThisDisk() {
    return numberOfThisDisk;
  }

  public void setNumberOfThisDisk(int numberOfThisDisk) {
    this.numberOfThisDisk = numberOfThisDisk;
  }

  public int getNumberOfThisDiskStartOfCentralDir() {
    return numberOfThisDiskStartOfCentralDir;
  }

  public void setNumberOfThisDiskStartOfCentralDir(int numberOfThisDiskStartOfCentralDir) {
    this.numberOfThisDiskStartOfCentralDir = numberOfThisDiskStartOfCentralDir;
  }

  public int getTotalNumberOfEntriesInCentralDirectoryOnThisDisk() {
    return totalNumberOfEntriesInCentralDirectoryOnThisDisk;
  }

  public void setTotalNumberOfEntriesInCentralDirectoryOnThisDisk(
      int totalNumberOfEntriesInCentralDirectoryOnThisDisk) {
    this.totalNumberOfEntriesInCentralDirectoryOnThisDisk = totalNumberOfEntriesInCentralDirectoryOnThisDisk;
  }

  public int getTotalNumberOfEntriesInCentralDirectory() {
    return totalNumberOfEntriesInCentralDirectory;
  }

  public void setTotalNumberOfEntriesInCentralDirectory(int totNoOfEntrisInCentralDir) {
    this.totalNumberOfEntriesInCentralDirectory = totNoOfEntrisInCentralDir;
  }

  public int getSizeOfCentralDirectory() {
    return sizeOfCentralDirectory;
  }

  public void setSizeOfCentralDirectory(int sizeOfCentralDirectory) {
    this.sizeOfCentralDirectory = sizeOfCentralDirectory;
  }

  public long getOffsetOfStartOfCentralDirectory() {
    return offsetOfStartOfCentralDirectory;
  }

  public void setOffsetOfStartOfCentralDirectory(long offSetOfStartOfCentralDir) {
    this.offsetOfStartOfCentralDirectory = offSetOfStartOfCentralDir;
  }

  public long getOffsetOfEndOfCentralDirectory() {
    return offsetOfEndOfCentralDirectory;
  }

  public void setOffsetOfEndOfCentralDirectory(long offsetOfEndOfCentralDirectory) {
    this.offsetOfEndOfCentralDirectory = offsetOfEndOfCentralDirectory;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    if (comment != null) {
      this.comment = comment;
    }
  }

}
