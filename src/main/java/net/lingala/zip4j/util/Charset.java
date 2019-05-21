package net.lingala.zip4j.util;

public enum Charset {

  UTF8("UTF8"),
  CP850("Cp850");

  private String charsetCode;

  Charset(String code) {
    this.charsetCode = code;
  }

  public String getCharsetCode() {
    return charsetCode;
  }
}
