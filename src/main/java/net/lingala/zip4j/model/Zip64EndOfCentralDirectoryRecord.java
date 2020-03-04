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

public class Zip64EndOfCentralDirectoryRecord extends ZipHeader {

  private long sizeOfZip64EndCentralDirectoryRecord;
  private int versionMadeBy;
  private int versionNeededToExtract;
  private int numberOfThisDisk;
  private int numberOfThisDiskStartOfCentralDirectory;
  private long totalNumberOfEntriesInCentralDirectoryOnThisDisk;
  private long totalNumberOfEntriesInCentralDirectory;
  private long sizeOfCentralDirectory;
  private long offsetStartCentralDirectoryWRTStartDiskNumber = -1;
  private byte[] extensibleDataSector;

  public long getSizeOfZip64EndCentralDirectoryRecord() {
    return sizeOfZip64EndCentralDirectoryRecord;
  }

  public void setSizeOfZip64EndCentralDirectoryRecord(long sizeOfZip64EndCentralDirectoryRecord) {
    this.sizeOfZip64EndCentralDirectoryRecord = sizeOfZip64EndCentralDirectoryRecord;
  }

  public int getVersionMadeBy() {
    return versionMadeBy;
  }

  public void setVersionMadeBy(int versionMadeBy) {
    this.versionMadeBy = versionMadeBy;
  }

  public int getVersionNeededToExtract() {
    return versionNeededToExtract;
  }

  public void setVersionNeededToExtract(int versionNeededToExtract) {
    this.versionNeededToExtract = versionNeededToExtract;
  }

  public int getNumberOfThisDisk() {
    return numberOfThisDisk;
  }

  public void setNumberOfThisDisk(int numberOfThisDisk) {
    this.numberOfThisDisk = numberOfThisDisk;
  }

  public int getNumberOfThisDiskStartOfCentralDirectory() {
    return numberOfThisDiskStartOfCentralDirectory;
  }

  public void setNumberOfThisDiskStartOfCentralDirectory(int numberOfThisDiskStartOfCentralDirectory) {
    this.numberOfThisDiskStartOfCentralDirectory = numberOfThisDiskStartOfCentralDirectory;
  }

  public long getTotalNumberOfEntriesInCentralDirectoryOnThisDisk() {
    return totalNumberOfEntriesInCentralDirectoryOnThisDisk;
  }

  public void setTotalNumberOfEntriesInCentralDirectoryOnThisDisk(
      long totalNumberOfEntriesInCentralDirectoryOnThisDisk) {
    this.totalNumberOfEntriesInCentralDirectoryOnThisDisk = totalNumberOfEntriesInCentralDirectoryOnThisDisk;
  }

  public long getTotalNumberOfEntriesInCentralDirectory() {
    return totalNumberOfEntriesInCentralDirectory;
  }

  public void setTotalNumberOfEntriesInCentralDirectory(long totalNumberOfEntriesInCentralDirectory) {
    this.totalNumberOfEntriesInCentralDirectory = totalNumberOfEntriesInCentralDirectory;
  }

  public long getSizeOfCentralDirectory() {
    return sizeOfCentralDirectory;
  }

  public void setSizeOfCentralDirectory(long sizeOfCentralDirectory) {
    this.sizeOfCentralDirectory = sizeOfCentralDirectory;
  }

  public long getOffsetStartCentralDirectoryWRTStartDiskNumber() {
    return offsetStartCentralDirectoryWRTStartDiskNumber;
  }

  public void setOffsetStartCentralDirectoryWRTStartDiskNumber(
      long offsetStartCentralDirectoryWRTStartDiskNumber) {
    this.offsetStartCentralDirectoryWRTStartDiskNumber = offsetStartCentralDirectoryWRTStartDiskNumber;
  }

  public byte[] getExtensibleDataSector() {
    return extensibleDataSector;
  }

  public void setExtensibleDataSector(byte[] extensibleDataSector) {
    this.extensibleDataSector = extensibleDataSector;
  }


}
