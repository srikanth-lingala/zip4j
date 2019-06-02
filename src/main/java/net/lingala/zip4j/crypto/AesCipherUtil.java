package net.lingala.zip4j.crypto;

public class AesCipherUtil {

  public static void prepareBuffAESIVBytes(byte[] buff, int nonce, int length) {
    buff[0] = (byte) nonce;
    buff[1] = (byte) (nonce >> 8);
    buff[2] = (byte) (nonce >> 16);
    buff[3] = (byte) (nonce >> 24);
    buff[4] = 0;
    buff[5] = 0;
    buff[6] = 0;
    buff[7] = 0;
    buff[8] = 0;
    buff[9] = 0;
    buff[10] = 0;
    buff[11] = 0;
    buff[12] = 0;
    buff[13] = 0;
    buff[14] = 0;
    buff[15] = 0;
  }

}
