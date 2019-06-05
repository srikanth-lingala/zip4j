package net.lingala.zip4j.io.inputstream;

import net.lingala.zip4j.crypto.StandardDecrypter;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.LocalFileHeader;

import java.io.IOException;

import static net.lingala.zip4j.util.InternalZipConstants.STD_DEC_HDR_SIZE;

class ZipStandardCipherInputStream extends CipherInputStream<StandardDecrypter> {

  public ZipStandardCipherInputStream(ZipEntryInputStream zipEntryInputStream, LocalFileHeader localFileHeader, char[] password) throws IOException, ZipException {
    super(zipEntryInputStream, localFileHeader, password);
  }

  @Override
  protected StandardDecrypter initializeDecrypter(LocalFileHeader localFileHeader, char[] password) throws ZipException, IOException {
    return new StandardDecrypter(password, localFileHeader.getCrcRawData(), getStandardDecrypterHeaderBytes());
  }

  private byte[] getStandardDecrypterHeaderBytes() throws IOException {
    byte[] headerBytes = new byte[STD_DEC_HDR_SIZE];
    readRaw(headerBytes);
    return headerBytes;
  }
}
