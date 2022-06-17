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

package net.lingala.zip4j.crypto;

import net.lingala.zip4j.crypto.engine.ZipCryptoEngine;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.util.InternalZipConstants;

import static net.lingala.zip4j.util.InternalZipConstants.STD_DEC_HDR_SIZE;

public class StandardDecrypter implements Decrypter {

  private ZipCryptoEngine zipCryptoEngine;

  public StandardDecrypter(char[] password, long crc, long lastModifiedFileTime,
                           byte[] headerBytes, boolean useUtf8ForPassword) throws ZipException {
    this.zipCryptoEngine = new ZipCryptoEngine();
    init(headerBytes, password, lastModifiedFileTime, crc, useUtf8ForPassword);
  }

  public int decryptData(byte[] buff, int start, int len) throws ZipException {
    if (start < 0 || len < 0) {
      throw new ZipException("one of the input parameters were null in standard decrypt data");
    }

    for (int i = start; i < start + len; i++) {
      int val = buff[i] & 0xff;
      val = (val ^ zipCryptoEngine.decryptByte()) & 0xff;
      zipCryptoEngine.updateKeys((byte) val);
      buff[i] = (byte) val;
    }

    return len;
  }

  private void init(byte[] headerBytes, char[] password, long lastModifiedFileTime, long crc,
                    boolean useUtf8ForPassword) throws ZipException {
    if (password == null || password.length <= 0) {
      throw new ZipException("Wrong password!", ZipException.Type.WRONG_PASSWORD);
    }

    zipCryptoEngine.initKeys(password, useUtf8ForPassword);

    int result = headerBytes[0];
    for (int i = 0; i < STD_DEC_HDR_SIZE; i++) {
      if (i + 1 == InternalZipConstants.STD_DEC_HDR_SIZE) {
        byte verificationByte = (byte) (result ^ zipCryptoEngine.decryptByte());
        if (verificationByte != (byte) (crc >> 24) && verificationByte != (byte) (lastModifiedFileTime >> 8)) {
          throw new ZipException("Wrong password!", ZipException.Type.WRONG_PASSWORD);
        }
      }

      zipCryptoEngine.updateKeys((byte) (result ^ zipCryptoEngine.decryptByte()));

      if (i + 1 != STD_DEC_HDR_SIZE) {
        result = headerBytes[i + 1];
      }
    }
  }

}
