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
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.AesVersion;
import net.lingala.zip4j.model.enums.CompressionMethod;

public class AESExtraDataRecord extends ZipHeader {

  private int dataSize;
  private AesVersion aesVersion;
  private String vendorID;
  private AesKeyStrength aesKeyStrength;
  private CompressionMethod compressionMethod;

  public AESExtraDataRecord() {
    setSignature(HeaderSignature.AES_EXTRA_DATA_RECORD);
    dataSize = 7;
    aesVersion = AesVersion.TWO;
    vendorID = "AE";
    aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256;
    compressionMethod = CompressionMethod.DEFLATE;
  }

  public int getDataSize() {
    return dataSize;
  }

  public void setDataSize(int dataSize) {
    this.dataSize = dataSize;
  }

  public AesVersion getAesVersion() {
    return aesVersion;
  }

  public void setAesVersion(AesVersion aesVersion) {
    this.aesVersion = aesVersion;
  }

  public String getVendorID() {
    return vendorID;
  }

  public void setVendorID(String vendorID) {
    this.vendorID = vendorID;
  }

  public AesKeyStrength getAesKeyStrength() {
    return aesKeyStrength;
  }

  public void setAesKeyStrength(AesKeyStrength aesKeyStrength) {
    this.aesKeyStrength = aesKeyStrength;
  }

  public CompressionMethod getCompressionMethod() {
    return compressionMethod;
  }

  public void setCompressionMethod(CompressionMethod compressionMethod) {
    this.compressionMethod = compressionMethod;
  }
}
