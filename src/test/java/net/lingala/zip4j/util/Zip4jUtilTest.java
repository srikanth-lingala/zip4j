package net.lingala.zip4j.util;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.enums.CompressionMethod;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Zip4jUtilTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testIsStringNotNullAndNotEmptyReturnsFalseWhenNull() {
    assertThat(Zip4jUtil.isStringNotNullAndNotEmpty(null)).isFalse();
  }

  @Test
  public void testIsStringNotNullAndNotEmptyReturnsFalseWhenEmpty() {
    assertThat(Zip4jUtil.isStringNotNullAndNotEmpty("")).isFalse();
  }

  @Test
  public void testIsStringNotNullAndNotEmptyReturnsFalseWithWhitespaces() {
    assertThat(Zip4jUtil.isStringNotNullAndNotEmpty("   ")).isFalse();
  }

  @Test
  public void testIsStringNotNullAndNotEmptyReturnsTrueForValidString() {
    assertThat(Zip4jUtil.isStringNotNullAndNotEmpty("  Some string   ")).isTrue();
  }

  @Test
  public void testCreateDirectoryIfNotExistsThrowsExceptionWhenPathIsNull() throws ZipException {
    expectedException.expectMessage("output path is null");
    expectedException.expect(ZipException.class);

    Zip4jUtil.createDirectoryIfNotExists(null);
  }

  @Test
  public void testCreateDirectoryIfNotExistsThrowsExceptionWhenFileExistsButNotDirectory() throws ZipException {
    File file = mock(File.class);
    when(file.exists()).thenReturn(true);
    when(file.isDirectory()).thenReturn(false);

    expectedException.expect(ZipException.class);
    expectedException.expectMessage("output directory is not valid");

    Zip4jUtil.createDirectoryIfNotExists(file);
  }

  @Test
  public void testCreateDirectoryIfNotExistsReturnsTrueWhenFileExistsAndIsDirectory() throws ZipException {
    File file = mock(File.class);
    when(file.exists()).thenReturn(true);
    when(file.isDirectory()).thenReturn(true);

    assertThat(Zip4jUtil.createDirectoryIfNotExists(file)).isTrue();
  }

  @Test
  public void testCreateDirectoryIfNotExistsThrowsExceptionWhenFileDoesNotExistAndCannotCreate() throws ZipException {
    File file = mock(File.class);
    when(file.exists()).thenReturn(false);
    when(file.mkdirs()).thenReturn(false);

    expectedException.expect(ZipException.class);
    expectedException.expectMessage("Cannot create output directories");

    Zip4jUtil.createDirectoryIfNotExists(file);
  }

  @Test
  public void testCreateDirectoryIfNotExistsReturnsTrueWhenFileDoesNotExistAndCreated() throws ZipException {
    File file = mock(File.class);
    when(file.exists()).thenReturn(false);
    when(file.mkdirs()).thenReturn(true);

    assertThat(Zip4jUtil.createDirectoryIfNotExists(file)).isTrue();
  }

  @Test
  public void testJavaToDosTime() {
    assertThat(Zip4jUtil.javaToDosTime(1560526564503L)).isEqualTo(1322159234);
  }

  @Test
  public void testDosToJavaTime() {
    assertThat(Zip4jUtil.dosToJavaTme(1322159234)).isEqualTo((1560526564503L / 1000) * 1000);
  }

  @Test
  public void testConvertCharArrayToByteArray() {
    char[] charArray = "CharArray".toCharArray();

    byte[] byteArray = Zip4jUtil.convertCharArrayToByteArray(charArray);

    assertThat(byteArray.length).isEqualTo(charArray.length);
    assertThat(byteArray[0]).isEqualTo((byte)'C');
    assertThat(byteArray[1]).isEqualTo((byte)'h');
    assertThat(byteArray[2]).isEqualTo((byte)'a');
    assertThat(byteArray[3]).isEqualTo((byte)'r');
    assertThat(byteArray[4]).isEqualTo((byte)'A');
    assertThat(byteArray[5]).isEqualTo((byte)'r');
    assertThat(byteArray[6]).isEqualTo((byte)'r');
    assertThat(byteArray[7]).isEqualTo((byte)'a');
    assertThat(byteArray[8]).isEqualTo((byte)'y');
  }

  @Test
  public void testGetCompressionMethodForNonAesReturnsAsIs() {
    LocalFileHeader localFileHeader = new LocalFileHeader();
    localFileHeader.setCompressionMethod(CompressionMethod.DEFLATE);

    assertThat(Zip4jUtil.getCompressionMethod(localFileHeader)).isEqualTo(CompressionMethod.DEFLATE);
  }

  @Test
  public void testGetCompressionMethodForAesWhenAesExtraDataMissingThrowsException() {
    expectedException.expectMessage("AesExtraDataRecord not present in local header for aes encrypted data");
    expectedException.expect(RuntimeException.class);

    LocalFileHeader localFileHeader = new LocalFileHeader();
    localFileHeader.setCompressionMethod(CompressionMethod.AES_INTERNAL_ONLY);

    Zip4jUtil.getCompressionMethod(localFileHeader);
  }

  @Test
  public void testGetCompressionMethidForAesReturnsFromAesExtraDataRecord() {
    AESExtraDataRecord aesExtraDataRecord = new AESExtraDataRecord();
    aesExtraDataRecord.setCompressionMethod(CompressionMethod.STORE);

    LocalFileHeader localFileHeader = new LocalFileHeader();
    localFileHeader.setCompressionMethod(CompressionMethod.AES_INTERNAL_ONLY);
    localFileHeader.setAesExtraDataRecord(aesExtraDataRecord);

    assertThat(Zip4jUtil.getCompressionMethod(localFileHeader)).isEqualTo(CompressionMethod.STORE);
  }
}