package net.lingala.zip4j.io.inputstream;

import net.lingala.zip4j.crypto.Decrypter;
import net.lingala.zip4j.model.LocalFileHeader;

import java.io.IOException;

class NoCipherInputStream extends CipherInputStream {

  public NoCipherInputStream(ZipEntryInputStream zipEntryInputStream, LocalFileHeader localFileHeader,
                             char[] password, int bufferSize) throws IOException {
    super(zipEntryInputStream, localFileHeader, password, bufferSize, true);
  }

  @Override
  protected Decrypter initializeDecrypter(LocalFileHeader localFileHeader, char[] password, boolean useUtf8ForPassword) {
    return new NoDecrypter();
  }

  static class NoDecrypter implements Decrypter {

    @Override
    public int decryptData(byte[] buff, int start, int len) {
      return len;
    }
  }
}
