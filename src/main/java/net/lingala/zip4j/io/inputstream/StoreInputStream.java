package net.lingala.zip4j.io.inputstream;

class StoreInputStream extends DecompressedInputStream {

  public StoreInputStream(CipherInputStream cipherInputStream, long compressedSize) {
    super(cipherInputStream, compressedSize);
  }
}
