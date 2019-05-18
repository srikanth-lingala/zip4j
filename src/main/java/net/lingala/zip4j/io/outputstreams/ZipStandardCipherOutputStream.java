package net.lingala.zip4j.io.outputstreams;

import net.lingala.zip4j.crypto.StandardEncrypter;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

import java.io.IOException;
import java.io.OutputStream;

class ZipStandardCipherOutputStream extends CipherOutputStream<StandardEncrypter> {

  public ZipStandardCipherOutputStream(ZipEntryOutputStream outputStream, ZipParameters zipParameters) throws IOException, ZipException {
    super(outputStream, zipParameters);
  }

  @Override
  protected StandardEncrypter initializeEncrypter(OutputStream outputStream, ZipParameters zipParameters) throws IOException, ZipException {
    StandardEncrypter encrypter = new StandardEncrypter(zipParameters.getPassword(), (zipParameters.getLastModifiedFileTime() & 0x0000ffff) << 16);
    outputStream.write(encrypter.getHeaderBytes());
    return encrypter;
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
    encryptAndWrite(b, off, len);
  }
}
