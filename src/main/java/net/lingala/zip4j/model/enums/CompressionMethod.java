package net.lingala.zip4j.model.enums;

public enum CompressionMethod {

  STORE(0),
  DEFLATE(8),
  AES_INTERNAL_ONLY(99);

  private int code;

  CompressionMethod(int code) {
    this.code = code;
  }

  public int getCode() {
    return code;
  }

  public static CompressionMethod getCompressionMethodFromCode(int code) {
    for (CompressionMethod compressionMethod : values()) {
      if (compressionMethod.getCode() == code) {
        return compressionMethod;
      }
    }

    return null;
  }
}
