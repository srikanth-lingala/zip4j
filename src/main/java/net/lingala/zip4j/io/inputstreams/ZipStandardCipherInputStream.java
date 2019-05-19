package net.lingala.zip4j.io.inputstreams;

import net.lingala.zip4j.crypto.Decrypter;
import net.lingala.zip4j.crypto.StandardDecrypter;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.util.InternalZipConstants;

import java.io.IOException;

class ZipStandardCipherInputStream extends CipherInputStream {

  public ZipStandardCipherInputStream(ZipEntryInputStream zipEntryInputStream, LocalFileHeader localFileHeader) throws IOException, ZipException {
    super(zipEntryInputStream, localFileHeader);
  }

  @Override
  protected Decrypter initializeDecrypter(LocalFileHeader localFileHeader) throws ZipException, IOException {
    return new StandardDecrypter(localFileHeader, getStandardDecrypterHeaderBytes());
  }

  private byte[] getStandardDecrypterHeaderBytes() throws IOException {
    byte[] headerBytes = new byte[InternalZipConstants.STD_DEC_HDR_SIZE];
    readRaw(headerBytes);
    return headerBytes;
  }
}
