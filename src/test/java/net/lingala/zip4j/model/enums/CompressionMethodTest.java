package net.lingala.zip4j.model.enums;

import net.lingala.zip4j.exception.ZipException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CompressionMethodTest {

  @Test
  public void testGetCompressionMethodFromCodeForUnknownTypeReturnsDefault() {
    assertThat(CompressionMethod.getCompressionMethodFromCode(34)).isEqualTo(CompressionMethod.STORE);
  }

  @Test
  public void testGetCompressionMethodFromCodeForDeflateReturnsDeflate() throws ZipException {
    assertThat(CompressionMethod.getCompressionMethodFromCode(8)).isEqualTo(CompressionMethod.DEFLATE);
  }

}