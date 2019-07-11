package net.lingala.zip4j.model.enums;

public enum AesVersion {

  ONE(1),
  TWO(2);

  private int versionNumber;

  AesVersion(int versionNumber) {
    this.versionNumber = versionNumber;
  }

  public int getVersionNumber() {
    return versionNumber;
  }

  public static AesVersion getFromVersionNumber(int versionNumber) {
    for (AesVersion aesVersion : values()) {
      if (aesVersion.versionNumber == versionNumber) {
        return aesVersion;
      }
    }

    throw new IllegalArgumentException("Unsupported Aes version");
  }
}
