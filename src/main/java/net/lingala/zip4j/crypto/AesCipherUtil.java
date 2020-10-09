package net.lingala.zip4j.crypto;

import net.lingala.zip4j.crypto.PBKDF2.MacBasedPRF;
import net.lingala.zip4j.crypto.PBKDF2.PBKDF2Engine;
import net.lingala.zip4j.crypto.PBKDF2.PBKDF2Parameters;
import net.lingala.zip4j.crypto.engine.AESEngine;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.enums.AesKeyStrength;

import static net.lingala.zip4j.util.InternalZipConstants.AES_HASH_CHARSET;
import static net.lingala.zip4j.util.InternalZipConstants.AES_HASH_ITERATIONS;
import static net.lingala.zip4j.util.InternalZipConstants.AES_MAC_ALGORITHM;
import static net.lingala.zip4j.util.InternalZipConstants.AES_PASSWORD_VERIFIER_LENGTH;

public class AesCipherUtil {
  private static final int START_INDEX = 0;

  /**
   * Derive Password-Based Key for AES according to AE-1 and AE-2 Specifications
   *
   * @param salt Salt used for PBKDF2
   * @param password Password used for PBKDF2 containing characters matching ISO-8859-1 character set
   * @param aesKeyStrength Requested AES Key and MAC Strength
   * @return Derived Password-Based Key
   * @throws ZipException Thrown when Derived Key is not valid
   */
  public static byte[] derivePasswordBasedKey(final byte[] salt, final char[] password, final AesKeyStrength aesKeyStrength) throws ZipException {
    final PBKDF2Parameters parameters = new PBKDF2Parameters(AES_MAC_ALGORITHM, AES_HASH_CHARSET, salt, AES_HASH_ITERATIONS);
    final PBKDF2Engine engine = new PBKDF2Engine(parameters);

    final int keyLength = aesKeyStrength.getKeyLength();
    final int macLength = aesKeyStrength.getMacLength();
    final int derivedKeyLength = keyLength + macLength + AES_PASSWORD_VERIFIER_LENGTH;
    final byte[] derivedKey = engine.deriveKey(password, derivedKeyLength);
    if (derivedKey != null && derivedKey.length == derivedKeyLength) {
      return derivedKey;
    } else {
      final String message = String.format("Derived Key invalid for Key Length [%d] MAC Length [%d]", keyLength, macLength);
      throw new ZipException(message);
    }
  }

  /**
   * Derive Password Verifier using Derived Key and requested AES Key Strength
   *
   * @param derivedKey Derived Key
   * @param aesKeyStrength AES Key Strength
   * @return Derived Password Verifier
   */
  public static byte[] derivePasswordVerifier(final byte[] derivedKey, final AesKeyStrength aesKeyStrength) {
    byte[] derivedPasswordVerifier = new byte[AES_PASSWORD_VERIFIER_LENGTH];
    final int keyMacLength = aesKeyStrength.getKeyLength() + aesKeyStrength.getMacLength();
    System.arraycopy(derivedKey, keyMacLength, derivedPasswordVerifier, START_INDEX, AES_PASSWORD_VERIFIER_LENGTH);
    return derivedPasswordVerifier;
  }

  /**
   * Get MAC-Based PRF using default HMAC Algorithm defined in AE-1 and AE-2 Specification
   *
   * @param derivedKey Derived Key
   * @param aesKeyStrength AES Key Strength
   * @return Initialized MAC-Based PRF
   */
  public static MacBasedPRF getMacBasedPRF(final byte[] derivedKey, final AesKeyStrength aesKeyStrength) {
    final int macLength = aesKeyStrength.getMacLength();
    final byte[] macKey = new byte[macLength];
    System.arraycopy(derivedKey, aesKeyStrength.getKeyLength(), macKey, START_INDEX, macLength);
    final MacBasedPRF macBasedPRF = new MacBasedPRF(AES_MAC_ALGORITHM);
    macBasedPRF.init(macKey);
    return macBasedPRF;
  }

  /**
   * Get AES Engine using derived key and requested AES Key Strength
   *
   * @param derivedKey Derived Key
   * @param aesKeyStrength AES Key Strength
   * @return AES Engine configured with AES Key
   * @throws ZipException Thrown on AESEngine initialization failures
   */
  public static AESEngine getAESEngine(final byte[] derivedKey, final AesKeyStrength aesKeyStrength) throws ZipException {
    final int keyLength = aesKeyStrength.getKeyLength();
    final byte[] aesKey = new byte[keyLength];
    System.arraycopy(derivedKey, START_INDEX, aesKey, START_INDEX, keyLength);
    return new AESEngine(aesKey);
  }

  public static void prepareBuffAESIVBytes(byte[] buff, int nonce) {
    buff[0] = (byte) nonce;
    buff[1] = (byte) (nonce >> 8);
    buff[2] = (byte) (nonce >> 16);
    buff[3] = (byte) (nonce >> 24);

    for (int i = 4; i <= 15; i++) {
      buff[i] = 0;
    }
  }

}
