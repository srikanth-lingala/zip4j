package net.lingala.zip4j.testutils;

import java.io.IOException;
import java.io.InputStream;

public class ControlledReadInputStream extends InputStream {

  private InputStream inputStream;
  private int readLimit;
  private byte[] singleByteBuffer = new byte[1];

  public ControlledReadInputStream(InputStream inputStream, int maximumNumberOfBytesToReadAtOnce) {
    this.inputStream = inputStream;
    this.readLimit = maximumNumberOfBytesToReadAtOnce;
  }

  @Override
  public int read() throws IOException {
    int readLen = read(singleByteBuffer);

    if (readLen == -1) {
      return -1;
    }

    return singleByteBuffer[0];
  }

  @Override
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int toRead = len > readLimit ? readLimit : len;
    return inputStream.read(b, off, toRead);
  }
}
