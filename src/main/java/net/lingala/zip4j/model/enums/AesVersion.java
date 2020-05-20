package net.lingala.zip4j.model.enums;

/**
 * Indicates the AES format used
 */
public enum AesVersion {

  /**
   * Version 1 of the AES format 
   */
  ONE(1),
  /**
   * Version 2 of the AES format 
   */
  TWO(2);

  private int versionNumber;

  AesVersion(int versionNumber) {
    this.versionNumber = versionNumber;
  }

  /**
   * Get the AES version number as an integer
   * @return the AES version number
   */
  public int getVersionNumber() {
    return versionNumber;
  }
  /**
   * Get the AESVersion instance from an integer AES version number
   * @return the AESVersion instance for a given version
   * @throws IllegalArgumentException if an unsupported version is given
   */
  public static AesVersion getFromVersionNumber(int versionNumber) {
    for (AesVersion aesVersion : values()) {
      if (aesVersion.versionNumber == versionNumber) {
        return aesVersion;
      }
    }

    throw new IllegalArgumentException("Unsupported Aes version");
  }
}
