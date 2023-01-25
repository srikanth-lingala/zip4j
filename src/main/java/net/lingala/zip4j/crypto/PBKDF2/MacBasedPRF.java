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

package net.lingala.zip4j.crypto.PBKDF2;

import net.lingala.zip4j.util.InternalZipConstants;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static net.lingala.zip4j.util.InternalZipConstants.AES_BLOCK_SIZE;

/*
 * Source referred from Matthias Gartner's PKCS#5 implementation -
 * see http://rtner.de/software/PBKDF2.html
 */

public class MacBasedPRF implements PRF {
  private Mac mac;
  private int hLen;
  private String macAlgorithm;
  private ByteArrayOutputStream macCache;

  public MacBasedPRF(String macAlgorithm) {
    this.macAlgorithm = macAlgorithm;
    this.macCache = new ByteArrayOutputStream(InternalZipConstants.BUFF_SIZE);
    try {
      mac = Mac.getInstance(macAlgorithm);
      hLen = mac.getMacLength();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public byte[] doFinal(byte[] M) {
    if (macCache.size() > 0) {
      doMacUpdate(0);
    }
    return mac.doFinal(M);
  }

  public byte[] doFinal() {
    return doFinal(0);
  }

  public byte[] doFinal(int numberOfBytesToPushbackForMac) {
    if (macCache.size() > 0) {
      doMacUpdate(numberOfBytesToPushbackForMac);
    }
    return mac.doFinal();
  }

  public int getHLen() {
    return hLen;
  }

  public void init(byte[] P) {
    try {
      mac.init(new SecretKeySpec(P, macAlgorithm));
    } catch (InvalidKeyException e) {
      throw new RuntimeException(e);
    }
  }

  public void update(byte[] u) {
    update(u, 0, u.length);
  }

  public void update(byte[] u, int start, int len) {
    try {
      if (macCache.size() + len > InternalZipConstants.BUFF_SIZE) {
        doMacUpdate(0);
      }
      macCache.write(u, start, len);
    } catch (IllegalStateException e) {
      throw new RuntimeException(e);
    }
  }

  private void doMacUpdate(int numberOfBytesToPushBack) {
    byte[] macBytes = macCache.toByteArray();
    int numberOfBytesToRead = macBytes.length - numberOfBytesToPushBack;
    int updateLength;
    for (int i = 0; i < numberOfBytesToRead; i += InternalZipConstants.AES_BLOCK_SIZE) {
      updateLength = (i + AES_BLOCK_SIZE) <= numberOfBytesToRead ? AES_BLOCK_SIZE : numberOfBytesToRead - i;
      mac.update(macBytes, i, updateLength);
    }
    macCache.reset();
  }
}
