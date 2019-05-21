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

public class Zip64ExtendedInfo extends ZipHeader {

  private int size;
  private long compressedSize;
  private long uncompressedSize;
  private long offsetLocalHeader;
  private int diskNumberStart;

  public Zip64ExtendedInfo() {
    compressedSize = -1;
    uncompressedSize = -1;
    offsetLocalHeader = -1;
    diskNumberStart = -1;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public long getCompressedSize() {
    return compressedSize;
  }

  public void setCompressedSize(long compressedSize) {
    this.compressedSize = compressedSize;
  }

  public long getUncompressedSize() {
    return uncompressedSize;
  }

  public void setUncompressedSize(long uncompressedSize) {
    this.uncompressedSize = uncompressedSize;
  }

  public long getOffsetLocalHeader() {
    return offsetLocalHeader;
  }

  public void setOffsetLocalHeader(long offsetLocalHeader) {
    this.offsetLocalHeader = offsetLocalHeader;
  }

  public int getDiskNumberStart() {
    return diskNumberStart;
  }

  public void setDiskNumberStart(int diskNumberStart) {
    this.diskNumberStart = diskNumberStart;
  }


}
