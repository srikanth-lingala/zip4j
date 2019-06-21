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
import net.lingala.zip4j.crypto.PBKDF2.PBKDF2Engine;
import net.lingala.zip4j.crypto.PBKDF2.PBKDF2Parameters;
import net.lingala.zip4j.crypto.engine.AESEngine;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.enums.AesKeyStrength;

import java.util.Random;

import static net.lingala.zip4j.crypto.AesCipherUtil.prepareBuffAESIVBytes;
import static net.lingala.zip4j.util.InternalZipConstants.AES_BLOCK_SIZE;

public class AESEncrpyter implements Encrypter {

  private static final int PASSWORD_VERIFIER_LENGTH = 2;

  private char[] password;
  private AesKeyStrength aesKeyStrength;
  private AESEngine aesEngine;
  private MacBasedPRF mac;

  private boolean finished;

  private int nonce = 1;
  private int loopCount = 0;

  private byte[] iv;
  private byte[] counterBlock;
  private byte[] derivedPasswordVerifier;
  private byte[] saltBytes;

  public AESEncrpyter(char[] password, AesKeyStrength aesKeyStrength) throws ZipException {
    if (password == null || password.length == 0) {
      throw new ZipException("input password is empty or null");
    }
    if (aesKeyStrength != AesKeyStrength.KEY_STRENGTH_128 &&
        aesKeyStrength != AesKeyStrength.KEY_STRENGTH_256) {
      throw new ZipException("Invalid AES key strength");
    }

    this.password = password;
    this.aesKeyStrength = aesKeyStrength;
    this.finished = false;
    counterBlock = new byte[AES_BLOCK_SIZE];
    iv = new byte[AES_BLOCK_SIZE];
    init();
  }

  private void init() throws ZipException {
    int keyLength = aesKeyStrength.getKeyLength();
    int macLength = aesKeyStrength.getMacLength();
    int saltLength = aesKeyStrength.getSaltLength();

    saltBytes = generateSalt(saltLength);
    byte[] keyBytes = deriveKey(saltBytes, password, keyLength, macLength);

    if (keyBytes == null || keyBytes.length != (keyLength + macLength + PASSWORD_VERIFIER_LENGTH)) {
      throw new ZipException("invalid key generated, cannot decrypt file");
    }

    byte[] aesKey = new byte[keyLength];
    byte[] macKey = new byte[macLength];
    derivedPasswordVerifier = new byte[PASSWORD_VERIFIER_LENGTH];

    System.arraycopy(keyBytes, 0, aesKey, 0, keyLength);
    System.arraycopy(keyBytes, keyLength, macKey, 0, macLength);
    System.arraycopy(keyBytes, keyLength + macLength, derivedPasswordVerifier, 0, PASSWORD_VERIFIER_LENGTH);

    aesEngine = new AESEngine(aesKey);
    mac = new MacBasedPRF("HmacSHA1");
    mac.init(macKey);
  }

  private byte[] deriveKey(byte[] salt, char[] password, int keyLength, int macLength) throws ZipException {
    try {
      PBKDF2Parameters p = new PBKDF2Parameters("HmacSHA1", "ISO-8859-1",
          salt, 1000);
      PBKDF2Engine e = new PBKDF2Engine(p);
      byte[] derivedKey = e.deriveKey(password, keyLength + macLength + PASSWORD_VERIFIER_LENGTH);
      return derivedKey;
    } catch (Exception e) {
      throw new ZipException(e);
    }
  }

  public int encryptData(byte[] buff) throws ZipException {

    if (buff == null) {
      throw new ZipException("input bytes are null, cannot perform AES encrpytion");
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
      aesEngine.processBlock(iv, counterBlock);

      for (int k = 0; k < loopCount; k++) {
        buff[j + k] = (byte) (buff[j + k] ^ counterBlock[k]);
      }

      mac.update(buff, j, loopCount);
      nonce++;
    }

    return len;
  }

  private static byte[] generateSalt(int size) throws ZipException {

    if (size != 8 && size != 16) {
      throw new ZipException("invalid salt size, cannot generate salt");
    }

    int rounds = 0;

    if (size == 8)
      rounds = 2;
    if (size == 16)
      rounds = 4;

    byte[] salt = new byte[size];
    for (int j = 0; j < rounds; j++) {
      Random rand = new Random();
      int i = rand.nextInt();
      salt[0 + j * 4] = (byte) (i >> 24);
      salt[1 + j * 4] = (byte) (i >> 16);
      salt[2 + j * 4] = (byte) (i >> 8);
      salt[3 + j * 4] = (byte) i;
    }
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
