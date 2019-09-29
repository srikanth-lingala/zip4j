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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ZipModel implements Cloneable {

  private List<LocalFileHeader> localFileHeaders = new ArrayList<>();
  private List<DataDescriptor> dataDescriptors = new ArrayList<>();
  private ArchiveExtraDataRecord archiveExtraDataRecord = new ArchiveExtraDataRecord();
  private CentralDirectory centralDirectory = new CentralDirectory();
  private EndOfCentralDirectoryRecord endOfCentralDirectoryRecord = new EndOfCentralDirectoryRecord();
  private Zip64EndOfCentralDirectoryLocator zip64EndOfCentralDirectoryLocator = new Zip64EndOfCentralDirectoryLocator();
  private Zip64EndOfCentralDirectoryRecord zip64EndOfCentralDirectoryRecord = new Zip64EndOfCentralDirectoryRecord();

  private boolean splitArchive;
  private long splitLength;
  private File zipFile;
  private boolean isZip64Format = false;
  private boolean isNestedZipFile;
  private long start;
  private long end;

  public ZipModel() {
    splitLength = -1;
  }

  public List<LocalFileHeader> getLocalFileHeaders() {
    return localFileHeaders;
  }

  public void setLocalFileHeaders(List<LocalFileHeader> localFileHeaderList) {
    this.localFileHeaders = localFileHeaderList;
  }

  public List<DataDescriptor> getDataDescriptors() {
    return dataDescriptors;
  }

  public void setDataDescriptors(List<DataDescriptor> dataDescriptors) {
    this.dataDescriptors = dataDescriptors;
  }

  public CentralDirectory getCentralDirectory() {
    return centralDirectory;
  }

  public void setCentralDirectory(CentralDirectory centralDirectory) {
    this.centralDirectory = centralDirectory;
  }

  public EndOfCentralDirectoryRecord getEndOfCentralDirectoryRecord() {
    return endOfCentralDirectoryRecord;
  }

  public void setEndOfCentralDirectoryRecord(EndOfCentralDirectoryRecord endOfCentralDirectoryRecord) {
    this.endOfCentralDirectoryRecord = endOfCentralDirectoryRecord;
  }

  public ArchiveExtraDataRecord getArchiveExtraDataRecord() {
    return archiveExtraDataRecord;
  }

  public void setArchiveExtraDataRecord(
      ArchiveExtraDataRecord archiveExtraDataRecord) {
    this.archiveExtraDataRecord = archiveExtraDataRecord;
  }

  public boolean isSplitArchive() {
    return splitArchive;
  }

  public void setSplitArchive(boolean splitArchive) {
    this.splitArchive = splitArchive;
  }

  public File getZipFile() {
    return zipFile;
  }

  public void setZipFile(File zipFile) {
    this.zipFile = zipFile;
  }

  public Zip64EndOfCentralDirectoryLocator getZip64EndOfCentralDirectoryLocator() {
    return zip64EndOfCentralDirectoryLocator;
  }

  public void setZip64EndOfCentralDirectoryLocator(
      Zip64EndOfCentralDirectoryLocator zip64EndOfCentralDirectoryLocator) {
    this.zip64EndOfCentralDirectoryLocator = zip64EndOfCentralDirectoryLocator;
  }

  public Zip64EndOfCentralDirectoryRecord getZip64EndOfCentralDirectoryRecord() {
    return zip64EndOfCentralDirectoryRecord;
  }

  public void setZip64EndOfCentralDirectoryRecord(
      Zip64EndOfCentralDirectoryRecord zip64EndOfCentralDirectoryRecord) {
    this.zip64EndOfCentralDirectoryRecord = zip64EndOfCentralDirectoryRecord;
  }

  public boolean isZip64Format() {
    return isZip64Format;
  }

  public void setZip64Format(boolean isZip64Format) {
    this.isZip64Format = isZip64Format;
  }

  public boolean isNestedZipFile() {
    return isNestedZipFile;
  }

  public void setNestedZipFile(boolean isNestedZipFile) {
    this.isNestedZipFile = isNestedZipFile;
  }

  public long getStart() {
    return start;
  }

  public void setStart(long start) {
    this.start = start;
  }

  public long getEnd() {
    return end;
  }

  public void setEnd(long end) {
    this.end = end;
  }

  public long getSplitLength() {
    return splitLength;
  }

  public void setSplitLength(long splitLength) {
    this.splitLength = splitLength;
  }

  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }
}
