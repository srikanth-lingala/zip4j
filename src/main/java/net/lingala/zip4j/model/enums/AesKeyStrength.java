package net.lingala.zip4j.model.enums;

/**
 * Indicates the AES encryption key length 
 *
 */
public enum AesKeyStrength {

  /**
   * 128-bit AES key length 
   */
  KEY_STRENGTH_128(1, 8, 16, 16),
  /**
   * 192-bit AES key length 
   */
  KEY_STRENGTH_192(2, 12, 24, 24),
  /**
   * 256-bit AES key length 
   */
  KEY_STRENGTH_256(3, 16, 32, 32);

  private int rawCode;
  private int saltLength;
  private int macLength;
  private int keyLength;

  AesKeyStrength(int rawCode, int saltLength, int macLength, int keyLength) {
    this.rawCode = rawCode;
    this.saltLength = saltLength;
    this.macLength = macLength;
    this.keyLength = keyLength;
  }

  /**
   * Get the code written to the ZIP file
   * @return the code written the ZIP file
   */
   public int getRawCode() {
    return rawCode;
  }

  public int getSaltLength() {
    return saltLength;
  }

  public int getMacLength() {
    return macLength;
  }
  /**
   * Get the key length in bytes that this AesKeyStrength represents
   * @return the key length in bytes
   */
  public int getKeyLength() {
    return keyLength;
  }

  /**
   * Get a AesKeyStrength given a code from the ZIP file
   * @param code the code from the ZIP file
   * @return the AesKeyStrength that represents the given code, or null if the code does not match
   */
  public static AesKeyStrength getAesKeyStrengthFromRawCode(int code) {
    for (AesKeyStrength aesKeyStrength : values()) {
      if (aesKeyStrength.getRawCode() == code) {
        return aesKeyStrength;
      }
    }

    return null;
  }
}
