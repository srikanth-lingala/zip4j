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
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.enums.AesKeyStrength;

import java.util.Arrays;

import javax.crypto.Cipher;

import static net.lingala.zip4j.crypto.AesCipherUtil.prepareBuffAESIVBytes;
import static net.lingala.zip4j.exception.ZipException.Type.WRONG_PASSWORD;
import static net.lingala.zip4j.util.InternalZipConstants.AES_BLOCK_SIZE;

/**
 * AES Decrypter supports AE-1 and AE-2 decryption for AES-CTR with 128, 192, or 256 Key Strength
 */
public class AESDecrypter implements Decrypter {

  private Cipher aes;
  private MacBasedPRF mac;

  private int nonce = 1;
  private byte[] iv;
  private byte[] counterBlock;

  public AESDecrypter(AESExtraDataRecord aesExtraDataRecord, char[] password, byte[] salt,
                      byte[] passwordVerifier) throws ZipException {
    iv = new byte[AES_BLOCK_SIZE];
    counterBlock = new byte[AES_BLOCK_SIZE];
    init(salt, passwordVerifier, password, aesExtraDataRecord);
  }

  private void init(byte[] salt, byte[] passwordVerifier, char[] password,
                    AESExtraDataRecord aesExtraDataRecord) throws ZipException {

    if (password == null || password.length <= 0) {
      throw new ZipException("empty or null password provided for AES decryption", WRONG_PASSWORD);
    }

    final AesKeyStrength aesKeyStrength = aesExtraDataRecord.getAesKeyStrength();
    final byte[] derivedKey = AesCipherUtil.derivePasswordBasedKey(salt, password, aesKeyStrength);
    final byte[] derivedPasswordVerifier = AesCipherUtil.derivePasswordVerifier(derivedKey, aesKeyStrength);
    if (!Arrays.equals(passwordVerifier, derivedPasswordVerifier)) {
      throw new ZipException("Wrong Password", ZipException.Type.WRONG_PASSWORD);
    }

    aes = AesCipherUtil.getAESEngine(derivedKey, aesKeyStrength, Cipher.ENCRYPT_MODE);
    mac = AesCipherUtil.getMacBasedPRF(derivedKey, aesKeyStrength);
  }

  @Override
  public int decryptData(byte[] buff, int start, int len) throws ZipException {

    for (int j = start; j < (start + len); j += AES_BLOCK_SIZE) {
      int loopCount = (j + AES_BLOCK_SIZE <= (start + len)) ?
          AES_BLOCK_SIZE : ((start + len) - j);

      mac.update(buff, j, loopCount);
      prepareBuffAESIVBytes(iv, nonce);
      counterBlock = aes.update(iv);

      for (int k = 0; k < loopCount; k++) {
        buff[j + k] = (byte) (buff[j + k] ^ counterBlock[k]);
      }

      nonce++;
    }

    return len;
  }

  public byte[] getCalculatedAuthenticationBytes(int numberOfBytesPushedBack) {
    return mac.doFinal(numberOfBytesPushedBack);
  }
}
