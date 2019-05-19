package net.lingala.zip4j.io.inputstreams;

import java.io.IOException;

class StoreInputStream extends DecompressedInputStream {

  private long entrySize;
  private long bytesRead;

  public StoreInputStream(CipherInputStream cipherInputStream, long entrySize) {
    super(cipherInputStream);
    this.entrySize = entrySize;
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
    if (bytesRead >= entrySize) {
      return -1;
    }

    if (len > entrySize - bytesRead) {
      len = (int) (entrySize - bytesRead);
    }

    int readLen = super.read(b, off, len);
    bytesRead += readLen;
    return readLen;
  }
}
