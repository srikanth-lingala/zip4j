package net.lingala.zip4j.io.inputstreams;

import net.lingala.zip4j.crypto.Decrypter;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.LocalFileHeader;

import java.io.IOException;

class NoCipherInputStream extends CipherInputStream {

  public NoCipherInputStream(ZipEntryInputStream zipEntryInputStream, LocalFileHeader localFileHeader) throws IOException, ZipException {
    super(zipEntryInputStream, localFileHeader);
  }

  @Override
  protected Decrypter initializeDecrypter(LocalFileHeader localFileHeader) {
    return new NoDecrypter();
  }

  static class NoDecrypter implements Decrypter {

    @Override
    public int decryptData(byte[] buff) {
      return buff.length;
    }

    @Override
    public int decryptData(byte[] buff, int start, int len) {
      return len;
    }
  }
}
