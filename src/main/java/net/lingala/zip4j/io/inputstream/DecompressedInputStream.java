package net.lingala.zip4j.io.inputstream;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

abstract class DecompressedInputStream extends InputStream {

  private CipherInputStream cipherInputStream;
  protected byte[] oneByteBuffer = new byte[1];

  public DecompressedInputStream(CipherInputStream cipherInputStream) {
    this.cipherInputStream = cipherInputStream;
  }

  @Override
  public int read() throws IOException {
    int readLen = read(oneByteBuffer);

    if (readLen == -1) {
      return -1;
    }

    return oneByteBuffer[0];
  }

  @Override
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return cipherInputStream.read(b, off, len);
  }

  @Override
  public void close() throws IOException {
    cipherInputStream.close();
  }

  public void endOfEntryReached(InputStream inputStream) throws IOException {
    cipherInputStream.endOfEntryReached(inputStream);
  }

  public void pushBackInputStreamIfNecessary(PushbackInputStream pushbackInputStream) throws IOException {
    // Do nothing by default
  }

  protected byte[] getLastReadRawDataCache() {
    return cipherInputStream.getLastReadRawDataCache();
  }
}
