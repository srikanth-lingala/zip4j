package net.lingala.zip4j.io.inputstream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

import org.apache.commons.compress.compressors.deflate64.Deflate64CompressorInputStream;

public class Inflater64InputStream extends DecompressedInputStream {

  private Deflate64CompressorInputStream inflater;
  private BufferedInputStream buffer;

  public Inflater64InputStream(CipherInputStream<?> cipherInputStream, int bufferSize) {
    super(cipherInputStream);
    this.inflater = new Deflate64CompressorInputStream(cipherInputStream);
    this.buffer = new BufferedInputStream(this.inflater, bufferSize);
  }

  @Override
  public int read() throws IOException {
    return this.buffer.read();
  }

  @Override
  public int read(byte[] b) throws IOException {
    return this.buffer.read(b);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    return this.buffer.read(b, off, len);
  }

  @Override
  public void endOfEntryReached(InputStream inputStream, int numberOfBytesPushedBack) throws IOException {
    if (inflater != null) {
      buffer.close();
      inflater.close();  // this is redundant, but leaves no doubt for readability
      inflater = null;
      buffer = null;
    }
    super.endOfEntryReached(inputStream, numberOfBytesPushedBack);
  }

  @Override
  public int pushBackInputStreamIfNecessary(PushbackInputStream pushbackInputStream) throws IOException {
    // Deflate64CompressorInputStream should not overread
    return 0;
  }

  @Override
  public void close() throws IOException {
    if (inflater != null) {
      buffer.close();
      inflater.close();  // this is redundant, but leaves no doubt for readability
    }
    super.close();
  }
}
