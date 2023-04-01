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

import net.lingala.zip4j.crypto.PBKDF2.MacBasedPRF;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.enums.AesKeyStrength;

import java.security.SecureRandom;

import javax.crypto.Cipher;

import static net.lingala.zip4j.crypto.AesCipherUtil.derivePasswordBasedKey;
import static net.lingala.zip4j.crypto.AesCipherUtil.derivePasswordVerifier;
import static net.lingala.zip4j.crypto.AesCipherUtil.getAESEngine;
import static net.lingala.zip4j.crypto.AesCipherUtil.getMacBasedPRF;
import static net.lingala.zip4j.crypto.AesCipherUtil.prepareBuffAESIVBytes;
import static net.lingala.zip4j.util.InternalZipConstants.AES_BLOCK_SIZE;

/**
 * AES Encrypter supports AE-1 and AE-2 encryption using AES-CTR with either 128 or 256 Key Strength
 */
public class AESEncrypter implements Encrypter {

  private Cipher aes;
  private MacBasedPRF mac;
  private final SecureRandom random = new SecureRandom();

  private boolean finished;

  private int nonce = 1;
  private int loopCount = 0;

  private final byte[] iv;
  private byte[] derivedPasswordVerifier;
  private byte[] saltBytes;

  public AESEncrypter(char[] password, AesKeyStrength aesKeyStrength) throws ZipException {
    if (password == null || password.length == 0) {
      throw new ZipException("input password is empty or null");
    }
    if (aesKeyStrength != AesKeyStrength.KEY_STRENGTH_128 &&
        aesKeyStrength != AesKeyStrength.KEY_STRENGTH_256) {
      throw new ZipException("Invalid AES key strength");
    }

    this.finished = false;
    iv = new byte[AES_BLOCK_SIZE];
    init(password, aesKeyStrength);
  }

  private void init(char[] password, AesKeyStrength aesKeyStrength) throws ZipException {
    saltBytes = generateSalt(aesKeyStrength.getSaltLength());
    byte[] derivedKey = derivePasswordBasedKey(saltBytes, password, aesKeyStrength);
    derivedPasswordVerifier = derivePasswordVerifier(derivedKey, aesKeyStrength);
    aes = getAESEngine(derivedKey, aesKeyStrength, Cipher.ENCRYPT_MODE);
    mac = getMacBasedPRF(derivedKey, aesKeyStrength);
  }

  public int encryptData(byte[] buff) throws ZipException {
    if (buff == null) {
      throw new ZipException("input bytes are null, cannot perform AES encryption");
    }
    return encryptData(buff, 0, buff.length);
  }

  public int encryptData(byte[] buff, int start, int len) throws ZipException {

    if (finished) {
      // A non 16 byte block has already been passed to encrypter
      // non 16 byte block should be the last block of compressed data in AES encryption
      // any more encryption will lead to corruption of data
      throw new ZipException("AES Encrypter is in finished state (A non 16 byte block has already been passed to encrypter)");
    }

    if (len % 16 != 0) {
      this.finished = true;
    }

    for (int j = start; j < (start + len); j += AES_BLOCK_SIZE) {
      loopCount = (j + AES_BLOCK_SIZE <= (start + len)) ?
          AES_BLOCK_SIZE : ((start + len) - j);

      prepareBuffAESIVBytes(iv, nonce);
      byte[] counterBlock = aes.update(iv);

      for (int k = 0; k < loopCount; k++) {
        buff[j + k] = (byte) (buff[j + k] ^ counterBlock[k]);
      }

      mac.update(buff, j, loopCount);
      nonce++;
    }

    return len;
  }

  private byte[] generateSalt(int size) throws ZipException {
    if (size != 8 && size != 16) {
      throw new ZipException("invalid salt size, cannot generate salt");
    }

    byte[] salt = new byte[size];
    random.nextBytes(salt);
    return salt;
  }

  public byte[] getFinalMac() {
    byte[] rawMacBytes = mac.doFinal();
    byte[] macBytes = new byte[10];
    System.arraycopy(rawMacBytes, 0, macBytes, 0, 10);
    return macBytes;
  }

  public byte[] getDerivedPasswordVerifier() {
    return derivedPasswordVerifier;
  }

  public byte[] getSaltBytes() {
    return saltBytes;
  }
}
