package net.lingala.zip4j.io.inputstream;

import java.io.IOException;
import java.io.InputStream;

class ZipEntryInputStream extends InputStream {

  private static final int MAX_RAW_READ_FULLY_RETRY_ATTEMPTS = 15;

  private InputStream inputStream;
  private long numberOfBytesRead = 0;
  private byte[] singleByteArray = new byte[1];
  private long compressedSize;

  public ZipEntryInputStream(InputStream inputStream, long compressedSize) {
    this.inputStream = inputStream;
    this.compressedSize = compressedSize;
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

    if (compressedSize != -1) {
      if (numberOfBytesRead >= compressedSize) {
        return -1;
      }

      if (len > compressedSize - numberOfBytesRead) {
        len = (int) (compressedSize - numberOfBytesRead);
      }
    }

    int readLen = inputStream.read(b, off, len);

    if (readLen > 0) {
      numberOfBytesRead += readLen;
    }

    return readLen;
  }

  public int readRawFully(byte[] b) throws  IOException {

    int readLen = inputStream.read(b);

    if (readLen != b.length) {
      readLen = readUntilBufferIsFull(b, readLen);

      if (readLen != b.length) {
        throw new IOException("Cannot read fully into byte buffer");
      }
    }

    return readLen;
  }

  private int readUntilBufferIsFull(byte[] b, int readLength) throws IOException {
    int remainingLength = b.length - readLength;
    int loopReadLength = 0;
    int retryAttempt = 0;

    while (readLength < b.length && loopReadLength != -1 && retryAttempt < MAX_RAW_READ_FULLY_RETRY_ATTEMPTS) {
      loopReadLength += inputStream.read(b, readLength, remainingLength);

      if (loopReadLength > 0) {
        readLength += loopReadLength;
        remainingLength -= loopReadLength;
      }

      retryAttempt++;
    }

    return readLength;
  }

  @Override
  public void close() throws IOException {
    inputStream.close();
  }

  public long getNumberOfBytesRead() {
    return numberOfBytesRead;
  }
}
