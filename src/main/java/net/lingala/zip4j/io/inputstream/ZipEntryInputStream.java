package net.lingala.zip4j.io.inputstream;

import java.io.IOException;
import java.io.InputStream;

class ZipEntryInputStream extends InputStream {

  private InputStream inputStream;
  private long numberOfBytesRead = 0;
  private byte[] singleByteArray = new byte[1];

  public ZipEntryInputStream(InputStream inputStream) {
    this.inputStream = inputStream;
  }

  @Override
  public int read() throws IOException {
    int readLen = read(singleByteArray);
    if (readLen == -1) {
      return -1;
    }

    return singleByteArray[0];
  }

  @Override
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int readLen = inputStream.read(b, off, len);
    numberOfBytesRead += readLen;
    return readLen;
  }

  @Override
  public void close() throws IOException {
    inputStream.close();
  }

  public long getNumberOfBytesRead() {
    return numberOfBytesRead;
  }
}
