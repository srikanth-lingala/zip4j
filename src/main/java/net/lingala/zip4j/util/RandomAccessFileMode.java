package net.lingala.zip4j.util;

public enum RandomAccessFileMode {

  READ("r"),
  WRITE("rw");

  private String code;

  RandomAccessFileMode(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}
