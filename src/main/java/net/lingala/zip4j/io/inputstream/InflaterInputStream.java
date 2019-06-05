package net.lingala.zip4j.io.inputstream;

import java.io.EOFException;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class InflaterInputStream extends DecompressedInputStream {

  private Inflater inflater;
  private byte[] buff;
  private byte[] singleByteBuffer = new byte[1];
  private int len;

  public InflaterInputStream(CipherInputStream cipherInputStream, long compressedSize) {
    super(cipherInputStream, compressedSize);
    this.inflater = new Inflater(true);
    buff = new byte[512];
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
    try {
      int n;
      while ((n = inflater.inflate(b, off, len)) == 0) {
        if (inflater.finished() || inflater.needsDictionary()) {
          return -1;
        }
        if (inflater.needsInput()) {
          fill();
        }
      }
      return n;
    } catch (DataFormatException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void pushBackInputStreamIfNecessary(PushbackInputStream pushbackInputStream) throws IOException {
    int n = inflater.getRemaining();
    if (n > 0) {
      byte[] rawDataCache = getLastReadRawDataCache();
      pushbackInputStream.unread(rawDataCache, len - n, n);
    }
  }

  private void fill() throws IOException {
    len = super.read(buff, 0, buff.length);
    if (len == -1) {
      throw new EOFException("Unexpected end of ZLIB input stream");
    }
    inflater.setInput(buff, 0, len);
  }
}
