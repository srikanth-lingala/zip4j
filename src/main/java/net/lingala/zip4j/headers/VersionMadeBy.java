package net.lingala.zip4j.headers;

public enum VersionMadeBy {

  SPECIFICATION_VERSION((byte) 51),
  WINDOWS((byte) 0),
  UNIX((byte) 3);

  private byte code;

  VersionMadeBy(byte code) {
    this.code = code;
  }

  public byte getCode() {
    return code;
  }
}
