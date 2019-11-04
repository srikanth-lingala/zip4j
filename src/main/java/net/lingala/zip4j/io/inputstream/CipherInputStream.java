package net.lingala.zip4j.io.inputstream;

import net.lingala.zip4j.crypto.Decrypter;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.enums.CompressionMethod;

import java.io.IOException;
import java.io.InputStream;

import static net.lingala.zip4j.util.Zip4jUtil.readFully;

abstract class CipherInputStream<T extends Decrypter> extends InputStream {

  private ZipEntryInputStream zipEntryInputStream;
  private T decrypter;
  private byte[] lastReadRawDataCache;
  private byte[] singleByteBuffer = new byte[1];
  private LocalFileHeader localFileHeader;

  public CipherInputStream(ZipEntryInputStream zipEntryInputStream, LocalFileHeader localFileHeader, char[] password) throws IOException, ZipException {
    this.zipEntryInputStream = zipEntryInputStream;
    this.decrypter = initializeDecrypter(localFileHeader, password);
    this.localFileHeader = localFileHeader;

    if (getCompressionMethod(localFileHeader) == CompressionMethod.DEFLATE) {
      lastReadRawDataCache = new byte[512];
    }
  }

  @Override
  public int read() throws IOException {
    int readLen = read(singleByteBuffer);

    if (readLen == -1) {
      return -1;
    }

    return singleByteBuffer[0] & 0xff;
  }

  @Override
  public int read(byte[] b) throws IOException {
    return this.read(b, 0, b.length);
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int readLen = readFully(zipEntryInputStream, b, off, len);

    if (readLen > 0) {
      cacheRawData(b, readLen);
      decrypter.decryptData(b, off, readLen);
    }

    return readLen;
  }

  @Override
  public void close() throws IOException {
    zipEntryInputStream.close();
  }

  public byte[] getLastReadRawDataCache() {
    return lastReadRawDataCache;
  }

  protected int readRaw(byte[] b) throws IOException {
    return zipEntryInputStream.readRawFully(b);
  }

  private void cacheRawData(byte[] b, int len) {
    if (lastReadRawDataCache != null) {
      System.arraycopy(b, 0, lastReadRawDataCache, 0, len);
    }
  }

  private CompressionMethod getCompressionMethod(LocalFileHeader localFileHeader) throws ZipException {
    if (localFileHeader.getCompressionMethod() != CompressionMethod.AES_INTERNAL_ONLY) {
      return localFileHeader.getCompressionMethod();
    }

    if (localFileHeader.getAesExtraDataRecord() == null) {
      throw new ZipException("AesExtraDataRecord not present in localheader for aes encrypted data");
    }

    return localFileHeader.getAesExtraDataRecord().getCompressionMethod();
  }

  public T getDecrypter() {
    return decrypter;
  }

  protected void endOfEntryReached(InputStream inputStream) throws IOException {
    // is optional but useful for AES
  }

  protected long getNumberOfBytesReadForThisEntry() {
    return zipEntryInputStream.getNumberOfBytesRead();
  }

  public LocalFileHeader getLocalFileHeader() {
    return localFileHeader;
  }

  protected abstract T initializeDecrypter(LocalFileHeader localFileHeader, char[] password) throws IOException, ZipException;
}
