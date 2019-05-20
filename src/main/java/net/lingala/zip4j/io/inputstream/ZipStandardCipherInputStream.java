package net.lingala.zip4j.io.inputstream;

import net.lingala.zip4j.crypto.Decrypter;
import net.lingala.zip4j.crypto.StandardDecrypter;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.util.InternalZipConstants;

import java.io.IOException;

class ZipStandardCipherInputStream extends CipherInputStream {

  public ZipStandardCipherInputStream(ZipEntryInputStream zipEntryInputStream, LocalFileHeader localFileHeader, char[] password) throws IOException, ZipException {
    super(zipEntryInputStream, localFileHeader, password);
  }

  @Override
  protected Decrypter initializeDecrypter(LocalFileHeader localFileHeader, char[] password) throws ZipException, IOException {
    return new StandardDecrypter(password, localFileHeader.getCrcRawData(), getStandardDecrypterHeaderBytes());
  }

  private byte[] getStandardDecrypterHeaderBytes() throws IOException {
    byte[] headerBytes = new byte[InternalZipConstants.STD_DEC_HDR_SIZE];
    readRaw(headerBytes);
    return headerBytes;
  }
}
