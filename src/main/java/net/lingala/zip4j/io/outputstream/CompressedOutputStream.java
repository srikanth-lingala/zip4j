package net.lingala.zip4j.io.outputstream;

import java.io.IOException;
import java.io.OutputStream;

abstract class CompressedOutputStream extends OutputStream {

  private CipherOutputStream cipherOutputStream;

  public CompressedOutputStream(CipherOutputStream cipherOutputStream) {
    this.cipherOutputStream = cipherOutputStream;
  }

  @Override
  public void write(int b) throws IOException {
    write(new byte[] {(byte) b});
  }

  @Override
  public void write(byte[] b) throws IOException {
    write(b, 0, b.length);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    cipherOutputStream.write(b, off, len);
  }

  protected void closeEntry() throws IOException {
    cipherOutputStream.closeEntry();
  }

  @Override
  public void close() throws IOException {
    cipherOutputStream.close();
  }

  public long getCompressedSize() {
    return cipherOutputStream.getNumberOfBytesWrittenForThisEntry();
  }
}
