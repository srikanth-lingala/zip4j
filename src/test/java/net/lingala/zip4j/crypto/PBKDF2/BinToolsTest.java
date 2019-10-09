package net.lingala.zip4j.crypto.PBKDF2;

import java.lang.reflect.Array;
import net.lingala.zip4j.crypto.PBKDF2.BinTools;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;

public class BinToolsTest {
  @Rule
  public final Timeout globalTimeout = new Timeout(10000);

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void bin2hexInput1OutputNotNull2() {
    final byte[] b = {(byte) 112};
    Assert.assertEquals("70", BinTools.bin2hex(b));
  }

  @Test
  public void bin2hexInput0OutputNotNull() {
    final byte[] b = {};
    Assert.assertEquals("", BinTools.bin2hex(b));
  }

  @Test
  public void bin2hexInputNullOutputNotNull() {
    Assert.assertEquals("", BinTools.bin2hex(null));
  }

  @Test
  public void hex2binInputNullOutput0() {
    final String s = null;
    final byte[] actual = BinTools.hex2bin(s);
    Assert.assertArrayEquals(new byte[]{}, actual);
  }

  @Test
  public void hex2binInputNotNullOutputIllegalArgumentException3() {
    final String s = "foo";
    thrown.expect(IllegalArgumentException.class);
    BinTools.hex2bin(s);
  }

  @Test
  public void hex2binInputAOutputPositive() {
    Assert.assertEquals(10, BinTools.hex2bin('A'));
  }

  @Test
  public void hex2binInputNotNullOutputIllegalArgumentException2() {
    thrown.expect(IllegalArgumentException.class);
    BinTools.hex2bin('\u013c');
  }

  @Test
  public void hex2binInputNotNullOutputIllegalArgumentException() {
    thrown.expect(IllegalArgumentException.class);
    BinTools.hex2bin('\u0018');
  }
}
