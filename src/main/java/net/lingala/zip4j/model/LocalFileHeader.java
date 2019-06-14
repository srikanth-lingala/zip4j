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

public class LocalFileHeader extends AbstractFileHeader {

  private byte[] extraField;
  private long offsetStartOfData;
  private boolean writeCompressedSizeInZip64ExtraRecord;

  public LocalFileHeader() {
    setSignature(HeaderSignature.LOCAL_FILE_HEADER);
  }

  public byte[] getExtraField() {
    return extraField;
  }

  public void setExtraField(byte[] extraField) {
    this.extraField = extraField;
  }

  public long getOffsetStartOfData() {
    return offsetStartOfData;
  }

  public void setOffsetStartOfData(long offsetStartOfData) {
    this.offsetStartOfData = offsetStartOfData;
  }

  public boolean isWriteCompressedSizeInZip64ExtraRecord() {
    return writeCompressedSizeInZip64ExtraRecord;
  }

  public void setWriteCompressedSizeInZip64ExtraRecord(boolean writeCompressedSizeInZip64ExtraRecord) {
    this.writeCompressedSizeInZip64ExtraRecord = writeCompressedSizeInZip64ExtraRecord;
  }
}
