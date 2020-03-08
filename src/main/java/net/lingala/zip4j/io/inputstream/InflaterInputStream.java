package net.lingala.zip4j.io.inputstream;

import net.lingala.zip4j.util.InternalZipConstants;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class InflaterInputStream extends DecompressedInputStream {

  private Inflater inflater;
  private byte[] buff;
  private byte[] singleByteBuffer = new byte[1];
  private int len;

  public InflaterInputStream(CipherInputStream cipherInputStream) {
    super(cipherInputStream);
    this.inflater = new Inflater(true);
    buff = new byte[InternalZipConstants.BUFF_SIZE];
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
  public void endOfEntryReached(InputStream inputStream) throws IOException {
    if (inflater != null) {
      inflater.end();
      inflater = null;
    }
    super.endOfEntryReached(inputStream);
  }

  @Override
  public void pushBackInputStreamIfNecessary(PushbackInputStream pushbackInputStream) throws IOException {
    int n = inflater.getRemaining();
    if (n > 0) {
      byte[] rawDataCache = getLastReadRawDataCache();
      pushbackInputStream.unread(rawDataCache, len - n, n);
    }
  }

  @Override
  public void close() throws IOException {
    if (inflater != null) {
      inflater.end();
    }
    super.close();
  }

  private void fill() throws IOException {
    len = super.read(buff, 0, buff.length);
    if (len == -1) {
      throw new EOFException("Unexpected end of input stream");
    }
    inflater.setInput(buff, 0, len);
  }
}
