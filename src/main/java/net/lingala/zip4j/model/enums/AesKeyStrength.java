package net.lingala.zip4j.model.enums;

public enum AesKeyStrength {

  KEY_STRENGTH_128(1, 8, 16, 16),
  KEY_STRENGTH_192(2, 12, 24, 24),
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

  public int getRawCode() {
    return rawCode;
  }

  public int getSaltLength() {
    return saltLength;
  }

  public int getMacLength() {
    return macLength;
  }

  public int getKeyLength() {
    return keyLength;
  }

  public static AesKeyStrength getAesKeyStrengthFromRawCode(int code) {
    for (AesKeyStrength aesKeyStrength : values()) {
      if (aesKeyStrength.getRawCode() == code) {
        return aesKeyStrength;
      }
    }

    return null;
  }
}
