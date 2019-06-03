package net.lingala.zip4j.util;

public class BitUtils {

  public static boolean isBitSet(byte b, int pos) {
    return (b & (1L << pos)) != 0;
  }

  public static byte setBitOfByte(byte b, int pos) {
    return (byte) (b | 1 << pos);
  }

  public static byte unsetBitOfByte(byte b, int pos) {
    return (byte) (b & ~(1 << pos));
  }
}
