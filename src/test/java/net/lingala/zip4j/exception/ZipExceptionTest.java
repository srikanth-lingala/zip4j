package net.lingala.zip4j.exception;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ZipExceptionTest {

  @Test
  public void testZipExceptionForErrorMessage() {
    String message = "SOME_MESSAGE";
    ZipException zipException = new ZipException(message);
    assertThat(zipException.getMessage()).isEqualTo(message);
    assertThat(zipException.getType()).isEqualTo(ZipException.Type.UNKNOWN);
  }

  @Test
  public void testZipExceptionWithRootException() {
    String errorMessage = "SOME_MESSAGE";
    Exception rootException = new RuntimeException(errorMessage);
    ZipException zipException = new ZipException(rootException);
    assertThat(zipException.getCause()).isEqualTo(rootException);
    assertThat(zipException.getCause().getMessage()).isEqualTo(errorMessage);
    assertThat(zipException.getType()).isEqualTo(ZipException.Type.UNKNOWN);
    assertThat(zipException.getMessage()).contains(errorMessage);
  }

  @Test
  public void testZipExceptionWithMessageAndRootException() {
    String errorMessage = "SOME_MESSAGE";
    String rootErrorMessage = "ROOT_ERROR_MESSAGE";
    Exception rootException = new RuntimeException(rootErrorMessage);
    ZipException zipException = new ZipException(errorMessage, rootException);
    assertThat(zipException.getCause()).isEqualTo(rootException);
    assertThat(zipException.getCause().getMessage()).isEqualTo(rootErrorMessage);
    assertThat(zipException.getType()).isEqualTo(ZipException.Type.UNKNOWN);
    assertThat(zipException.getMessage()).isEqualTo(errorMessage);
  }

  @Test
  public void testZipExceptionWithMessageAndExceptionType() {
    String errorMessage = "SOME_MESSAGE";
    ZipException.Type exceptionType = ZipException.Type.WRONG_PASSWORD;
    ZipException zipException = new ZipException(errorMessage, exceptionType);
    assertThat(zipException.getType()).isEqualTo(exceptionType);
    assertThat(zipException.getMessage()).isEqualTo(errorMessage);
  }

}