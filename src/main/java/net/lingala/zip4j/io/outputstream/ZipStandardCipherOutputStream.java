package net.lingala.zip4j.io.outputstream;

import net.lingala.zip4j.crypto.StandardEncrypter;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

import java.io.IOException;
import java.io.OutputStream;

class ZipStandardCipherOutputStream extends CipherOutputStream<StandardEncrypter> {

  public ZipStandardCipherOutputStream(ZipEntryOutputStream outputStream, ZipParameters zipParameters, char[] password) throws IOException, ZipException {
    super(outputStream, zipParameters, password);
  }

  @Override
  protected StandardEncrypter initializeEncrypter(OutputStream outputStream, ZipParameters zipParameters, char[] password) throws IOException, ZipException {
    StandardEncrypter encrypter = new StandardEncrypter(password, (zipParameters.getLastModifiedFileTime() & 0x0000ffff) << 16);
    writeHeaders(encrypter.getHeaderBytes());
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
    super.write(b, off, len);
  }
}
