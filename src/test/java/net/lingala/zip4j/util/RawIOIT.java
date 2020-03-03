package net.lingala.zip4j.util;

import net.lingala.zip4j.AbstractIT;
import net.lingala.zip4j.model.enums.RandomAccessFileMode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import static org.assertj.core.api.Assertions.assertThat;

public class RawIOIT extends AbstractIT {

  private File fileToTest;
  private RawIO rawIO = new RawIO();

  @Before
  public void before() throws IOException {
    fileToTest = temporaryFolder.newFile();
    writeDummyData(fileToTest);
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void testReadLongLittleEndianWithRandomAccessFile() throws IOException {
    try(RandomAccessFile randomAccessFile = new RandomAccessFile(fileToTest, RandomAccessFileMode.READ.getValue())) {
      assertThat(rawIO.readLongLittleEndian(randomAccessFile)).isEqualTo(InternalZipConstants.ZIP_64_SIZE_LIMIT + 1000);
      assertThat(rawIO.readLongLittleEndian(randomAccessFile)).isEqualTo(InternalZipConstants.ZIP_64_SIZE_LIMIT + 23423);
      assertThat(rawIO.readLongLittleEndian(randomAccessFile, 4)).isEqualTo(32332);
    }
  }

  @Test
  public void testReadLongLittleEndianWithInputStream() throws IOException {
    try(InputStream inputStream = new FileInputStream(fileToTest)) {
      assertThat(rawIO.readLongLittleEndian(inputStream)).isEqualTo(InternalZipConstants.ZIP_64_SIZE_LIMIT + 1000);
      assertThat(rawIO.readLongLittleEndian(inputStream)).isEqualTo(InternalZipConstants.ZIP_64_SIZE_LIMIT + 23423);
      assertThat(rawIO.readLongLittleEndian(inputStream, 4)).isEqualTo(32332);
    }
  }

  @Test
  public void testReadLongLittleEndianWithInputStreamNotEnoughDataThrowsException() throws IOException {
    expectedException.expect(IOException.class);
    expectedException.expectMessage("Could not fill buffer");

    try(InputStream inputStream = new FileInputStream(fileToTest)) {
      inputStream.skip(26);
      rawIO.readLongLittleEndian(inputStream);
    }
  }

  @Test
  public void testReadLongLittleEndianWithByteArray() {
    byte[] b = new byte[4];
    rawIO.writeIntLittleEndian(b, 0, 234233);

    assertThat(rawIO.readLongLittleEndian(b, 0)).isEqualTo(234233);
  }

  @Test
  public void testReadLongLitteEndianWithByteArrayAndOffset() {
    byte[] b = new byte[9];
    rawIO.writeLongLittleEndian(b, 1, 3463463735346821298L);

    assertThat(rawIO.readLongLittleEndian(b, 1)).isEqualTo(3463463735346821298L);
  }

  @Test
  public void testReadLongLittleEndianWithSmallByteArrayAndOffset() {
    byte[] b = new byte[5];
    rawIO.writeIntLittleEndian(b, 1, 234233);

    assertThat(rawIO.readLongLittleEndian(b, 1)).isEqualTo(234233);
  }

  @Test
  public void testReadIntLittleEndianWithRandomAccessFile() throws IOException {
    try(RandomAccessFile randomAccessFile = new RandomAccessFile(fileToTest, RandomAccessFileMode.READ.getValue())) {
      randomAccessFile.seek(16);
      assertThat(rawIO.readIntLittleEndian(randomAccessFile)).isEqualTo(32332);
      assertThat(rawIO.readIntLittleEndian(randomAccessFile)).isEqualTo(231);
    }
  }

  @Test
  public void testReadIntLittleEndianWithInputStream() throws IOException {
    try(InputStream inputStream = new FileInputStream(fileToTest)) {
      inputStream.skip(16);
      assertThat(rawIO.readIntLittleEndian(inputStream)).isEqualTo(32332);
      assertThat(rawIO.readIntLittleEndian(inputStream)).isEqualTo(231);
    }
  }

  @Test
  public void testReadIntLittleEndianWithByteArray() {
    byte[] b = new byte[8];
    rawIO.writeLongLittleEndian(b, 0, 23423L);

    assertThat(rawIO.readIntLittleEndian(b)).isEqualTo(23423);
    assertThat(rawIO.readIntLittleEndian(b, 1)).isEqualTo(91);
  }

  @Test
  public void testReadShortLittleEndianWithRandomAccessFile() throws IOException {
    try(RandomAccessFile randomAccessFile = new RandomAccessFile(fileToTest, RandomAccessFileMode.READ.getValue())) {
      randomAccessFile.seek(24);
      assertThat(rawIO.readShortLittleEndian(randomAccessFile)).isEqualTo(23);
      assertThat(rawIO.readShortLittleEndian(randomAccessFile)).isEqualTo(77);
    }
  }

  @Test
  public void testReadShortLittleEndianWithInputStream() throws IOException {
    try(InputStream inputStream = new FileInputStream(fileToTest)) {
      inputStream.skip(24);
      assertThat(rawIO.readShortLittleEndian(inputStream)).isEqualTo(23);
      assertThat(rawIO.readShortLittleEndian(inputStream)).isEqualTo(77);
    }
  }

  @Test
  public void testReadShortLittleEndianWithByteArray() {
    byte[] b = new byte[8];
    rawIO.writeShortLittleEndian(b, 0, 88);
    rawIO.writeShortLittleEndian(b, 2, 67);

    assertThat(rawIO.readShortLittleEndian(b, 0)).isEqualTo(88);
    assertThat(rawIO.readShortLittleEndian(b, 2)).isEqualTo(67);
  }

  @Test
  public void testWriteShortLittleEndianWithOutputStream() throws IOException {
    File shortFile = temporaryFolder.newFile();
    try(OutputStream outputStream = new FileOutputStream(shortFile)) {
      rawIO.writeShortLittleEndian(outputStream, 444);
    }

    try(InputStream inputStream = new FileInputStream(shortFile)) {
      assertThat(rawIO.readShortLittleEndian(inputStream)).isEqualTo(444);
    }
  }

  @Test
  public void testWriteShortLittleEndianWithByteBuffer() {
    byte[] b = new byte[10];

    rawIO.writeShortLittleEndian(b, 0, 12);
    rawIO.writeShortLittleEndian(b, 6, 67);

    assertThat(rawIO.readShortLittleEndian(b, 0)).isEqualTo(12);
    assertThat(rawIO.readShortLittleEndian(b, 6)).isEqualTo(67);
  }

  @Test
  public void testWriteIntLittleEndianWithOutputStream() throws IOException {
    File shortFile = temporaryFolder.newFile();
    try(OutputStream outputStream = new FileOutputStream(shortFile)) {
      rawIO.writeIntLittleEndian(outputStream, 4562);
    }

    try(InputStream inputStream = new FileInputStream(shortFile)) {
      assertThat(rawIO.readIntLittleEndian(inputStream)).isEqualTo(4562);
    }
  }

  @Test
  public void testWriteIntLittleEndianWithByteBuffer() {
    byte[] b = new byte[12];

    rawIO.writeIntLittleEndian(b, 0, 23423);
    rawIO.writeIntLittleEndian(b, 7, 6765);

    assertThat(rawIO.readIntLittleEndian(b, 0)).isEqualTo(23423);
    assertThat(rawIO.readIntLittleEndian(b, 7)).isEqualTo(6765);
  }

  @Test
  public void testWriteLongLittleEndianWithOutputStream() throws IOException {
    File shortFile = temporaryFolder.newFile();
    try(OutputStream outputStream = new FileOutputStream(shortFile)) {
      rawIO.writeLongLittleEndian(outputStream, 2342342L);
    }

    try(InputStream inputStream = new FileInputStream(shortFile)) {
      assertThat(rawIO.readLongLittleEndian(inputStream)).isEqualTo(2342342L);
    }
  }

  @Test
  public void testWriteLongLittleEndianWithByteBuffer() {
    byte[] b = new byte[50];

    rawIO.writeLongLittleEndian(b, 0, 54545454233L);
    rawIO.writeLongLittleEndian(b, 18, 9988898778L);

    assertThat(rawIO.readLongLittleEndian(b, 0)).isEqualTo(54545454233L);
    assertThat(rawIO.readLongLittleEndian(b, 18)).isEqualTo(9988898778L);
  }

  private void writeDummyData(File outputFile) throws IOException {
    try (OutputStream outputStream = new FileOutputStream(outputFile)) {
      byte[] longByte = new byte[8];
      byte[] intByte = new byte[4];
      byte[] shortByte = new byte[2];

      rawIO.writeLongLittleEndian(longByte, 0, InternalZipConstants.ZIP_64_SIZE_LIMIT + 1000);
      outputStream.write(longByte);

      rawIO.writeLongLittleEndian(longByte, 0, InternalZipConstants.ZIP_64_SIZE_LIMIT + 23423);
      outputStream.write(longByte);

      rawIO.writeIntLittleEndian(intByte, 0, 32332);
      outputStream.write(intByte);

      rawIO.writeIntLittleEndian(intByte, 0, 231);
      outputStream.write(intByte);

      rawIO.writeShortLittleEndian(shortByte, 0, 23);
      outputStream.write(shortByte);

      rawIO.writeShortLittleEndian(shortByte, 0, 77);
      outputStream.write(shortByte);
    }
  }

}