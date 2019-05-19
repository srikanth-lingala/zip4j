package net.lingala.zip4j.io.inputstreams;

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
  public int read(byte[] b, int off, int len) throws IOException {
    return cipherInputStream.read(b, off, len);
  }

  public void endOfEntryReached(InputStream inputStream) throws IOException {
    cipherInputStream.endOfEntryReached(inputStream);
  }

  public void pushBackInputStreamIfNecessary(PushbackInputStream pushbackInputStream) throws IOException {
    return;
  }

  protected byte[] getLastReadRawDataCache() {
    return cipherInputStream.getLastReadRawDataCache();
  }
}
