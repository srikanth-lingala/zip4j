package net.lingala.zip4j.testutils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class RandomInputStream extends InputStream {

  private static final Random RANDOM = new Random();

  private long remaining;
  private boolean streamClosed;

  public RandomInputStream(long length) {
    this.remaining = length;
    this.streamClosed = false;
  }

  @Override
  public int read() throws IOException {
    if (remaining <= 0) {
      return -1;
    }

    assertStreamNotClosed();

    return RANDOM.nextInt(127);
  }

  @Override
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    if (remaining <= 0) {
      return -1;
    }

    assertStreamNotClosed();

    int toRead = len;
    if (remaining <= len) {
      toRead = (int) remaining;
    }

    RANDOM.nextBytes(b);
    remaining -= toRead;
    return toRead;
  }

  private void assertStreamNotClosed() throws IOException {
    if (streamClosed) {
      throw new IOException("Stream closed");
    }
  }

  @Override
  public void close() {
    streamClosed = true;
  }
}
