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

public class Zip64EndOfCentralDirectoryLocator extends ZipHeader {

  private int numberOfDiskStartOfZip64EndOfCentralDirectoryRecord;
  private long offsetZip64EndOfCentralDirectoryRecord;
  private int totalNumberOfDiscs;

  public int getNumberOfDiskStartOfZip64EndOfCentralDirectoryRecord() {
    return numberOfDiskStartOfZip64EndOfCentralDirectoryRecord;
  }

  public void setNumberOfDiskStartOfZip64EndOfCentralDirectoryRecord(
      int noOfDiskStartOfZip64EndOfCentralDirRec) {
    this.numberOfDiskStartOfZip64EndOfCentralDirectoryRecord = noOfDiskStartOfZip64EndOfCentralDirRec;
  }

  public long getOffsetZip64EndOfCentralDirectoryRecord() {
    return offsetZip64EndOfCentralDirectoryRecord;
  }

  public void setOffsetZip64EndOfCentralDirectoryRecord(long offsetZip64EndOfCentralDirectoryRecord) {
    this.offsetZip64EndOfCentralDirectoryRecord = offsetZip64EndOfCentralDirectoryRecord;
  }

  public int getTotalNumberOfDiscs() {
    return totalNumberOfDiscs;
  }

  public void setTotalNumberOfDiscs(int totNumberOfDiscs) {
    this.totalNumberOfDiscs = totNumberOfDiscs;
  }


}
