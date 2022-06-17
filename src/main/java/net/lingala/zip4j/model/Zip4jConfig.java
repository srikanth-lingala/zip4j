package net.lingala.zip4j.model;

import java.nio.charset.Charset;

public class Zip4jConfig {

  private final Charset charset;
  private final int bufferSize;
  private final boolean useUtf8CharsetForPasswords;

  public Zip4jConfig(Charset charset, int bufferSize, boolean useUtf8CharsetForPasswords) {
    this.charset = charset;
    this.bufferSize = bufferSize;
    this.useUtf8CharsetForPasswords = useUtf8CharsetForPasswords;
  }

  public Charset getCharset() {
    return charset;
  }

  public int getBufferSize() {
    return bufferSize;
  }

  public boolean isUseUtf8CharsetForPasswords() {
    return useUtf8CharsetForPasswords;
  }
}
