package net.lingala.zip4j.crypto;

public class AesCipherUtil {

  public static void prepareBuffAESIVBytes(byte[] buff, int nonce) {
    buff[0] = (byte) nonce;
    buff[1] = (byte) (nonce >> 8);
    buff[2] = (byte) (nonce >> 16);
    buff[3] = (byte) (nonce >> 24);

    for (int i = 4; i <= 15; i++) {
      buff[i] = 0;
    }
  }

}
