package net.lingala.zip4j.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BitUtilsTest {

  @Test
  public void testIsBitSet() {
    byte b = 0;
    b = (byte) (b | 1);
    b = (byte) (b | 1 << 3);
    b = (byte) (b | 1 << 7);

    assertThat(BitUtils.isBitSet(b, 0)).isTrue();
    assertThat(BitUtils.isBitSet(b, 3)).isTrue();
    assertThat(BitUtils.isBitSet(b, 7)).isTrue();

    assertThat(BitUtils.isBitSet(b, 1)).isFalse();
    assertThat(BitUtils.isBitSet(b, 2)).isFalse();
    assertThat(BitUtils.isBitSet(b, 4)).isFalse();
    assertThat(BitUtils.isBitSet(b, 5)).isFalse();
    assertThat(BitUtils.isBitSet(b, 6)).isFalse();
  }

  @Test
  public void testSetBit() {
    byte b = 0;

    b = BitUtils.setBit(b, 2);
    b = BitUtils.setBit(b, 5);
    b = BitUtils.setBit(b, 6);

    assertThat((b & (1L << 2))).isNotZero();
    assertThat((b & (1L << 5))).isNotZero();
    assertThat((b & (1L << 6))).isNotZero();

    assertThat((b & (1L))).isEqualTo(0);
    assertThat((b & (1L << 1))).isEqualTo(0);
    assertThat((b & (1L << 3))).isEqualTo(0);
    assertThat((b & (1L << 4))).isEqualTo(0);
    assertThat((b & (1L << 7))).isEqualTo(0);
  }

  @Test
  public void unsetBit() {
    byte b = 0;
    b = (byte) (b | 1 << 3);
    b = (byte) (b | 1 << 7);

    b = BitUtils.unsetBit(b, 3);

    assertThat(BitUtils.isBitSet(b, 3)).isFalse();
    assertThat(BitUtils.isBitSet(b, 7)).isTrue();
  }

}