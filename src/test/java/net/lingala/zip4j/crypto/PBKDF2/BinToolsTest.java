package net.lingala.zip4j.crypto.PBKDF2;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class BinToolsTest {

  @Rule
  public final ExpectedException expectedException = ExpectedException.none();

  @Test
  public void bin2hexForValidInputReturnsValidHex() {
    final byte[] b = {(byte) 112};
    assertThat(BinTools.bin2hex(b)).isEqualTo("70");
  }

  @Test
  public void bin2hexForEmptyInputReturnsEmptyString() {
    assertThat(BinTools.bin2hex(new byte[]{})).isEqualTo("");
  }

  @Test
  public void bin2hexForNullInputReturnsEmptyString() {
    assertThat(BinTools.bin2hex(null)).isEqualTo("");
  }

  @Test
  public void bin2hexForNullInputReturnsEmptyArray() {
    assertThat(BinTools.hex2bin(null)).isEqualTo(new byte[]{});
  }

  @Test
  public void hex2binForInvalidInputOutputIllegalArgumentException() {
    expectedException.expect(IllegalArgumentException.class);
    BinTools.hex2bin("foo");
  }

  @Test
  public void hex2binCharacterInputOutputPositive() {
    assertThat(BinTools.hex2bin('A')).isEqualTo(10);
  }

  @Test
  public void hex2binInvalidInputOutputIllegalArgumentException() {
    expectedException.expect(IllegalArgumentException.class);
    BinTools.hex2bin('\u013c');
  }
}
