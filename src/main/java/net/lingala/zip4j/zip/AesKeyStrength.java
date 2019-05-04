package net.lingala.zip4j.zip;

public enum AesKeyStrength {

  KEY_STRENGTH_128(1, 8),
  KEY_STRENGTH_192(2, 12),
  KEY_STRENGTH_256(3, 16);

  private int rawCode;
  private int saltLength;

  AesKeyStrength(int rawCode, int saltLength) {
    this.rawCode = rawCode;
    this.saltLength = saltLength;
  }

  public int getRawCode() {
    return rawCode;
  }

  public int getSaltLength() {
    return saltLength;
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
