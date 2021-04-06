package net.lingala.zip4j.model;

import java.nio.charset.Charset;

public class Zip4jConfig {

  private final Charset charset;
  private final int bufferSize;

  public Zip4jConfig(Charset charset, int bufferSize) {
    this.charset = charset;
    this.bufferSize = bufferSize;
  }

  public Charset getCharset() {
    return charset;
  }

  public int getBufferSize() {
    return bufferSize;
  }
}
